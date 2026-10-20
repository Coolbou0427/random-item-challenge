package jaxon.ric.mixin;

import jaxon.ric.RicMobCleanup;
import jaxon.ric.command.GoCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class RicMobAllyMixin {
    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void ric$allowRicMobsToTargetTeammates(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (!GoCommand.enableMobFriendlyFire || !((Object) this instanceof Mob mob) || !(other instanceof Player)) return;

        if (mob.entityTags().contains(RicMobCleanup.PLACED_TAG)
                || mob.entityTags().contains("ric_spawn_egg_handled")
                || mob.entityTags().contains("ric_jockey")) {
            cir.setReturnValue(false);
        }
    }
}
