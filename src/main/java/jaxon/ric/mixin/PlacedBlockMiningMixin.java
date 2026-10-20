package jaxon.ric.mixin;

import jaxon.ric.PlacedBlockTracking;
import jaxon.ric.command.GoCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class PlacedBlockMiningMixin {
	@Shadow protected net.minecraft.server.level.ServerLevel level;
	@Shadow protected net.minecraft.server.level.ServerPlayer player;
	@Inject(method = "destroyBlock", at = @At("HEAD"))
	private void ric$beginPlacedBlockMining(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		boolean eligible = GoCommand.enablePlayerPlacedBlockDrops
				&& !player.getAbilities().instabuild
				&& !level.getGameRules().get(GameRules.BLOCK_DROPS)
				&& PlacedBlockTracking.isTracked(level, pos, level.getBlockState(pos));
		PlacedBlockTracking.beginMining(level, pos, eligible);
	}

	@Inject(method = "destroyBlock", at = @At("RETURN"))
	private void ric$endPlacedBlockMining(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		try {
			if (cir.getReturnValueZ()) PlacedBlockTracking.clearIfReplaced(level, pos, level.getBlockState(pos));
		} finally {
			PlacedBlockTracking.endMining();
		}
	}
}
