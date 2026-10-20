package jaxon.ric.ai;

import jaxon.ric.Random_Item_Challenge;
import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;

/** Shared acquisition rules for RIC mobs, including mounted riders. */
public final class RicMobTargeting {
    // Vanilla Ghast.createAttributes() sets FOLLOW_RANGE to 100 blocks.
    private static final double VANILLA_GHAST_FOLLOW_RANGE = 100.0;
    private static final double LARGE_MOB_FOLLOW_RANGE = 128.0;

    private RicMobTargeting() {}

    public static double followRange(Mob mob) {
        double configuredRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        // The Happy Ghast hunt and its jockeys share vanilla Ghast's exact
        // acquisition and abandon distance. Once acquired, LOS is intentionally
        // ignored so the hunt can route around cover until the player leaves it.
        if (mob instanceof HappyGhast || mob.entityTags().contains("ric_jockey")) {
            return VANILLA_GHAST_FOLLOW_RANGE;
        }
        if (mob.getBbWidth() >= 2.5 || mob.getBbHeight() >= 3.0) {
            return Math.max(configuredRange, LARGE_MOB_FOLLOW_RANGE);
        }
        return configuredRange;
    }

    public static boolean isEligible(Mob mob, ServerPlayer player) {
        return player.isAlive() && !player.isRemoved() && !player.isSpectator() && !player.isCreative()
                && player.level() == mob.level()
                // Returning to survival explicitly makes a player targetable
                // again. The elimination ledger belongs to match scoring, not
                // to the player's current combat eligibility.
                && (!StopCommand.isRunning || GoCommand.testMode
                    || Random_Item_Challenge.participantsWithoutRemovals.contains(player.getUUID()))
                && (GoCommand.enableMobFriendlyFire || !mob.isAlliedTo(player));
    }

    public static boolean isControlled(Mob mob) {
        return mob.entityTags().contains("ric_retarget_players")
                || mob.entityTags().contains("ric_jockey");
    }

    public static boolean inRange(Mob mob, ServerPlayer player) {
        double range = followRange(mob);
        return mob.distanceToSqr(player) <= range * range;
    }

    public static boolean canAcquire(Mob mob, ServerPlayer player) {
        if (!(mob.level() instanceof ServerLevel level) || !isEligible(mob, player)) return false;
        TargetingConditions conditions = GoCommand.enableMobFriendlyFire
                ? TargetingConditions.forNonCombat()
                : TargetingConditions.forCombat();
        // The mount must find a player behind cover before it can route its
        // riders around that cover. Requiring visibility here prevents a hunt.
        if (mob instanceof HappyGhast || mob.entityTags().contains("ric_jockey")) conditions.ignoreLineOfSight();
        return conditions.range(followRange(mob)).test(level, mob, player);
    }

    public static void setTarget(Mob mob, ServerPlayer player) {
        if (mob.getTarget() == player) {
            if (player == null) clearBrainTargetMemory(mob);
            return;
        }

        mob.setTarget(player);
        if (player != null && mob instanceof NeutralMob anger) {
            anger.setLastHurtByMob(player);
            anger.setPersistentAngerTarget(EntityReference.of(player.getUUID()));
            anger.startPersistentAngerTimer();
        }
        refreshBrainAttackMemory(mob, player);
    }

    /** Reasserts attack memories without resetting anger, which can spam mob voices. */
    public static void refreshBrainAttackMemory(Mob mob, ServerPlayer player) {
        if (mob instanceof Hoglin hoglin) {
            if (player == null) {
                hoglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            } else {
                LivingEntity attackTarget = hoglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
                if (attackTarget != player) {
                    hoglin.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                    hoglin.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
                    hoglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, player, 200L);
                }
            }
        }

        if (mob instanceof Piglin piglin) {
            if (player == null) {
                piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            } else {
                var brain = piglin.getBrain();
                // A passenger cannot walk to its target; its mount handles
                // that. A failed walking path must not cancel ranged combat.
                if (piglin.isPassenger()) brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                LivingEntity attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
                if (attackTarget != player) {
                    brain.setMemory(MemoryModuleType.ATTACK_TARGET, player);
                }
                java.util.UUID angryAt = brain.getMemory(MemoryModuleType.ANGRY_AT).orElse(null);
                if (!player.getUUID().equals(angryAt)) brain.setMemory(MemoryModuleType.ANGRY_AT, player.getUUID());
            }
        }
    }

    private static void clearBrainTargetMemory(Mob mob) {
        if (mob instanceof Hoglin hoglin) {
            hoglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        if (mob instanceof Piglin piglin) {
            piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }

    public static ServerPlayer findNearest(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            double distance = mob.distanceToSqr(player);
            if (distance < bestDistance && canAcquire(mob, player)) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }
}
