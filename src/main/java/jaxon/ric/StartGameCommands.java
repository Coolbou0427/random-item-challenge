package jaxon.ric;

import java.util.Arrays;
import java.util.List;

public class StartGameCommands {
    public static final List<String> regularCommands = Arrays.asList(
            "title @a title {\"text\":\"Random Items\",\"color\":\"blue\"}",
            "worldborder set 9999999",
            "gamemode creative @a",
            "gamerule doMobLoot false",
            "gamerule doTileDrops false",
            "gamerule keepInventory true",
            "gamerule spectatorsGenerateChunks true",
            "gamerule doImmediateRespawn true",
            "gamerule spawnRadius 0",
            "advancement revoke @a everything",
            "execute in minecraft:overworld run worldborder set 500",
            "execute in minecraft:the_nether run worldborder set 500",
            "execute in minecraft:the_end run worldborder set 500",
            "execute in minecraft:overworld run worldborder set 30 3600",
            "execute in minecraft:the_nether run worldborder set 30 3600",
            "execute in minecraft:the_end run worldborder set 30 3600",
            "kill @e[type=!player,type=!villager,type=!cow,type=!sheep,type=!chicken,type=!goat,type=!dolphin,type=!allay,type=!ender_dragon,type=!end_crystal,type=!armadillo]",
            "tp @a %s",
            "clear @a",
            "effect clear @a",
            "xp set @a 0 levels",
            "gamemode survival @a",
            "item replace entity @a hotbar.7 with cooked_beef 64",
            "item replace entity @a hotbar.6 with crafting_table 1",
            "item replace entity @a hotbar.8 with minecraft:compass[minecraft:custom_data={tracker:true}]"
    );

    public static final List<String> resumeCommands = List.of(
            "title @a title {\"text\":\"Random Items\",\"color\":\"blue\"}"
    );
}
