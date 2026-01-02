package jaxon.ric.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class FireChargeItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void ric$fireChargeLaunch(World world,
                                      PlayerEntity user,
                                      Hand hand,
                                      CallbackInfoReturnable<ActionResult> cir) {

        ItemStack stack = user.getStackInHand(hand);
        if (!(stack.getItem() instanceof FireChargeItem)) return;

        if (!world.isClient()) {
            Vec3d dir = user.getRotationVec(1.0F);
            FireballEntity fireball = new FireballEntity(world, user, dir, 1);

            fireball.setPos(
                    user.getX() + dir.x * 2.0,
                    user.getEyeY() + dir.y * 2.0,
                    user.getZ() + dir.z * 2.0
            );
            fireball.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 0.0F);
            world.spawnEntity(fireball);

            if (!user.getAbilities().creativeMode) stack.decrement(1);
            user.getItemCooldownManager().set(stack, 20);
        }

        cir.setReturnValue(world.isClient()
                ? ActionResult.SUCCESS
                : ActionResult.CONSUME
        );
    }
}
