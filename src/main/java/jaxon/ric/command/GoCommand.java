package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import jaxon.ric.Gamerz;
import jaxon.ric.Random_Item_Challenge;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;

import java.util.*;

import static jaxon.ric.Random_Item_Challenge.server;

public class GoCommand {
    public static boolean testMode = false;
    public static boolean resumeMode;
    public static Thread mainThread;
    public static String playerName;
    public static boolean teamMode;
    public static int numberPerTeam;
    public static boolean enableMobFriendlyFire = false;
    public static boolean enableTntAutoExplode = true;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("go")
                        .requires(Permissions.require("ric.go", 2))
                        .executes(GoCommand::executeGoCommand)
                        .then(CommandManager.literal("teams").requires(Permissions.require("ric.go.teams", 2))
                                .executes(GoCommand::automaticTeams)
                                .then(CommandManager.literal("choose").requires(Permissions.require("ric.go.teams.choose", 2))
                                        .executes(GoCommand::chooseTeams)
                                )
                        )
        );
    }

    private static int automaticTeams(CommandContext<ServerCommandSource> context) {
        teamMode = true;
        ServerCommandSource source = context.getSource();
        playerName = Objects.requireNonNull(source.getPlayer()).getName().getString();
        MinecraftServer server = source.getServer();
        List<ServerPlayerEntity> gamertags = new ArrayList<>(server.getPlayerManager().getPlayerList());
        Gamerz.gamersList.clear();
        for (ServerPlayerEntity gamer : gamertags) {
            new Gamerz(gamer);
        }
        if (Gamerz.gamersList.isEmpty()) return 0;
        // ... rest unchanged ...
        TeamsCommand.teamsWithoutRemovals = new HashMap<>(TeamsCommand.teams);
        return 1;
    }

    private static int chooseTeams(CommandContext<ServerCommandSource> context) {
        // unchanged chooseTeams logic...
        return 1;
    }

    private static int executeGoCommand(CommandContext<ServerCommandSource> context) {
        try {
            if (teamMode && TeamsCommand.teams.isEmpty()) {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.sendMessage(Text.literal("Error: No teams have been created").formatted(Formatting.RED), false);
                }
                return 0;
            }
            if (!checkAllPlayersAreOnATeam()) {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.sendMessage(Text.literal("Error: Each player must be on a team").formatted(Formatting.RED), false);
                }
                return 0;
            }
            synchronized (Gamerz.gamersList) {
                if (mainThread != null && mainThread.isAlive()) {
                    mainThread.interrupt();
                    Random_Item_Challenge.sendCommand("title @a title [{\"text\":\"Stopped\",\"color\":\"red\"}]");
                }

                ServerCommandSource source = context.getSource();
                ServerPlayerEntity player = source.getPlayer();
                if (player == null) {
                    player.sendMessage(Text.literal("Finding new spawn…").formatted(Formatting.GREEN), false);
                    return 0;
                }
                playerName = player.getName().getString();
                MinecraftServer srv = source.getServer();

                List<ServerPlayerEntity> gamertags = new ArrayList<>(srv.getPlayerManager().getPlayerList());
                Gamerz.gamersList.clear();
                for (ServerPlayerEntity gamer : gamertags) new Gamerz(gamer);

                mainThread = new Thread(() -> {
                    try {
                        Random_Item_Challenge.mainFunc();
                    } finally {
                        mainThread = null;
                    }
                });
                mainThread.start();

                // Immediately recenter the world border after teleport commands
                srv.execute(GoCommand::centerWorldBorderAndSetSpawnpoint);
            }
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error in executeGoCommand", e);
        }
        return 1;
    }

    public static void centerWorldBorderAndSetSpawnpoint() {
        try {
            MinecraftServer srv = Random_Item_Challenge.server;
            ServerPlayerEntity player = srv.getPlayerManager().getPlayer(playerName);
            if (player == null) return;

            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();

            // Overworld center
            ServerWorld overworld = srv.getWorld(World.OVERWORLD);
            if (overworld != null) {
                overworld.getWorldBorder().setCenter(px, pz);
            }

            // Nether center
            ServerWorld nether = srv.getWorld(World.NETHER);
            if (nether != null) {
                nether.getWorldBorder().setCenter(px, pz);
            }

            // End center
            ServerWorld end = srv.getWorld(World.END);
            if (end != null) {
                end.getWorldBorder().setCenter(px, pz);
            }

            // Set spawn in current dimension
            RegistryKey<World> currentKey = player.getEntityWorld().getRegistryKey();
            ServerWorld current = srv.getWorld(currentKey);
            if (current != null) {
                BlockPos bp = BlockPos.ofFloored(px, py, pz);
                GlobalPos gp = GlobalPos.create(current.getRegistryKey(), bp);
                current.setSpawnPoint(new WorldProperties.SpawnPoint(gp, 0.0f, 0.0f));
            }
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error in centering borders", e);
        }
    }

    public static boolean checkAllPlayersAreOnATeam() {
        if (teamMode) {
            for (Gamerz g : Gamerz.gamersList) {
                boolean onTeam = false;
                for (List<String> members : TeamsCommand.teams.values()) {
                    if (members.contains(g.name)) { onTeam = true; break; }
                }
                if (!onTeam) return false;
            }
        }
        return true;
    }
}
