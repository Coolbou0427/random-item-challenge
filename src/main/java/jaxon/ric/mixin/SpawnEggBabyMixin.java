package jaxon.ric.mixin;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(SpawnEggItem.class)
public class SpawnEggBabyMixin {

    @Inject(method = "spawnBaby", at = @At("RETURN"))
    private void ric$afterSpawnBaby(
            PlayerEntity user,
            MobEntity entity,
            EntityType<? extends MobEntity> entityType,
            ServerWorld world,
            Vec3d pos,
            ItemStack stack,
            CallbackInfoReturnable<Optional<MobEntity>> cir
    ) {
        Optional<MobEntity> babyOpt = cir.getReturnValue();
        if (babyOpt == null || babyOpt.isEmpty()) return;

        SpawnEggHooks.onSpawnedBySpawnEgg(world, stack, user, babyOpt.get());
    }
}
