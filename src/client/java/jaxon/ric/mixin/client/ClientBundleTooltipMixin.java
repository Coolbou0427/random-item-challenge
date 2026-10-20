package jaxon.ric.mixin.client;

import jaxon.ric.client.DropsMenuClientPresentation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBundleTooltip.class)
abstract class ClientBundleTooltipMixin {
    @Shadow @Final private BundleContents contents;

    @Inject(method = "extractProgressbar", at = @At("HEAD"), cancellable = true)
    private static void ric$hideFullnessBarAndPercentageInDropsMenu(
            int x, int y, Font font, GuiGraphicsExtractor graphics, Fraction fullness, CallbackInfo ci) {
        if (DropsMenuClientPresentation.isActiveDropsTooltip()) ci.cancel();
    }

    @Inject(method = "getHeight", at = @At("RETURN"), cancellable = true)
    private void ric$removeHiddenFullnessSpace(Font font, CallbackInfoReturnable<Integer> cir) {
        if (contents != null && !contents.isEmpty()
                && DropsMenuClientPresentation.isActiveDropsBundleTooltip(contents)) {
            // ClientBundleTooltip reserves 13px for the bar plus 8px vertical margin.
            cir.setReturnValue(Math.max(0, cir.getReturnValue() - 21));
        }
    }
}
