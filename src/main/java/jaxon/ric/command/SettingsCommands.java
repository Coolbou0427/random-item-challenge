package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import jaxon.ric.Gamerz;
import jaxon.ric.DirectEnchantingHooks;
import jaxon.ric.Random_Item_Challenge;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class SettingsCommands {

    private static final SuggestionProvider<CommandSourceStack> TRUE_FALSE = (c, b) -> {
        b.suggest("true");
        b.suggest("false");
        return b.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("ric")
                        .then(Commands.literal("settings")
                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))

                                .then(Commands.literal("delay")
                                        .executes(SettingsCommands::showDelay)
                                        .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.01))
                                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeDelay(c, DoubleArgumentType.getDouble(c, "seconds")))
                                        )
                                )

                                .then(Commands.literal("numberPerTeam")
                                        .executes(SettingsCommands::showNumberPerTeam)
                                        .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeNumberPerTeam(c, IntegerArgumentType.getInteger(c, "value")))
                                        )
                                )

                                .then(Commands.literal("testMode")
                                        .executes(c -> showSetting(c, "testMode"))
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "testMode"))
                                        )
                                )

                                .then(Commands.literal("resumeMode")
                                        .executes(c -> showSetting(c, "resumeMode"))
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "resumeMode"))
                                        )
                                )

                                .then(Commands.literal("teamMode")
                                        .executes(c -> showSetting(c, "teamMode"))
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "teamMode"))
                                        )
                                )

                                .then(Commands.literal("enableMobFriendlyFire")
                                        .executes(c -> showSetting(c, "enableMobFriendlyFire"))
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "enableMobFriendlyFire"))
                                        )
                                )

                                .then(Commands.literal("enableDirectEnchanting")
                                        .executes(c -> showSetting(c, "enableDirectEnchanting"))
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "enableDirectEnchanting"))
                                        )
                                )

                                .then(booleanSetting("enableAutoTntIgnite", "enableAutoTntIgnite"))

                                .then(booleanSetting("enablePlayerPlacedBlockDrops", "enablePlayerPlacedBlockDrops"))

                                .then(Commands.literal("clearTeams")
                                        .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                        .executes(c -> {
                                            clearAllTeams(c.getSource());
                                            return 1;
                                        })
                                )
                        )
        );
    }

    private static int showNumberPerTeam(CommandContext<CommandSourceStack> c) {
        c.getSource().sendSuccess(() -> Component.literal("Number per team is set to " + GoCommand.numberPerTeam).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> booleanSetting(String commandName, String settingName) {
        return Commands.literal(commandName)
                .executes(c -> showSetting(c, settingName))
                .then(Commands.argument("value", StringArgumentType.word())
                        .suggests(TRUE_FALSE)
                        .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .executes(c -> changeSetting(c, settingName))
                );
    }

    private static int changeNumberPerTeam(CommandContext<CommandSourceStack> c, int v) {
        int pc = Gamerz.gamersList.size();
        if (pc == 0) {
            c.getSource().sendFailure(Component.literal("No players are currently in the game.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (v > pc / 2) {
            c.getSource().sendFailure(Component.literal("Number per team can't be bigger than half the number of players").withStyle(ChatFormatting.RED));
            return 0;
        }
        GoCommand.numberPerTeam = v;
        c.getSource().sendSuccess(() -> Component.literal("Number per team set to " + v).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int showDelay(CommandContext<CommandSourceStack> c) {
        c.getSource().sendSuccess(() -> Component.literal("Delay is set to " + Random_Item_Challenge.delay + " seconds").withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int changeDelay(CommandContext<CommandSourceStack> c, double s) {
        if (s <= 0) {
            c.getSource().sendFailure(Component.literal("Delay must be greater than 0").withStyle(ChatFormatting.RED));
            return 0;
        }
        Random_Item_Challenge.delay = s;
        Random_Item_Challenge.getConfigManager().delay = s;
        Random_Item_Challenge.getConfigManager().saveConfig();
        c.getSource().sendSuccess(() -> Component.literal("Delay set to " + s + " seconds").withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int showSetting(CommandContext<CommandSourceStack> c, String s) {
        Boolean v = getSettingValue(s);
        if (v == null) {
            c.getSource().sendFailure(Component.literal("Unknown setting: " + s).withStyle(ChatFormatting.RED));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal(s + " is set to " + v).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int changeSetting(CommandContext<CommandSourceStack> c, String s) {
        boolean on = "true".equalsIgnoreCase(StringArgumentType.getString(c, "value"));

        switch (s) {
            case "testMode" -> GoCommand.testMode = on;
            case "resumeMode" -> GoCommand.resumeMode = on;
            case "teamMode" -> {
                GoCommand.teamMode = on;
                if (!on) {
                    TeamsCommand.clearAllTeams(c.getSource(), false);
                }
            }

            case "enableMobFriendlyFire" -> {
                GoCommand.enableMobFriendlyFire = on;
                Random_Item_Challenge.getConfigManager().enablemobfriendlyfire = on;
                Random_Item_Challenge.getConfigManager().saveConfig();
                TeamsCommand.keepRicPlayerFriendlyFireDisabled(c.getSource().getServer().getScoreboard());
            }

            case "enableDirectEnchanting" -> {
                DirectEnchantingHooks.enabled = on;
                Random_Item_Challenge.getConfigManager().enableDirectEnchanting = on;
                Random_Item_Challenge.getConfigManager().saveConfig();
            }

            case "enableAutoTntIgnite" -> {
                GoCommand.enableTntAutoExplode = on;
                Random_Item_Challenge.getConfigManager().enabletntautoexplode = on;
                Random_Item_Challenge.getConfigManager().saveConfig();
            }
            case "enablePlayerPlacedBlockDrops" -> {
                GoCommand.enablePlayerPlacedBlockDrops = on;
                Random_Item_Challenge.getConfigManager().enablePlayerPlacedBlockDrops = on;
                Random_Item_Challenge.getConfigManager().saveConfig();
            }
            default -> {
                c.getSource().sendFailure(Component.literal("Unknown setting: " + s).withStyle(ChatFormatting.RED));
                return 0;
            }
        }

        c.getSource().sendSuccess(() -> Component.literal(s + " set to " + on).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static Boolean getSettingValue(String s) {
        return switch (s) {
            case "testMode" -> GoCommand.testMode;
            case "resumeMode" -> GoCommand.resumeMode;
            case "teamMode" -> GoCommand.teamMode;
            case "enableMobFriendlyFire" -> GoCommand.enableMobFriendlyFire;
            case "enableDirectEnchanting" -> DirectEnchantingHooks.enabled;
            case "enableAutoTntIgnite" -> GoCommand.enableTntAutoExplode;
            case "enablePlayerPlacedBlockDrops" -> GoCommand.enablePlayerPlacedBlockDrops;
            default -> null;
        };
    }

    private static void clearAllTeams(CommandSourceStack src) {
        TeamsCommand.clearAllTeams(src, true);
    }


}
