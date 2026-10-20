package jaxon.ric.mixin;

import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Wolf.class)
public class WolfGuardMixin {
    private static final double GUARD_RADIUS_SQUARED = 16.0D * 16.0D;

    @Inject(method = "tick", at = @At("TAIL"))
    private void ric$guardNearbyOpponents(CallbackInfo ci) {
        Wolf wolf = (Wolf) (Object) this;
        if (!StopCommand.isRunning || !wolf.entityTags().contains("ric_spawn_egg_handled")) return;
        if (!wolf.isTame() || wolf.isInSittingPose() || wolf.tickCount % 20 != 0) return;
        if (!(wolf.level() instanceof ServerLevel world)) return;

        LivingEntity owner = wolf.getOwner();
        if (!(owner instanceof ServerPlayer ownerPlayer) || ownerPlayer.isSpectator()) return;

        if (wolf.getTarget() != null && wolf.distanceToSqr(ownerPlayer) > GUARD_RADIUS_SQUARED) {
            wolf.setTarget(null);
        }
        if (wolf.getTarget() != null) return;

        PlayerTeam ownerTeam = ownerPlayer.getTeam();
        ServerPlayer target = null;
        double closest = GUARD_RADIUS_SQUARED;
        for (ServerPlayer candidate : world.getServer().getPlayerList().getPlayers()) {
            if (candidate == ownerPlayer || candidate.isSpectator() || candidate.level() != world) continue;
            if (!GoCommand.enableMobFriendlyFire && ownerTeam != null && ownerTeam.equals(candidate.getTeam())) continue;

            double distance = ownerPlayer.distanceToSqr(candidate);
            if (distance < closest) {
                closest = distance;
                target = candidate;
            }
        }

        if (target != null) wolf.setTarget(target);
    }
}
