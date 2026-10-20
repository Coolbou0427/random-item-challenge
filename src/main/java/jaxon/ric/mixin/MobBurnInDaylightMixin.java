package jaxon.ric.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public class MobBurnInDaylightMixin {

    @Inject(method = "burnUndead", at = @At("HEAD"), cancellable = true)
    private void ric$cancelSunBurn(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.entityTags().contains("ric_no_burn")) return;
        ci.cancel();
    }
}
