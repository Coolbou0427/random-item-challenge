package jaxon.ric.mixin;

import jaxon.ric.command.GoCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class AutoIgniteTNT {
    @Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
    private void ric$igniteTnt(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
        if (!GoCommand.enableTntAutoExplode) return;
        if (ctx.getLevel().isClientSide()) return;
        if (!ctx.getItemInHand().is(Blocks.TNT.asItem())) return;

        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        if (world instanceof ServerLevel serverWorld && ctx.getPlayer() instanceof ServerPlayer player) {
            PrimedTnt tnt = new PrimedTnt(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, player);
            tnt.setFuse(80);
            serverWorld.addFreshEntity(tnt);
            serverWorld.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            ctx.getItemInHand().shrink(1);
            cir.setReturnValue(null);
        }
    }
}
