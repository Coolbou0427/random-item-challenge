package jaxon.ric.mixin;

import jaxon.ric.PlacedBlockTracking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class PlacedBlockWorldMixin {
	@Inject(method = "setBlock", at = @At("RETURN"))
	private void ric$clearReplacedPlacedBlock(BlockPos pos, BlockState newState, int flags, int recursionLeft,
			CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			PlacedBlockTracking.clearIfReplaced((Level) (Object) this, pos, newState);
		}
	}
}
