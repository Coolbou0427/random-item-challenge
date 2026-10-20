package jaxon.ric.mixin.client;

import jaxon.ric.client.DropsMenuClientPresentation;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenTooltipMixin {
    private static final Pattern RIC_BUNDLE_FULLNESS = Pattern.compile(
            ".*\\d+(?:\\.\\d+)?\\s*/\\s*64\\b.*\\d+(?:\\.\\d+)?\\s*%.*");

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void ric$hideBundleFullnessText(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        DropsMenuClientPresentation.updateActiveTooltip(screen, stack);
        if (!stack.is(Items.BUNDLE) || !DropsMenuClientPresentation.isDropsMenuScreen(screen)) return;

        List<Component> lines = cir.getReturnValue();
        List<Component> filtered = new ArrayList<>(lines.size());
        boolean removedFullness = false;
        for (Component line : lines) {
            if (RIC_BUNDLE_FULLNESS.matcher(line.getString()).matches()) {
                removedFullness = true;
            } else {
                filtered.add(line);
            }
        }
        if (removedFullness) cir.setReturnValue(filtered);
    }

}
