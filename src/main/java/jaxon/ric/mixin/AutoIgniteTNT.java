package jaxon.ric.mixin;

import jaxon.ric.command.GoCommand;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.TntEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class AutoIgniteTNT {
    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void ric$igniteTnt(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        if (!GoCommand.enableTntAutoExplode) return;
        if (ctx.getWorld().isClient()) return;
        if (!ctx.getStack().isOf(Blocks.TNT.asItem())) return;
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        if (world instanceof ServerWorld serverWorld && ctx.getPlayer() instanceof ServerPlayerEntity player) {
            TntEntity tnt = new TntEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, player);
            serverWorld.spawnEntity(tnt);
            ctx.getStack().decrement(1);
            cir.setReturnValue(null);
        }
    }
}
