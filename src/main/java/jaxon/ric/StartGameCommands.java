package jaxon.ric;

import java.util.Arrays;
import java.util.List;

public class StartGameCommands {
    public static final List<String> regularCommands = Arrays.asList(
            "gamerule mob_drops false",
            "gamerule block_drops false",
            "gamerule keep_inventory true",
            "gamerule spectators_generate_chunks true",
            "gamerule immediate_respawn true",
            "gamerule respawn_radius 0",
            "execute in minecraft:overworld run worldborder set 500",
            "execute in minecraft:the_nether run worldborder set 500",
            "execute in minecraft:the_end run worldborder set 500",
            "execute in minecraft:overworld run worldborder set 30 3600",
            "execute in minecraft:the_nether run worldborder set 30 3600",
            "execute in minecraft:the_end run worldborder set 30 3600",
            "kill @e[type=!player,type=!villager,type=!cow,type=!sheep,type=!chicken,type=!goat,type=!dolphin,type=!allay,type=!ender_dragon,type=!end_crystal,type=!armadillo]",
            "clear @a",
            "effect clear @a",
            "item replace entity @a hotbar.7 with cooked_beef 64",
            "item replace entity @a hotbar.6 with crafting_table 1"
    );

    public static final List<String> resumeCommands = List.of();
}
