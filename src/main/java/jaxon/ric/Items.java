package jaxon.ric;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import jaxon.ric.Random_Item_Challenge.SafeRandom;
import net.minecraft.world.World;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class Items {
    public static List<WeightedItem> itemsAndNumbers;
    static {
        ConfigManager configManager = new ConfigManager();
        Items.itemsAndNumbers = configManager.items;
    }

    static List<WeightedItem> getDefaultItems() {
        List<WeightedItem> defaultItems = new ArrayList<>();
        defaultItems.add(new WeightedItem(Arrays.asList("wooden_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wooden_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wooden_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wooden_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_hoe"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("trident 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("crossbow 9", "arrow 9"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("shield 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("fishing_rod 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("snow_block 64", "netherite_shovel[enchantments={levels:{\"efficiency\":2}}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("mace"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("leather_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("leather_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("leather_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("leather_boots 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("chainmail_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("chainmail_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("chainmail_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("chainmail_boots 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_boots 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_boots 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_boots 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_boots[enchantments={levels:{\"soul_speed\":3}}] 1", "soul_soil 32"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_boots 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_ingot 1", "smithing_table", "netherite_upgrade_smithing_template"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("turtle_helmet"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:trident[enchantments={levels:{\"riptide\":5}}]"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:trident[enchantments={levels:{\"loyalty\":5}}]"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:mace[enchantments={levels:{\"breach\":4}}]"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:mace[enchantments={levels:{\"density\":5}}]"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:mace[enchantments={levels:{\"wind_burst\":3}}]", "netherite_boots[minecraft:enchantments={levels:{'feather_falling':4}},minecraft:damage=481]"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bow 1", "arrow 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bow 1", "spectral_arrow 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("quartz 320"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_ingot 16"), 30));
        defaultItems.add(new WeightedItem(Arrays.asList("gold_ingot 32", "apple 2"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("oak_log 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("spruce_log 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("oak_wood 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("spruce_wood 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("jungle_wood 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("acacia_wood 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("dark_oak_wood 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("warped_hyphae 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("crimson_hyphae 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("mangrove_wood 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bamboo_planks 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bamboo_block 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("cherry_wood 32"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("blaze_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("cave_spider_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("creeper_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("evoker_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("ghast_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("hoglin_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("horse_spawn_egg 1", "saddle 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("phantom_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("piglin_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("piglin_brute_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("pillager_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("shulker_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("skeleton_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stray_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("vindicator_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("witch_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wither_skeleton_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("zombie_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("dolphin_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("ravager_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wolf_spawn_egg 5", "bone 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("silverfish_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("warden_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("elder_guardian_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("endermite_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bogged_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("breeze_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bee_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("enderman_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("spider_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_golem_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("rabbit_spawn_egg[custom_name=\"'Killer Bunny'\",entity_data={id:rabbit,RabbitType:99}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("skeleton_horse_spawn_egg[entity_data={id:skeleton_horse_spawn_egg,SkeletonTrap:1},minecraft:custom_name='\"Skeleton Horsemen Trap\"'] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("skeleton_horse_spawn_egg[entity_data={id:\"skeleton_horse\",Tame:1b}] 1", "saddle 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("zombie_horse_spawn_egg[entity_data={id:\"zombie_horse\",Tame:1b}] 1", "saddle 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("allay_spawn_egg[custom_name=\"Illusioner\",minecraft:entity_data={id:\"illusioner\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("cobblestone 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("blackstone 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("black_glazed_terracotta 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("stone 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("smooth_stone 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("deepslate_tiles 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("deepslate_bricks 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("cobbled_deepslate 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("polished_deepslate 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_bricks 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("bricks 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("end_stone 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("end_stone_bricks 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("oak_leaves 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("dark_oak_leaves 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("glass 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("dirt 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("cherry_planks 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("mossy_cobblestone 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("mud_bricks 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("packed_mud 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("mud 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("moss_block 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("mossy_stone_bricks 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("sandstone 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("oxidized_copper 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_block 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("oxidized_copper_grate 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_grate 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("tuff_bricks 16"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("big_dripleaf 32"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("blue_ice 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("oak_door 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("mangrove_door 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_door 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_door 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("oxidized_copper_door 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("sand 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("obsidian 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("slime_block 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("honey_block 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("lily_pad 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bamboo_fence 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("soul_sand 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("magma_block 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("potion[potion_contents={potion:regeneration}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("potion[potion_contents={potion:water_breathing}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("lingering_potion[potion_contents={potion:poison}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("lingering_potion[potion_contents={potion:strong_harming}] 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:strength}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:strong_swiftness}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:strong_leaping}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:poison}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:strong_harming}] 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:weakness}] 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:invisibility}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:strong_slowness}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:slow_falling}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:fire_resistance}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={potion:strong_healing}] 2"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={custom_effects:[{id:nausea,duration:600,amplifier:1}]},custom_name='[{\"text\":\"Potion of Nausea\"}]']"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={custom_effects:[{id:blindness,duration:200,amplifier:1}]},custom_name='[{\"text\":\"Potion of Blindness\"}]']"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={custom_effects:[{id:levitation,duration:200,amplifier:1}]},custom_name='[{\"text\":\"Potion of Levitation\"}]']"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={custom_effects:[{id:wither,duration:300,amplifier:1}]},custom_name='[{\"text\":\"Potion of Wither\"}]']"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={custom_effects:[{id:resistance,duration:600,amplifier:1}]},custom_name=\"Potion of Resistance\"]"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[potion_contents={custom_effects:[{id:resistance,duration:200,amplifier:2}]},custom_name=\"Potion of Resistance\"]"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bedrock 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("barrier 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("reinforced_deepslate 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("end_portal_frame 12", "ender_eye 12"), 5));
        defaultItems.add(new WeightedItem(Arrays.asList("obsidian 14", "flint_and_steel[damage=62] 2"), 5));
        defaultItems.add(new WeightedItem(Arrays.asList("enchanting_table 1", "lapis_lazuli 64", "experience_bottle 32"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{sharpness:2}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{sharpness:3}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{sharpness:4}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{sharpness:5}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{protection:2}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{protection:3}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{protection:4}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{feather_falling:2}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{feather_falling:3}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{feather_falling:4}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{fire_aspect:2}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{depth_strider:2}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{depth_strider:3}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "enchanted_book[stored_enchantments={levels:{knockback:2}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "bow", "arrow 8", "enchanted_book[stored_enchantments={levels:{punch:2}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "bow", "arrow 8", "enchanted_book[stored_enchantments={levels:{flame:1}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "crossbow", "arrow 8", "enchanted_book[stored_enchantments={levels:{piercing:2}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "crossbow", "arrow 8", "enchanted_book[stored_enchantments={levels:{piercing:3}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "crossbow", "arrow 8", "enchanted_book[stored_enchantments={levels:{piercing:4}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "crossbow", "arrow 8", "enchanted_book[stored_enchantments={levels:{quick_charge:2}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil", "experience_bottle 32", "crossbow", "arrow 8", "enchanted_book[stored_enchantments={levels:{quick_charge:3}}]"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_carrot 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("elytra[damage=422] 1", "firework_rocket 2"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_apple 2"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("enchanted_golden_apple 1"), 3));
        defaultItems.add(new WeightedItem(Arrays.asList("end_crystal 1", "obsidian 1", "netherite_chestplate[minecraft:enchantments={levels:{'blast_protection':255}},damage=565]"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("respawn_anchor 1", "glowstone 1", "netherite_chestplate[minecraft:enchantments={levels:{'blast_protection':255}},damage=575]"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("water_bucket 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:hay_block 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("ladder 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("cobweb 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("scaffolding 64"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("twisting_vines 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("ender_pearl 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("chorus_fruit 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("totem_of_undying 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wind_charge 32"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("red_bed"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("blue_bed"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("orange_bed"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("sweet_berries 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("powder_snow_bucket"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("oak_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("spruce_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("cherry_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("dark_oak_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("bamboo_raft"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("oak_chest_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("spruce_chest_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("cherry_chest_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("dark_oak_chest_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("bamboo_chest_raft"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("pufferfish_bucket 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("tnt 5", "flint_and_steel[damage=59] 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("lava_bucket 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("rail 64", "tnt_minecart 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("milk_bucket"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("flint_and_steel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("fire_charge 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("tnt 1", "fishing_rod 1", "flint_and_steel[damage=62] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("soul_sand 4", "wither_skeleton_skull 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("sticky_piston 1", "slime_block 1", "lever 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("pointed_dripstone 20"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("jukebox 1"), 5));
        defaultItems.add(new WeightedItem(Arrays.asList("pig_spawn_egg", "saddle", "carrot_on_a_stick"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("strider_spawn_egg", "saddle", "warped_fungus_on_a_stick"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("rotten_flesh 576"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("vault[block_state={ominous:\"true\"},block_entity_data={id:\"minecraft:vault\",config:{key_item:{count:1,id:\"minecraft:ominous_trial_key\"},loot_table:\"minecraft:chests/trial_chambers/reward_ominous\"}}]", "ominous_trial_key"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wolf_spawn_egg", "wolf_armor", "bone 12"), 10));
        return defaultItems;
    }

    public static final Map<String, List<String>> randomItems = new HashMap<>();

    static {
        randomItems.put("jukebox 1", Arrays.asList("music_disc_pigstep", "music_disc_stal", "music_disc_otherside", "music_disc_relic", "music_disc_5", "music_disc_creator"));
    }
    public static void dayChanceAndRainChance() {
        Random random = SafeRandom.getUniqueRandom();

        // Set a random time from our list
        List<Integer> times = Arrays.asList(1000, 6000, 13000);
        int tickValue = times.get(random.nextInt(times.size()));
        Random_Item_Challenge.server.getWorld(World.OVERWORLD).setTimeOfDay(tickValue);

        // Weather odds: for example, 10% rain, 80% clear, 10% thunder
        Map<String, Integer> weatherOdds = Map.of(
                "rain", 10,
                "clear", 85,
                "thunder", 5
        );

        int totalWeight = weatherOdds.values().stream().mapToInt(Integer::intValue).sum();
        int randomWeight = random.nextInt(totalWeight) + 1;
        String selectedWeather = null;
        for (Map.Entry<String, Integer> entry : weatherOdds.entrySet()) {
            randomWeight -= entry.getValue();
            if (randomWeight <= 0) {
                selectedWeather = entry.getKey();
                break;
            }
        }

        if (selectedWeather.equals("rain")) {
            Random_Item_Challenge.server.getWorld(World.OVERWORLD).setWeather(0, 0, true, false);
        } else if (selectedWeather.equals("thunder")) {
            Random_Item_Challenge.server.getWorld(World.OVERWORLD).setWeather(0, 0, true, true);
        } else {
            Random_Item_Challenge.server.getWorld(World.OVERWORLD).setWeather(0, 0, false, false);
        }
    }


}