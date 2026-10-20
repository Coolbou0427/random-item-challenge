package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class NewSpawn {
    private static final int RANGE = 100_000;
    private static final int MAX_ATTEMPTS = 200;
    private static final int MIN_DIST = 1_000;
    private static final int NEAR_ATTEMPTS = 8;
    private static final int NEAR_RADIUS = 128;
    private static final Map<UUID, Set<Long>> visitedChunksByPlayer = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("newspawn")
                .requires(src -> src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .executes(NewSpawn::newSpawn));
    }

    private static int newSpawn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }

        player.sendOverlayMessage(Component.literal("Finding new spawn…").withStyle(ChatFormatting.GREEN));
        player.setGameMode(GameType.CREATIVE);

        UUID uuid = player.getUUID();
        visitedChunksByPlayer.putIfAbsent(uuid, new HashSet<>());
        Set<Long> visited = visitedChunksByPlayer.get(uuid);

        ServerLevel world = source.getLevel();
        boolean success = switch (world.dimension().identifier().getPath()) {
            case "overworld" -> trySpawnOverworld(player, world, visited);
            case "the_nether" -> trySpawnNether(player, world, visited);
            case "the_end" -> trySpawnEnd(player, world, visited);
            default -> false;
        };

        if (!success) {
            player.sendSystemMessage(Component.literal("Could not find a safe random spawn.").withStyle(ChatFormatting.RED));
        }
        return success ? 1 : 0;
    }

    private static boolean commonChecks(int x, int z, int baseX, int baseZ, Set<Long> visited) {
        long dx = x - baseX, dz = z - baseZ;
        if (dx * dx + dz * dz < (long) MIN_DIST * MIN_DIST) return false;
        int cx = x >> 4, cz = z >> 4;
        long key = ((long) cx << 32) | (cz & 0xffffffffL);
        if (visited.contains(key)) return false;
        visited.add(key);
        return true;
    }

    private static boolean trySpawnOverworld(ServerPlayer player, ServerLevel world, Set<Long> visited) {
        int baseX = player.getBlockX();
        int baseZ = player.getBlockZ();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            int baseCandidateX = rnd.nextInt(-RANGE, RANGE + 1);
            int baseCandidateZ = rnd.nextInt(-RANGE, RANGE + 1);

            for (int n = 0; n <= NEAR_ATTEMPTS; n++) {
                int x = n == 0 ? baseCandidateX : baseCandidateX + rnd.nextInt(-NEAR_RADIUS, NEAR_RADIUS + 1);
                int z = n == 0 ? baseCandidateZ : baseCandidateZ + rnd.nextInt(-NEAR_RADIUS, NEAR_RADIUS + 1);
                if (!commonChecks(x, z, baseX, baseZ, visited)) continue;

                BlockPos biomePos = new BlockPos(x, 64, z);
                ResourceKey<Biome> key = world.getBiome(biomePos).unwrapKey().orElse(null);
                if (key == null) continue;
                String path = key.identifier().getPath();
                if (path.contains("ocean") || path.contains("river")) continue;

                ChunkAccess chunk = world.getChunk(x >> 4, z >> 4);
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1;
                if (y < 50 || y > 150) continue;

                BlockPos ground = new BlockPos(x, y - 1, z);
                if (!world.getBlockState(ground).isRedstoneConductor(world, ground)) continue;
                if (!world.getFluidState(ground).isEmpty()) continue;

                player.teleportTo(x + 0.5, y, z + 0.5);
                return true;
            }
        }
        return false;
    }

    private static boolean trySpawnNether(ServerPlayer player, ServerLevel world, Set<Long> visited) {
        int baseX = player.getBlockX();
        int baseZ = player.getBlockZ();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            int x = rnd.nextInt(-RANGE, RANGE + 1);
            int z = rnd.nextInt(-RANGE, RANGE + 1);
            if (!commonChecks(x, z, baseX, baseZ, visited)) continue;

            for (int y = 118; y >= 32; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = world.getBlockState(pos);

                if (!state.isRedstoneConductor(world, pos)) continue;
                if (isNetherTreeCanopy(state)) continue;
                if (!isClearAbove(world, pos)) continue;

                Holder<Biome> biomeEntry = world.getBiome(pos);
                ResourceKey<Biome> biomeKey = biomeEntry.unwrapKey().orElse(null);
                if (Biomes.BASALT_DELTAS.equals(biomeKey)) break;

                player.teleportTo(x + 0.5, y + 1, z + 0.5);
                return true;
            }
        }
        return false;
    }

    private static boolean trySpawnEnd(ServerPlayer player, ServerLevel world, Set<Long> visited) {
        int baseX = player.getBlockX();
        int baseZ = player.getBlockZ();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            int x = rnd.nextInt(-RANGE, RANGE + 1);
            int z = rnd.nextInt(-RANGE, RANGE + 1);
            if (!commonChecks(x, z, baseX, baseZ, visited)) continue;

            ChunkAccess chunk = world.getChunk(x >> 4, z >> 4);
            int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
            BlockPos ground = new BlockPos(x, y - 1, z);
            if (!world.getBlockState(ground).isRedstoneConductor(world, ground)) continue;
            if (countSolidBlocks(world, ground) < 64) continue;

            player.teleportTo(x + 0.5, y, z + 0.5);
            return true;
        }
        return false;
    }

    private static boolean isNetherTreeCanopy(BlockState state) {
        return state.is(Blocks.WARPED_WART_BLOCK) || state.is(Blocks.NETHER_WART_BLOCK);
    }

    private static boolean isClearAbove(ServerLevel world, BlockPos pos) {
        for (int i = 1; i <= 4; i++) {
            if (!world.isEmptyBlock(pos.above(i))) return false;
        }
        return true;
    }

    private static int countSolidBlocks(ServerLevel world, BlockPos center) {
        int count = 0;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                if (world.getBlockState(center.offset(dx, 0, dz)).isRedstoneConductor(world, center)) count++;
            }
        }
        return count;
    }
}
