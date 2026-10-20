package jaxon.ric.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class FireChargeItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void ric$fireChargeLaunch(Level world,
                                      Player user,
                                      InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir) {

        ItemStack stack = user.getItemInHand(hand);
        if (!(stack.getItem() instanceof FireChargeItem)) return;

        if (!world.isClientSide()) {
            Vec3 dir = user.getViewVector(1.0F);
            LargeFireball fireball = new LargeFireball(world, user, dir, 1);

            fireball.setPosRaw(
                    user.getX() + dir.x * 2.0,
                    user.getEyeY() + dir.y * 2.0,
                    user.getZ() + dir.z * 2.0
            );
            fireball.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, 1.5F, 0.0F);
            world.addFreshEntity(fireball);

            if (!user.getAbilities().instabuild) stack.shrink(1);
            user.getCooldowns().addCooldown(stack, 20);
        }

        cir.setReturnValue(world.isClientSide()
                ? InteractionResult.SUCCESS
                : InteractionResult.CONSUME
        );
    }
}
