package jaxon.ric.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class ZombieHorseAvoidLowCeilingGoal extends Goal {
    private final MobEntity horse;
    private final double speed;
    private final int searchRadius;
    private final int requiredAirBlocks;
    private BlockPos targetPos;
    private int cooldown;

    public ZombieHorseAvoidLowCeilingGoal(MobEntity horse, double speed, int searchRadius, int requiredAirBlocks) {
        this.horse = horse;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.requiredAirBlocks = requiredAirBlocks;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!(horse.getEntityWorld() instanceof ServerWorld)) return false;
        if (!horse.hasPassengers()) return false;
        if (hasEnoughHeadroom()) return false;

        targetPos = findNearestSafePos();
        if (targetPos == null) {
            cooldown = 10;
            return false;
        }

        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (targetPos == null) return false;
        if (!horse.hasPassengers()) return false;
        if (hasEnoughHeadroom()) return false;

        EntityNavigation nav = horse.getNavigation();
        if (nav.isIdle()) return false;

        double dx = (targetPos.getX() + 0.5) - horse.getX();
        double dy = (targetPos.getY()) - horse.getY();
        double dz = (targetPos.getZ() + 0.5) - horse.getZ();
        return dx * dx + dy * dy + dz * dz > 2.25;
    }

    @Override
    public void start() {
        if (targetPos == null) return;
        horse.getNavigation().startMovingTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, speed);
    }

    @Override
    public void stop() {
        targetPos = null;
        cooldown = 10;
    }

    private boolean hasEnoughHeadroom() {
        if (!(horse.getEntityWorld() instanceof ServerWorld world)) return true;
        BlockPos base = horse.getBlockPos();
        for (int i = 1; i <= requiredAirBlocks; i++) {
            if (!world.getBlockState(base.up(i)).isAir()) return false;
        }
        return true;
    }

    private BlockPos findNearestSafePos() {
        if (!(horse.getEntityWorld() instanceof ServerWorld world)) return null;

        BlockPos origin = horse.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();

        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int x = ox + dx;
                int z = oz + dz;

                for (int dy = -2; dy <= 2; dy++) {
                    int y = oy + dy;
                    m.set(x, y, z);

                    if (!isSafeSpot(world, m)) continue;

                    double distSq = squaredDistToCenter(m);
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        best = m.toImmutable();
                    }
                }
            }
        }

        return best;
    }

    private boolean isSafeSpot(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir()) return false;

        for (int i = 1; i <= requiredAirBlocks; i++) {
            if (!world.getBlockState(pos.up(i)).isAir()) return false;
        }

        BlockPos below = pos.down();
        if (world.getBlockState(below).isAir()) return false;
        if (!world.getBlockState(below).isSolidBlock(world, below)) return false;

        return true;
    }

    private double squaredDistToCenter(BlockPos pos) {
        double px = pos.getX() + 0.5;
        double py = pos.getY();
        double pz = pos.getZ() + 0.5;
        double dx = px - horse.getX();
        double dy = py - horse.getY();
        double dz = pz - horse.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
