package jaxon.ric;

import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import jaxon.ric.command.TeamsCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

import static jaxon.ric.Random_Item_Challenge.server;

public class Winner {
    public static void Declare() {
        String playerName = Gamerz.gamersList.isEmpty() ? "Unknown" : Gamerz.gamersList.getFirst().name;
        StopCommand.isRunning = false;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (GoCommand.teamMode) {
            if (TeamsCommand.teams == null || TeamsCommand.teams.isEmpty()) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(Text.literal("Error: No winning team found.").formatted(Formatting.RED), false);
                }
                return;
            }
            String team = TeamsCommand.teams.keySet().iterator().next();
            List<String> winners = TeamsCommand.teams.get(team);
            String winnersList = String.join(", ", winners);
            String escapedTeam = escapeJson(team);
            String escapedWinnersList = escapeJson(winnersList);
            String titleMessage = String.format("title @a title {\"text\":\"%s Won!\",\"color\":\"gold\"}", escapedTeam);
            String actionbarMessage = String.format("title @a actionbar {\"text\":\"Winners: %s\",\"color\":\"gold\"}", escapedWinnersList);
            String tellrawMessage = String.format("tellraw @a {\"text\":\"%s Won!\",\"color\":\"gold\"}", escapedTeam);
            Random_Item_Challenge.sendCommand(titleMessage);
            Random_Item_Challenge.sendCommand(actionbarMessage);
            Random_Item_Challenge.sendCommand(tellrawMessage);
            Random_Item_Challenge.sendCommand("worldborder set 49999999");
            TeamsCommand.teams = null;
            GoCommand.teamMode = false;
        } else {
            String escapedPlayerName = escapeJson(playerName);
            int health = Math.round(Gamerz.getHealth(playerName)) / 2;
            String escapedHealth = escapeJson(health + " ❤");
            Random_Item_Challenge.sendCommand(String.format("title @a subtitle {\"text\":\"Health: %s\",\"color\":\"red\"}", escapedHealth));
            Random_Item_Challenge.sendCommand(String.format("title @a title {\"text\":\"%s Won!\",\"color\":\"gold\"}", escapedPlayerName));
            Random_Item_Challenge.sendCommand(String.format("tellraw @a {\"text\":\"%s Won!\",\"color\":\"gold\"}", escapedPlayerName));
            Random_Item_Challenge.sendCommand(String.format("tellraw @a {\"text\":\"Health: %s\",\"color\":\"red\"}", escapedHealth));
            Random_Item_Challenge.sendCommand("worldborder set 49999999");
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
