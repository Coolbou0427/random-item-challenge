package jaxon.ric.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;

/** Routes the whole ghast/boat assembly to a place from which its riders can actually shoot. */
public class HappyGhastHuntGoal extends Goal {
    private static final double MAX_SHOT_HEIGHT = 4.0;
    private static final int SEARCH_NODES_PER_TICK = 96;
    private static final int SEARCH_NODE_LIMIT = 6000;
    private static final double WAYPOINT_REACHED_SQ = 0.65 * 0.65;

    private final HappyGhast ghast;
    private ServerPlayer target;
    private FlightPathSearch search;
    private Vec3 plannedTarget;
    private List<FlightPathSearch.Point> route = List.of();
    private List<FlightPathSearch.Point> plannedApproaches = List.of();
    private int routeIndex;
    private int retryTicks;
    private double searchStep = 2.0;
    private Vec3 progressWaypoint;
    private double closestWaypointDistance;
    private int noProgressTicks;
    private Vec3 escapeWaypoint;
    private int escapeRetryTicks;
    private Vec3 roamingGoal;
    private FlightPathSearch roamingSearch;
    private final List<BlockedPassage> blockedPassages = new ArrayList<>();
    private final List<RejectedFiringPosition> rejectedFiringPositions = new ArrayList<>();
    private int disengagedTicks;
    private final Map<Mob, AttackProgress> attackProgress = new IdentityHashMap<>();

    private record BlockedPassage(Vec3 from, Vec3 direction, int expiresAt) {}
    private record RejectedFiringPosition(Vec3 position, Vec3 targetPosition, int expiresAt) {}
    private record RiderPosition(Mob mob, Vec3 feetOffset, Vec3 eyeOffset, double weaponRange) {}
    private record AttackProgress(int usingTicks, boolean charged, int lastActionTick) {}

