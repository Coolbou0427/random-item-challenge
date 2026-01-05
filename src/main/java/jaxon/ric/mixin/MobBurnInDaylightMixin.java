package jaxon.ric.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MobBurnInDaylightMixin {

    @Inject(method = "tickBurnInDaylight", at = @At("HEAD"), cancellable = true)
    private void ric$cancelSunBurn(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.getCommandTags().contains("ric_no_burn")) return;
        ci.cancel();
    }
}
