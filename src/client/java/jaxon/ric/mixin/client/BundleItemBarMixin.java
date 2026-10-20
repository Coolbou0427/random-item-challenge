package jaxon.ric.mixin.client;

import jaxon.ric.client.DropsMenuClientPresentation;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleItem.class)
abstract class BundleItemBarMixin {
    @Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
    private void ric$hideBundleInventoryBarInDropsMenu(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (DropsMenuClientPresentation.isDropsMenuOpen()) cir.setReturnValue(false);
    }
}
