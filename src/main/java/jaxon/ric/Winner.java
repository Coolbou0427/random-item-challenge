package jaxon.ric;

import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import jaxon.ric.command.TeamsCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static jaxon.ric.Random_Item_Challenge.server;

public class Winner {

    private static final int ACTIONBAR_MAX_CHARS = 240;

    public static void Declare() {
        MinecraftServer srv = server;
        if (srv == null) return;

        try {
            if (GoCommand.teamMode) {
                List<TeamWin> wins = collectWinningTeams();

                if (wins.isEmpty()) {
                    broadcastChat(srv, Component.literal("Error: No winning team found.").withStyle(ChatFormatting.RED));
                    stopAndReset(srv);
                    return;
                }

                wins.sort(Comparator.comparing(a -> a.displayTeam.toLowerCase(Locale.ROOT)));

                Component titleText;
                Component actionbarText;

                if (wins.size() == 1) {
                    TeamWin win = wins.getFirst();

                    titleText = Component.literal(win.displayTeam + " Won!").withStyle(ChatFormatting.GOLD);
                    actionbarText = Component.literal(safeTruncate("Winners: " + win.winnersList, ACTIONBAR_MAX_CHARS)).withStyle(ChatFormatting.GOLD);

                    broadcastChat(srv, Component.literal(win.displayTeam + " Won!").withStyle(ChatFormatting.GOLD));
                } else {
                    titleText = Component.literal("Teams Tied!").withStyle(ChatFormatting.GOLD);

                    String combined = buildCombinedWinnersLine(wins);
                    actionbarText = Component.literal(safeTruncate(combined, ACTIONBAR_MAX_CHARS)).withStyle(ChatFormatting.GOLD);

                    broadcastChat(srv, Component.literal("Teams Tied!").withStyle(ChatFormatting.GOLD));
                    for (TeamWin win : wins) {
                        broadcastChat(srv, Component.literal(win.displayTeam + " Won!").withStyle(ChatFormatting.GOLD));
                    }
                }

                broadcastWin(srv, titleText, null, actionbarText);

                stopAndReset(srv);
                return;
            }

            if (Gamerz.gamersList.isEmpty()) {
                broadcastChat(srv, Component.literal("Error: No winner found.").withStyle(ChatFormatting.RED));
                stopAndReset(srv);
                return;
            }

            UUID winnerUuid = Gamerz.gamersList.getFirst().uuid;
            ServerPlayer winnerPlayer = Random_Item_Challenge.getPlayerByUuid(winnerUuid);

            String winnerName = winnerPlayer != null ? winnerPlayer.getName().getString() : "Unknown";
            float healthRaw = winnerPlayer != null ? winnerPlayer.getHealth() : 0.0F;
            int hearts = (int) Math.ceil(healthRaw / 2.0F);

            Component titleText = Component.literal(winnerName + " Won!").withStyle(ChatFormatting.GOLD);
            Component subtitleText = Component.literal("Health: " + hearts + " ❤").withStyle(ChatFormatting.RED);

            broadcastChat(srv, Component.literal(winnerName + " Won!").withStyle(ChatFormatting.GOLD));
            broadcastChat(srv, Component.literal("Health: " + hearts + " ❤").withStyle(ChatFormatting.RED));
            broadcastWin(srv, titleText, subtitleText, null);

            stopAndReset(srv);
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error while declaring winner: ", e);
        }
    }

    private static List<TeamWin> collectWinningTeams() {
        List<TeamWin> out = new ArrayList<>();

        Map<String, List<String>> aliveMap = TeamsCommand.teams;
        Map<String, List<String>> allMap = TeamsCommand.teamsWithoutRemovals;
        Map<String, String> displayMap = TeamsCommand.teamDisplayNames;

        if (aliveMap == null || aliveMap.isEmpty()) return out;

        for (Map.Entry<String, List<String>> entry : aliveMap.entrySet()) {
            String teamKey = entry.getKey();
            List<String> winnersAlive = entry.getValue();
            List<String> winnersAll = allMap != null ? allMap.get(teamKey) : null;

            List<String> winnersSource = (winnersAll != null && !winnersAll.isEmpty())
                    ? winnersAll
                    : (winnersAlive != null ? winnersAlive : List.of());

            String displayTeamRaw = (displayMap != null) ? displayMap.getOrDefault(teamKey, teamKey) : teamKey;
            String displayTeam = capitalizeDisplay(displayTeamRaw.replace('_', ' '));

            String winnersList;
            if (winnersSource.isEmpty()) {
                winnersList = "(none)";
            } else {
                winnersList = String.join(", ", winnersSource);
                if (winnersList.isBlank()) winnersList = "(none)";
            }

            out.add(new TeamWin(displayTeam, winnersList));
        }

        return out;
    }

    private static String buildCombinedWinnersLine(List<TeamWin> wins) {
        StringBuilder sb = new StringBuilder();
        sb.append("Winners: ");

        for (int i = 0; i < wins.size(); i++) {
            TeamWin w = wins.get(i);

            if (i > 0) sb.append(" | ");
            sb.append(w.displayTeam).append(": ").append(w.winnersList);
        }

        return sb.toString();
    }

    private static String safeTruncate(String s, int maxChars) {
        if (s == null) return "";
        if (s.length() <= maxChars) return s;
        if (maxChars <= 3) return s.substring(0, maxChars);
        return s.substring(0, maxChars - 3) + "...";
    }

    private static void stopAndReset(MinecraftServer srv) {
        srv.execute(() -> {
            Random_Item_Challenge.finishMatchPreservingDeathHandling();
        });
    }

    private static void broadcastChat(MinecraftServer srv, Component msg) {
        srv.getPlayerList().broadcastSystemMessage(msg, false);
    }

    private static void broadcastWin(MinecraftServer srv, Component title, Component subtitle, Component actionbar) {
        ClientboundSetTitlesAnimationPacket timings = new ClientboundSetTitlesAnimationPacket(10, 60, 10);
        ClientboundSetTitleTextPacket titlePacket = title != null ? new ClientboundSetTitleTextPacket(title) : null;
        ClientboundSetSubtitleTextPacket subtitlePacket = subtitle != null ? new ClientboundSetSubtitleTextPacket(subtitle) : null;
        ClientboundSetActionBarTextPacket actionbarPacket = actionbar != null ? new ClientboundSetActionBarTextPacket(actionbar) : null;

        for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
            player.connection.send(timings);
            if (titlePacket != null) player.connection.send(titlePacket);
            if (subtitlePacket != null) player.connection.send(subtitlePacket);
            if (actionbarPacket != null) player.connection.send(actionbarPacket);
        }
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

    private record TeamWin(String displayTeam, String winnersList) {}
}
