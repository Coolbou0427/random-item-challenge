package jaxon.ric;

import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import jaxon.ric.command.TeamsCommand;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static jaxon.ric.Random_Item_Challenge.server;

public class Winner {

    public static void Declare() {
        StopCommand.isRunning = false;

        MinecraftServer srv = server;
        if (srv == null) return;

        if (GoCommand.teamMode) {
            if (TeamsCommand.teams == null || TeamsCommand.teams.isEmpty()) {
                broadcastChat(srv, Text.literal("Error: No winning team found.").formatted(Formatting.RED));
                return;
            }

            String teamKey = TeamsCommand.teams.keySet().iterator().next();
            List<String> winnersAlive = TeamsCommand.teams.get(teamKey);
            List<String> winnersAll = TeamsCommand.teamsWithoutRemovals.get(teamKey);
            List<String> winnersSource = (winnersAll != null && !winnersAll.isEmpty()) ? winnersAll : winnersAlive;

            String displayTeamRaw = TeamsCommand.teamDisplayNames.getOrDefault(teamKey, teamKey);
            String displayTeam = capitalizeDisplay(displayTeamRaw.replace('_', ' '));
            String winnersList = winnersSource == null ? "" : String.join(", ", winnersSource);

            Text titleText = Text.literal(displayTeam + " Won!").formatted(Formatting.GOLD);
            Text actionbarText = Text.literal("Winners: " + winnersList).formatted(Formatting.GOLD);

            broadcastChat(srv, Text.literal(displayTeam + " Won!").formatted(Formatting.GOLD));
            broadcastWin(srv, titleText, null, actionbarText);

            resetWorldBorder(srv);
            return;
        }

        if (Gamerz.gamersList.isEmpty()) {
            broadcastChat(srv, Text.literal("Error: No winner found.").formatted(Formatting.RED));
            return;
        }

        UUID winnerUuid = Gamerz.gamersList.getFirst().uuid;
        ServerPlayerEntity winnerPlayer = Random_Item_Challenge.getPlayerByUuid(winnerUuid);

        String winnerName = winnerPlayer != null ? winnerPlayer.getName().getString() : "Unknown";
        float healthRaw = winnerPlayer != null ? winnerPlayer.getHealth() : 0.0F;
        int hearts = Math.round(healthRaw) / 2;

        Text titleText = Text.literal(winnerName + " Won!").formatted(Formatting.GOLD);
        Text subtitleText = Text.literal("Health: " + hearts + " ❤").formatted(Formatting.RED);

        broadcastChat(srv, Text.literal(winnerName + " Won!").formatted(Formatting.GOLD));
        broadcastChat(srv, Text.literal("Health: " + hearts + " ❤").formatted(Formatting.RED));
        broadcastWin(srv, titleText, subtitleText, null);

        resetWorldBorder(srv);
    }

    private static void broadcastChat(MinecraftServer srv, Text msg) {
        srv.getPlayerManager().broadcast(msg, false);
    }

    private static void broadcastWin(MinecraftServer srv, Text title, Text subtitle, Text actionbar) {
        TitleFadeS2CPacket timings = new TitleFadeS2CPacket(10, 60, 10);
        TitleS2CPacket titlePacket = title != null ? new TitleS2CPacket(title) : null;
        SubtitleS2CPacket subtitlePacket = subtitle != null ? new SubtitleS2CPacket(subtitle) : null;
        OverlayMessageS2CPacket actionbarPacket = actionbar != null ? new OverlayMessageS2CPacket(actionbar) : null;

        for (ServerPlayerEntity player : srv.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(timings);
            if (titlePacket != null) player.networkHandler.sendPacket(titlePacket);
            if (subtitlePacket != null) player.networkHandler.sendPacket(subtitlePacket);
            if (actionbarPacket != null) player.networkHandler.sendPacket(actionbarPacket);
        }
    }

    private static void resetWorldBorder(MinecraftServer srv) {
        for (var world : srv.getWorlds()) {
            world.getWorldBorder().setSize(4.9999999E7);
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
}
