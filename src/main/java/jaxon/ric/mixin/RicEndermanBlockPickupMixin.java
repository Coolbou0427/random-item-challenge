package jaxon.ric.mixin;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.monster.Enderman")
public abstract class RicEndermanBlockPickupMixin {
    @Inject(method = "setCarriedBlock", at = @At("HEAD"), cancellable = true)
    private void ric$preventBlockPickup(BlockState blockState, CallbackInfo ci) {
        if (blockState != null
                && ((net.minecraft.world.entity.Entity) (Object) this).entityTags().contains("ric_retarget_players")) {
            ci.cancel();
        }
    }
}
