package jaxon.ric.mixin.client;

import jaxon.ric.client.DropsMenuClientPresentation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Removes the fullness row after vanilla has converted the tooltip to render components. */
@Mixin(GuiGraphicsExtractor.class)
abstract class ClientTooltipLayoutMixin {
    private static final Pattern RIC_BUNDLE_FULLNESS = Pattern.compile(
            ".*\\d+(?:\\.\\d+)?\\s*/\\s*64\\b.*\\d+(?:\\.\\d+)?\\s*%.*");

    @ModifyVariable(method = "tooltip", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<ClientTooltipComponent> ric$removeFullnessTextBeforeLayout(
            List<ClientTooltipComponent> components) {
        if (!DropsMenuClientPresentation.isDropsMenuOpen()
                && !DropsMenuClientPresentation.isActiveDropsTooltip()) {
            return components;
        }

        List<ClientTooltipComponent> filtered = new ArrayList<>(components.size());
        boolean removed = false;
        for (ClientTooltipComponent component : components) {
            if (component instanceof ClientTextTooltip textTooltip
                    && hasBundleFullnessText(((ClientTextTooltipAccessor) component).ric$getText())) {
                removed = true;
            } else {
                filtered.add(component);
            }
        }
        return removed ? filtered : components;
    }

    private static boolean hasBundleFullnessText(FormattedCharSequence text) {
        StringBuilder plainText = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            plainText.appendCodePoint(codePoint);
            return true;
        });
        return RIC_BUNDLE_FULLNESS.matcher(plainText).matches();
    }
}
