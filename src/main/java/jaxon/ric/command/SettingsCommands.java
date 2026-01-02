package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import jaxon.ric.Gamerz;
import jaxon.ric.Random_Item_Challenge;
import net.minecraft.command.CommandSource;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import me.lucko.fabric.api.permissions.v0.Permissions;

import java.util.ArrayList;
import java.util.Objects;

public class SettingsCommands {
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("ricsettings")
                        .then(CommandManager.literal("delay")
                                .executes(SettingsCommands::showDelay)
                                .then(CommandManager.argument("seconds", DoubleArgumentType.doubleArg())
                                        .requires(Permissions.require("ric.settings.delay", 2))
                                        .executes(c -> changeDelay(c, DoubleArgumentType.getDouble(c, "seconds")))))
                        .then(CommandManager.literal("numberPerTeam")
                                .executes(SettingsCommands::showNumberPerTeam)
                                .then(CommandManager.argument("value", IntegerArgumentType.integer())
                                        .requires(Permissions.require("ric.settings.numberperteam", 2))
                                        .executes(c -> changeNumberPerTeam(c, IntegerArgumentType.getInteger(c, "value")))))
                        .then(CommandManager.literal("testMode")
                                .executes(c -> showSetting(c, "testMode"))
                                .then(CommandManager.argument("value", StringArgumentType.word())
                                        .suggests((c, b) -> CommandSource.suggestMatching(new String[]{"true","false"}, b))
                                        .requires(Permissions.require("ric.settings.testmode", 2))
                                        .executes(c -> changeSetting(c, "testMode"))))
                        .then(CommandManager.literal("resumeMode")
                                .executes(c -> showSetting(c, "resumeMode"))
                                .then(CommandManager.argument("value", StringArgumentType.word())
                                        .suggests((c, b) -> CommandSource.suggestMatching(new String[]{"true","false"}, b))
                                        .requires(Permissions.require("ric.settings.resumemode", 2))
                                        .executes(c -> changeSetting(c, "resumeMode"))))
                        .then(CommandManager.literal("teamMode")
                                .executes(c -> showSetting(c, "teamMode"))
                                .then(CommandManager.argument("value", StringArgumentType.word())
                                        .suggests((c, b) -> CommandSource.suggestMatching(new String[]{"true","false"}, b))
                                        .requires(Permissions.require("ric.settings.teammode", 2))
                                        .executes(c -> changeSetting(c, "teamMode"))))
                        .then(CommandManager.literal("enableMobFriendlyFire")
                                .executes(c -> showSetting(c, "enableMobFriendlyFire"))
                                .then(CommandManager.argument("value", StringArgumentType.word())
                                        .suggests((c, b) -> CommandSource.suggestMatching(new String[]{"true","false"}, b))
                                        .requires(Permissions.require("ric.settings.mobfriendlyfire", 2))
                                        .executes(c -> changeSetting(c, "enableMobFriendlyFire"))))
                        .then(CommandManager.literal("autoTntIgnite")
                                .executes(c -> showSetting(c, "autoTntIgnite"))
                                .then(CommandManager.argument("value", StringArgumentType.word())
                                        .suggests((c, b) -> CommandSource.suggestMatching(new String[]{"true","false"}, b))
                                        .requires(Permissions.require("ric.settings.autotntignite", 2))
                                        .executes(c -> changeSetting(c, "autoTntIgnite"))))
        );
    }


    private static int showNumberPerTeam(CommandContext<ServerCommandSource> c) {
        c.getSource().sendFeedback(() -> Text.literal("Number per team is set to " + GoCommand.numberPerTeam).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int changeNumberPerTeam(CommandContext<ServerCommandSource> c, int v) {
        int pc = Gamerz.gamersList.size();
        if (pc == 0) {
            c.getSource().sendError(Text.literal("No players are currently in the game."));
            return 0;
        }
        if (v > pc / 2) {
            c.getSource().sendError(Text.literal("Number per team can't be bigger than half the number of players").formatted(Formatting.RED));
        } else {
            GoCommand.numberPerTeam = v;
            c.getSource().sendFeedback(() -> Text.literal("Number per team set to " + v).formatted(Formatting.AQUA), false);
        }
        return 1;
    }

    private static int showDelay(CommandContext<ServerCommandSource> c) {
        Objects.requireNonNull(c.getSource().getPlayer()).sendMessage(
                Text.literal("Delay is set to " + Random_Item_Challenge.delay + " seconds").formatted(Formatting.AQUA), false);
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
        boolean v;
        switch (s) {
            case "testMode"              -> v = GoCommand.testMode;
            case "resumeMode"            -> v = GoCommand.resumeMode;
            case "teamMode"              -> v = GoCommand.teamMode;
            case "enableMobFriendlyFire" -> v = GoCommand.enableMobFriendlyFire;
            case "autoTntIgnite"         -> v = GoCommand.enableTntAutoExplode;
            default -> {
                c.getSource().sendError(Text.literal("Unknown setting: " + s));
                return 0;
            }
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
                if (!on) clearAllTeams(c.getSource());
            }
            case "enableMobFriendlyFire" -> {
                if (StopCommand.isRunning && !on) {
                    c.getSource().sendError(Text.of("You cannot disable mob friendly fire while the game is running."));
                    return 0;
                }
                GoCommand.enableMobFriendlyFire = on;
                Random_Item_Challenge.getConfigManager().enablemobfriendlyfire = on;
                Random_Item_Challenge.getConfigManager().saveConfig();
                Scoreboard sb = c.getSource().getServer().getScoreboard();
                if (on) sb.getTeams().forEach(sb::removeTeam);
                else sb.getTeams().forEach(t -> t.setFriendlyFireAllowed(false));
            }
            case "autoTntIgnite" -> {
                GoCommand.enableTntAutoExplode = on;
                Random_Item_Challenge.getConfigManager().enabletntautoexplode = on;
                Random_Item_Challenge.getConfigManager().saveConfig();
            }
            default -> {
                c.getSource().sendError(Text.literal("Unknown setting: " + s));
                return 0;
            }
        }
        c.getSource().sendFeedback(() -> Text.literal(s + " set to " + on).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static void clearAllTeams(ServerCommandSource src) {
        TeamsCommand.teams.clear();
        TeamsCommand.teamsWithoutRemovals.clear();
        TeamsCommand.teamColors.clear();
        Scoreboard sb = src.getServer().getScoreboard();
        new ArrayList<>(sb.getTeams()).forEach(sb::removeTeam);
    }
}
