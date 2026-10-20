package jaxon.ric.mixin;

import jaxon.ric.ai.RicMobTargeting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.WeakHashMap;

@Mixin(PiglinAi.class)
public abstract class RicPiglinAiMixin {
    @Unique private static final WeakHashMap<Entity, Long> ric$activitySoundTimes = new WeakHashMap<>();

    @Inject(method = "findNearestValidAttackTarget", at = @At("HEAD"), cancellable = true)
    private static void ric$useSharedTargetRules(ServerLevel level, Piglin piglin,
            CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        if (!RicMobTargeting.isControlled(piglin)) return;
        // The native fight activity asks this again every tick. Use the same
        // eligibility/range as acquisition, including players behind cover,
        // so it does not erase and immediately reacquire RIC's target.
        if (piglin.getTarget() instanceof ServerPlayer player
                && RicMobTargeting.isEligible(piglin, player) && RicMobTargeting.inRange(piglin, player)) {
            cir.setReturnValue(Optional.of(player));
        } else {
            cir.setReturnValue(Optional.ofNullable(RicMobTargeting.findNearest(piglin)));
        }
    }

    @Redirect(method = "updateActivity", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;getSoundForCurrentActivity(Lnet/minecraft/world/entity/monster/piglin/Piglin;)Ljava/util/Optional;"))
    private static Optional<SoundEvent> ric$spaceActivityVoices(Piglin piglin) {
        Optional<SoundEvent> sound = PiglinAi.getSoundForCurrentActivity(piglin);
        if (!RicMobTargeting.isControlled(piglin) || sound.isEmpty()) return sound;
        // Share the limit between boat riders. Only activity-transition voices
        // are limited; ambient, hurt, death and crossbow sounds stay normal.
        Entity source = piglin.isPassenger() ? piglin.getVehicle() : piglin;
        long now = piglin.level().getGameTime();
        Long previous = ric$activitySoundTimes.get(source);
        if (previous != null && now - previous >= 0 && now - previous < 40) return Optional.empty();
        ric$activitySoundTimes.put(source, now);
        return sound;
    }
}