    public HappyGhastHuntGoal(HappyGhast ghast) {
        this.ghast = ghast;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!canHunt()) return false;
        target = findTarget();
        // Keep control of the entire assembly even between targets. Vanilla
        // roaming only checks the ghast and can drag its boat into blocks.
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canHunt();
    }

    private boolean canHunt() {
        return !ghast.isBaby()
                && ghast.level() instanceof ServerLevel && occupiedBoat() != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        invalidateRoute();
        retryTicks = 0;
        escapeRetryTicks = 0;
        searchStep = 2.0;
        blockedPassages.clear();
        rejectedFiringPositions.clear();
        disengagedTicks = 0;
        attackProgress.clear();
        roamingGoal = null;
        roamingSearch = null;
        ghast.setTarget(target);
    }

    @Override
    public void tick() {
        if (!(ghast.level() instanceof ServerLevel world)) return;
        AbstractBoat boat = occupiedBoat();
        if (boat == null) return;
        ServerPlayer available = target != null && RicMobTargeting.isEligible(ghast, target)
                && RicMobTargeting.inRange(ghast, target) ? target : RicMobTargeting.findNearest(ghast);
        if (target != available) {
            target = available;
            ghast.setTarget(target);
            invalidateRoute();
            rejectedFiringPositions.clear();
            blockedPassages.clear();
            escapeWaypoint = null;
            disengagedTicks = 0;
            attackProgress.clear();
            retryTicks = 0;
            roamingGoal = null;
            roamingSearch = null;
        }
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof Mob rider && RicMobTargeting.isControlled(rider)) {
                RicMobTargeting.setTarget(rider, target != null && RicMobTargeting.isEligible(rider, target) ? target : null);
            }
        }
        FlightSpace space = new FlightSpace(world, boat);
        Vec3 position = ghast.position();
        if (target == null) {
            tickRoaming(space, position);
            return;
        }
        if (ghast.isOnStillTimeout()) {
            hover();
            return;
        }
        blockedPassages.removeIf(passage -> passage.expiresAt <= ghast.tickCount);
        rejectedFiringPositions.removeIf(rejected -> rejected.expiresAt <= ghast.tickCount
                || rejected.targetPosition.distanceToSqr(target.position()) > 4.0);
        // Check the actual bodies with a small margin, rather than filling the
        // empty space inside the whole hanging assembly with a solid box.
        if (!space.canOccupy(point(position))) {
            invalidateRoute();
            if (escapeWaypoint == null && escapeRetryTicks-- > 0) {
                hover();
                return;
            }
            if (escapeWaypoint == null || !space.canOccupy(point(escapeWaypoint))
                    || !space.canEscape(position, escapeWaypoint)) {
                escapeWaypoint = findEscape(space, position);
                escapeRetryTicks = escapeWaypoint == null ? 2 : 0;
            }
            if (escapeWaypoint != null) steer(escapeWaypoint, space, true);
            else hover();
            return;
        }
        escapeWaypoint = null;

        // Horizontal proximity and a view from above the canopy are insufficient.
        boolean firingPosition = space.ridersCanShoot(position);
        if (firingPosition && space.ridersAreEngaging()) {
            disengagedTicks = 0;
            invalidateRoute();
            ghast.getLookControl().setLookAt(target, 30.0F, 30.0F);
            hover();
            return;
        }
        if (firingPosition && !space.isRejectedFiringPosition(position)) {
            // Give native behavior a few ticks to start charging after arrival.
            // Geometry alone must never leave the mount hovering indefinitely.
            if (++disengagedTicks <= 4) {
                hover();
                return;
            }
            rejectedFiringPositions.add(new RejectedFiringPosition(position, target.position(), ghast.tickCount + 80));
            invalidateRoute();
            retryTicks = 0;
        } else {
            disengagedTicks = 0;
        }

        if (plannedTarget != null && plannedTarget.distanceToSqr(target.position()) > 9.0) {
            invalidateRoute();
            retryTicks = 0;
        }

        if (followHuntRoute(space, position)) return;

        if (search == null) {
            if (retryTicks-- > 0) {
                hover();
                return;
            }
            plannedTarget = target.position();
            List<FlightPathSearch.Point> approaches = space.approachPositions();
            plannedApproaches = approaches;
            List<FlightPathSearch.Point> overflight = FlightPathSearch.directOrOverflight(
                    point(position), plannedApproaches, space, 24.0);
            if (!overflight.isEmpty()) {
                route = overflight;
                routeIndex = 0;
                resetProgress();
                if (followHuntRoute(space, position)) return;
            }
            // A failed immediate steering attempt clears the old route state.
            // Keep this search aimed at the openings we just found.
            plannedTarget = target.position();
            plannedApproaches = approaches;
            search = new FlightPathSearch(point(position), space,
                    candidate -> !space.isRejectedFiringPosition(vector(candidate))
                            && space.ridersCanShoot(vector(candidate)), space::estimate,
                    searchStep, SEARCH_NODE_LIMIT);
        }
        search.advance(SEARCH_NODES_PER_TICK);
        if (search.isFinished()) {
            route = search.path();
            routeIndex = 0;
            // After a partial approach, keep searching; finer cells can resolve
            // an opening between the first search's grid positions.
            searchStep = search.reachedGoal() ? 2.0 : (searchStep > 0.5 ? searchStep * 0.5 : 2.0);
            retryTicks = route.isEmpty() ? 2 : 0;
            search = null;
            resetProgress();
            if (followHuntRoute(space, position)) return;
        }
        // Continue a safe closing motion while a difficult grid search runs.
        // Planning must not turn a stationary, out-of-range mount into an idle one.
        Vec3 closing = space.safeClosingStep(position);
        if (closing != null) steer(closing, space, false);
        else hover();
    }

    private boolean followHuntRoute(FlightSpace space, Vec3 position) {
        while (routeIndex < route.size() && position.distanceToSqr(vector(route.get(routeIndex)))
                < (routeIndex == route.size() - 1 ? 0.15 * 0.15 : WAYPOINT_REACHED_SQ)) {
            routeIndex++;
            resetProgress();
        }
        if (routeIndex >= route.size()) return false;
        FlightPathSearch.RouteAim aim = FlightPathSearch.lookAhead(point(position), route, routeIndex, space);
        if (aim != null) {
            Vec3 next = vector(aim.position());
            if (!trackProgress(next)) {
                routeIndex = aim.index();
                if (steer(next, space, false)) return true;
            } else {
                blockedPassages.add(new BlockedPassage(position, next.subtract(position).normalize(), ghast.tickCount + 40));
            }
        }
        // Replan on this tick rather than inserting a hover between attempts.
        invalidateRoute();
        retryTicks = 0;
        return false;
    }

    /** Keep the jockey moving when every nearby player is a spectator or creative. */
    private void tickRoaming(FlightSpace space, Vec3 position) {
        if (roamingGoal != null && position.distanceToSqr(roamingGoal) < 4.0) {
            roamingGoal = null;
            roamingSearch = null;
            invalidateRoute();
        }

        while (routeIndex < route.size()
                && position.distanceToSqr(vector(route.get(routeIndex))) < WAYPOINT_REACHED_SQ) {
            routeIndex++;
            resetProgress();
        }
        if (routeIndex < route.size()) {
            FlightPathSearch.RouteAim aim = FlightPathSearch.lookAhead(point(position), route, routeIndex, space);
            if (aim != null && !trackProgress(vector(aim.position()))) {
                routeIndex = aim.index();
                steer(vector(aim.position()), space, false);
                return;
            }
            // An obstructed or stale waypoint is discarded immediately, then
            // a fresh safe destination is picked on this tick.
            invalidateRoamingRoute();
        }

        if (roamingGoal == null) chooseRoamingGoal(space, position);
        if (roamingGoal == null) {
            hover();
            return;
        }

        if (roamingSearch == null && space.canMove(point(position), point(roamingGoal))) {
            route = List.of(point(roamingGoal));
            routeIndex = 0;
            resetProgress();
            steer(roamingGoal, space, false);
            return;
        }

        if (roamingSearch == null) {
            roamingSearch = new FlightPathSearch(point(position), space,
                    candidate -> candidate.distanceTo(point(roamingGoal)) < 2.0,
                    candidate -> candidate.distanceTo(point(roamingGoal)), 2.0, 2000);
        }
        roamingSearch.advance(SEARCH_NODES_PER_TICK);
        if (roamingSearch.isFinished()) {
            route = roamingSearch.path();
            routeIndex = 0;
            roamingSearch = null;
            resetProgress();
            if (route.isEmpty()) {
                invalidateRoamingRoute();
                hover();
                return;
            }
            FlightPathSearch.RouteAim aim = FlightPathSearch.lookAhead(point(position), route, routeIndex, space);
            if (aim != null) {
                routeIndex = aim.index();
                steer(vector(aim.position()), space, false);
                return;
            }
            invalidateRoamingRoute();
        }

        // Make incremental progress while A* is searching, just as the hunt
        // does. Each step is swept against the ghast, hanging boat, and riders.
        Vec3 closing = space.safeClosingStep(position);
        if (closing != null) steer(closing, space, false);
        else hover();
    }

    private void chooseRoamingGoal(FlightSpace space, Vec3 position) {
        Vec3 fallback = null;
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = ghast.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = 6.0 + ghast.getRandom().nextDouble() * 10.0;
            double height = -6.0 + ghast.getRandom().nextDouble() * 12.0;
            Vec3 candidate = position.add(Math.cos(angle) * distance, height, Math.sin(angle) * distance);
            if (!space.canOccupy(point(candidate))) continue;
            if (fallback == null) fallback = candidate;
            if (space.canMove(point(position), point(candidate))) {
                roamingGoal = candidate;
                return;
            }
        }
        // If every random heading runs into the canopy, route toward one clear
        // destination around it instead of idling against the nearest block.
        roamingGoal = fallback;
    }

    private void invalidateRoamingRoute() {
        route = List.of();
        routeIndex = 0;
        roamingGoal = null;
        roamingSearch = null;
        resetProgress();
    }

    private boolean trackProgress(Vec3 waypoint) {
        double distance = ghast.position().distanceTo(waypoint);
        if (progressWaypoint == null || progressWaypoint.distanceToSqr(waypoint) > 0.01) {
            progressWaypoint = waypoint;
            closestWaypointDistance = distance;
            noProgressTicks = 0;
        } else if (distance < closestWaypointDistance - 0.04) {
            closestWaypointDistance = distance;
            noProgressTicks = 0;
        } else {
            noProgressTicks++;
        }
        return noProgressTicks >= 5;
    }

    private boolean steer(Vec3 destination, FlightSpace space, boolean escaping) {
        Vec3 delta = destination.subtract(ghast.position());
        double flyingSpeed = ghast.getAttributeValue(Attributes.FLYING_SPEED);
        Vec3 velocity = vector(FlightSteering.nextVelocity(point(ghast.getDeltaMovement()), point(delta), flyingSpeed));
        Vec3 next = ghast.position().add(velocity.scale(2.0));
        boolean clear = escaping ? space.canReduceOverlap(ghast.position(), next)
                : space.canMove(point(ghast.position()), point(next));
        if (!clear) {
            // Correct the heading now instead of waiting for inertia to decay
            // against a block. Keep the same normal cruising speed cap.
            velocity = delta.normalize().scale(Math.min(FlightSteering.cruisingSpeed(flyingSpeed), delta.length() * 0.5));
            next = ghast.position().add(velocity.scale(2.0));
            clear = escaping ? space.canReduceOverlap(ghast.position(), next)
                    : space.canMove(point(ghast.position()), point(next));
        }
        if (!clear) {
            invalidateRoute();
            escapeWaypoint = null;
            hover();
            return false;
        }
        // Follow the smoothed travel heading instead of snapping the face at
        // each newly selected waypoint. This avoids rapid mid-flight double
        // takes when a route bends around foliage.
        Vec3 facing = velocity.lengthSqr() > 1.0E-5 ? velocity.normalize() : delta.normalize();
        ghast.getLookControl().setLookAt(ghast.getX() + facing.x * 4.0,
                ghast.getEyeY() + facing.y * 4.0, ghast.getZ() + facing.z * 4.0);
        // The vanilla controller adds impulses after a random delay and uses a
        // much larger clearance. During this goal we steer every tick ourselves.
        ghast.getMoveControl().setWait();
        ghast.setDeltaMovement(velocity);
        return true;
    }

    private void hover() {
        ghast.getMoveControl().setWait();
        ghast.setDeltaMovement(ghast.getDeltaMovement().scale(0.55));
    }

    private Vec3 findEscape(FlightSpace space, Vec3 from) {
        // Clear the canopy vertically first when it is safe, instead of
        // repeatedly sidestepping along its edge toward the player.
        for (double height : new double[]{1.0, 2.0, 4.0, 6.0, 8.0}) {
            Vec3 above = from.add(0.0, height, 0.0);
            if (space.canOccupy(point(above)) && space.canEscape(from, above)) return above;
        }
        Vec3 best = null;
        double bestScore = Double.MAX_VALUE;
        for (double distance : new double[]{1.0, 2.0, 4.0, 6.0, 8.0}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Vec3 candidate = from.add(new Vec3(dx, dy, dz).normalize().scale(distance));
                        if (!space.canOccupy(point(candidate)) || !space.canEscape(from, candidate)) continue;
                        double score = distance + space.estimate(point(candidate));
                        if (score < bestScore) {
                            best = candidate;
                            bestScore = score;
                        }
                    }
                }
            }
        }
        return best;
    }

    private void invalidateRoute() {
        route = List.of();
        routeIndex = 0;
        search = null;
        plannedTarget = null;
        plannedApproaches = List.of();
        resetProgress();
    }

    private void resetProgress() {
        progressWaypoint = null;
        noProgressTicks = 0;
    }

    private AbstractBoat occupiedBoat() {
        for (Leashable held : Leashable.leashableLeashedTo(ghast)) {
            if (held instanceof AbstractBoat boat && boat.isVehicle()
                    && boat.entityTags().contains("ric_jockey")) return boat;
        }
        return null;
    }

    private ServerPlayer findTarget() {
        if (ghast.getTarget() instanceof ServerPlayer player && RicMobTargeting.isEligible(ghast, player)
                && RicMobTargeting.inRange(ghast, player)) return player;
        return RicMobTargeting.findNearest(ghast);
    }

    @Override
    public void stop() {
        invalidateRoute();
        target = null;
        escapeWaypoint = null;
        rejectedFiringPositions.clear();
        disengagedTicks = 0;
        attackProgress.clear();
        roamingGoal = null;
        roamingSearch = null;
        ghast.setTarget(null);
        hover();
    }

    private static FlightPathSearch.Point point(Vec3 value) { return new FlightPathSearch.Point(value.x, value.y, value.z); }
    private static Vec3 vector(FlightPathSearch.Point value) { return new Vec3(value.x(), value.y(), value.z()); }

    private final class FlightSpace implements FlightPathSearch.Space {
        private final ServerLevel world;
        private final Vec3 origin = ghast.position();
        private final Vec3 targetPosition = target == null ? origin : target.position();
        private final Vec3 targetEye = target == null ? origin : target.getEyePosition();
        private final Vec3 targetBody = target == null ? origin
                : target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        private final List<RiderPosition> riders = new ArrayList<>();
        private final List<AABB> bodies = new ArrayList<>();
        private final AABB hull;

        FlightSpace(ServerLevel world, AbstractBoat boat) {
            this.world = world;
            bodies.add(ghast.getBoundingBox().inflate(0.1));
            // Keep a half-block of clearance below the boat while allowing
            // it to descend into narrow ground-level openings.
            bodies.add(boat.getBoundingBox().inflate(0.1, 0.5, 0.1));
            AABB assembly = bodies.getFirst().minmax(bodies.getLast());
            for (Entity passenger : boat.getPassengers()) {
                AABB riderBody = passenger.getBoundingBox().inflate(0.1);
                bodies.add(riderBody);
                assembly = assembly.minmax(riderBody);
                if (passenger instanceof Mob rider
                        && rider.getMainHandItem().getItem() instanceof ProjectileWeaponItem weapon
                        && rider.canUseNonMeleeWeapon(rider.getMainHandItem())) {
                    // Use the same real seat offsets for both the search and
                    // its stopping check, including a boat that is swinging.
                    riders.add(new RiderPosition(rider, rider.position().subtract(origin),
                            rider.getEyePosition().subtract(origin), weapon.getDefaultProjectileRange()));
                }
            }
            hull = assembly;
        }

        private AABB boxAt(Vec3 position) { return hull.move(position.subtract(origin)); }

        @Override
        public boolean canOccupy(FlightPathSearch.Point candidate) {
            Vec3 position = vector(candidate);
            double minX = target == null ? origin.x - 17.0 : Math.min(origin.x, targetPosition.x) - 28.0;
            double maxX = target == null ? origin.x + 17.0 : Math.max(origin.x, targetPosition.x) + 28.0;
            double minZ = target == null ? origin.z - 17.0 : Math.min(origin.z, targetPosition.z) - 28.0;
            double maxZ = target == null ? origin.z + 17.0 : Math.max(origin.z, targetPosition.z) + 28.0;
            double minY = target == null ? origin.y - 8.0 : Math.min(origin.y, targetPosition.y) - 8.0;
            double maxY = target == null ? origin.y + 8.0 : Math.max(origin.y + 16.0, targetPosition.y + 40.0);
            if (position.x < minX || position.x > maxX || position.z < minZ || position.z > maxZ
                    || position.y < minY || position.y > maxY) return false;
            AABB box = boxAt(position);
            if (box.minY < world.getMinY() || box.maxY > world.getMaxY()) return false;
            BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
            BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
            if (!world.getWorldBorder().isWithinBounds(min) || !world.getWorldBorder().isWithinBounds(max)) return false;
            if (!world.hasChunkAt(min) || !world.hasChunkAt(max)
                    || !world.hasChunkAt(new BlockPos(min.getX(), min.getY(), max.getZ()))
                    || !world.hasChunkAt(new BlockPos(max.getX(), min.getY(), min.getZ()))) return false;
            Vec3 offset = position.subtract(origin);
            for (AABB body : bodies) {
                AABB moved = body.move(offset);
                if (!world.noBlockCollision(ghast, moved, true)
                        || world.getBlockStates(moved).anyMatch(state -> state.is(BlockTags.HAPPY_GHAST_AVOIDS))) return false;
            }
            return true;
        }

        @Override
        public boolean canMove(FlightPathSearch.Point from, FlightPathSearch.Point to) {
            Vec3 start = vector(from), end = vector(to);
            Vec3 direction = end.subtract(start).normalize();
            for (BlockedPassage blocked : blockedPassages) {
                if (start.distanceToSqr(blocked.from) < 0.75 * 0.75 && direction.dot(blocked.direction) > 0.9) return false;
            }
            Vec3 delta = end.subtract(start);
            List<AABB> obstacles = collisions(boxAt(start).minmax(boxAt(end)));
            for (AABB body : bodies) {
                AABB moved = body.move(start.subtract(origin));
                for (AABB obstacle : obstacles) {
                    if (FlightGeometry.sweptIntersects(geometry(moved), point(delta), geometry(obstacle))) return false;
                }
            }
            return true;
        }

        Vec3 safeClosingStep(Vec3 from) {
            Vec3 destination = target == null ? roamingGoal : targetPosition;
            if (target != null && !plannedApproaches.isEmpty()) {
                destination = vector(plannedApproaches.stream()
                        .min(Comparator.comparingDouble(candidate -> candidate.distanceTo(point(from)))).orElseThrow());
            }
            if (destination == null) return null;
            Vec3 flat = destination.subtract(from).multiply(1.0, 0.0, 1.0).normalize();
            Vec3 best = null;
            double bestEstimate = estimate(point(from)) - 0.05;
            for (double turn : new double[]{0.0, Math.PI / 4.0, -Math.PI / 4.0}) {
                double x = flat.x * Math.cos(turn) - flat.z * Math.sin(turn);
                double z = flat.x * Math.sin(turn) + flat.z * Math.cos(turn);
                for (double height : new double[]{-1.0, 0.0, 1.0}) {
                    Vec3 candidate = from.add(new Vec3(x, height, z).normalize().scale(1.5));
                    double value = estimate(point(candidate));
                    if (value < bestEstimate && canOccupy(point(candidate)) && canMove(point(from), point(candidate))) {
                        best = candidate;
                        bestEstimate = value;
                    }
                }
            }
            return best;
        }

        double estimate(FlightPathSearch.Point candidate) {
            Vec3 position = vector(candidate);
            if (target == null) return roamingGoal == null ? 0.0 : position.distanceTo(roamingGoal);
            // Aim at actual clear firing openings, including the far side of
            // a tree. Pure weapon distance creates a flat zero-cost region
            // behind leaves, so a partial search used to give up right there.
            if (!plannedApproaches.isEmpty()) {
                return plannedApproaches.stream().mapToDouble(candidate::distanceTo).min().orElse(0.0);
            }
            double best = Double.MAX_VALUE;
            for (RiderPosition rider : riders) {
                Vec3 feet = position.add(rider.feetOffset);
                Vec3 eye = position.add(rider.eyeOffset);
                double distance = Math.max(0.0, feet.distanceTo(targetPosition)
                        - RiderAttackRange.approachRange(rider.weaponRange) + 0.5);
                double height = Math.max(0.0, eye.y - targetEye.y - 3.0);
                best = Math.min(best, Math.max(distance, height));
            }
            best = best == Double.MAX_VALUE ? position.distanceTo(targetPosition) : best;
            for (RejectedFiringPosition rejected : rejectedFiringPositions) {
                best = Math.max(best, 1.5 - position.distanceTo(rejected.position));
            }
            return best + (ridersCanShoot(position) ? 0.0 : 3.0);
        }

        boolean isRejectedFiringPosition(Vec3 position) {
            for (RejectedFiringPosition rejected : rejectedFiringPositions) {
                if (position.distanceToSqr(rejected.position) < 1.5 * 1.5) return true;
            }
            return false;
        }

        boolean ridersCanShoot(Vec3 position) {
            if (target == null) return false;
            for (RiderPosition rider : riders) {
                Vec3 feet = position.add(rider.feetOffset);
                Vec3 eye = position.add(rider.eyeOffset);
                if (!RiderAttackRange.contains(point(feet), point(targetPosition), rider.weaponRange)
                        || eye.y > targetEye.y + MAX_SHOT_HEIGHT
                        || Math.hypot(eye.x - targetEye.x, eye.z - targetEye.z) < 1.0) continue;
                if (clearShot(eye, targetEye) && clearShot(eye, targetBody)) return true;
            }
            return false;
        }

        boolean ridersAreEngaging() {
            if (target == null) return false;
            for (RiderPosition rider : riders) {
                Mob mob = rider.mob;
                if (!RiderAttackRange.contains(point(mob.position()), point(targetPosition), rider.weaponRange)
                        || !mob.getSensing().hasLineOfSight(target) || !BehaviorUtils.canSee(mob, target)
                        || !clearShot(mob.getEyePosition(), targetEye)
                        || !clearShot(mob.getEyePosition(), targetBody)) continue;
                var brain = mob.getBrain();
                if (brain.isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) && brain.isActive(Activity.FIGHT)
                        && brain.getRunningBehaviors().stream().anyMatch(behavior -> behavior instanceof CrossbowAttack<?, ?>)) {
                    int usingTicks = mob.isUsingItem() && mob.getUseItem().getItem() instanceof CrossbowItem
                            ? mob.getTicksUsingItem() : 0;
                    boolean charged = CrossbowItem.isCharged(mob.getMainHandItem());
                    AttackProgress previous = attackProgress.get(mob);
                    int lastAction = previous == null ? ghast.tickCount - 100 : previous.lastActionTick;
                    // Charging must advance, or the loaded state must change
                    // as it loads/fires. A running behavior alone is not a shot.
                    if ((usingTicks > 0 && (previous == null || usingTicks != previous.usingTicks))
                            || (previous != null && charged != previous.charged)) lastAction = ghast.tickCount;
                    attackProgress.put(mob, new AttackProgress(usingTicks, charged, lastAction));
                    // Native crossbows wait 20..39 ticks after loading.
                    if (ghast.tickCount - lastAction <= 45) return true;
                }
            }
            return false;
        }

        List<FlightPathSearch.Point> approachPositions() {
            if (target == null || riders.isEmpty()) return List.of();
            List<FlightPathSearch.Point> candidates = new ArrayList<>();
            // Keep candidates from every side. Choosing only the closest dozen
            // used to discard the far-side opening before routing even began.
            for (int direction = 0; direction < 16; direction++) {
                List<FlightPathSearch.Point> sector = new ArrayList<>();
                double angle = direction * Math.PI / 8.0;
                for (RiderPosition rider : riders) {
                    for (double radius : new double[]{2.5, 3.5, 4.5, 5.0}) {
                        // Keep the boat clear of the ground and canopy while
                        // searching for a shot from each real rider's seat.
                        for (double height : new double[]{0.5, 1.0, 1.5, 2.0, 2.5, 3.5}) {
                            Vec3 candidate = new Vec3(targetPosition.x + Math.cos(angle) * radius - rider.feetOffset.x,
                                    targetPosition.y + height - rider.feetOffset.y,
                                    targetPosition.z + Math.sin(angle) * radius - rider.feetOffset.z);
                            if (!isRejectedFiringPosition(candidate) && canOccupy(point(candidate))
                                    && ridersCanShoot(candidate)) sector.add(point(candidate));
                        }
                    }
                }
                sector.sort(Comparator.comparingDouble(candidate -> candidate.distanceTo(point(origin))));
                if (!sector.isEmpty()) {
                    FlightPathSearch.Point nearest = sector.getFirst();
                    candidates.add(nearest);
                    sector.stream().filter(candidate -> candidate.distanceTo(nearest) >= 1.0)
                            .findFirst().ifPresent(candidates::add);
                }
            }
            candidates.sort(Comparator.comparingDouble(candidate -> candidate.distanceTo(point(origin))));
            return candidates;
        }

        private boolean clearShot(Vec3 from, Vec3 to) {
            return world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, ghast)).getType() == HitResult.Type.MISS;
        }

        boolean canEscape(Vec3 from, Vec3 to) {
            return escapeClear(from, to, true);
        }

        boolean canReduceOverlap(Vec3 from, Vec3 to) {
            return escapeClear(from, to, false);
        }

        private boolean escapeClear(Vec3 from, Vec3 to, boolean requireClearEnd) {
            List<AABB> initial = collisions(boxAt(from)).stream().filter(obstacle -> bodies.stream()
                    .anyMatch(body -> body.move(from.subtract(origin)).intersects(obstacle))).toList();
            List<AABB> swept = collisions(boxAt(from).minmax(boxAt(to)));
            for (AABB obstacle : swept) {
                if (initial.contains(obstacle)) continue;
                for (AABB body : bodies) {
                    if (FlightGeometry.sweptIntersects(geometry(body.move(from.subtract(origin))),
                            point(to.subtract(from)), geometry(obstacle))) return false;
                }
            }
            int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) / 0.25));
            double previous = assemblyOverlap(from, initial);
            for (int step = 1; step <= steps; step++) {
                double overlap = assemblyOverlap(from.lerp(to, step / (double) steps), initial);
                if (overlap > previous + 0.0001) return false;
                previous = overlap;
            }
            return !requireClearEnd || previous < 0.0001;
        }

        private double assemblyOverlap(Vec3 position, List<AABB> obstacles) {
            double result = 0.0;
            for (AABB body : bodies) result += overlap(body.move(position.subtract(origin)), obstacles);
            return result;
        }

        private List<AABB> collisions(AABB box) {
            List<AABB> collisions = new ArrayList<>();
            for (VoxelShape shape : world.getBlockAndLiquidCollisions(ghast, box)) collisions.addAll(shape.toAabbs());
            return collisions;
        }

        private double overlap(AABB box, List<AABB> obstacles) {
            double volume = 0.0;
            for (AABB obstacle : obstacles) {
                double x = Math.max(0.0, Math.min(box.maxX, obstacle.maxX) - Math.max(box.minX, obstacle.minX));
                double y = Math.max(0.0, Math.min(box.maxY, obstacle.maxY) - Math.max(box.minY, obstacle.minY));
                double z = Math.max(0.0, Math.min(box.maxZ, obstacle.maxZ) - Math.max(box.minZ, obstacle.minZ));
                volume += x * y * z;
            }
            return volume;
        }
    }

    private static FlightGeometry.Box geometry(AABB box) {
        return new FlightGeometry.Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}
