package jaxon.ric.mixin;

import jaxon.ric.RicMobCleanup;
import jaxon.ric.command.GoCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class RicMobFriendlyFireMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void ric$protectMobTeammates(ServerLevel level, DamageSource source, float damage,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!GoCommand.enableMobFriendlyFire && source.getEntity() instanceof Mob attacker
                && (attacker.entityTags().contains(RicMobCleanup.PLACED_TAG)
                    || attacker.entityTags().contains("ric_spawn_egg_handled")
                    || attacker.entityTags().contains("ric_jockey"))
                && attacker.isAlliedTo((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }
}
