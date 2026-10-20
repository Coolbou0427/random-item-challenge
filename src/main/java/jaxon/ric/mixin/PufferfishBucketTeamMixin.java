package jaxon.ric.mixin;

import jaxon.ric.SpawnEggHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.item.MobBucketItem.class)
public abstract class PufferfishBucketTeamMixin {
    @Unique
    private static final ThreadLocal<LivingEntity> ric$bucketUser = new ThreadLocal<>();

    @Inject(method = "checkExtraContent", at = @At("HEAD"))
    private void ric$rememberBucketUser(LivingEntity user, Level level, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        ric$bucketUser.remove();
        if (stack.is(Items.PUFFERFISH_BUCKET)) ric$bucketUser.set(user);
    }

    @Inject(method = "checkExtraContent", at = @At("TAIL"))
    private void ric$clearBucketUser(LivingEntity user, Level level, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        ric$bucketUser.remove();
    }

    @Redirect(
            method = "spawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"
            )
    )
    private void ric$assignPufferfishBucketTeam(ServerLevel world, Entity spawned) {
        LivingEntity user = ric$bucketUser.get();
        if (user != null) SpawnEggHooks.onPufferfishBucketSpawn(world, user, spawned);
        world.addFreshEntityWithPassengers(spawned);
    }
}
