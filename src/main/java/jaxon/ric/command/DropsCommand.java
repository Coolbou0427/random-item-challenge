package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import jaxon.ric.Items;
import jaxon.ric.Gamerz;
import jaxon.ric.Random_Item_Challenge;
import jaxon.ric.WeightedItem;
import jaxon.ric.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Opens a paged preview of the active weighted drop configuration. */
public final class DropsCommand {
    private static final int PAGE_SIZE = 45;
    private static final int INVENTORY_SIZE = 54;
    private static final int PREVIOUS_SLOT = 45;
    private static final int CLEAR_SLOT = 46;
    private static final int RESET_SLOT = 47;
    private static final int NEXT_SLOT = 53;
    private static final int INFO_SLOT = 49;
    private static final int EDIT_SLOT = 48;
    private static final int VIEW_SLOT = 50;
    private static final int DEFAULT_WEIGHT = 10;
    private static final String EMPTY_SLOT_PREFIX = "__ric_internal_empty_slot";
    private static final String EMPTY_SLOT_MARKER = EMPTY_SLOT_PREFIX + "__";
    private static final String PRIORITY_EMPTY_SLOT_MARKER = EMPTY_SLOT_PREFIX + "_priority__";
    private static final String ARMOR_SET_EMPTY_SLOT_PREFIX = EMPTY_SLOT_PREFIX + "_armor_set_";
    private static final String ARMOR_OTHER_EMPTY_SLOT_MARKER = EMPTY_SLOT_PREFIX + "_armor_other__";
    private static final String ARMOR_BUNDLE_EMPTY_SLOT_MARKER = EMPTY_SLOT_PREFIX + "_armor_bundle__";
    private static final Map<UUID, Boolean> CATEGORY_VIEW_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Pattern TOOL_ID = Pattern.compile("(?:minecraft:)?(wooden|stone|golden|copper|iron|diamond|netherite)_(sword|pickaxe|axe|shovel|hoe|spear)(?:$|\\[)");
    private static final Pattern ARMOR_SET_ID = Pattern.compile("(?:minecraft:)?(leather|chainmail|iron|copper|golden|diamond|netherite)_(helmet|chestplate|leggings|boots)(?:$|\\[)");
    private static final Map<String, Integer> ARMOR_MATERIAL_ORDER = Map.of(
            "leather", 0, "chainmail", 1, "iron", 2, "copper", 3, "golden", 4, "diamond", 5, "netherite", 6);
    private static final Map<String, Integer> ARMOR_PIECE_ORDER = Map.of(
            "helmet", 0, "chestplate", 1, "leggings", 2, "boots", 3);
    private static final Set<String> TOOL_WEAPON_IDS = Set.of(
            "trident", "mace", "bow", "crossbow", "fishing_rod", "flint_and_steel", "shears", "brush",
            "carrot_on_a_stick", "warped_fungus_on_a_stick");
    private static final List<String> CATEGORY_ORDER = List.of(
            "Tools & Weapons", "Food & Supplies", "Armor", "Resources", "Building Blocks",
            "Potions & Enchanted Books", "Mob Eggs", "Miscellaneous");
    private static final Set<String> RESOURCE_IDS = Set.of(
            "diamond", "iron_ingot", "gold_ingot", "copper_ingot", "netherite_ingot", "quartz",
            "emerald", "lapis_lazuli", "redstone", "coal", "charcoal", "raw_iron", "raw_gold",
            "raw_copper", "amethyst_shard", "stick", "flint", "string", "leather", "bone", "scaffolding",
            "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
            "mangrove_log", "cherry_log", "pale_oak_log", "oak_wood", "spruce_wood", "birch_wood",
            "jungle_wood", "acacia_wood", "dark_oak_wood", "mangrove_wood", "cherry_wood",
            "pale_oak_wood", "crimson_hyphae", "warped_hyphae", "bamboo_block", "bamboo_planks",
            "white_bed", "orange_bed", "magenta_bed", "light_blue_bed", "yellow_bed", "lime_bed", "pink_bed",
            "gray_bed", "light_gray_bed", "cyan_bed", "purple_bed", "blue_bed", "brown_bed", "green_bed",
            "red_bed", "black_bed",
            "oak_boat", "spruce_boat", "birch_boat", "jungle_boat", "acacia_boat", "dark_oak_boat",
            "mangrove_boat", "cherry_boat", "pale_oak_boat", "bamboo_raft", "oak_chest_boat",
            "spruce_chest_boat", "birch_chest_boat", "jungle_chest_boat", "acacia_chest_boat",
            "dark_oak_chest_boat", "mangrove_chest_boat", "cherry_chest_boat", "pale_oak_chest_boat",
            "bamboo_chest_raft");
    private static final Set<String> UTILITY_OR_SPECIAL_BLOCK_IDS = Set.of(
            "pointed_dripstone", "jukebox", "crying_obsidian", "respawn_anchor", "glowstone",
            "soul_soil", "wither_skeleton_skull", "wither_skeleton_wall_skull", "end_crystal",
            "powder_snow_bucket", "powder_snow", "turtle_egg", "dragon_egg", "spawner", "trial_spawner",
            "vault", "ominous_trial_spawner", "end_portal_frame", "nether_portal", "end_gateway",
            "ladder", "cobweb", "twisting_vines", "weeping_vines", "rail", "tnt_minecart",
            "minecart", "chest_minecart", "furnace_minecart", "hopper_minecart", "command_block_minecart",
            "sticky_piston", "lever",
            "oak_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door",
            "mangrove_door", "cherry_door", "pale_oak_door", "bamboo_door", "crimson_door", "warped_door",
            "iron_door", "copper_door", "exposed_copper_door", "weathered_copper_door", "oxidized_copper_door",
            "waxed_copper_door", "waxed_exposed_copper_door", "waxed_weathered_copper_door", "waxed_oxidized_copper_door",
            "white_bed", "orange_bed", "magenta_bed", "light_blue_bed", "yellow_bed", "lime_bed", "pink_bed",
            "gray_bed", "light_gray_bed", "cyan_bed", "purple_bed", "blue_bed", "brown_bed", "green_bed",
            "red_bed", "black_bed");
    private static final Set<String> FOOD_IDS = Set.of(
            "apple", "golden_apple", "enchanted_golden_apple", "golden_carrot", "sweet_berries",
            "glow_berries", "chorus_fruit", "rotten_flesh", "bread", "baked_potato", "carrot",
            "potato", "cooked_beef", "cooked_porkchop", "cooked_chicken", "cooked_mutton",
            "cooked_rabbit", "cooked_cod", "cooked_salmon", "cookie", "pumpkin_pie", "melon_slice",
            "honey_bottle", "milk_bucket", "totem_of_undying");
    private static final Set<String> SUPPLY_IDS = Set.of(
            "ender_pearl", "wind_charge", "water_bucket", "powder_snow_bucket", "lava_bucket",
            "pufferfish_bucket", "tnt_minecart", "vault", "sticky_piston");
    private static final Set<String> PAGE_TWO_UTILITY_IDS = Set.of(
            "pointed_dripstone", "twisting_vines", "cobweb", "ladder", "fire_charge",
            "lava_bucket", "pufferfish_bucket", "tnt_minecart", "vault", "sticky_piston",
            "soul_sand", "wither_skeleton_skull");

