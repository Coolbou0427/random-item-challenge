package jaxon.ric.mixin;

import jaxon.ric.PlacedBlockTracking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class PlacedBlockItemMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void ric$recordPlayerPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction()) return;
		if (!(context.getLevel() instanceof ServerLevel level)) return;

		var pos = context.getClickedPos();
		var state = level.getBlockState(pos);
		if (state.getBlock() == ((BlockItem) (Object) this).getBlock()) {
			PlacedBlockTracking.recordPlacement(level, pos, state);
			var other = companionPosition(pos, state);
			if (other != null) {
				BlockState otherState = level.getBlockState(other);
				if (otherState.getBlock() == state.getBlock()) {
					PlacedBlockTracking.recordPlacement(level, other, otherState);
				}
			}
		}
	}

	private static net.minecraft.core.BlockPos companionPosition(net.minecraft.core.BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof DoorBlock && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return pos.relative(state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF).getDirectionToOther());
		}
		if (state.getBlock() instanceof BedBlock && state.hasProperty(BlockStateProperties.BED_PART)
				&& state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			return pos.relative(state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT
					? facing : facing.getOpposite());
		}
		return null;
	}
}
