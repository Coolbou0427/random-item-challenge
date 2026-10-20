package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import jaxon.ric.Random_Item_Challenge;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class StopCommand {
    public static volatile boolean isRunning = false;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("halt")
                        .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .executes(StopCommand::stopEverything)
        );
    }

    public static int stopEverything(CommandContext<CommandSourceStack> context) {
        try {
            isRunning = false;
            GoCommand.testMode = false;
            GoCommand.teamMode = false;

            Random_Item_Challenge.resetGameState();

            broadcastTitle(context.getSource().getServer(), Component.literal("Stopped").withStyle(ChatFormatting.RED));
            resetWorldBorder(context.getSource().getServer());

            Random_Item_Challenge.clearRicTeams(context.getSource().getServer().getScoreboard());

            return 1;
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error while stopping the game: ", e);
            return 0;
        }
    }

    private static void broadcastTitle(MinecraftServer srv, Component title) {
        ClientboundSetTitlesAnimationPacket timings = new ClientboundSetTitlesAnimationPacket(10, 70, 20);
        ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(title);
        for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
            player.connection.send(timings);
            player.connection.send(titlePacket);
        }
    }

    private static void resetWorldBorder(MinecraftServer srv) {
        for (var world : srv.getAllLevels()) {
            world.getWorldBorder().setSize(4.9999999E7);
        }
    }
}
