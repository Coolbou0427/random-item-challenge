package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import jaxon.ric.Random_Item_Challenge;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;

public class StopCommand {
    public static volatile boolean isRunning = true;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("halt")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(StopCommand::stopEverything));
    }

    public static int stopEverything(CommandContext<ServerCommandSource> context) {
        try {
            isRunning = false;
            GoCommand.testMode = false;
            if (Random_Item_Challenge.mainThread != null && Random_Item_Challenge.mainThread.isAlive()) {
                Random_Item_Challenge.mainThread.interrupt();
                Random_Item_Challenge.sendCommand("title @a title {\"text\":\"Stopped\",\"color\":\"red\"}");
                Random_Item_Challenge.sendCommand("worldborder set 49999999");
                Scoreboard sb = context.getSource().getWorld().getServer().getScoreboard();
                for (Team t : new ArrayList<>(sb.getTeams())) {
                    sb.removeTeam(t);
                }
                GoCommand.teamMode = false;
            }
            return 1;
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error while stopping the game: ", e);
            return 0;
        }
    }
}
