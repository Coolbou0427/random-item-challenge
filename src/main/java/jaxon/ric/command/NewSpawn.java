package jaxon.ric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.concurrent.ThreadLocalRandom;

public class NewSpawn {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("newspawn")
                .requires(src -> Permissions.check(src,"ric.newspawn")|| src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                .executes(NewSpawn::newSpawn));
    }

    private static int newSpawn(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (StopCommand.isRunning) {
            source.sendFeedback(() -> Text.literal("Error: Cannot run /newspawn while the game is running").formatted(Formatting.RED), false);
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command can only be run by a player.").formatted(Formatting.RED));
            return 0;
        }

        ServerWorld world = source.getWorld();
        boolean ok = newSpawnForPlayer(world, player);

        if (!ok) {
            source.sendFeedback(() -> Text.literal("Failed to find a safe spawn. Try again.").formatted(Formatting.RED), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Teleported to a new spawn.").formatted(Formatting.AQUA), false);
        return 1;
    }

    private static boolean newSpawnForPlayer(ServerWorld world, ServerPlayerEntity player) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < 200; attempt++) {
            int x = rnd.nextInt(-8000, 8001);
            int z = rnd.nextInt(-8000, 8001);

            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            int y = topY + 1;

            BlockPos feet = new BlockPos(x, y, z);
            BlockPos ground = feet.down();

            if (!world.getBlockState(ground).isSolidBlock(world, ground)) continue;
            if (!isClearAbove(world, feet)) continue;
            if (countSolidBlocks(world, ground) < 64) continue;

            player.requestTeleport(x + 0.5, y, z + 0.5);
            return true;
        }

        return false;
    }

    private static boolean isClearAbove(ServerWorld world, BlockPos pos) {
        for (int i = 0; i < 4; i++) {
            if (!world.isAir(pos.up(i))) return false;
        }
        return true;
    }

    private static int countSolidBlocks(ServerWorld world, BlockPos center) {
        int count = 0;

        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                BlockPos pos = center.add(dx, 0, dz);
                if (world.getBlockState(pos).isSolidBlock(world, pos)) count++;
            }
        }

        return count;
    }
}
