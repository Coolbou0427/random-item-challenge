package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static jaxon.ric.Random_Item_Challenge.server;

public class TeamsCommand {
    public static HashMap<String, List<String>> teams = new HashMap<>();
    public static HashMap<String, List<String>> teamsWithoutRemovals = new HashMap<>();
    public static HashMap<String, Formatting> teamColors = new HashMap<>();
    public static HashMap<String, String> teamDisplayNames = new HashMap<>();

    private static final Formatting[] POSSIBLE_COLORS = {
            Formatting.BLUE, Formatting.RED, Formatting.GREEN, Formatting.YELLOW,
            Formatting.GOLD, Formatting.DARK_PURPLE, Formatting.LIGHT_PURPLE, Formatting.AQUA
    };

    private static Formatting lastAssignedTeamColor = null;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("jointeam")
                .then(CommandManager.argument("teamName", StringArgumentType.greedyString())
                        .suggests(TeamsCommand::suggestTeams)
                        .executes(TeamsCommand::joinTeam)));

        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("teams")
                .then(CommandManager.literal("reset")
                        .executes(TeamsCommand::resetTeams)));
    }

    private static CompletableFuture<Suggestions> suggestTeams(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        for (String display : teamDisplayNames.values()) builder.suggest(capitalizeDisplay(display));
        return builder.buildFuture();
    }

    private static int joinTeam(CommandContext<ServerCommandSource> context) {
        String displayName = StringArgumentType.getString(context, "teamName").trim();
        if (displayName.isEmpty()) {
            context.getSource().sendError(Text.of("Team name cannot be empty"));
            return 0;
        }

        ServerPlayerEntity playerEntity = context.getSource().getPlayer();
        if (playerEntity == null) return 0;

        String player = playerEntity.getName().getString();
        Scoreboard board = context.getSource().getServer().getScoreboard();

        String id = sanitize(displayName);
        Team current = board.getScoreHolderTeam(player);

        if (current != null && current.getName().equals(id)) {
            rollColour(id, current);
            broadcastTeams();
            return 1;
        }

        if (current != null) {
            board.removeScoreHolderFromTeam(player, current);
            removePlayerFromTeamLists(player);
            cleanupEmptyTeam(current.getName(), board);
        } else {
            removePlayerFromTeamLists(player);
        }

        teams.computeIfAbsent(id, k -> new ArrayList<>());
        String canonicalDisplay = displayName.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        teamDisplayNames.putIfAbsent(id, canonicalDisplay);

        if (!teamColors.containsKey(id)) teamColors.put(id, pickColour());
        Formatting colour = teamColors.get(id);

        Team mcTeam = board.getTeam(id);
        if (mcTeam == null) mcTeam = board.addTeam(id);
        mcTeam.setDisplayName(Text.of(capitalizeDisplay(canonicalDisplay)));
        mcTeam.setColor(colour);

        if (!teams.get(id).contains(player)) teams.get(id).add(player);
        board.addScoreHolderToTeam(player, mcTeam);

        broadcastTeams();
        return 1;
    }

    private static void removePlayerFromTeamLists(String player) {
        for (List<String> members : teams.values()) {
            members.remove(player);
        }
    }

    private static void cleanupEmptyTeam(String teamId, Scoreboard board) {
        List<String> members = teams.get(teamId);
        if (members != null && members.isEmpty()) {
            teams.remove(teamId);
            teamColors.remove(teamId);
            teamDisplayNames.remove(teamId);
        }

        Team t = board.getTeam(teamId);
        if (t != null && t.getPlayerList().isEmpty()) {
            board.removeTeam(t);
        }
    }

    public static void clearAllTeams(ServerCommandSource src, boolean sendFeedback) {
        teams.clear();
        teamsWithoutRemovals.clear();
        teamColors.clear();
        teamDisplayNames.clear();
        lastAssignedTeamColor = null;

        Scoreboard board = src.getServer().getScoreboard();
        for (Team t : new ArrayList<>(board.getTeams())) board.removeTeam(t);

        if (sendFeedback) {
            src.sendFeedback(() -> Text.literal("All RIC teams cleared.").formatted(Formatting.AQUA), false);
        }
    }

    private static void rollColour(String id, Team team) {
        Formatting current = team.getColor();
        List<Formatting> pool = new ArrayList<>(Arrays.asList(POSSIBLE_COLORS));
        pool.remove(current);
        pool.removeAll(teamColors.values());
        if (pool.isEmpty()) {
            pool = new ArrayList<>(Arrays.asList(POSSIBLE_COLORS));
            pool.remove(current);
        }
        Formatting newColour = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        team.setColor(newColour);
        teamColors.put(id, newColour);
        lastAssignedTeamColor = newColour;
    }

    private static Formatting pickColour() {
        List<Formatting> pool = new ArrayList<>(Arrays.asList(POSSIBLE_COLORS));
        pool.removeAll(teamColors.values());

        if (lastAssignedTeamColor != null && pool.size() > 1) {
            pool.remove(lastAssignedTeamColor);
        }

        if (pool.isEmpty()) {
            pool = new ArrayList<>(Arrays.asList(POSSIBLE_COLORS));
            if (lastAssignedTeamColor != null && pool.size() > 1) {
                pool.remove(lastAssignedTeamColor);
            }
        }

        Formatting chosen = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        lastAssignedTeamColor = chosen;
        return chosen;
    }

    private static void broadcastTeams() {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("Current Teams:").formatted(Formatting.GREEN), false);
        }

        teams.forEach((id, members) -> {
            if (members == null || members.isEmpty()) return;

            Formatting c = teamColors.getOrDefault(id, Formatting.AQUA);
            String name = teamDisplayNames.getOrDefault(id, id.replace('_', ' '));
            String display = capitalizeDisplay(name.replace('_', ' '));

            Text line = Text.literal(display + ": ").formatted(Formatting.DARK_GRAY)
                    .append(Text.literal(String.join(", ", members)).formatted(c));

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(line, false);
            }
        });
    }

    private static int resetTeams(CommandContext<ServerCommandSource> context) {
        teams.clear();
        teamsWithoutRemovals.clear();
        teamColors.clear();
        teamDisplayNames.clear();
        lastAssignedTeamColor = null;

        Scoreboard board = context.getSource().getServer().getScoreboard();
        for (Team t : new ArrayList<>(board.getTeams())) board.removeTeam(t);

        GoCommand.teamMode = false;
        GoCommand.numberPerTeam = 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("All teams have been reset.").formatted(Formatting.RED), false);
        }
        return 1;
    }

    private static String sanitize(String display) {
        String id = display.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
        return id.length() > 16 ? id.substring(0, 16) : id;
    }

    private static String capitalizeDisplay(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] parts = s.split("[ _]+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(p.substring(0, 1).toUpperCase(Locale.ROOT));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }
}
