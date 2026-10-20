package jaxon.ric.mixin;

import jaxon.ric.DirectEnchantingHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class EnchantedBookLoreMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ric$addDirectEnchantingLoreToInventory(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            DirectEnchantingHooks.ensureLore(stack);
        }
    }
}
