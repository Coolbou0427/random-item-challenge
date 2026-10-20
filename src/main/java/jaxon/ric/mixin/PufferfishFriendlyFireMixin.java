package jaxon.ric.mixin;

import jaxon.ric.command.GoCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Pufferfish.class)
public abstract class PufferfishFriendlyFireMixin {
    @Redirect(
            method = {"playerTouch", "touch"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/fish/Pufferfish;doTeamsAllowDamage(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean ric$useRicFriendlyFireForPufferfish(Pufferfish fish, Entity target) {
        if (GoCommand.enableMobFriendlyFire && target instanceof Player) return true;
        return fish.doTeamsAllowDamage(target);
    }
}
