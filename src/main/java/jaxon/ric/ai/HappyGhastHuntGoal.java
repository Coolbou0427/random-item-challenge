package jaxon.ric.ai;

import jaxon.ric.command.GoCommand;
import java.util.EnumSet;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

        Vec3d t = target.getEntityPos();
        Vec3d g = ghast.getEntityPos();

        Vec3d flat = new Vec3d(g.x - t.x, 0.0, g.z - t.z);
        double flatLen = flat.length();

        if (flatLen < 1.0e-4) {
            flat = new Vec3d(1.0, 0.0, 0.0);
            flatLen = 1.0;
        }

        double distError = flatLen - DESIRED_DIST;

        ghast.getLookControl().lookAt(target, 30.0F, 30.0F);

        if (Math.abs(distError) <= DIST_TOLERANCE) {
            ghast.getMoveControl().setWaiting();
            return;
        }

        Vec3d dir = flat.multiply(1.0 / flatLen);
        Vec3d desiredPos = new Vec3d(
                t.x + dir.x * DESIRED_DIST,
                t.y + HEIGHT_ABOVE_TARGET,
                t.z + dir.z * DESIRED_DIST
        );

        ghast.getMoveControl().moveTo(desiredPos.x, desiredPos.y, desiredPos.z, 1.0);
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
    }
}
