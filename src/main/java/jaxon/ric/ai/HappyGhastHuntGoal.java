package jaxon.ric.ai;

import jaxon.ric.command.GoCommand;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class HappyGhastHuntGoal extends Goal {
    private final HappyGhastEntity ghast;
    private LivingEntity target;
    private int retargetCooldown;
    private static final double HEIGHT_ABOVE_TARGET = 8.0;
    private static final double DESIRED_DIST = 5.0;
    private static final double DIST_TOLERANCE = 1;
    private static final double ACQUIRE_RANGE = 48.0;
    private static final double CHASE_RANGE = 48.0;

    private static final double PAYLOAD_PAD_XZ = 0.6;
    private static final double BOAT_HANG_DOWN = 3.5;
    private static final int CLEARANCE_TRIES = 10;
    private static final double CLEARANCE_STEP_Y = 1.0;

    private double lastDistSq = Double.NaN;
    private int stuckTicks = 0;

    private int detourTicks = 0;
    private Vec3d detourPos = null;

    public HappyGhastHuntGoal(HappyGhastEntity ghast) {
        this.ghast = ghast;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (ghast.isBaby()) return false;
        if (ghast.isStill()) return false;
        if (!isHoldingBoat()) return false;

        if (!(ghast.getEntityWorld() instanceof ServerWorld world)) return false;
        return findTarget(world) != null;
    }

    @Override
    public boolean shouldContinue() {
        if (ghast.isBaby()) return false;
        if (ghast.isStill()) return false;
        if (!isHoldingBoat()) return false;

        if (!isValidTarget(target)) {
            target = null;
            ghast.setTarget(null);
            return false;
        }
        if (target != null && ghast.squaredDistanceTo(target) > CHASE_RANGE * CHASE_RANGE) {
            target = null;
            ghast.setTarget(null);
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        retargetCooldown = 0;
        lastDistSq = Double.NaN;
        stuckTicks = 0;
        detourTicks = 0;
        detourPos = null;
    }

    @Override
    public void tick() {
        if (!(ghast.getEntityWorld() instanceof ServerWorld world)) return;

        LivingEntity current = ghast.getTarget();
        if (!isValidTarget(current)) {
            ghast.setTarget(null);
            target = null;
        }

        if (target != null && ghast.squaredDistanceTo(target) > CHASE_RANGE * CHASE_RANGE) {
            target = null;
            ghast.setTarget(null);
            ghast.getMoveControl().setWaiting();
            return;
        }

        if (retargetCooldown-- <= 0 || target == null || !target.isAlive()) {
            target = findTarget(world);
            retargetCooldown = 20;
        }

        if (!isValidTarget(target)) {
            target = null;
            ghast.setTarget(null);
            return;
        }

        ghast.getLookControl().lookAt(target, 30.0F, 30.0F);

        if (detourTicks > 0 && detourPos != null) {
            detourTicks--;
            Vec3d safe = resolveSafePos(world, detourPos);
            if (safe == null) {
                detourTicks = 0;
                detourPos = null;
            } else {
                ghast.getMoveControl().moveTo(safe.x, safe.y, safe.z, 1.15);
                if (ghast.getEntityPos().squaredDistanceTo(safe) <= 4.0) {
                    detourTicks = 0;
                    detourPos = null;
                    lastDistSq = Double.NaN;
                    stuckTicks = 0;
                }
                return;
            }
        }

        double distSq = ghast.squaredDistanceTo(target);
        if (!Double.isNaN(lastDistSq)) {
            if (distSq >= lastDistSq - 0.25) stuckTicks++;
            else stuckTicks = 0;
        }
        lastDistSq = distSq;

        if (stuckTicks >= 25) {
            detourPos = pickDetourPos(target.getEntityPos(), 8.0);
            detourTicks = 45;
            stuckTicks = 0;
            lastDistSq = Double.NaN;
            Vec3d safe = resolveSafePos(world, detourPos);
            if (safe != null) ghast.getMoveControl().moveTo(safe.x, safe.y, safe.z, 1.25);
            return;
        }

        Vec3d t = target.getEntityPos();
        Vec3d g = ghast.getEntityPos();

        Vec3d flat = new Vec3d(g.x - t.x, 0.0, g.z - t.z);
        double flatLen = flat.length();

        if (flatLen < 1.0e-4) {
            flat = new Vec3d(1.0, 0.0, 0.0);
            flatLen = 1.0;
        }

        double distError = flatLen - DESIRED_DIST;

        Vec3d dir = flat.multiply(1.0 / flatLen);
        Vec3d desiredPos = new Vec3d(
                t.x + dir.x * DESIRED_DIST,
                t.y + HEIGHT_ABOVE_TARGET,
                t.z + dir.z * DESIRED_DIST
        );

        Vec3d safeDesired = resolveSafePos(world, desiredPos);
        if (safeDesired == null) {
            detourPos = pickDetourPos(t, 10.0);
            detourTicks = 35;
            Vec3d safe = resolveSafePos(world, detourPos);
            if (safe != null) ghast.getMoveControl().moveTo(safe.x, safe.y, safe.z, 1.25);
            return;
        }

        if (Math.abs(distError) <= DIST_TOLERANCE) {
            ghast.getMoveControl().moveTo(safeDesired.x, safeDesired.y, safeDesired.z, 0.95);
            return;
        }

        ghast.getMoveControl().moveTo(safeDesired.x, safeDesired.y, safeDesired.z, 1.0);
    }

    private Vec3d resolveSafePos(ServerWorld world, Vec3d desiredPos) {
        Vec3d current = ghast.getEntityPos();
        double dx = desiredPos.x - current.x;
        double dy = desiredPos.y - current.y;
        double dz = desiredPos.z - current.z;

        Box base = ghast.getBoundingBox().offset(dx, dy, dz);
        double minX = base.minX - PAYLOAD_PAD_XZ;
        double minY = base.minY - BOAT_HANG_DOWN;
        double minZ = base.minZ - PAYLOAD_PAD_XZ;
        double maxX = base.maxX + PAYLOAD_PAD_XZ;
        double maxY = base.maxY;
        double maxZ = base.maxZ + PAYLOAD_PAD_XZ;

        Box payload = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        if (world.isSpaceEmpty(ghast, payload)) return desiredPos;

        for (int i = 1; i <= CLEARANCE_TRIES; i++) {
            double up = i * CLEARANCE_STEP_Y;
            Box raised = payload.offset(0.0, up, 0.0);
            if (world.isSpaceEmpty(ghast, raised)) return desiredPos.add(0.0, up, 0.0);
        }

        return null;
    }

    private Vec3d pickDetourPos(Vec3d targetPos, double extraUp) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double angle = r.nextDouble(0.0, Math.PI * 2.0);
        double radius = DESIRED_DIST + r.nextDouble(6.0, 12.0);

        double x = targetPos.x + Math.cos(angle) * radius;
        double z = targetPos.z + Math.sin(angle) * radius;
        double y = targetPos.y + HEIGHT_ABOVE_TARGET + extraUp + r.nextDouble(2.0, 8.0);

        return new Vec3d(x, y, z);
    }

    private boolean isHoldingBoat() {
        for (Leashable held : Leashable.collectLeashablesHeldBy(ghast)) {
            if (held instanceof net.minecraft.entity.vehicle.AbstractBoatEntity boat) {
                return boat.hasPassengers();
            }
        }
        return false;
    }

    private LivingEntity findTarget(ServerWorld world) {
        LivingEntity existing = ghast.getTarget();
        if (existing instanceof ServerPlayerEntity sp) {
            if (sp.isAlive() && !sp.isSpectator() && !sp.isCreative() && !isSameTeam(world, sp)) {
                if (ghast.squaredDistanceTo(sp) <= CHASE_RANGE * CHASE_RANGE) return sp;
            }
            ghast.setTarget(null);
        }

        double best = Double.MAX_VALUE;
        ServerPlayerEntity bestPlayer = null;

        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            if (p.isSpectator()) continue;
            if (p.isCreative()) continue;
            if (isSameTeam(world, p)) continue;

            double d = ghast.squaredDistanceTo(p);
            if (d > ACQUIRE_RANGE * ACQUIRE_RANGE) continue;
            if (d < best) {
                best = d;
                bestPlayer = p;
            }
        }

        if (bestPlayer == null) return null;

        ghast.setTarget(bestPlayer);
        return bestPlayer;
    }

    private boolean isValidTarget(LivingEntity e) {
        if (e == null) return false;
        if (!e.isAlive()) return false;

        if (e instanceof ServerPlayerEntity sp) {
            if (sp.isSpectator()) return false;
            if (sp.isCreative()) return false;
            if (!(ghast.getEntityWorld() instanceof ServerWorld world)) return false;
            return !isSameTeam(world, sp);
        }

        return true;
    }

    private boolean isSameTeam(ServerWorld world, ServerPlayerEntity player) {
        if (GoCommand.enableMobFriendlyFire) return false;
        Scoreboard sb = world.getScoreboard();
        Team ghastTeam = sb.getScoreHolderTeam(ghast.getUuidAsString());
        Team playerTeam = player.getScoreboardTeam();
        return ghastTeam != null && ghastTeam.equals(playerTeam);
    }

    @Override
    public void stop() {
        target = null;
        ghast.setTarget(null);
        lastDistSq = Double.NaN;
        stuckTicks = 0;
        detourTicks = 0;
        detourPos = null;
    }
}
