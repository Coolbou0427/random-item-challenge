package jaxon.ric.mixin;

import jaxon.ric.PlacedBlockTracking;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.block.Block")
public abstract class PlacedBlockDropMixin {
	@Redirect(
			method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z")
	)
	private static boolean ric$allowPlacedBlockBrokenByLostSupport(LevelAccessor level, BlockPos pos,
			boolean drops, Entity breaker, int flags, BlockState oldState, BlockState newState,
			LevelAccessor originalLevel, BlockPos originalPos, int updateFlags, int recursionLeft) {
		boolean eligible = newState.isAir() && level instanceof ServerLevel serverLevel
				&& PlacedBlockTracking.isTracked(serverLevel, pos, oldState);
		if (!eligible) return level.destroyBlock(pos, drops, breaker, flags);

		PlacedBlockTracking.beginSupportBreak(level, pos);
		try {
			return level.destroyBlock(pos, drops, breaker, flags);
		} finally {
			PlacedBlockTracking.endSupportBreak();
		}
	}

	@Inject(method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
	private static void ric$beginItemDrop(Level level, net.minecraft.core.BlockPos pos,
			net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
		PlacedBlockTracking.beginEmission(level, pos);
	}

	@Inject(method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
	private static void ric$endItemDrop(Level level, net.minecraft.core.BlockPos pos,
			net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
		PlacedBlockTracking.endEmission();
	}

	@Inject(method = "popResourceFromFace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
	private static void ric$beginFacedItemDrop(Level level, net.minecraft.core.BlockPos pos,
			net.minecraft.core.Direction face, net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
		PlacedBlockTracking.beginEmission(level, pos);
	}

	@Inject(method = "popResourceFromFace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
	private static void ric$endFacedItemDrop(Level level, net.minecraft.core.BlockPos pos,
			net.minecraft.core.Direction face, net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
		PlacedBlockTracking.endEmission();
	}

	@Redirect(
			method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;")
	)
	private static Object ric$allowTrackedBlockDrop(GameRules rules, GameRule<?> rule) {
		Object value = rules.get(rule);
		if (rule == GameRules.BLOCK_DROPS && Boolean.FALSE.equals(value)
				&& PlacedBlockTracking.shouldAllowBlockDrop()) return Boolean.TRUE;
		return value;
	}
}
