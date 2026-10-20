package jaxon.ric;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class DirectEnchantingHooks {
    public static volatile boolean enabled;
    private static final Component DIRECT_ENCHANTING_LORE = Component.literal("Right-click to apply")
            .withStyle(style -> style.withColor(0x55FFFF).withItalic(false));
    private static final Component LEGACY_DIRECT_ENCHANTING_LORE = Component.literal("Click this book onto a compatible item to apply it.")
            .withStyle(style -> style.withColor(0x55FFFF).withItalic(false));

    private DirectEnchantingHooks() {
    }

    public static boolean isDirectEnchantBook(ItemStack book) {
        if (!enabled || book.isEmpty() || !book.is(Items.ENCHANTED_BOOK)) return false;
        ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
        return stored != null && !stored.isEmpty();
    }

    public static boolean tryApply(ItemStack book, ItemStack target, Player player) {
        if (!isDirectEnchantBook(book) || target.isEmpty()) return false;

        ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) return false;

        final boolean[] applied = {false};
        for (var entry : stored.entrySet()) {
            var enchantment = entry.getKey();
            int level = entry.getIntValue();
            if (!enchantment.value().canEnchant(target)) continue;
            int existing = target.getEnchantments().getLevel(enchantment);
            if (existing > 0 && level <= existing) continue;
            EnchantmentHelper.updateEnchantments(target, targetMutable -> {
                targetMutable.removeIf(other -> !Enchantment.areCompatible(enchantment, other));
                targetMutable.set(enchantment, Math.max(existing, level));
            });
            applied[0] = true;
            break;
        }
        return applied[0];
    }

    public static void ensureLore(ItemStack stack) {
        if (!enabled || stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) return;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) return;
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        boolean hasCurrentLore = lore.lines().contains(DIRECT_ENCHANTING_LORE);
        boolean hasOldBlank = false;
        for (int i = 0; i + 1 < lore.lines().size(); i++) {
            if (lore.lines().get(i).getString().isEmpty()
                    && lore.lines().get(i + 1).equals(DIRECT_ENCHANTING_LORE)) {
                hasOldBlank = true;
                break;
            }
        }
        if (hasCurrentLore && !hasOldBlank && !lore.lines().contains(LEGACY_DIRECT_ENCHANTING_LORE)) return;

        ItemLore updatedLore = ItemLore.EMPTY;
        for (int i = 0; i < lore.lines().size(); i++) {
            Component line = lore.lines().get(i);
            if (line.equals(LEGACY_DIRECT_ENCHANTING_LORE)) continue;
            if (line.getString().isEmpty() && i + 1 < lore.lines().size()
                    && lore.lines().get(i + 1).equals(DIRECT_ENCHANTING_LORE)) continue;
            updatedLore = updatedLore.withLineAdded(line);
        }
        if (!hasCurrentLore) updatedLore = updatedLore.withLineAdded(DIRECT_ENCHANTING_LORE);
        stack.set(DataComponents.LORE, updatedLore);
    }
}
