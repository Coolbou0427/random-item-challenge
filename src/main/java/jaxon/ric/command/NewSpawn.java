package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class NewSpawn {
    private static final int RANGE = 100_000;
    private static final int MAX_ATTEMPTS = 200;
    private static final int MIN_DIST = 1_000;
    private static final int NEAR_ATTEMPTS = 8;
    private static final int NEAR_RADIUS = 128;
    private static final Map<UUID, Set<Long>> visitedChunksByPlayer = new HashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("newspawn")
                .requires(src -> src.hasPermissionLevel(2))
                .executes(NewSpawn::newSpawn));
    }

    private static int newSpawn(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (GoCommand.mainThread != null && GoCommand.mainThread.isAlive()) {
            source.sendFeedback(() -> Text.literal("Error: Cannot find a new spawn while the game is running").formatted(Formatting.RED), false);
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command can only be run by a player."));
            return 0;
        }

        player.sendMessage(Text.literal("Finding new spawn…").formatted(Formatting.GREEN), true);
        player.changeGameMode(GameMode.CREATIVE);

        UUID uuid = player.getUuid();
        visitedChunksByPlayer.putIfAbsent(uuid, new HashSet<>());
        Set<Long> visited = visitedChunksByPlayer.get(uuid);

        ServerWorld world = source.getWorld();
        boolean success = switch (world.getRegistryKey().getValue().getPath()) {
            case "overworld" -> trySpawnOverworld(player, world, visited);
            case "the_nether" -> trySpawnNether(player, world, visited);
            case "the_end" -> trySpawnEnd(player, world, visited);
            default -> false;
        };

        if (!success) {
            player.sendMessage(Text.literal("Could not find a safe random spawn.").formatted(Formatting.RED), false);
        }
        return success ? 1 : 0;
    }

    private static boolean commonChecks(ServerWorld world, int x, int z, int baseX, int baseZ, Set<Long> visited) {
        long dx = x - baseX, dz = z - baseZ;
        if (dx * dx + dz * dz < (long) MIN_DIST * MIN_DIST) return false;
        int cx = x >> 4, cz = z >> 4;
        long key = ((long) cx << 32) | (cz & 0xffffffffL);
        if (visited.contains(key)) return false;
        visited.add(key);
        return true;
    }

    private static boolean trySpawnOverworld(ServerPlayerEntity player, ServerWorld world, Set<Long> visited) {
        int baseX = player.getBlockX();
        int baseZ = player.getBlockZ();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            int baseCandidateX = rnd.nextInt(-RANGE, RANGE + 1);
            int baseCandidateZ = rnd.nextInt(-RANGE, RANGE + 1);

            for (int n = 0; n <= NEAR_ATTEMPTS; n++) {
                int x = n == 0 ? baseCandidateX : baseCandidateX + rnd.nextInt(-NEAR_RADIUS, NEAR_RADIUS + 1);
                int z = n == 0 ? baseCandidateZ : baseCandidateZ + rnd.nextInt(-NEAR_RADIUS, NEAR_RADIUS + 1);
                if (!commonChecks(world, x, z, baseX, baseZ, visited)) continue;

                BlockPos biomePos = new BlockPos(x, 64, z);
                RegistryKey<Biome> key = world.getBiome(biomePos).getKey().orElse(null);
                if (key == null) continue;
                String path = key.getValue().getPath();
                if (path.contains("ocean") || path.contains("river")) continue;

                Chunk chunk = world.getChunk(x >> 4, z >> 4);
                int y = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x & 15, z & 15) + 1;
                if (y < 50 || y > 150) continue;

                BlockPos ground = new BlockPos(x, y - 1, z);
                if (!world.getBlockState(ground).isSolidBlock(world, ground)) continue;
                if (!world.getFluidState(ground).isEmpty()) continue;

                player.requestTeleport(x + 0.5, y, z + 0.5);
                return true;
            }
        }
        return false;
    }

    private static boolean trySpawnNether(ServerPlayerEntity player, ServerWorld world, Set<Long> visited) {
        int baseX = player.getBlockX();
        int baseZ = player.getBlockZ();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            int x = rnd.nextInt(-RANGE, RANGE + 1);
            int z = rnd.nextInt(-RANGE, RANGE + 1);
            if (!commonChecks(world, x, z, baseX, baseZ, visited)) continue;

            for (int y = 118; y >= 32; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!world.getBlockState(pos).isSolidBlock(world, pos)) continue;
                if (!isClearAbove(world, pos, 4)) continue;

                RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
                RegistryKey<Biome> biomeKey = biomeEntry.getKey().orElse(null);
                if (BiomeKeys.BASALT_DELTAS.equals(biomeKey)) break;

                player.requestTeleport(x + 0.5, y + 1, z + 0.5);
                return true;
            }
        }
        return false;
    }

    private static boolean trySpawnEnd(ServerPlayerEntity player, ServerWorld world, Set<Long> visited) {
        int baseX = player.getBlockX();
        int baseZ = player.getBlockZ();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            int x = rnd.nextInt(-RANGE, RANGE + 1);
            int z = rnd.nextInt(-RANGE, RANGE + 1);
            if (!commonChecks(world, x, z, baseX, baseZ, visited)) continue;

            Chunk chunk = world.getChunk(x >> 4, z >> 4);
            int y = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
            BlockPos ground = new BlockPos(x, y - 1, z);
            if (!world.getBlockState(ground).isSolidBlock(world, ground)) continue;
            if (countSolidBlocks(world, ground, 8) < 64) continue;

            player.requestTeleport(x + 0.5, y, z + 0.5);
            return true;
        }
        return false;
    }

    private static boolean isClearAbove(ServerWorld world, BlockPos pos, int height) {
        for (int i = 1; i <= height; i++) {
            if (!world.isAir(pos.up(i))) return false;
        }
        return true;
    }

    private static int countSolidBlocks(ServerWorld world, BlockPos center, int radius) {
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (world.getBlockState(center.add(dx, 0, dz)).isSolidBlock(world, center)) count++;
            }
        }
        return count;
    }
}
