package jaxon.ric.mixin;

import jaxon.ric.command.GoCommand;
import jaxon.ric.ai.RicMobTargeting;
import jaxon.ric.ai.RicPlayerTargetGoal;
import jaxon.ric.SpawnEggHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class CustomAggroRetargetMixin {
    @Unique private Vec3 ric$lastRecoveryPosition;
    @Unique private int ric$stalledTicks;
    @Unique private int ric$unseenTicks;
    @Unique private boolean ric$targetGoalInstalled;
    @Unique private boolean ric$friendlyFireInitialized;
    @Unique private boolean ric$lastFriendlyFire;
    @Unique private int ric$lastBrainMemoryRefresh = -20;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ric$retargetCustomAggroMob(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!RicMobTargeting.isControlled(mob)) return;
        if (!(mob.level() instanceof ServerLevel level) || level.getServer() == null) return;
        if (!ric$friendlyFireInitialized || ric$lastFriendlyFire != GoCommand.enableMobFriendlyFire) {
            ric$friendlyFireInitialized = true;
            ric$lastFriendlyFire = GoCommand.enableMobFriendlyFire;
            RicMobTargeting.setTarget(mob, null);
            ric$unseenTicks = 0;
        }
        if (!ric$targetGoalInstalled && SpawnEggHooks.isRicPlayerTargetMob(mob)) {
            ((MobEntityGoalSelectorAccessor) mob).ric$getTargetSelector().addGoal(-1, new RicPlayerTargetGoal(mob));
            ric$targetGoalInstalled = true;
        }

        LivingEntity current = mob.getTarget();
        if (current instanceof ServerPlayer currentPlayer && RicMobTargeting.isEligible(mob, currentPlayer)
                && RicMobTargeting.inRange(mob, currentPlayer)) {
            // Mounted riders cannot path themselves. Keep their head aimed at
            // the shared target so spear thrusts and crossbow fire don't drift
            // sideways while the mount navigates around an obstacle.
            if (mob.isPassenger()) mob.getLookControl().setLookAt(currentPlayer, 30.0F, 30.0F);
            // Refresh only missing attack memories at a low rate. Re-setting the
            // target/anger every tick makes piglin riders repeatedly vocalize.
            if (mob.isPassenger() || mob.tickCount - ric$lastBrainMemoryRefresh >= 20) {
                RicMobTargeting.refreshBrainAttackMemory(mob, currentPlayer);
                ric$lastBrainMemoryRefresh = mob.tickCount;
            }
            // Like vanilla TargetGoal, allow a short loss of sight while chasing.
            if (mob.getSensing().hasLineOfSight(currentPlayer)) ric$unseenTicks = 0;
            else ric$unseenTicks++;
            if (ric$unseenTicks <= 60) return;
        }

        ric$unseenTicks = 0;
        ServerPlayer best = RicMobTargeting.findNearest(mob);
        if (best != current) {
            if (mob instanceof NeutralMob anger) anger.stopBeingAngry();
            RicMobTargeting.setTarget(mob, best);
            if (best != null) ric$setAngerTarget(mob, best);
        } else if (best instanceof ServerPlayer bestPlayer) {
            RicMobTargeting.refreshBrainAttackMemory(mob, bestPlayer);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ric$recoverStalledLargeMob(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        boolean ravager = mob instanceof Ravager;
        boolean hoglinJockey = mob instanceof Hoglin && mob.entityTags().contains("ric_jockey") && mob.isVehicle();
        if (!ravager && !hoglinJockey) {
            ric$lastRecoveryPosition = null;
            ric$stalledTicks = 0;
            return;
        }
        if (mob.tickCount % 5 != 0) return;

        if (!(mob.getTarget() instanceof ServerPlayer target)
                || !RicMobTargeting.isEligible(mob, target)
                || !RicMobTargeting.inRange(mob, target)
                || mob.distanceToSqr(target) <= 36.0) {
            ric$lastRecoveryPosition = mob.position();
            ric$stalledTicks = 0;
            return;
        }

        Vec3 position = mob.position();
        if (ric$lastRecoveryPosition != null
                && position.distanceToSqr(ric$lastRecoveryPosition) < 0.0625) {
            ric$stalledTicks += 5;
        } else {
            ric$stalledTicks = 0;
        }
        ric$lastRecoveryPosition = position;
        if (ric$stalledTicks < 20) return;

        // Don't shove a Ravager jockey out of a useful crossbow firing position.
        if (ravager && hasVisibleCrossbowRider(mob, target)) {
            ric$stalledTicks = 0;
            return;
        }

        if (mob.getNavigation().isDone() || mob.getNavigation().isStuck()) {
            mob.getNavigation().stop();
            mob.getNavigation().moveTo(target, hoglinJockey ? 1.2 : 1.0);
            ric$stalledTicks = 0;
        }
    }

    @Unique
    private static boolean hasVisibleCrossbowRider(Mob mount, ServerPlayer target) {
        for (net.minecraft.world.entity.Entity passenger : mount.getPassengers()) {
            if (passenger instanceof Pillager pillager && pillager.isHolding(Items.CROSSBOW)
                    && pillager.distanceToSqr(target) <= 256.0
                    && pillager.getSensing().hasLineOfSight(target)) return true;
        }
        return false;
    }

    private static void ric$setAngerTarget(Mob mob, ServerPlayer target) {
        if (!(mob instanceof NeutralMob anger)) return;
        anger.setLastHurtByMob(target);
        anger.setPersistentAngerTarget(EntityReference.of(target.getUUID()));
        anger.startPersistentAngerTimer();
    }
}
