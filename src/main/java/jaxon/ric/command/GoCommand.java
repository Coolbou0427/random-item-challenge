package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import jaxon.ric.Gamerz;
import jaxon.ric.Random_Item_Challenge;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.scores.Scoreboard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static jaxon.ric.Random_Item_Challenge.server;

public class GoCommand {
    public static boolean testMode = false;
    public static boolean resumeMode;
    public static boolean teamMode;
    public static int numberPerTeam;
    public static boolean enableMobFriendlyFire = false;
    public static boolean enableTntAutoExplode = true;
    public static boolean enablePlayerPlacedBlockDrops = true;

    private static final CommandSource NO_FEEDBACK_OUTPUT = new CommandSource() {
        @Override
        public void sendSystemMessage(Component message) {
        }

        @Override
        public boolean acceptsSuccess() {
            return false;
        }

        @Override
        public boolean acceptsFailure() {
            return false;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("go")
                        .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .executes(GoCommand::executeGoCommand)
                        .then(Commands.literal("teams").requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                .executes(GoCommand::automaticTeams)
                                .then(Commands.literal("choose").requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                        .executes(GoCommand::chooseTeams)
                                )
                        )
        );
    }

    private static int automaticTeams(CommandContext<CommandSourceStack> context) {
        teamMode = true;

        CommandSourceStack source = context.getSource();
        TeamsCommand.clearAllTeams(source, false);

        ServerPlayer executor = source.getPlayer();
        if (executor == null) {
            source.sendFailure(Component.literal("This command must be run by a player.").withStyle(ChatFormatting.RED));
            return 0;
        }

        MinecraftServer srv = source.getServer();

        int perTeam = numberPerTeam;
        if (perTeam < 1) {
            perTeam = 1;
            numberPerTeam = 1;
            source.sendSuccess(() -> Component.literal("numberPerTeam was not set. Defaulting to 1.").withStyle(ChatFormatting.YELLOW), false);
        }

        List<ServerPlayer> online = new ArrayList<>(srv.getPlayerList().getPlayers());
        if (online.isEmpty()) return 0;

        List<String> names = new ArrayList<>();
        for (ServerPlayer p : online) {
            if (p == null) continue;
            names.add(p.getName().getString());
        }

        if (names.isEmpty()) return 0;

        java.util.Collections.shuffle(names, java.util.concurrent.ThreadLocalRandom.current());

        int teamCount = (int) Math.ceil(names.size() / (double) perTeam);
        if (teamCount < 1) teamCount = 1;

        Scoreboard sb = srv.getScoreboard();

        for (int i = 0; i < teamCount; i++) {
            String id = "team_" + (i + 1);
            String display = "Team " + (i + 1);
            TeamsCommand.createOrUpdateTeam(sb, id, display);
        }

        for (int i = 0; i < names.size(); i++) {
            String player = names.get(i);
            String teamId = "team_" + ((i % teamCount) + 1);
            TeamsCommand.addPlayerToTeam(sb, player, teamId);
        }

        Gamerz.gamersList.clear();
        for (ServerPlayer p : online) {
            if (p == null) continue;
            new Gamerz(p);
        }
        if (Gamerz.gamersList.isEmpty()) return 0;

        TeamsCommand.teamsWithoutRemovals = deepCopyTeams(TeamsCommand.teams);

        source.sendSuccess(() -> Component.literal("Automatic teams created (" + numberPerTeam + " per team).").withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static HashMap<String, List<String>> deepCopyTeams(HashMap<String, List<String>> in) {
        HashMap<String, List<String>> out = new HashMap<>();
        if (in == null) return out;

        for (var e : in.entrySet()) {
            String k = e.getKey();
            List<String> v = e.getValue();
            if (k == null) continue;
            if (v == null) out.put(k, new ArrayList<>());
            else out.put(k, new ArrayList<>(v));
        }
        return out;
    }



    private static int chooseTeams(CommandContext<CommandSourceStack> context) {
        TeamsCommand.clearAllTeams(context.getSource(), false);
        if (TeamsCommand.teams == null) {
            TeamsCommand.teams = new HashMap<>();
        }

        Component header = Component.literal("Choose your teams").withStyle(ChatFormatting.GREEN);

        Component line1 = Component.literal("- ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Start with ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/go").withStyle(ChatFormatting.AQUA));

        Component line2 = Component.literal("- ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Join: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/jointeam <name>").withStyle(ChatFormatting.AQUA));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(" "));
            player.sendSystemMessage(header);
            player.sendSystemMessage(line1);
            player.sendSystemMessage(line2);
            player.sendSystemMessage(Component.literal(" "));
        }

        teamMode = true;
        return 1;
    }

    private static int executeGoCommand(CommandContext<CommandSourceStack> context) {
        try {
            if (teamMode && TeamsCommand.teams.isEmpty()) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(Component.literal("Error: No teams have been created").withStyle(ChatFormatting.RED));
                }
                return 0;
            }

            if (!checkAllPlayersAreOnATeam()) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(Component.literal("Error: Each player must be on a team").withStyle(ChatFormatting.RED));
                }
                return 0;
            }

            CommandSourceStack source = context.getSource();
            ServerPlayer starter = source.getPlayer();
            if (starter == null) {
                source.sendFailure(Component.literal("This command must be run by a player.").withStyle(ChatFormatting.RED));
                return 0;
            }

            if (StopCommand.isRunning) {
                StopCommand.isRunning = false;
                broadcastTitle(server, Component.literal("Stopped").withStyle(ChatFormatting.RED));
            }

            MinecraftServer srv = source.getServer();

            List<ServerPlayer> gamertags = new ArrayList<>(srv.getPlayerList().getPlayers());
            Gamerz.gamersList.clear();
            for (ServerPlayer gamer : gamertags) {
                new Gamerz(gamer);
            }

            srv.execute(() -> {
                try {
                    ServerLevel w = starter.level();
                    ResourceKey<Level> key = w.dimension();

                    double tx = starter.getX();
                    double ty = starter.getY();
                    double tz = starter.getZ();

                    // Create the vanilla End arrival platform before bringing
                    // players into the dimension, so they cannot fall while
                    // RIC waits for the start teleports to finish.
                    if (key == Level.END) ensureVanillaEndArrivalPlatform(w);

                    for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                        if (p == null) continue;
                        executeCommandNow(srv, "gamemode creative " + p.getName().getString());
                    }

                    for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                        if (p == null) continue;
                        // The starter is already at the destination. Avoid a
                        // redundant self-teleport, which causes a camera snap.
                        if (p == starter) continue;
                        p.teleportTo(w, tx, ty, tz, Set.of(), p.getYRot(), p.getXRot(), true);
                    }

                    Random_Item_Challenge.beginStartAfterTeleports(key, tx, ty, tz);
                } catch (Exception e) {
                    Random_Item_Challenge.LOGGER.error("Error while teleporting players to starter", e);
                }
            });

        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error in executeGoCommand", e);
        }
        return 1;
    }

    private static void executeCommandNow(MinecraftServer srv, String rawCommand) {
        try {
            String command = rawCommand == null ? "" : rawCommand.trim();
            if (command.isEmpty()) return;
            if (command.startsWith("/")) command = command.substring(1);

            CommandSourceStack src = srv.createCommandSourceStack().withSource(NO_FEEDBACK_OUTPUT);
            var dispatcher = srv.getCommands().getDispatcher();
            var parseResults = dispatcher.parse(command, src);
            dispatcher.execute(parseResults);

        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Command failed: {}", rawCommand, e);
        }
    }

    public static void centerWorldBorderAndSetSpawnpoint(ResourceKey<Level> currentKey,
                                                         double px, double py, double pz) {
        try {
            MinecraftServer srv = Random_Item_Challenge.server;
            if (srv == null || currentKey == null) return;

            BlockPos bpCentered = BlockPos.containing(px, py, pz);
            double centerX = bpCentered.getX();
            double centerZ = bpCentered.getZ();

            double overworldCenterX = centerX;
            double overworldCenterZ = centerZ;

            if (currentKey == Level.NETHER) {
                overworldCenterX = centerX * 8.0;
                overworldCenterZ = centerZ * 8.0;
            }

            ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
            if (overworld != null) overworld.getWorldBorder().setCenter(overworldCenterX, overworldCenterZ);

            ServerLevel nether = srv.getLevel(Level.NETHER);
            if (nether != null) nether.getWorldBorder().setCenter(overworldCenterX / 8.0, overworldCenterZ / 8.0);

            double endCenterX = (currentKey == Level.END) ? centerX : 0.0;
            double endCenterZ = (currentKey == Level.END) ? centerZ : 0.0;

            ServerLevel end = srv.getLevel(Level.END);
            if (end != null) {
                end.getWorldBorder().setCenter(endCenterX, endCenterZ);
                ensureVanillaEndArrivalPlatform(end);
            }

            // Persistent world spawn belongs to the Overworld. An End start
            // leaves the existing Overworld spawn unchanged.
            if (overworld != null && currentKey != Level.END) {
                double spawnX = currentKey == Level.NETHER ? centerX * 8.0 : centerX;
                double spawnZ = currentKey == Level.NETHER ? centerZ * 8.0 : centerZ;
                BlockPos spawnPos = BlockPos.containing(spawnX, py, spawnZ);
                GlobalPos gp = GlobalPos.of(Level.OVERWORLD, spawnPos);
                overworld.setRespawnData(new LevelData.RespawnData(gp, 0.0f, 0.0f));
            }
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error in centering borders", e);
        }
    }

    /**
     * Keep the normal End-portal arrival platform inside any startup border
     * command. The End platform is offset from the central island, so a small
     * border centered on the island can otherwise strand portal arrivals.
     */
    public static String keepEndArrivalInsideBorderCommand(String command) {
        String prefix = "execute in minecraft:the_end run worldborder set ";
        if (command == null || !command.startsWith(prefix)) return command;

        String[] parts = command.substring(prefix.length()).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return command;
        try {
            double configuredSize = Double.parseDouble(parts[0]);
            MinecraftServer srv = Random_Item_Challenge.server;
            ServerLevel end = srv == null ? null : srv.getLevel(Level.END);
            double centerX = end == null ? 0.0 : end.getWorldBorder().getCenterX();
            double centerZ = end == null ? 0.0 : end.getWorldBorder().getCenterZ();
            BlockPos arrival = ServerLevel.END_SPAWN_POINT;
            double requiredSize = 2.0 * Math.max(Math.abs(arrival.getX() - centerX),
                    Math.abs(arrival.getZ() - centerZ)) + 16.0;
            if (configuredSize >= requiredSize) return command;

            StringBuilder adjusted = new StringBuilder(prefix)
                    .append(String.format(java.util.Locale.ROOT, "%.1f", requiredSize));
            for (int i = 1; i < parts.length; i++) adjusted.append(' ').append(parts[i]);
            Random_Item_Challenge.LOGGER.info("Raised End world border startup size from {} to {} to include the arrival platform",
                    configuredSize, requiredSize);
            return adjusted.toString();
        } catch (NumberFormatException ignored) {
            return command;
        }
    }

    private static void ensureVanillaEndArrivalPlatform(ServerLevel end) {
        try {
            // Direct command teleports into the End do not necessarily perform
            // the normal End-portal arrival initialization. Load the central
            // End terrain and create the vanilla arrival platform without
            // resetting the End or touching dragon-fight state.
            for (int cx = -2; cx <= 2; cx++) {
                for (int cz = -2; cz <= 2; cz++) {
                    end.getChunk(cx, cz);
                }
            }
            EndPlatformFeature.createEndPlatform(end, ServerLevel.END_SPAWN_POINT, false);
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Failed to initialize the vanilla End arrival platform", e);
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
