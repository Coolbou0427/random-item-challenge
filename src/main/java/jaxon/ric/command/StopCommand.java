package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import jaxon.ric.Random_Item_Challenge;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class StopCommand {
    public static volatile boolean isRunning = false;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("halt")
                        .requires(src -> Permissions.check(src,"ric.halt")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                        .executes(StopCommand::stopEverything)
        );
    }

    public static int stopEverything(CommandContext<ServerCommandSource> context) {
        try {
            isRunning = false;
            GoCommand.testMode = false;
            GoCommand.teamMode = false;

            Random_Item_Challenge.resetGameState();

            broadcastTitle(context.getSource().getServer(), Text.literal("Stopped").formatted(Formatting.RED));
            resetWorldBorder(context.getSource().getServer());

            Random_Item_Challenge.clearRicTeams(context.getSource().getServer().getScoreboard());

            return 1;
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error while stopping the game: ", e);
            return 0;
        }
    }

    private static void broadcastTitle(MinecraftServer srv, Text title) {
        TitleFadeS2CPacket timings = new TitleFadeS2CPacket(10, 70, 20);
        TitleS2CPacket titlePacket = new TitleS2CPacket(title);
        for (ServerPlayerEntity player : srv.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(timings);
            player.networkHandler.sendPacket(titlePacket);
        }
    }

    private static void resetWorldBorder(MinecraftServer srv) {
        for (var world : srv.getWorlds()) {
            world.getWorldBorder().setSize(4.9999999E7);
        }
    }
}
