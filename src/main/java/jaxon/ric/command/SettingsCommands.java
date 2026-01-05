package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import jaxon.ric.Gamerz;
import jaxon.ric.Random_Item_Challenge;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class SettingsCommands {

    private static final SuggestionProvider<ServerCommandSource> TRUE_FALSE = (c, b) -> {
        b.suggest("true");
        b.suggest("false");
        return b.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("ric")
                        .then(CommandManager.literal("settings")
                                .requires(src -> Permissions.check(src,"ric.settings")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))

                                .then(CommandManager.literal("delay")
                                        .executes(SettingsCommands::showDelay)
                                        .then(CommandManager.argument("seconds", DoubleArgumentType.doubleArg(0.01))
                                                .requires(src -> Permissions.check(src,"ric.settings.delay")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeDelay(c, DoubleArgumentType.getDouble(c, "seconds")))
                                        )
                                )

                                .then(CommandManager.literal("numberPerTeam")
                                        .executes(SettingsCommands::showNumberPerTeam)
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(1))
                                                .requires(src -> Permissions.check(src,"ric.settings.numberPerTeam")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeNumberPerTeam(c, IntegerArgumentType.getInteger(c, "value")))
                                        )
                                )

                                .then(CommandManager.literal("testMode")
                                        .executes(c -> showSetting(c, "testMode"))
                                        .then(CommandManager.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> Permissions.check(src,"ric.settings.testMode")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "testMode"))
                                        )
                                )

                                .then(CommandManager.literal("resumeMode")
                                        .executes(c -> showSetting(c, "resumeMode"))
                                        .then(CommandManager.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> Permissions.check(src,"ric.settings.resumeMode")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "resumeMode"))
                                        )
                                )

                                .then(CommandManager.literal("teamMode")
                                        .executes(c -> showSetting(c, "teamMode"))
                                        .then(CommandManager.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> Permissions.check(src,"ric.settings.teamMode")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "teamMode"))
                                        )
                                )

                                .then(CommandManager.literal("enableMobFriendlyFire")
                                        .executes(c -> showSetting(c, "enableMobFriendlyFire"))
                                        .then(CommandManager.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> Permissions.check(src,"ric.settings.enableMobFriendlyFire")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "enableMobFriendlyFire"))
                                        )
                                )

                                .then(CommandManager.literal("autoTntIgnite")
                                        .executes(c -> showSetting(c, "autoTntIgnite"))
                                        .then(CommandManager.argument("value", StringArgumentType.word())
                                                .suggests(TRUE_FALSE)
                                                .requires(src -> Permissions.check(src,"ric.settings.autoTntIgnite")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                                .executes(c -> changeSetting(c, "autoTntIgnite"))
                                        )
                                )

                                .then(CommandManager.literal("clearTeams")
                                        .requires(src -> Permissions.check(src,"ric.settings.clearTeams")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                        .executes(c -> {
                                            clearAllTeams(c.getSource());
                                            return 1;
                                        })
                                )
                        )
        );
    }

    private static int showNumberPerTeam(CommandContext<ServerCommandSource> c) {
        c.getSource().sendFeedback(() -> Text.literal("Number per team is set to " + GoCommand.numberPerTeam).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int changeNumberPerTeam(CommandContext<ServerCommandSource> c, int v) {
        int pc = Gamerz.gamersList.size();
        if (pc == 0) {
            c.getSource().sendError(Text.literal("No players are currently in the game.").formatted(Formatting.RED));
            return 0;
        }
        if (v > pc / 2) {
            c.getSource().sendError(Text.literal("Number per team can't be bigger than half the number of players").formatted(Formatting.RED));
            return 0;
        }
        GoCommand.numberPerTeam = v;
        c.getSource().sendFeedback(() -> Text.literal("Number per team set to " + v).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int showDelay(CommandContext<ServerCommandSource> c) {
        c.getSource().sendFeedback(() -> Text.literal("Delay is set to " + Random_Item_Challenge.delay + " seconds").formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int changeDelay(CommandContext<ServerCommandSource> c, double s) {
        if (s <= 0) {
            c.getSource().sendError(Text.literal("Delay must be greater than 0").formatted(Formatting.RED));
            return 0;
        }
        Random_Item_Challenge.delay = s;
        Random_Item_Challenge.getConfigManager().delay = s;
        Random_Item_Challenge.getConfigManager().saveConfig();
        c.getSource().sendFeedback(() -> Text.literal("Delay set to " + s + " seconds").formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int showSetting(CommandContext<ServerCommandSource> c, String s) {
        Boolean v = getSettingValue(s);
        if (v == null) {
            c.getSource().sendError(Text.literal("Unknown setting: " + s).formatted(Formatting.RED));
            return 0;
        }
        c.getSource().sendFeedback(() -> Text.literal(s + " is set to " + v).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int changeSetting(CommandContext<ServerCommandSource> c, String s) {
        boolean on = "true".equalsIgnoreCase(StringArgumentType.getString(c, "value"));

        switch (s) {
            case "testMode" -> GoCommand.testMode = on;
            case "resumeMode" -> GoCommand.resumeMode = on;
            case "teamMode" -> {
                GoCommand.teamMode = on;
                if (!on) {
                    c.getSource().sendFeedback(() -> Text.literal(s + " set to " + on).formatted(Formatting.AQUA), false);
                    TeamsCommand.clearAllTeams(c.getSource(), false);
                    return 1;
                }
            }
            case "enableMobFriendlyFire" -> {
                if (StopCommand.isRunning && !on) {
                    c.getSource().sendError(Text.literal("You cannot disable mob friendly fire while the game is running.").formatted(Formatting.RED));
                    return 0;
                }
                GoCommand.enableMobFriendlyFire = on;
                Random_Item_Challenge.getConfigManager().enablemobfriendlyfire = on;
                Random_Item_Challenge.getConfigManager().saveConfig();

                Scoreboard sb = c.getSource().getServer().getScoreboard();
                if (on) new ArrayList<>(sb.getTeams()).forEach(sb::removeTeam);
                else sb.getTeams().forEach(t -> t.setFriendlyFireAllowed(false));
            }
            case "autoTntIgnite" -> {
                GoCommand.enableTntAutoExplode = on;
                Random_Item_Challenge.getConfigManager().enabletntautoexplode = on;
                Random_Item_Challenge.getConfigManager().saveConfig();
            }
            default -> {
                c.getSource().sendError(Text.literal("Unknown setting: " + s).formatted(Formatting.RED));
                return 0;
            }
        }

        c.getSource().sendFeedback(() -> Text.literal(s + " set to " + on).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static Boolean getSettingValue(String s) {
        return switch (s) {
            case "testMode" -> GoCommand.testMode;
            case "resumeMode" -> GoCommand.resumeMode;
            case "teamMode" -> GoCommand.teamMode;
            case "enableMobFriendlyFire" -> GoCommand.enableMobFriendlyFire;
            case "autoTntIgnite" -> GoCommand.enableTntAutoExplode;
            default -> null;
        };
    }

    private static void clearAllTeams(ServerCommandSource src) {
        TeamsCommand.teams.clear();
        TeamsCommand.teamsWithoutRemovals.clear();
        TeamsCommand.teamColors.clear();

        Scoreboard sb = src.getServer().getScoreboard();
        List<net.minecraft.scoreboard.Team> teams = new ArrayList<>(sb.getTeams());
        teams.forEach(sb::removeTeam);

        src.sendFeedback(() -> Text.literal("All RIC teams cleared.").formatted(Formatting.AQUA), false);
    }
}
