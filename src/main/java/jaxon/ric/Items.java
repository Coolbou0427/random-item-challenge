package jaxon.ric;

import java.util.*;

import net.minecraft.world.level.Level;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class Items {
    public static List<WeightedItem> itemsAndNumbers;
    static {
        ConfigManager configManager = new ConfigManager();
        Items.itemsAndNumbers = configManager.items;
    }

    public static List<WeightedItem> getDefaultItems() {
        List<WeightedItem> defaultItems = new ArrayList<>();
        defaultItems.add(new WeightedItem(Arrays.asList("wooden_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wooden_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wooden_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wooden_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wooden_spear 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stone_spear 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_spear 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_spear 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_spear 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_hoe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("diamond_spear 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_sword 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_shovel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_pickaxe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_axe 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_spear 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("trident 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("crossbow 9", "arrow 9"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("shield 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("fishing_rod 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("snow_block 64", "minecraft:netherite_shovel[minecraft:enchantments={\"minecraft:efficiency\":2},minecraft:attribute_modifiers=[{id:\"ric:no_damage\",type:\"minecraft:attack_damage\",amount:-1,operation:\"add_value\",slot:\"mainhand\",display:{type:\"override\",value:{text:\"+0 Attack Damage\",color:\"green\"}}},{id:\"ric:attack_speed\",type:\"minecraft:attack_speed\",amount:-3,operation:\"add_value\",slot:\"mainhand\",display:{type:\"override\",value:{text:\"+1 Attack Speed\",color:\"green\"}}}]] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("mace 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("leather_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("leather_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("leather_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("leather_boots 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("chainmail_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("chainmail_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("chainmail_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("chainmail_boots 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("copper_boots 1"), 10));
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
        defaultItems.add(new WeightedItem(Arrays.asList("golden_boots[minecraft:enchantments={\"minecraft:soul_speed\":3}] 1", "soul_soil 32"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_chestplate 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_leggings 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_boots 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_ingot 1", "smithing_table 1", "netherite_upgrade_smithing_template 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("turtle_helmet 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:trident[minecraft:enchantments={\"minecraft:riptide\":5}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:trident[minecraft:enchantments={\"minecraft:loyalty\":5}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:mace[minecraft:enchantments={\"minecraft:breach\":4}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:mace[minecraft:enchantments={\"minecraft:density\":5}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("minecraft:mace[minecraft:enchantments={\"minecraft:wind_burst\":3}] 1", "minecraft:netherite_boots[minecraft:enchantments={\"minecraft:feather_falling\":4},minecraft:tooltip_display={hidden_components:[\"minecraft:attribute_modifiers\"]},minecraft:custom_name={text:\"Feather Falling Boots\",italic:false},minecraft:lore=[{text:\"\",italic:false},{text:\"Breaks after blocking one explosion.\",color:\"gold\",italic:false},{text:\"When worn:\",color:\"gray\",italic:false},{text:\"Provides no normal armor protection.\",color:\"red\",italic:false}],minecraft:attribute_modifiers=[{id:\"ric:zero_armor\",type:\"minecraft:armor\",amount:-3,operation:\"add_value\",slot:\"feet\"},{id:\"ric:zero_armor_toughness\",type:\"minecraft:armor_toughness\",amount:-3,operation:\"add_value\",slot:\"feet\"},{id:\"ric:zero_knockback_resistance\",type:\"minecraft:knockback_resistance\",amount:-0.1,operation:\"add_value\",slot:\"feet\"}]] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("netherite_spear[minecraft:enchantments={\"minecraft:lunge\":10}] 1"), 10));
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
        defaultItems.add(new WeightedItem(Arrays.asList("cherry_wood 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bamboo_planks 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bamboo_block 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("blaze_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("cave_spider_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("creeper_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("creaking_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("evoker_spawn_egg 2"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("ghast_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("hoglin_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("horse_spawn_egg[entity_data={id:horse,Tame:1}] 1", "saddle 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("phantom_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("piglin_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("piglin_brute_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("pillager_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("shulker_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("skeleton_spawn_egg 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("skeleton_horse_spawn_egg[entity_data={id:\"minecraft:skeleton_horse\",SkeletonTrap:1b},minecraft:custom_name={text:\"Skeleton Horsemen Trap\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("stray_spawn_egg 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bogged_spawn_egg 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("parched_spawn_egg 5"), 10));
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
        defaultItems.add(new WeightedItem(Arrays.asList("breeze_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bee_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("enderman_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("spider_spawn_egg 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("iron_golem_spawn_egg 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("zombie_horse_spawn_egg[minecraft:custom_data={ric_spawn:\"zombie_horseman\"},minecraft:custom_name={text:\"Zombie Horseman Jockey Spawn Egg\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("camel_husk_spawn_egg[minecraft:custom_data={ric_spawn:\"camel_husk_jockey\"},minecraft:custom_name={text:\"Camel Husk Jockey Spawn Egg\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("ravager_spawn_egg[minecraft:custom_data={ric_spawn:\"ravager_jockey\"},minecraft:custom_name={text:\"Ravager Jockey Spawn Egg\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("zombie_nautilus_spawn_egg[minecraft:custom_data={ric_spawn:\"zombie_nautilus_jockey\"},minecraft:custom_name={text:\"Zombie Nautilus Jockey Spawn Egg\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("hoglin_spawn_egg[minecraft:custom_data={ric_spawn:\"hoglin_jockey\"},minecraft:custom_name={text:\"Hoglin Jockey Spawn Egg\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("happy_ghast_spawn_egg[minecraft:custom_data={ric_spawn:\"happy_ghast_mount\"},minecraft:custom_name={text:\"Happy Ghast Jockey Spawn Egg\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("sulfur_cube_spawn_egg 1"), 10));
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
        defaultItems.add(new WeightedItem(Arrays.asList("soul_sand 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("magma_block 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("potion[minecraft:potion_contents={potion:\"minecraft:regeneration\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("potion[minecraft:potion_contents={potion:\"minecraft:water_breathing\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("lingering_potion[minecraft:potion_contents={potion:\"minecraft:poison\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("lingering_potion[minecraft:potion_contents={potion:\"minecraft:strong_harming\"}] 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:strength\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:strong_swiftness\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:strong_leaping\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:poison\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:strong_harming\"}] 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:weakness\"}] 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:invisibility\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:strong_slowness\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:slow_falling\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:fire_resistance\"}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={potion:\"minecraft:strong_healing\"}] 2"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={custom_effects:[{id:\"minecraft:nausea\",duration:600,amplifier:1}]},minecraft:custom_name={text:\"Potion of Nausea\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={custom_effects:[{id:\"minecraft:blindness\",duration:200,amplifier:1}]},minecraft:custom_name={text:\"Potion of Blindness\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={custom_effects:[{id:\"minecraft:levitation\",duration:200,amplifier:1}]},minecraft:custom_name={text:\"Potion of Levitation\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={custom_effects:[{id:\"minecraft:wither\",duration:300,amplifier:1}]},minecraft:custom_name={text:\"Potion of Wither\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={custom_effects:[{id:\"minecraft:resistance\",duration:600,amplifier:1}]},minecraft:custom_name={text:\"Potion of Resistance\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("splash_potion[minecraft:potion_contents={custom_effects:[{id:\"minecraft:resistance\",duration:200,amplifier:2}]},minecraft:custom_name={text:\"Potion of Resistance\",italic:false}] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("bedrock 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("barrier 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("reinforced_deepslate 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("end_portal_frame 12", "ender_eye 12"), 5));
        defaultItems.add(new WeightedItem(Arrays.asList("obsidian 14", "flint_and_steel[damage=62] 1"), 5));
        defaultItems.add(new WeightedItem(Arrays.asList("enchanting_table 1", "lapis_lazuli 64", "experience_bottle 32"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:sharpness\":2}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:sharpness\":3}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:sharpness\":4}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:sharpness\":5}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:protection\":2}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:protection\":3}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:protection\":4}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:feather_falling\":2}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:feather_falling\":3}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:feather_falling\":4}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:fire_aspect\":2}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:depth_strider\":2}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:depth_strider\":3}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "enchanted_book[minecraft:stored_enchantments={\"minecraft:knockback\":2}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "bow 1", "arrow 8", "enchanted_book[minecraft:stored_enchantments={\"minecraft:punch\":2}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "bow 1", "arrow 8", "enchanted_book[minecraft:stored_enchantments={\"minecraft:flame\":1}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "crossbow 1", "arrow 8", "enchanted_book[minecraft:stored_enchantments={\"minecraft:piercing\":2}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "crossbow 1", "arrow 8", "enchanted_book[minecraft:stored_enchantments={\"minecraft:piercing\":3}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "crossbow 1", "arrow 8", "enchanted_book[minecraft:stored_enchantments={\"minecraft:piercing\":4}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "crossbow 1", "arrow 8", "enchanted_book[minecraft:stored_enchantments={\"minecraft:quick_charge\":2}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("anvil 1", "experience_bottle 32", "crossbow 1", "arrow 8", "enchanted_book[minecraft:stored_enchantments={\"minecraft:quick_charge\":3}] 1"), 2));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_carrot 8"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("elytra[damage=422] 1", "firework_rocket 2"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("golden_apple 2"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("enchanted_golden_apple 1"), 3));
        defaultItems.add(new WeightedItem(Arrays.asList("end_crystal 1","obsidian 1","minecraft:netherite_chestplate[minecraft:custom_data={ric_blast_armor:1b},minecraft:rarity=\"rare\",minecraft:enchantment_glint_override=true,minecraft:tooltip_display={hidden_components:[\"minecraft:attribute_modifiers\"]},minecraft:custom_name={text:\"Blast Chestplate\",color:\"aqua\",italic:false},minecraft:lore=[{text:\"Blast Protection 999,999,999\",color:\"gray\",italic:false},{text:\"\",italic:false},{text:\"Breaks after blocking one explosion.\",color:\"gold\",italic:false},{text:\"When worn:\",color:\"gray\",italic:false},{text:\"Provides no normal armor protection.\",color:\"red\",italic:false}],minecraft:attribute_modifiers=[{id:\"ric:zero_armor\",type:\"minecraft:armor\",amount:-8,operation:\"add_value\",slot:\"chest\"},{id:\"ric:zero_armor_toughness\",type:\"minecraft:armor_toughness\",amount:-3,operation:\"add_value\",slot:\"chest\"},{id:\"ric:zero_knockback_resistance\",type:\"minecraft:knockback_resistance\",amount:-0.1,operation:\"add_value\",slot:\"chest\"}]] 1"),10));
        defaultItems.add(new WeightedItem(Arrays.asList("respawn_anchor 1","glowstone 1","minecraft:netherite_chestplate[minecraft:custom_data={ric_blast_armor:1b},minecraft:rarity=\"rare\",minecraft:enchantment_glint_override=true,minecraft:tooltip_display={hidden_components:[\"minecraft:attribute_modifiers\"]},minecraft:custom_name={text:\"Blast Chestplate\",color:\"aqua\",italic:false},minecraft:lore=[{text:\"Blast Protection 999,999,999\",color:\"gray\",italic:false},{text:\"\",italic:false},{text:\"Breaks after blocking one explosion.\",color:\"gold\",italic:false},{text:\"When worn:\",color:\"gray\",italic:false},{text:\"Provides no normal armor protection.\",color:\"red\",italic:false}],minecraft:attribute_modifiers=[{id:\"ric:zero_armor\",type:\"minecraft:armor\",amount:-8,operation:\"add_value\",slot:\"chest\"},{id:\"ric:zero_armor_toughness\",type:\"minecraft:armor_toughness\",amount:-3,operation:\"add_value\",slot:\"chest\"},{id:\"ric:zero_knockback_resistance\",type:\"minecraft:knockback_resistance\",amount:-0.1,operation:\"add_value\",slot:\"chest\"}]] 1"),10));
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
        defaultItems.add(new WeightedItem(Arrays.asList("red_bed 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("blue_bed 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("orange_bed 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("sweet_berries 16"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("powder_snow_bucket 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("oak_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("spruce_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("cherry_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("dark_oak_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("bamboo_raft 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("oak_chest_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("spruce_chest_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("cherry_chest_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("dark_oak_chest_boat 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("bamboo_chest_raft 1"), 1));
        defaultItems.add(new WeightedItem(Arrays.asList("pufferfish_bucket 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("tnt 5", "flint_and_steel[damage=59] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("lava_bucket 4"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("rail 64", "tnt_minecart 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("milk_bucket 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("flint_and_steel 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("fire_charge 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("tnt 1", "fishing_rod 1", "flint_and_steel[damage=62] 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("soul_sand 4", "wither_skeleton_skull 3"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("sticky_piston 1", "slime_block 1", "lever 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("pointed_dripstone 20"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("jukebox 1"), 5));
        defaultItems.add(new WeightedItem(Arrays.asList("pig_spawn_egg 1", "saddle 1", "carrot_on_a_stick 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("camel_husk_spawn_egg 1", "saddle 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("camel_spawn_egg 1", "saddle 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("nautilus_spawn_egg 1", "saddle 1", "pufferfish 10"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("zombie_nautilus_spawn_egg 1", "saddle 1", "pufferfish 5"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("strider_spawn_egg 1", "saddle 1", "warped_fungus_on_a_stick 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("rotten_flesh 576"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("vault[minecraft:block_state={ominous:\"true\"},minecraft:block_entity_data={id:\"minecraft:vault\",config:{key_item:{count:1,id:\"minecraft:ominous_trial_key\"},loot_table:\"minecraft:chests/trial_chambers/reward_ominous\"}}] 1", "ominous_trial_key 1"), 10));
        defaultItems.add(new WeightedItem(Arrays.asList("wolf_spawn_egg 1", "wolf_armor 1", "bone 12"), 10));
        sortMatchingDefaults(defaultItems, Items::isDefaultToolDrop, Comparator.comparingInt(Items::defaultToolOrder));
        sortMatchingDefaults(defaultItems, Items::isDefaultArmorDrop, Comparator.comparingInt(Items::defaultArmorOrder));
        return defaultItems;
    }

    private static void sortMatchingDefaults(List<WeightedItem> drops,
                                             java.util.function.Predicate<WeightedItem> matches,
                                             Comparator<WeightedItem> comparator) {
        List<Integer> slots = new ArrayList<>();
        List<WeightedItem> matching = new ArrayList<>();
        for (int index = 0; index < drops.size(); index++) {
            WeightedItem drop = drops.get(index);
            if (matches.test(drop)) {
                slots.add(index);
                matching.add(drop);
            }
        }
        matching.sort(comparator);
        for (int index = 0; index < slots.size(); index++) drops.set(slots.get(index), matching.get(index));
    }

    private static boolean isDefaultToolDrop(WeightedItem drop) {
        if (hasDefaultSpawnEgg(drop)) return false;
        return drop.items().stream().filter(Objects::nonNull).map(Items::defaultItemId)
                .anyMatch(id -> id.matches("(wooden|stone|golden|copper|iron|diamond|netherite)_(sword|pickaxe|axe|shovel|hoe|spear)")
                        || Set.of("trident", "mace", "bow", "crossbow", "fishing_rod", "flint_and_steel",
                        "shears", "brush", "carrot_on_a_stick", "warped_fungus_on_a_stick").contains(id));
    }

    private static int defaultToolOrder(WeightedItem drop) {
        int best = 1_000;
        for (String raw : drop.items()) {
            String id = defaultItemId(raw);
            java.util.regex.Matcher match = java.util.regex.Pattern
                    .compile("(wooden|stone|golden|copper|iron|diamond|netherite)_(sword|pickaxe|axe|shovel|hoe|spear)")
                    .matcher(id);
            if (!match.matches()) continue;
            int material = switch (match.group(1)) {
                case "wooden" -> 0; case "stone" -> 1; case "golden" -> 2; case "copper" -> 3;
                case "iron" -> 4; case "diamond" -> 5; default -> 6;
            };
            int type = switch (match.group(2)) {
                case "sword" -> 0; case "pickaxe" -> 1; case "axe" -> 2; case "shovel" -> 3;
                case "hoe" -> 4; default -> 5;
            };
            best = Math.min(best, material * 10 + type);
        }
        return best;
    }

    private static boolean isDefaultArmorDrop(WeightedItem drop) {
        if (hasDefaultSpawnEgg(drop) || isDefaultToolDrop(drop)) return false;
        return drop.items().stream().filter(Objects::nonNull).map(Items::defaultItemId)
                .anyMatch(id -> id.endsWith("_helmet") || id.endsWith("_chestplate") || id.endsWith("_leggings")
                        || id.endsWith("_boots") || Set.of("elytra", "wolf_armor", "shield").contains(id));
    }

    private static int defaultArmorOrder(WeightedItem drop) {
        List<String> ids = drop.items().stream().filter(Objects::nonNull).map(Items::defaultItemId).toList();
        boolean blastArmor = drop.items().stream().filter(Objects::nonNull)
                .anyMatch(raw -> raw.contains("ric_blast_armor"));
        if (blastArmor && ids.contains("end_crystal")) return 15;
        if (blastArmor && ids.contains("respawn_anchor")) return 35;

        int best = 10_000;
        for (String raw : drop.items()) {
            if (raw.contains("soul_speed")) return 55;
            String id = defaultItemId(raw);
            java.util.regex.Matcher match = java.util.regex.Pattern
                    .compile("(leather|chainmail|iron|copper|golden|diamond|netherite)_(helmet|chestplate|leggings|boots)")
                    .matcher(id);
            if (match.matches()) {
                int material = switch (match.group(1)) {
                    case "leather" -> 0; case "chainmail" -> 1; case "iron" -> 2; case "copper" -> 3;
                    case "golden" -> 4; case "diamond" -> 5; default -> 6;
                };
                int piece = switch (match.group(2)) {
                    case "helmet" -> 0; case "chestplate" -> 1; case "leggings" -> 2; default -> 3;
                };
                best = Math.min(best, (raw.contains("[") ? 2_000 : 0) + material * 10 + piece);
            } else if (id.equals("turtle_helmet")) best = Math.min(best, 1_000);
            else if (id.equals("elytra")) best = Math.min(best, 3_000);
            else if (id.equals("wolf_armor")) best = Math.min(best, 3_100);
            else if (id.equals("shield")) best = Math.min(best, 4_000);
        }
        return best;
    }

    private static String defaultItemId(String raw) {
        String value = raw.trim();
        int space = value.lastIndexOf(' ');
        if (space > 0) value = value.substring(0, space);
        int components = value.indexOf('[');
        if (components >= 0) value = value.substring(0, components);
        int namespace = value.indexOf(':');
        return namespace >= 0 ? value.substring(namespace + 1) : value;
    }

    private static boolean hasDefaultSpawnEgg(WeightedItem drop) {
        return drop.items().stream().filter(Objects::nonNull).map(Items::defaultItemId)
                .anyMatch(id -> id.endsWith("_spawn_egg"));
    }


    public static final Map<String, List<String>> randomItems = new HashMap<>();

    static {
        randomItems.put("jukebox 1", Arrays.asList("music_disc_pigstep", "music_disc_stal", "music_disc_otherside", "music_disc_relic", "music_disc_5", "music_disc_creator", "music_disc_lava_chicken", "music_disc_bounce"));
    }
    public static void dayChanceAndRainChance() {
        Random random = java.util.concurrent.ThreadLocalRandom.current();

        List<Integer> times = Arrays.asList(1000, 6000, 13000);
        int tickValue = times.get(random.nextInt(times.size()));
        Random_Item_Challenge.executeCommandNow("time set " + tickValue);

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

        assert selectedWeather != null;
        if (selectedWeather.equals("rain")) {
            Random_Item_Challenge.executeCommandNow("weather rain");
        } else if (selectedWeather.equals("thunder")) {
            Random_Item_Challenge.executeCommandNow("weather thunder");
        } else {
            Random_Item_Challenge.executeCommandNow("weather clear");
        }
    }


}
