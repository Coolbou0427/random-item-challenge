package jaxon.ric.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

public class ZombieHorseAvoidLowCeilingGoal extends Goal {
    private final Mob horse;
    private final double speed;
    private final int searchRadius;
    private final int requiredAirBlocks;
    private BlockPos targetPos;
    private int cooldown;

    public ZombieHorseAvoidLowCeilingGoal(Mob horse, double speed, int searchRadius, int requiredAirBlocks) {
        this.horse = horse;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.requiredAirBlocks = requiredAirBlocks;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!(horse.level() instanceof ServerLevel)) return false;
        if (!horse.isVehicle()) return false;
        if (hasEnoughHeadroom()) return false;

        targetPos = findNearestSafePos();
        if (targetPos == null) {
            cooldown = 10;
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPos == null) return false;
        if (!horse.isVehicle()) return false;
        if (hasEnoughHeadroom()) return false;

        PathNavigation nav = horse.getNavigation();
        if (nav.isDone()) return false;

        double dx = (targetPos.getX() + 0.5) - horse.getX();
        double dy = (targetPos.getY()) - horse.getY();
        double dz = (targetPos.getZ() + 0.5) - horse.getZ();
        return dx * dx + dy * dy + dz * dz > 2.25;
    }

    @Override
    public void start() {
        if (targetPos == null) return;
        horse.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, speed);
    }

    @Override
    public void stop() {
        targetPos = null;
        cooldown = 10;
    }

    private boolean hasEnoughHeadroom() {
        if (!(horse.level() instanceof ServerLevel world)) return true;
        BlockPos base = horse.blockPosition();
        for (int i = 1; i <= requiredAirBlocks; i++) {
            if (!world.getBlockState(base.above(i)).isAir()) return false;
        }
        return true;
    }

    private BlockPos findNearestSafePos() {
        if (!(horse.level() instanceof ServerLevel world)) return null;

        BlockPos origin = horse.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

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
                        best = m.immutable();
                    }
                }
            }
        }

        return best;
    }

    private boolean isSafeSpot(ServerLevel world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir()) return false;

        for (int i = 1; i <= requiredAirBlocks; i++) {
            if (!world.getBlockState(pos.above(i)).isAir()) return false;
        }

        BlockPos below = pos.below();
        if (world.getBlockState(below).isAir()) return false;
        return world.getBlockState(below).isRedstoneConductor(world, below);
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
