package jaxon.ric.mixin;

import jaxon.ric.DirectEnchantingHooks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class DirectEnchantingMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void ric$applyBook(int slotId, int button, ContainerInput input, Player player, CallbackInfo ci) {
        if (input != ContainerInput.PICKUP || slotId < 0) return;
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotId >= menu.slots.size()) return;

        ItemStack book = menu.getCarried();
        Slot slot = menu.slots.get(slotId);
        ItemStack target = slot.getItem();
        if (!DirectEnchantingHooks.isDirectEnchantBook(book) || target.isEmpty()) return;
        if (!DirectEnchantingHooks.tryApply(book, target, player)) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
            ci.cancel();
            return;
        }

        book.shrink(1);
        menu.setCarried(book);
        slot.setChanged();
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        ci.cancel();
    }
}
