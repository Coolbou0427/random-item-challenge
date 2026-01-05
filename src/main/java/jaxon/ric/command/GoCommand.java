package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import jaxon.ric.Gamerz;
import jaxon.ric.Random_Item_Challenge;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static jaxon.ric.Random_Item_Challenge.server;

public class GoCommand {
    public static boolean testMode = false;
    public static boolean resumeMode;
    public static String playerName;
    public static boolean teamMode;
    public static int numberPerTeam;
    public static boolean enableMobFriendlyFire = false;
    public static boolean enableTntAutoExplode = true;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("go")
                        .requires(src -> Permissions.check(src,"ric.go")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                        .executes(GoCommand::executeGoCommand)
                        .then(CommandManager.literal("teams").requires(src -> Permissions.check(src,"ric.go.teams")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                .executes(GoCommand::automaticTeams)
                                .then(CommandManager.literal("choose").requires(src -> Permissions.check(src,"ric.go.teams.choose")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                                        .executes(GoCommand::chooseTeams)
                                )
                        )
        );
    }

    private static int automaticTeams(CommandContext<ServerCommandSource> context) {
        teamMode = true;

        ServerCommandSource source = context.getSource();
        TeamsCommand.clearAllTeams(source, false);

        ServerPlayerEntity executor = source.getPlayer();
        if (executor == null) {
            source.sendError(Text.literal("This command must be run by a player.").formatted(Formatting.RED));
            return 0;
        }

        playerName = executor.getName().getString();

        MinecraftServer srv = source.getServer();
        List<ServerPlayerEntity> gamertags = new ArrayList<>(srv.getPlayerManager().getPlayerList());
        Gamerz.gamersList.clear();
        for (ServerPlayerEntity gamer : gamertags) {
            new Gamerz(gamer);
        }
        if (Gamerz.gamersList.isEmpty()) return 0;

        TeamsCommand.teamsWithoutRemovals = new HashMap<>(TeamsCommand.teams);
        return 1;
    }

    private static int chooseTeams(CommandContext<ServerCommandSource> context) {
        TeamsCommand.clearAllTeams(context.getSource(), false);
        if (TeamsCommand.teams == null) {
            TeamsCommand.teams = new HashMap<>();
        }

        Text header = Text.literal("Choose your teams").formatted(Formatting.GREEN);

        Text line1 = Text.literal("- ").formatted(Formatting.GRAY)
                .append(Text.literal("Start with ").formatted(Formatting.GRAY))
                .append(Text.literal("/go").formatted(Formatting.AQUA));

        Text line2 = Text.literal("- ").formatted(Formatting.GRAY)
                .append(Text.literal("Join: ").formatted(Formatting.GRAY))
                .append(Text.literal("/jointeam <name>").formatted(Formatting.AQUA));

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal(" "), false);
            player.sendMessage(header, false);
            player.sendMessage(line1, false);
            player.sendMessage(line2, false);
            player.sendMessage(Text.literal(" "), false);
        }

        teamMode = true;
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

            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("This command must be run by a player.").formatted(Formatting.RED));
                return 0;
            }

            if (StopCommand.isRunning) {
                StopCommand.isRunning = false;
                broadcastTitle(server, Text.literal("Stopped").formatted(Formatting.RED));
            }

            playerName = player.getName().getString();
            MinecraftServer srv = source.getServer();

            List<ServerPlayerEntity> gamertags = new ArrayList<>(srv.getPlayerManager().getPlayerList());
            Gamerz.gamersList.clear();
            for (ServerPlayerEntity gamer : gamertags) {
                new Gamerz(gamer);
            }

            srv.execute(() -> {
                try {
                    double px = player.getX();
                    double py = player.getY();
                    double pz = player.getZ();
                    BlockPos bp = BlockPos.ofFloored(px, py, pz);
                    double tx = bp.getX() + 0.5;
                    double tz = bp.getZ() + 0.5;
                    for (ServerPlayerEntity p : srv.getPlayerManager().getPlayerList()) {
                        if (p == null) continue;
                        p.requestTeleport(tx, py, tz);
                    }
                } catch (Exception e) {
                    Random_Item_Challenge.LOGGER.error("Error while teleporting players to starter", e);
                }

                Random_Item_Challenge.mainFunc();
            });

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

            BlockPos bpCentered = BlockPos.ofFloored(px, py, pz);
            double centerX = bpCentered.getX() + 0.5;
            double centerZ = bpCentered.getZ() + 0.5;

            ServerWorld overworld = srv.getWorld(World.OVERWORLD);
            if (overworld != null) overworld.getWorldBorder().setCenter(centerX, centerZ);

            ServerWorld nether = srv.getWorld(World.NETHER);
            if (nether != null) nether.getWorldBorder().setCenter(centerX / 8.0, centerZ / 8.0);

            ServerWorld end = srv.getWorld(World.END);
            if (end != null) end.getWorldBorder().setCenter(centerX, centerZ);

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

    private static void broadcastTitle(MinecraftServer srv, Text title) {
        TitleFadeS2CPacket timings = new TitleFadeS2CPacket(10, 70, 20);
        TitleS2CPacket titlePacket = new TitleS2CPacket(title);
        for (ServerPlayerEntity player : srv.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(timings);
            player.networkHandler.sendPacket(titlePacket);
        }
    }

    public static boolean checkAllPlayersAreOnATeam() {
        if (teamMode) {
            for (Gamerz g : Gamerz.gamersList) {
                boolean onTeam = false;
                for (List<String> members : TeamsCommand.teams.values()) {
                    if (members.contains(g.name)) {
                        onTeam = true;
                        break;
                    }
                }
                if (!onTeam) return false;
            }
        }
        return true;
    }
}