    private DropsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ric")
                .then(Commands.literal("settings")
                        .then(Commands.literal("drops")
                        .requires(source -> source.permissions().hasPermission(
                                new net.minecraft.server.permissions.Permission.HasCommandLevel(
                                        net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS)))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) {
                                context.getSource().sendFailure(Component.literal("This command can only be used by a player."));
                                return 0;
                            }
                            try {
                                openPage(player, 0);
                                return 1;
                            } catch (RuntimeException exception) {
                                Random_Item_Challenge.LOGGER.error(
                                        "Could not open configured drops menu for {} (match running: {})",
                                        player.getName().getString(), StopCommand.isRunning, exception);
                                context.getSource().sendFailure(Component.literal(
                                        "Could not open the drops menu. The server log has the error details.")
                                        .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                        }))));
    }

    private static void openPage(ServerPlayer player, int requestedPage) {
        List<WeightedItem> drops = orderedDrops();
        int pageCount = pageCount(drops.size());
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        SimpleContainer container = new SimpleContainer(INVENTORY_SIZE);

        final int shownPage = page;
        boolean categoryMode = CATEGORY_VIEW_BY_PLAYER.getOrDefault(player.getUUID(), true);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new DropsMenu(containerId, inventory, container, player, drops,
                        shownPage, categoryMode),
                Component.literal("Configured Drops")));
    }

    private static int pageCount(int dropCount) {
        return Math.max(1, (dropCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private static List<WeightedItem> orderedDrops() {
        return Items.itemsAndNumbers == null ? new ArrayList<>() : new ArrayList<>(Items.itemsAndNumbers);
    }

    private static String categoryFor(WeightedItem drop) {
        if (drop != null && drop.categoryOverride() != null && CATEGORY_ORDER.contains(drop.categoryOverride())) {
            return drop.categoryOverride();
        }
        if (drop == null || drop.items() == null || drop.items().isEmpty()) return "Miscellaneous";
        List<String> ids = drop.items().stream().filter(java.util.Objects::nonNull)
                .map(raw -> configuredItemId(raw).toLowerCase(Locale.ROOT)).toList();
        if (ids.stream().anyMatch(id -> id.endsWith("_spawn_egg"))) return "Mob Eggs";
        if (isPortalDrop(ids) || ids.stream().anyMatch(PAGE_TWO_UTILITY_IDS::contains)) return "Food & Supplies";
        if (ids.stream().anyMatch(DropsCommand::isToolOrWeapon)) return "Tools & Weapons";
        if (ids.stream().anyMatch(DropsCommand::isArmor)) return "Armor";
        if (ids.stream().anyMatch(DropsCommand::isPotion)) return "Potions & Enchanted Books";
        if (ids.stream().anyMatch("enchanted_book"::equals)
                || drop.items().stream().filter(java.util.Objects::nonNull)
                .anyMatch(raw -> raw.contains("stored_enchantments"))) return "Potions & Enchanted Books";
        if (ids.stream().anyMatch(RESOURCE_IDS::contains)) return "Resources";
        if (ids.stream().anyMatch(id -> FOOD_IDS.contains(id) || SUPPLY_IDS.contains(id))) return "Food & Supplies";
        if (ids.stream().anyMatch(DropsCommand::isBlockItem)) return "Building Blocks";
        return "Miscellaneous";
    }

    private static boolean isPageTwoUtilityDrop(WeightedItem drop) {
        if (drop == null || drop.items() == null) return false;
        if (drop.items().contains(PRIORITY_EMPTY_SLOT_MARKER)) return true;
        if (drop.items().stream().anyMatch(item -> item != null && item.startsWith(EMPTY_SLOT_PREFIX))) return false;
        List<String> ids = drop.items().stream().filter(java.util.Objects::nonNull)
                .map(DropsCommand::configuredItemId).map(id -> id.toLowerCase(Locale.ROOT)).toList();
        return isPortalDrop(ids) || ids.stream().anyMatch(PAGE_TWO_UTILITY_IDS::contains);
    }

    private static boolean isPortalDrop(List<String> ids) {
        boolean netherPortal = ids.contains("obsidian") && ids.contains("flint_and_steel");
        boolean endPortal = ids.contains("end_portal_frame")
                && (ids.contains("ender_eye") || ids.contains("eye_of_ender"));
        return netherPortal || endPortal;
    }

    private static boolean isToolOrWeapon(String id) {
        return TOOL_ID.matcher(id).matches() || TOOL_WEAPON_IDS.contains(id);
    }

    private static boolean isArmor(String id) {
        return id.endsWith("_helmet") || id.endsWith("_chestplate") || id.endsWith("_leggings")
                || id.endsWith("_boots") || Set.of("elytra", "wolf_armor", "shield").contains(id);
    }

    private static boolean isPotion(String id) {
        return Set.of("potion", "splash_potion", "lingering_potion", "tipped_arrow").contains(id);
    }

    private static boolean isBlockItem(String id) {
        return !UTILITY_OR_SPECIAL_BLOCK_IDS.contains(id)
                && BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id)) instanceof BlockItem;
    }

    private static void populatePage(SimpleContainer container, List<DisplayDrop> pageEntries, int page,
                                     int pageCount, int totalDrops, String pageLabel,
                                     boolean editMode, boolean categoryMode) {
        container.clearContent();
        for (int index = 0; index < pageEntries.size(); index++) {
            ItemStack display = pageEntries.get(index).preview().displayStack();
            if (!display.isEmpty()) container.setItem(index, display);
        }
        if (page > 0) container.setItem(PREVIOUS_SLOT, button(net.minecraft.world.item.Items.ARROW, "Previous page"));
        if (page + 1 < pageCount) container.setItem(NEXT_SLOT, button(net.minecraft.world.item.Items.ARROW, "Next page"));
        if (editMode) {
            ItemStack clearButton = new ItemStack(net.minecraft.world.item.Items.TNT);
            clearButton.set(DataComponents.CUSTOM_NAME, Component.literal("Clear all drops")
                    .withStyle(ChatFormatting.RED));
            clearButton.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                    Component.literal("Remove every drop from this draft.").withStyle(ChatFormatting.GRAY),
                    Component.literal("Finish editing to save; Reset drops to undo.").withStyle(ChatFormatting.GRAY))));
            container.setItem(CLEAR_SLOT, clearButton);

            ItemStack resetButton = new ItemStack(net.minecraft.world.item.Items.BARRIER);
            resetButton.set(DataComponents.CUSTOM_NAME, Component.literal("Reset drops")
                    .withStyle(ChatFormatting.RED));
            resetButton.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                    Component.literal("Restore the built-in mod default drops.").withStyle(ChatFormatting.GRAY),
                    Component.literal("Finish editing to save the defaults.").withStyle(ChatFormatting.GRAY))));
            container.setItem(RESET_SLOT, resetButton);
        }
        ItemStack editButton = new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
        editButton.set(DataComponents.CUSTOM_NAME, Component.literal(editMode ? "Finish editing" : "Edit drops")
                .withStyle(editMode ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        editButton.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                Component.literal(editMode ? "Click to return to preview mode." : "Click to add or rearrange drops.")
                        .withStyle(ChatFormatting.GRAY),
                Component.literal("Changes are saved to the config immediately.").withStyle(ChatFormatting.GRAY))));
        container.setItem(EDIT_SLOT, editButton);
        ItemStack viewButton = new ItemStack(net.minecraft.world.item.Items.COMPASS);
        viewButton.set(DataComponents.CUSTOM_NAME, Component.literal(categoryMode ? "View: Categories" : "View: List")
                .withStyle(ChatFormatting.AQUA));
        viewButton.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                Component.literal(categoryMode ? "Click to show drops in config order." : "Click to group drops by category.")
                        .withStyle(ChatFormatting.GRAY))));
        container.setItem(VIEW_SLOT, viewButton);
        ItemStack info = new ItemStack(net.minecraft.world.item.Items.PAPER);
        info.set(DataComponents.CUSTOM_NAME, Component.literal("Page " + (page + 1) + " / " + pageCount)
                .withStyle(ChatFormatting.YELLOW));
        List<Component> infoLines = new ArrayList<>();
        infoLines.add(Component.literal(totalDrops + " configured drops").withStyle(ChatFormatting.GRAY));
        infoLines.add(Component.literal(pageLabel).withStyle(ChatFormatting.GOLD));
        infoLines.add(Component.literal(editMode
                ? "Select a drop, then click a destination to move it."
                : "Take the item or bundle to inspect it.").withStyle(ChatFormatting.GRAY));
        if (editMode) {
            infoLines.add(Component.literal("Left-click a destination to swap the selected drop.")
                    .withStyle(ChatFormatting.GRAY));
            infoLines.add(Component.literal("Right-click a bundle drop to merge the selected drop into it.")
                    .withStyle(ChatFormatting.GRAY));
            infoLines.add(Component.literal("Items or bundles from your inventory add to a clicked drop.")
                    .withStyle(ChatFormatting.GRAY));
        }
        info.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(infoLines));
        container.setItem(INFO_SLOT, info);
    }

    private static ItemStack button(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(ChatFormatting.GREEN));
        return stack;
    }

    private static PreviewEntry makePreviewEntry(WeightedItem drop, ServerPlayer player, int dropNumber) {
        if (DropsMenu.isEmptyDrop(drop)) return new PreviewEntry(ItemStack.EMPTY, ItemStack.EMPTY);
        try {
            List<ItemStack> configuredStacks = new ArrayList<>();
            boolean hasAnvil = drop.items().stream().filter(java.util.Objects::nonNull)
                    .map(DropsCommand::configuredItemId).anyMatch("anvil"::equals);
            boolean hasExperienceBottles = drop.items().stream().filter(java.util.Objects::nonNull)
                    .map(DropsCommand::configuredItemId).anyMatch("experience_bottle"::equals);
            boolean hideDirectEnchantingSupplies = hasAnvil && hasExperienceBottles;
            boolean hasTnt = drop.items().stream().filter(java.util.Objects::nonNull)
                    .map(DropsCommand::configuredItemId).anyMatch("tnt"::equals);
            boolean hasFlintAndSteel = drop.items().stream().filter(java.util.Objects::nonNull)
                    .map(DropsCommand::configuredItemId).anyMatch("flint_and_steel"::equals);
            boolean hideAutoIgniteFlintAndSteel = GoCommand.enableTntAutoExplode && hasTnt && hasFlintAndSteel;
            for (String raw : drop.items()) {
                if (raw == null || raw.isBlank()) continue;
                String itemSpec = raw.trim();
                int count = 1;
                int lastSpace = itemSpec.lastIndexOf(' ');
                if (lastSpace > 0) {
                    try {
                        count = Integer.parseInt(itemSpec.substring(lastSpace + 1).trim());
                        itemSpec = itemSpec.substring(0, lastSpace).trim();
                    } catch (NumberFormatException ignored) {
                        // Config item arguments without an explicit count default to one, as /give does.
                    }
                }
                if (count <= 0) continue;
                String itemId = configuredItemId(itemSpec);
                if (hideDirectEnchantingSupplies && (itemId.equals("anvil") || itemId.equals("experience_bottle"))) {
                    continue;
                }
                if (hideAutoIgniteFlintAndSteel && itemId.equals("flint_and_steel")) continue;

                ItemInput parsed = new ItemParser(player.registryAccess()).parse(new StringReader(itemSpec));
                ItemStack prototype = parsed.createItemStack(1);
                int maxStackSize = prototype.getItem().getDefaultMaxStackSize();
                int remaining = count;
                while (remaining > 0) {
                    int inStack = Math.min(remaining, maxStackSize);
                    configuredStacks.add(prototype.copyWithCount(inStack));
                    remaining -= inStack;
                }
            }

            if (configuredStacks.isEmpty()) return new PreviewEntry(ItemStack.EMPTY, ItemStack.EMPTY);

            ItemStack takeStack;
            if (configuredStacks.size() == 1) {
                takeStack = configuredStacks.getFirst();
            } else {
                List<ItemStackTemplate> contents = configuredStacks.stream()
                        .map(ItemStackTemplate::fromNonEmptyStack)
                        .toList();
                takeStack = new ItemStack(net.minecraft.world.item.Items.BUNDLE);
                takeStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(contents));
            }

            ItemStack displayStack = takeStack.copy();
            if (takeStack.is(net.minecraft.world.item.Items.FIRE_CHARGE)) {
                setFireballPresentation(takeStack);
                setFireballPresentation(displayStack);
            }
            if (takeStack.is(net.minecraft.world.item.Items.BUNDLE)) {
                displayStack.set(DataComponents.CUSTOM_NAME, Component.literal("Drop #" + dropNumber)
                        .withStyle(ChatFormatting.AQUA));
                List<ItemStackTemplate> displayContents = configuredStacks.stream().map(ItemStack::copy)
                        .map(stack -> {
                            if (stack.is(net.minecraft.world.item.Items.FIRE_CHARGE)) setFireballPresentation(stack);
                            return ItemStackTemplate.fromNonEmptyStack(stack);
                        }).toList();
                displayStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(displayContents));
            }
            List<Component> displayLore = new ArrayList<>();
            if (drop.items().stream().filter(java.util.Objects::nonNull)
                    .map(DropsCommand::configuredItemId).anyMatch("jukebox"::equals)) {
                displayLore.add(Component.literal("Includes a random music disc.")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (takeStack.is(net.minecraft.world.item.Items.BUNDLE)) {
                displayLore.add(Component.literal("Contents:").withStyle(ChatFormatting.GRAY));
                for (ItemStack content : summarizeContents(configuredStacks)) {
                    String itemName = content.is(net.minecraft.world.item.Items.FIRE_CHARGE)
                            ? "Fireball" : content.getHoverName().getString();
                    if (content.getCount() > 1) itemName += " ×" + content.getCount();
                    displayLore.add(Component.literal("• " + itemName).withStyle(ChatFormatting.GRAY));

                    Set<String> enchantments = new LinkedHashSet<>();
                    addEnchantments(enchantments, content.get(DataComponents.ENCHANTMENTS));
                    addEnchantments(enchantments, content.get(DataComponents.STORED_ENCHANTMENTS));
                    if (!enchantments.isEmpty()) {
                        displayLore.add(Component.literal("  Enchantments: " + String.join(", ", enchantments))
                                .withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            if (hideDirectEnchantingSupplies) {
                displayLore.add(Component.literal("+ Anvil and XP")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (hideAutoIgniteFlintAndSteel) {
                displayLore.add(Component.literal("+ Flint and steel if Auto-Ignite TNT is off")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (drop.weight() != DEFAULT_WEIGHT) {
                int percentage = (int) Math.round(drop.weight() * 100.0 / DEFAULT_WEIGHT);
                displayLore.add(Component.literal("Weight: " + percentage + "%")
                        .withStyle(ChatFormatting.GREEN));
            }
            if (!displayLore.isEmpty()) {
                net.minecraft.world.item.component.ItemLore combinedLore = displayStack.get(DataComponents.LORE);
                for (Component loreLine : displayLore) {
                    combinedLore = combinedLore == null
                            ? new net.minecraft.world.item.component.ItemLore(List.of(loreLine))
                            : combinedLore.withLineAdded(loreLine);
                }
                displayStack.set(DataComponents.LORE, combinedLore);
            }
            return new PreviewEntry(displayStack, takeStack);
        } catch (Exception exception) {
            Random_Item_Challenge.LOGGER.error("Could not build preview for configured drop {}", dropNumber, exception);
            ItemStack invalid = new ItemStack(net.minecraft.world.item.Items.BARRIER);
            invalid.set(DataComponents.CUSTOM_NAME, Component.literal("Invalid drop #" + dropNumber)
                    .withStyle(ChatFormatting.RED));
            return new PreviewEntry(invalid, ItemStack.EMPTY);
        }
    }

    private static void setFireballPresentation(ItemStack stack) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Fireball"));
        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                Component.literal("Right-click to launch.").withStyle(ChatFormatting.GRAY))));
    }

    private static String configuredItemId(String raw) {
        if (raw == null) return "";
        String itemSpec = raw.trim();
        int lastSpace = itemSpec.lastIndexOf(' ');
        if (lastSpace > 0) {
            try {
                Integer.parseInt(itemSpec.substring(lastSpace + 1).trim());
                itemSpec = itemSpec.substring(0, lastSpace).trim();
            } catch (NumberFormatException ignored) {
            }
        }
        int componentStart = itemSpec.indexOf('[');
        if (componentStart >= 0) itemSpec = itemSpec.substring(0, componentStart);
        int namespaceSeparator = itemSpec.lastIndexOf(':');
        if (namespaceSeparator >= 0) itemSpec = itemSpec.substring(namespaceSeparator + 1);
        return itemSpec;
    }

    private static List<ItemStack> summarizeContents(List<ItemStack> contents) {
        List<ItemStack> summary = new ArrayList<>();
        for (ItemStack stack : contents) {
            ItemStack existing = summary.stream()
                    .filter(candidate -> ItemStack.isSameItemSameComponents(candidate, stack))
                    .findFirst().orElse(null);
            if (existing == null) summary.add(stack.copy());
            else existing.grow(stack.getCount());
        }
        return summary;
    }

    private static void addEnchantments(Set<String> names, ItemEnchantments enchantments) {
        if (enchantments == null) return;
        for (var entry : enchantments.entrySet()) {
            names.add(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
        }
    }

    private static String serializeConfiguredStack(ItemStack stack, ServerPlayer player) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        StringBuilder result = new StringBuilder(itemId);
        DataComponentPatch patch = stack.getComponentsPatch();
        if (!patch.isEmpty()) {
            Tag encoded = DataComponentPatch.CODEC.encodeStart(
                    player.registryAccess().createSerializationContext(NbtOps.INSTANCE), patch).getOrThrow();
            if (!(encoded instanceof CompoundTag components)) {
                throw new IllegalStateException("Item components did not serialize as a compound");
            }
            if (!components.isEmpty()) {
                List<String> componentParts = new ArrayList<>();
                for (Map.Entry<String, Tag> entry : components.entrySet()) {
                    componentParts.add(entry.getKey() + "=" + entry.getValue());
                }
                result.append('[').append(String.join(",", componentParts)).append(']');
            }
        }
        return result.append(' ').append(stack.getCount()).toString();
    }

    private record PreviewEntry(ItemStack displayStack, ItemStack takeStack) {}
    private record DisplayDrop(int sourceIndex, PreviewEntry preview) {}
    private record DropPage(String label, List<Integer> sourceIndexes) {}
    private record QuickCraftEvent(int slot, int button) {}

    private static final class DropsMenu extends ChestMenu {
        private final SimpleContainer container;
        private final ServerPlayer player;
        private final List<WeightedItem> drops;
        private final List<DisplayDrop> pageEntries = new ArrayList<>();
        private final List<DropPage> pages = new ArrayList<>();
        private int page;
        private int pageCount;
        private boolean editMode;
        private boolean categoryMode;
        private boolean savedChangesDuringEdit;
        private WeightedItem heldDrop;
        private List<QuickCraftEvent> quickCraftEvents;
        /** Frozen list-view ordering for this edit session; only explicit cursor moves change visible positions. */
        private List<Integer> editListOrder;

        private DropsMenu(int containerId, Inventory playerInventory, SimpleContainer container,
                          ServerPlayer player, List<WeightedItem> drops, int page, boolean categoryMode) {
            super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
            this.container = container;
            this.player = player;
            this.drops = new ArrayList<>(drops);
            this.page = page;
            this.categoryMode = categoryMode;
            refreshPage();
        }

        @Override
        public void clicked(int slot, int button, ContainerInput clickType, net.minecraft.world.entity.player.Player player) {
            if (clickType == ContainerInput.QUICK_CRAFT) {
                handleQuickCraft(slot, button, player);
                return;
            }
            if (slot == PREVIOUS_SLOT) {
                if (page > 0) setPage(page - 1);
                return;
            }
            if (slot == NEXT_SLOT) {
                if (page + 1 < pageCount) setPage(page + 1);
                return;
            }
            if (slot == RESET_SLOT) {
                if (editMode) resetToModDefaults();
                return;
            }
            if (slot == CLEAR_SLOT) {
                if (editMode) clearAllDrops();
                return;
            }
            if (slot == EDIT_SLOT) {
                if (editMode && heldDrop != null) {
                    sendHeldDropMessage("Place or return the held drop before finishing editing.");
                    return;
                }
                boolean wasEditing = editMode;
                editMode = !editMode;
                if (wasEditing) {
                    commitChanges();
                    editListOrder = null;
                } else {
                    beginEditSession();
                }
                refreshPage();
                return;
            }
            if (slot == VIEW_SLOT) {
                if (heldDrop != null) {
                    sendHeldDropMessage("Place or return the held drop before changing views.");
                    return;
                }
                categoryMode = !categoryMode;
                CATEGORY_VIEW_BY_PLAYER.put(this.player.getUUID(), categoryMode);
                page = 0;
                refreshPage();
                return;
            }
            if (slot == INFO_SLOT || (slot >= PAGE_SIZE && slot < INVENTORY_SIZE)) return;

            // In the creative-style drops menu, shift-clicking an item from the
            // player's inventory is the trash action. Consume the whole stack
            // and use the same extinguish sound as dropping an item onto the grid.
            if (clickType == ContainerInput.QUICK_MOVE
                    && slot >= INVENTORY_SIZE && slot < this.slots.size()) {
                discardInventoryStack(slot);
                return;
            }

            if (slot >= 0 && slot < PAGE_SIZE) {
                if (clickType == ContainerInput.QUICK_MOVE) {
                    shiftClickDrop(slot, editMode);
                } else if (editMode) editDropSlot(slot, button, clickType);
                else if (clickType == ContainerInput.PICKUP && slot < pageEntries.size()) {
                    if (!getCarried().isEmpty()) {
                        setCarried(ItemStack.EMPTY);
                        this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                                SoundSource.PLAYERS, 0.4F, 1.6F);
                        return;
                    }
                    ItemStack takeStack = pageEntries.get(slot).preview().takeStack();
                    if (!takeStack.isEmpty()) setCarried(takeStack.copy());
                }
                return;
            }

            if (editMode && heldDrop != null && slot >= INVENTORY_SIZE) {
                super.clicked(slot, button, clickType, player);
                if (clickType == ContainerInput.PICKUP) {
                    heldDrop = null;
                    persistChanges();
                    refreshPage();
                }
                return;
            }
            if (editMode && heldDrop != null) return;
            super.clicked(slot, button, clickType, player);
        }

        private void discardInventoryStack(int slotId) {
            Slot inventorySlot = this.slots.get(slotId);
            if (!inventorySlot.hasItem()) return;
            inventorySlot.setByPlayer(ItemStack.EMPTY);
            inventorySlot.setChanged();
            this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.4F, 1.6F);
            broadcastChanges();
        }

        private void handleQuickCraft(int slot, int button, net.minecraft.world.entity.player.Player player) {
            int stage = button & 3;
            if (stage == 0) quickCraftEvents = new ArrayList<>();
            if (quickCraftEvents == null) return;
            quickCraftEvents.add(new QuickCraftEvent(slot, button));
            if (stage != 2) return;

            List<QuickCraftEvent> events = quickCraftEvents;
            quickCraftEvents = null;
            int destination = -1;
            for (QuickCraftEvent event : events) {
                if ((event.button() & 3) == 1) destination = event.slot();
            }
            if (destination >= 0 && destination < PAGE_SIZE) {
                if (editMode) {
                    int mouseButton = ((button >> 2) & 3) == 1 ? 1 : 0;
                    editDropSlot(destination, mouseButton, ContainerInput.PICKUP);
                }
                return;
            }

            if (destination >= INVENTORY_SIZE) {
                int mouseButton = ((button >> 2) & 3) == 1 ? 1 : 0;
                if (editMode && heldDrop != null) {
                    super.clicked(destination, mouseButton, ContainerInput.PICKUP, player);
                    if (getCarried().isEmpty()) {
                        heldDrop = null;
                        persistChanges();
                        refreshPage();
                    }
                    return;
                }
                // Preserve vanilla drag behavior within the player's inventory.
                if (events.stream().allMatch(event -> event.slot() == -999
                        || event.slot() >= INVENTORY_SIZE)) {
                    for (QuickCraftEvent event : events) {
                        super.clicked(event.slot(), event.button(), ContainerInput.QUICK_CRAFT, player);
                    }
                } else if (!editMode && !getCarried().isEmpty()) {
                    // A drag that crosses the preview grid and ends in the player inventory
                    // returns the carried item to the final inventory slot.
                    super.clicked(destination, mouseButton, ContainerInput.PICKUP, player);
                }
            }
        }

        @Override
        public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slotId) {
            return ItemStack.EMPTY;
        }

        private void shiftClickDrop(int pageSlot, boolean removeFromDraft) {
            if (editMode && heldDrop != null) return;
            if (pageSlot < 0 || pageSlot >= pageEntries.size()) return;
            DisplayDrop displayDrop = pageEntries.get(pageSlot);
            ItemStack takeStack = displayDrop.preview().takeStack();
            if (takeStack.isEmpty()) return;

            ItemStack inventoryCopy = takeStack.copy();
            // Match a vanilla chest shift-click: transfer into the player slots
            // in reverse slot order, starting from the right side of the inventory.
            moveItemStackTo(inventoryCopy, 54, this.slots.size(), true);
            boolean fullyAdded = inventoryCopy.isEmpty();
            this.player.inventoryMenu.broadcastChanges();
            if (!removeFromDraft || !fullyAdded) return;

            int removedIndex = displayDrop.sourceIndex();
            if (removedIndex < 0 || removedIndex >= drops.size()) return;
            vacateDropSlot(removedIndex);
            persistChanges();
            refreshPage();
        }

        @Override
        public void removed(net.minecraft.world.entity.player.Player player) {
            super.removed(player);
        }

        private void editDropSlot(int pageSlot, int button, ContainerInput clickType) {
            if (clickType != ContainerInput.PICKUP) return;
            int targetIndex = pageSlot < pageEntries.size()
                    ? pageEntries.get(pageSlot).sourceIndex() : -1;

            if (heldDrop != null) {
                boolean heldBundle = isBundleDrop(heldDrop, -1);
                if (heldBundle && button == 0 && targetIndex >= 0 && targetIndex < drops.size()
                        && !isEmptyDrop(drops.get(targetIndex))) {
                    insertDropIntoHeldBundle(targetIndex);
                    return;
                }
                // Match vanilla bundle clicks: left-click tries to insert the
                // clicked stack into the cursor bundle; right-click falls
                // through to the normal swap behavior.
                placeHeldDrop(pageSlot, targetIndex);
                return;
            }

            ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                // Inventory-carried items do not represent an existing configured
                // drop, so they cannot swap with the target drop. Keep left-click
                // on a bundle inert and reserve bundle merging for right-click.
                if (button != 1 && targetIndex >= 0 && targetIndex < drops.size()
                        && isBundleDrop(drops.get(targetIndex), targetIndex)) return;
                int amount = button == 1 ? 1 : carried.getCount();
                ItemStack added = carried.copyWithCount(amount);
                try {
                    List<String> configuredStacks = new ArrayList<>();
                    flattenBundleContents(added, configuredStacks, 0);
                    if (configuredStacks.isEmpty()) return;
                    int addedDropIndex;
                    if (targetIndex >= 0 && targetIndex < drops.size()) {
                        WeightedItem existing = drops.get(targetIndex);
                        String categoryOverride = categoryAtListPosition(pageSlot, targetIndex);
                        if (isEmptyDrop(existing)) {
                            drops.set(targetIndex, new WeightedItem(configuredStacks, DEFAULT_WEIGHT, categoryOverride));
                        } else {
                            List<String> combined = new ArrayList<>(existing.items());
                            combined.addAll(configuredStacks);
                            drops.set(targetIndex, new WeightedItem(combined, existing.weight(), categoryOverride));
                        }
                        addedDropIndex = targetIndex;
                    } else {
                        addedDropIndex = insertionIndexForPageSlot(pageSlot);
                        insertDropAt(addedDropIndex, new WeightedItem(configuredStacks, DEFAULT_WEIGHT,
                                categoryAtListPosition(pageSlot, targetIndex)), pageSlot);
                    }
                    carried.shrink(amount);
                    setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                    persistChanges();
                    refreshPage();
                    setPage(pageForSourceIndex(addedDropIndex));
                } catch (Exception exception) {
                    Random_Item_Challenge.LOGGER.error("Could not add item to configured drops", exception);
                this.player.sendSystemMessage(Component.literal("Could not save that item's components; it was not added.")
                        .withStyle(ChatFormatting.RED));
                }
                return;
            }

            if (targetIndex >= 0 && targetIndex < drops.size() && !isEmptyDrop(drops.get(targetIndex))) {
                WeightedItem selectedDrop = drops.get(targetIndex);
                ItemStack selected = pageEntries.get(pageSlot).preview().takeStack();
                if (selected.isEmpty()) return;
                heldDrop = selectedDrop;
                vacateDropSlot(targetIndex);
                setCarried(selected.copy());
                persistChanges();
                refreshPage();
            }
        }

        private boolean isBundleDrop(WeightedItem drop, int sourceIndex) {
            if (isEmptyDrop(drop)) return false;
            return makePreviewEntry(drop, this.player, sourceIndex + 1).takeStack()
                    .is(net.minecraft.world.item.Items.BUNDLE);
        }

        private void insertDropIntoHeldBundle(int targetIndex) {
            if (targetIndex < 0 || targetIndex >= drops.size() || isEmptyDrop(drops.get(targetIndex))) return;
            ItemStack heldBundle = makePreviewEntry(heldDrop, this.player, 1).takeStack();
            if (!heldBundle.is(net.minecraft.world.item.Items.BUNDLE)) return;
            BundleContents currentContents = heldBundle.get(DataComponents.BUNDLE_CONTENTS);
            if (currentContents == null) currentContents = BundleContents.EMPTY;

            WeightedItem target = drops.get(targetIndex);
            ItemStack moving = makePreviewEntry(target, this.player, targetIndex + 1).takeStack();
            if (moving.isEmpty()) return;
            BundleContents.Mutable updatedContents = currentContents.asMutable();
            int inserted = updatedContents.tryInsert(moving);
            if (inserted <= 0) {
                this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.BUNDLE_INSERT_FAIL,
                        SoundSource.PLAYERS, 0.8F, 1.0F);
                return;
            }

            BundleContents finalContents = updatedContents.toImmutable();
            heldBundle.set(DataComponents.BUNDLE_CONTENTS, finalContents);
            List<String> heldItems = finalContents.itemCopies()
                    .map(stack -> serializeConfiguredStack(stack, this.player)).toList();
            heldDrop = new WeightedItem(heldItems, heldDrop.weight(), heldDrop.categoryOverride());

            if (moving.isEmpty()) {
                vacateDropSlot(targetIndex);
            } else {
                drops.set(targetIndex, new WeightedItem(List.of(serializeConfiguredStack(moving, this.player)),
                        target.weight(), categoryFor(target)));
            }
            setCarried(heldBundle);
            this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.BUNDLE_INSERT,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
            persistChanges();
            refreshPage();
            setCarried(heldBundle);
        }

        private void placeHeldDrop(int pageSlot, int targetIndex) {
            WeightedItem moving = heldDrop;
            String destinationCategory = categoryAtListPosition(pageSlot, targetIndex);
            if (destinationCategory != null) moving = withCategory(moving, destinationCategory);

            if (targetIndex >= 0 && targetIndex < drops.size()) {
                WeightedItem target = drops.get(targetIndex);
                boolean targetIsEmpty = isEmptyDrop(target);
                drops.set(targetIndex, moving);
                if (targetIsEmpty) {
                    heldDrop = null;
                    setCarried(ItemStack.EMPTY);
                } else {
                    heldDrop = target;
                    ItemStack cursor = makePreviewEntry(target, this.player, targetIndex + 1).takeStack();
                    setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor.copy());
                }
            } else {
                int insertionIndex = insertionIndexForPageSlot(pageSlot);
                insertionIndex = Math.max(0, Math.min(insertionIndex, drops.size()));
                insertDropAt(insertionIndex, moving, pageSlot);
                heldDrop = null;
                setCarried(ItemStack.EMPTY);
            }
            persistChanges();
            refreshPage();
        }

        private void vacateDropSlot(int index) {
            if (index < 0 || index >= drops.size()) return;
            WeightedItem removed = drops.get(index);
            String category = categoryFor(removed);
            int armorOrder = "Armor".equals(category) ? armorSetOrder(removed) : -1;
            String marker;
            if (armorOrder >= 0) marker = ARMOR_SET_EMPTY_SLOT_PREFIX + armorOrder;
            else if ("Armor".equals(category) && isArmorBundleDrop(removed)) marker = ARMOR_BUNDLE_EMPTY_SLOT_MARKER;
            else if ("Armor".equals(category)) marker = ARMOR_OTHER_EMPTY_SLOT_MARKER;
            else marker = isPageTwoUtilityDrop(removed) ? PRIORITY_EMPTY_SLOT_MARKER : EMPTY_SLOT_MARKER;
            drops.set(index, new WeightedItem(List.of(marker), DEFAULT_WEIGHT, category));
        }

        private static boolean isEmptyDrop(WeightedItem drop) {
            return drop == null || drop.items() == null || drop.items().isEmpty()
                    || drop.items().stream().allMatch(item -> item == null || item.startsWith(EMPTY_SLOT_PREFIX));
        }

        private void sendHeldDropMessage(String message) {
            this.player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal(message).withStyle(ChatFormatting.YELLOW)));
        }

        private void flattenBundleContents(ItemStack stack, List<String> configuredStacks, int depth) {
            if (stack.isEmpty()) return;
            if (depth >= 16) {
                throw new IllegalArgumentException("Bundle nesting is too deep to save safely");
            }
            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (stack.is(net.minecraft.world.item.Items.BUNDLE) && contents != null) {
                for (ItemStack item : contents.itemCopies().toList()) {
                    flattenBundleContents(item, configuredStacks, depth + 1);
                }
                return;
            }
            configuredStacks.add(serializeConfiguredStack(stack, this.player));
        }

        private int insertionIndexForPageSlot(int pageSlot) {
            if (pages.isEmpty()) return drops.size();
            DropPage current = pages.get(page);
            if (pageSlot < current.sourceIndexes().size()) return current.sourceIndexes().get(pageSlot);
            if (categoryMode) {
                // Blank slots only occur after the last entry on a category page. Insert
                // after that page's final displayed entry instead of scanning the backing
                // list for the category's last raw index, which can be on another page.
                if (!current.sourceIndexes().isEmpty()) {
                    return current.sourceIndexes().getLast() + 1;
                }
                return drops.size();
            }

            // List view is paged from the flattened category order. A blank slot is at
            // the end of that view, so append to the backing list; reinserting at the
            // category's last raw index can make a new multi-item/bundle drop appear to
            // shuffle entries across pages when the view is rebuilt.
            return drops.size();
        }

        private String categoryAtListPosition(int pageSlot, int sourceIndex) {
            if (sourceIndex >= 0 && sourceIndex < drops.size()) return categoryFor(drops.get(sourceIndex));
            if (pages.isEmpty()) return null;
            List<Integer> sourceIndexes = pages.get(page).sourceIndexes();
            if (sourceIndexes.isEmpty()) return categoryForCurrentPage();
            int precedingSlot = Math.min(pageSlot - 1, sourceIndexes.size() - 1);
            if (precedingSlot >= 0) return categoryFor(drops.get(sourceIndexes.get(precedingSlot)));
            int followingSlot = Math.max(0, Math.min(pageSlot, sourceIndexes.size() - 1));
            return categoryFor(drops.get(sourceIndexes.get(followingSlot)));
        }

        private String categoryForCurrentPage() {
            if (!categoryMode || pages.isEmpty()) return null;
            String label = pages.get(page).label();
            return label.equals("Tools, Weapons & Supplies") ? "Food & Supplies" :
                    CATEGORY_ORDER.contains(label) ? label : null;
        }

        private static WeightedItem withCategory(WeightedItem drop, String category) {
            return category == null ? drop : new WeightedItem(drop.items(), drop.weight(), category);
        }

        private void persistChanges() {
            savedChangesDuringEdit = true;
        }

        private void resetToModDefaults() {
            drops.clear();
            drops.addAll(Items.getDefaultItems().stream()
                    .map(drop -> new WeightedItem(new ArrayList<>(drop.items()), drop.weight(), drop.categoryOverride()))
                    .toList());
            freezeDropCategories();
            editListOrder = orderedCategoryIndexes();
            savedChangesDuringEdit = true;
            heldDrop = null;
            setCarried(ItemStack.EMPTY);
            page = 0;
            refreshPage();
        }

        private void clearAllDrops() {
            if (!editMode || drops.isEmpty()) return;
            drops.clear();
            editListOrder = new ArrayList<>();
            savedChangesDuringEdit = true;
            heldDrop = null;
            setCarried(ItemStack.EMPTY);
            page = 0;
            refreshPage();
            this.player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("All drops cleared. Finish editing to save.").withStyle(ChatFormatting.RED)));
        }

        private void commitChanges() {
            if (!savedChangesDuringEdit) return;
            drops.removeIf(DropsMenu::isEmptyDrop);
            List<WeightedItem> savedDrops = new ArrayList<>(drops);
            Items.itemsAndNumbers = savedDrops;
            ConfigManager config = Random_Item_Challenge.getConfigManager();
            if (config != null) {
                config.items = savedDrops;
                config.saveConfig();
            }
            for (Gamerz gamer : Gamerz.gamersList) gamer.refillItems();
            savedChangesDuringEdit = false;
            this.player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("Drop configuration saved.").withStyle(ChatFormatting.GREEN)));
        }

        private void beginEditSession() {
            // Capture the exact order currently shown before allowing edits. Re-deriving
            // the list order after every cursor action can reclassify bundles (for
            // example, one containing a pufferfish bucket) and silently move unrelated
            // drops while the player is still arranging them.
            editListOrder = orderedCategoryIndexes();
            freezeDropCategories();
        }

        private void freezeDropCategories() {
            for (int index = 0; index < drops.size(); index++) {
                WeightedItem drop = drops.get(index);
                if (drop == null || (drop.categoryOverride() != null
                        && CATEGORY_ORDER.contains(drop.categoryOverride()))) continue;
                drops.set(index, withCategory(drop, categoryFor(drop)));
            }
        }

        private void insertDropAt(int sourceIndex, WeightedItem drop, int pageSlot) {
            int insertionIndex = Math.max(0, Math.min(sourceIndex, drops.size()));
            drops.add(insertionIndex, drop);
            if (editListOrder == null) return;

            for (int index = 0; index < editListOrder.size(); index++) {
                int existing = editListOrder.get(index);
                if (existing >= insertionIndex) editListOrder.set(index, existing + 1);
            }
            int displayIndex = Math.max(0, Math.min(pageSlot, pageEntries.size()));
            int beforeSource = displayIndex < pageEntries.size()
                    ? pageEntries.get(displayIndex).sourceIndex() : -1;
            int afterSource = displayIndex > 0
                    ? pageEntries.get(displayIndex - 1).sourceIndex() : -1;
            if (beforeSource >= insertionIndex) beforeSource++;
            if (afterSource >= insertionIndex) afterSource++;
            int orderIndex = beforeSource >= 0 ? editListOrder.indexOf(beforeSource)
                    : afterSource >= 0 ? editListOrder.indexOf(afterSource) + 1 : editListOrder.size();
            if (orderIndex < 0) orderIndex = editListOrder.size();
            editListOrder.add(Math.max(0, Math.min(orderIndex, editListOrder.size())), insertionIndex);
        }

        private void refreshPage() {
            pages.clear();
            List<Integer> stableIndexes = editMode && editListOrder != null
                    ? editListOrder : null;
            if (categoryMode) {
                boolean suppliesJoinedToTools = false;
                for (String category : CATEGORY_ORDER) {
                    List<Integer> indexes = new ArrayList<>();
                    List<Integer> categorySource = stableIndexes == null
                            ? java.util.stream.IntStream.range(0, drops.size()).boxed().toList()
                            : stableIndexes;
                    for (int index : categorySource) {
                        if (category.equals(categoryFor(drops.get(index)))) indexes.add(index);
                    }
                    if (category.equals("Armor")) indexes = armorGridOrder(indexes);
                    if (category.equals("Tools & Weapons")) {
                        List<Integer> combined = new ArrayList<>();
                        if (indexes.size() > PAGE_SIZE) {
                            appendPages(category, indexes.subList(0, PAGE_SIZE));
                            combined.addAll(indexes.subList(PAGE_SIZE, indexes.size()));
                        } else {
                            appendPages(category, indexes);
                        }
                        List<Integer> prioritizedSupplies = new ArrayList<>();
                        List<Integer> remainingSupplies = new ArrayList<>();
                        for (int index : categorySource) {
                            if (!"Food & Supplies".equals(categoryFor(drops.get(index)))) continue;
                            (stableIndexes == null && isPageTwoUtilityDrop(drops.get(index))
                                    ? prioritizedSupplies : remainingSupplies).add(index);
                        }
                        combined.addAll(prioritizedSupplies);
                        combined.addAll(remainingSupplies);
                        if (!combined.isEmpty()) {
                            appendPages("Tools, Weapons & Supplies", combined);
                            suppliesJoinedToTools = true;
                        }
                        continue;
                    }
                    if (category.equals("Food & Supplies") && suppliesJoinedToTools) continue;
                    appendPages(category, indexes);
                }
            } else {
                List<Integer> orderedIndexes = stableIndexes == null ? orderedCategoryIndexes() : stableIndexes;
                for (int start = 0; start < orderedIndexes.size(); start += PAGE_SIZE) {
                    pages.add(new DropPage("All drops · category order",
                            new ArrayList<>(orderedIndexes.subList(start, Math.min(orderedIndexes.size(), start + PAGE_SIZE)) )));
                }
            }
            if (pages.isEmpty()) pages.add(new DropPage(categoryMode ? "No drops" : "All drops · category order", List.of()));
            pageCount = pages.size();
            page = Math.max(0, Math.min(page, pageCount - 1));
            pageEntries.clear();
            DropPage currentPage = pages.get(page);
            for (int sourceIndex : currentPage.sourceIndexes()) {
                PreviewEntry preview = makePreviewEntry(drops.get(sourceIndex), player, sourceIndex + 1);
                pageEntries.add(new DisplayDrop(sourceIndex, preview));
            }
            populatePage(container, pageEntries, page, pageCount, drops.size(), currentPage.label(), editMode, categoryMode);
            broadcastChanges();
        }

        private void appendPages(String label, List<Integer> indexes) {
            for (int start = 0; start < indexes.size(); start += PAGE_SIZE) {
                pages.add(new DropPage(label,
                        new ArrayList<>(indexes.subList(start, Math.min(indexes.size(), start + PAGE_SIZE)))));
            }
        }

        private List<Integer> orderedCategoryIndexes() {
            List<Integer> ordered = new ArrayList<>();
            boolean suppliesJoinedToTools = false;
            for (String category : CATEGORY_ORDER) {
                List<Integer> indexes = new ArrayList<>();
                for (int index = 0; index < drops.size(); index++) {
                    if (category.equals(categoryFor(drops.get(index)))) indexes.add(index);
                }
                if (category.equals("Armor")) indexes = armorListOrder(indexes);
                if (category.equals("Tools & Weapons")) {
                    ordered.addAll(indexes);
                    List<Integer> supplies = new ArrayList<>();
                    for (int index = 0; index < drops.size(); index++) {
                        if ("Food & Supplies".equals(categoryFor(drops.get(index)))) supplies.add(index);
                    }
                    if (!indexes.isEmpty() && !supplies.isEmpty()) {
                        for (int index : supplies) {
                            if (isPageTwoUtilityDrop(drops.get(index))) ordered.add(index);
                        }
                        for (int index : supplies) {
                            if (!isPageTwoUtilityDrop(drops.get(index))) ordered.add(index);
                        }
                        suppliesJoinedToTools = true;
                    }
                    continue;
                }
                if (category.equals("Food & Supplies") && suppliesJoinedToTools) continue;
                ordered.addAll(indexes);
            }
            return ordered;
        }

        private List<Integer> armorListOrder(List<Integer> indexes) {
            List<Integer> armorSets = new ArrayList<>();
            List<Integer> oneOffs = new ArrayList<>();
            List<Integer> bundles = new ArrayList<>();
            for (int index : indexes) {
                WeightedItem drop = drops.get(index);
                if (armorSetOrder(drop) >= 0) armorSets.add(index);
                else if (isArmorBundleDrop(drop)) bundles.add(index);
                else oneOffs.add(index);
            }
            armorSets.sort(java.util.Comparator.comparingInt(index -> armorSetOrder(drops.get(index))));
            List<Integer> ordered = new ArrayList<>(indexes.size());
            ordered.addAll(armorSets);
            ordered.addAll(oneOffs);
            ordered.addAll(bundles);
            return ordered;
        }

        /** Keeps armor sets aligned in category grid rows, with the first three bundles
         * occupying the ninth column after chainmail, copper, and diamond respectively.
         * The list view deliberately uses armorListOrder instead. */
        private List<Integer> armorGridOrder(List<Integer> indexes) {
            List<Integer> sets = new ArrayList<>();
            List<Integer> oneOffs = new ArrayList<>();
            List<Integer> bundles = new ArrayList<>();
            for (int index : indexes) {
                WeightedItem drop = drops.get(index);
                if (armorSetOrder(drop) >= 0) sets.add(index);
                else if (isArmorBundleDrop(drop)) bundles.add(index);
                else oneOffs.add(index);
            }
            sets.sort(java.util.Comparator.comparingInt(index -> armorSetOrder(drops.get(index))));

            List<Integer> ordered = new ArrayList<>(indexes.size());
            int[] bundleAfterMaterial = {1, 3, 5}; // chainmail, copper, diamond
            int bundleIndex = 0;
            for (int setIndex : sets) {
                ordered.add(setIndex);
                int material = armorSetOrder(drops.get(setIndex)) / 4;
                if (armorSetOrder(drops.get(setIndex)) % 4 == 3
                        && bundleIndex < bundleAfterMaterial.length
                        && material == bundleAfterMaterial[bundleIndex]
                        && bundleIndex < bundles.size()) {
                    ordered.add(bundles.get(bundleIndex++));
                }
            }
            ordered.addAll(oneOffs);
            ordered.addAll(bundles.subList(bundleIndex, bundles.size()));
            return ordered;
        }

        private static int armorSetOrder(WeightedItem drop) {
            if (drop == null || drop.items() == null) return -1;
            if (drop.items().size() == 1) {
                String raw = drop.items().getFirst();
                if (raw != null && raw.startsWith(ARMOR_SET_EMPTY_SLOT_PREFIX)) {
                    try {
                        return Integer.parseInt(raw.substring(ARMOR_SET_EMPTY_SLOT_PREFIX.length()));
                    } catch (NumberFormatException ignored) {
                        return -1;
                    }
                }
                if (raw != null) {
                    Matcher matcher = ARMOR_SET_ID.matcher(configuredItemId(raw).toLowerCase(Locale.ROOT));
                    if (matcher.matches()) {
                        return ARMOR_MATERIAL_ORDER.get(matcher.group(1)) * 4
                                + ARMOR_PIECE_ORDER.get(matcher.group(2));
                    }
                }
            }
            return -1;
        }

        private static boolean isArmorBundleDrop(WeightedItem drop) {
            return drop != null && drop.items() != null && (drop.items().size() > 1
                    || drop.items().contains(ARMOR_BUNDLE_EMPTY_SLOT_MARKER));
        }

        private int pageForSourceIndex(int sourceIndex) {
            for (int index = 0; index < pages.size(); index++) {
                if (pages.get(index).sourceIndexes().contains(sourceIndex)) return index;
            }
            return Math.max(0, pageCount - 1);
        }

        private void setPage(int requestedPage) {
            page = Math.max(0, Math.min(requestedPage, pageCount - 1));
            refreshPage();
        }
    }
}
