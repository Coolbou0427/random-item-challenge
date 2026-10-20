package jaxon.ric.mixin;

import jaxon.ric.PlacedBlockTracking;
import jaxon.ric.command.GoCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonBaseBlock.class)
public abstract class PlacedBlockPistonMixin {
	@Redirect(
			method = "moveBlocks",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
			)
	)
	private void ric$allowDropsForTrackedBlocksDestroyedByPistons(BlockState state, LevelAccessor level,
			BlockPos pos, BlockEntity blockEntity) {
		boolean trackedPlacement = GoCommand.enablePlayerPlacedBlockDrops
				&& level instanceof ServerLevel serverLevel
				&& PlacedBlockTracking.isTracked(serverLevel, pos, state);
		if (!trackedPlacement) {
			Block.dropResources(state, level, pos, blockEntity);
			return;
		}

		PlacedBlockTracking.beginSupportBreak(level, pos);
		try {
			Block.dropResources(state, level, pos, blockEntity);
		} finally {
			PlacedBlockTracking.endSupportBreak();
		}
	}
}
