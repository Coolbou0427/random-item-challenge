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

    private static final Random random = new Random();

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
        for (String display : teamDisplayNames.values()) builder.suggest(display);
        return builder.buildFuture();
    }

    private static int joinTeam(CommandContext<ServerCommandSource> context) {
        String displayName = StringArgumentType.getString(context, "teamName").trim();
        if (displayName.isEmpty()) {
            context.getSource().sendError(Text.of("Team name cannot be empty"));
            return 0;
        }

        String id = sanitize(displayName);
        String player = Objects.requireNonNull(context.getSource().getPlayer()).getName().getString();
        Scoreboard board = context.getSource().getServer().getScoreboard();
        Team current = board.getScoreHolderTeam(player);

        if (current != null && current.getName().equals(id)) {
            rollColour(id, current);
            broadcastTeams();
            return 1;
        }

        pruneOldTeams(player, board);

        if (current != null) board.removeScoreHolderFromTeam(player, current);

        teams.computeIfAbsent(id, k -> new ArrayList<>());
        teamDisplayNames.putIfAbsent(id, displayName);

        if (!teamColors.containsKey(id)) teamColors.put(id, pickColour());
        Formatting colour = teamColors.get(id);

        Team mcTeam = board.getTeam(id);
        if (mcTeam == null) mcTeam = board.addTeam(id);
        mcTeam.setDisplayName(Text.of(displayName));
        mcTeam.setColor(colour);

        if (!teams.get(id).contains(player)) teams.get(id).add(player);
        board.addScoreHolderToTeam(player, mcTeam);

        broadcastTeams();
        return 1;
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
        Formatting newColour = pool.get(random.nextInt(pool.size()));
        team.setColor(newColour);
        teamColors.put(id, newColour);
    }

    private static void pruneOldTeams(String player, Scoreboard board) {
        for (Iterator<Map.Entry<String, List<String>>> it = teams.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<String>> e = it.next();
            List<String> members = e.getValue();
            if (members.remove(player) && members.isEmpty()) {
                String id = e.getKey();
                it.remove();
                teamColors.remove(id);
                teamDisplayNames.remove(id);
                Team t = board.getTeam(id);
                if (t != null) board.removeTeam(t);
            }
        }
    }

    private static Formatting pickColour() {
        List<Formatting> pool = new ArrayList<>(Arrays.asList(POSSIBLE_COLORS));
        pool.removeAll(teamColors.values());
        if (pool.isEmpty()) pool = new ArrayList<>(Arrays.asList(POSSIBLE_COLORS));
        return pool.get(random.nextInt(pool.size()));
    }

    private static void broadcastTeams() {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("Current Teams:").formatted(Formatting.GREEN), false);
        }
        teams.forEach((id, members) -> {
            Formatting c = teamColors.getOrDefault(id, Formatting.AQUA);
            String name = teamDisplayNames.getOrDefault(id, id.replace('_', ' '));
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(Text.literal(name + ": " + c + String.join(", ", members)).formatted(Formatting.GRAY), false);
            }
        });

    }

    private static int resetTeams(CommandContext<ServerCommandSource> context) {
        teams.clear();
        teamColors.clear();
        teamDisplayNames.clear();

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
}
