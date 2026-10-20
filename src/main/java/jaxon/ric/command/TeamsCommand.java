package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import jaxon.ric.Random_Item_Challenge;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class TeamsCommand {
    public static HashMap<String, List<String>> teams = new HashMap<>();
    public static HashMap<String, List<String>> teamsWithoutRemovals = new HashMap<>();
    public static HashMap<String, ChatFormatting> teamColors = new HashMap<>();
    public static HashMap<String, String> teamDisplayNames = new HashMap<>();

    private static final ChatFormatting[] POSSIBLE_COLORS = {
            ChatFormatting.BLUE, ChatFormatting.RED, ChatFormatting.GREEN, ChatFormatting.YELLOW,
            ChatFormatting.GOLD, ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.AQUA
    };

    private static ChatFormatting lastAssignedTeamColor = null;

    private static final Set<String> ricTeamIds = new HashSet<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("jointeam")
                .then(Commands.argument("teamName", StringArgumentType.greedyString())
                        .suggests(TeamsCommand::suggestTeams)
                        .executes(TeamsCommand::joinTeam)));

        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("teams")
                .then(Commands.literal("reset")
                        .executes(TeamsCommand::resetTeams)));
    }

    private static CompletableFuture<Suggestions> suggestTeams(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (String display : teamDisplayNames.values()) builder.suggest(capitalizeDisplay(display));
        return builder.buildFuture();
    }

    private static int joinTeam(CommandContext<CommandSourceStack> context) {
        String displayName = StringArgumentType.getString(context, "teamName").trim();
        if (displayName.isEmpty()) {
            context.getSource().sendFailure(Component.nullToEmpty("Team name cannot be empty"));
            return 0;
        }

        ServerPlayer playerEntity = context.getSource().getPlayer();
        if (playerEntity == null) return 0;

        String player = playerEntity.getName().getString();
        Scoreboard board = context.getSource().getServer().getScoreboard();

        String id = sanitize(displayName);
        PlayerTeam current = board.getPlayersTeam(player);

        if (current != null && current.getName().equals(id)) {
            rollColour(id, current);
            broadcastTeams();
            return 1;
        }

        if (current != null) {
            board.removePlayerFromTeam(player, current);
            removePlayerFromTeamLists(player);
            cleanupEmptyTeam(current.getName(), board);
        } else {
            removePlayerFromTeamLists(player);
        }

        teams.computeIfAbsent(id, k -> new ArrayList<>());
        String canonicalDisplay = displayName.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        teamDisplayNames.putIfAbsent(id, canonicalDisplay);

        if (!teamColors.containsKey(id)) teamColors.put(id, pickColour());
        ChatFormatting colour = teamColors.get(id);

        PlayerTeam mcTeam = board.getPlayerTeam(id);
        if (mcTeam == null) mcTeam = board.addPlayerTeam(id);
        mcTeam.setDisplayName(Component.nullToEmpty(capitalizeDisplay(canonicalDisplay)));
        mcTeam.setColor(java.util.Optional.ofNullable(net.minecraft.world.scores.TeamColor.byName(colour.name().toLowerCase(Locale.ROOT))));
        mcTeam.setAllowFriendlyFire(false);

        ricTeamIds.add(id);

        if (!teams.get(id).contains(player)) teams.get(id).add(player);
        board.addPlayerToTeam(player, mcTeam);

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
            ricTeamIds.remove(teamId);
        }

        PlayerTeam t = board.getPlayerTeam(teamId);
        if (t != null && t.getPlayers().isEmpty()) {
            board.removePlayerTeam(t);
        }
    }

    public static void createOrUpdateTeam(Scoreboard board, String id, String displayName) {
        if (board == null) return;
        if (id == null || id.isBlank()) return;

        teams.computeIfAbsent(id, k -> new ArrayList<>());

        String canonicalDisplay = displayName == null ? id.replace('_', ' ') : displayName.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        teamDisplayNames.putIfAbsent(id, canonicalDisplay);

        if (!teamColors.containsKey(id)) teamColors.put(id, pickColour());
        ChatFormatting colour = teamColors.get(id);

        PlayerTeam mcTeam = board.getPlayerTeam(id);
        if (mcTeam == null) mcTeam = board.addPlayerTeam(id);

        mcTeam.setDisplayName(Component.nullToEmpty(capitalizeDisplay(canonicalDisplay)));
        mcTeam.setColor(java.util.Optional.ofNullable(net.minecraft.world.scores.TeamColor.byName(colour.name().toLowerCase(Locale.ROOT))));
        mcTeam.setAllowFriendlyFire(false);

        ricTeamIds.add(id);
    }

    public static void keepRicPlayerFriendlyFireDisabled(Scoreboard board) {
        if (board == null) return;
        Set<String> teamIds = new HashSet<>(ricTeamIds);
        teamIds.addAll(teams.keySet());
        for (String id : teamIds) {
            PlayerTeam team = board.getPlayerTeam(id);
            if (team != null) team.setAllowFriendlyFire(false);
        }
    }

    public static void addPlayerToTeam(Scoreboard board, String playerName, String teamId) {
        if (board == null) return;
        if (playerName == null || playerName.isBlank()) return;
        if (teamId == null || teamId.isBlank()) return;

        PlayerTeam current = board.getPlayersTeam(playerName);
        if (current != null) {
            board.removePlayerFromTeam(playerName, current);
        }

        removePlayerFromTeamLists(playerName);

        teams.computeIfAbsent(teamId, k -> new ArrayList<>());
        if (!teams.get(teamId).contains(playerName)) teams.get(teamId).add(playerName);

        PlayerTeam t = board.getPlayerTeam(teamId);
        if (t != null) {
            board.addPlayerToTeam(playerName, t);
        }
    }


    public static void clearAllTeams(CommandSourceStack src, boolean sendFeedback) {
        Scoreboard board = src.getServer().getScoreboard();

        for (String teamId : new ArrayList<>(ricTeamIds)) {
            PlayerTeam t = board.getPlayerTeam(teamId);
            if (t != null) {
                board.removePlayerTeam(t);
            }
        }

        teams.clear();
        teamsWithoutRemovals.clear();
        teamColors.clear();
        teamDisplayNames.clear();
        ricTeamIds.clear();
        lastAssignedTeamColor = null;

        if (sendFeedback) {
            src.sendSuccess(() -> Component.literal("All RIC teams cleared.").withStyle(ChatFormatting.AQUA), false);
        }
    }

    private static void rollColour(String id, PlayerTeam team) {
        ChatFormatting current = team.getColor()
                .map(color -> ChatFormatting.valueOf(color.name()))
                .orElse(ChatFormatting.WHITE);
        List<ChatFormatting> pool = new ArrayList<>(Arrays.asList(POSSIBLE_COLORS));
        pool.remove(current);
        pool.removeAll(teamColors.values());
        if (pool.isEmpty()) {
            pool = new ArrayList<>(Arrays.asList(POSSIBLE_COLORS));
            pool.remove(current);
        }
        ChatFormatting newColour = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        team.setColor(java.util.Optional.ofNullable(net.minecraft.world.scores.TeamColor.byName(newColour.name().toLowerCase(Locale.ROOT))));
        teamColors.put(id, newColour);
        lastAssignedTeamColor = newColour;
    }

    private static ChatFormatting pickColour() {
        List<ChatFormatting> pool = new ArrayList<>(Arrays.asList(POSSIBLE_COLORS));
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

        ChatFormatting chosen = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        lastAssignedTeamColor = chosen;
        return chosen;
    }

    private static void broadcastTeams() {
        for (ServerPlayer player : Random_Item_Challenge.server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal("Current Teams:").withStyle(ChatFormatting.GREEN));
        }

        teams.forEach((id, members) -> {
            if (members == null || members.isEmpty()) return;

            ChatFormatting c = teamColors.getOrDefault(id, ChatFormatting.AQUA);
            String name = teamDisplayNames.getOrDefault(id, id.replace('_', ' '));
            String display = capitalizeDisplay(name.replace('_', ' '));

            Component line = Component.literal(display + ": ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(String.join(", ", members)).withStyle(c));

            for (ServerPlayer player : Random_Item_Challenge.server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(line);
            }
        });
    }

    private static int resetTeams(CommandContext<CommandSourceStack> context) {
        clearAllTeams(context.getSource(), false);

        for (ServerPlayer player : Random_Item_Challenge.server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal("RIC teams have been reset.").withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static String sanitize(String display) {
        String id = display.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
        return id.length() > 16 ? id.substring(0, 16) : id;
    }

    public static String capitalizeDisplay(String s) {
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
