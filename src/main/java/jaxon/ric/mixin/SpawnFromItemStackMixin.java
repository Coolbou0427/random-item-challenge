package jaxon.ric.mixin;

import jaxon.ric.SpawnEggHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import jaxon.ric.RicCleanupTracked;
import jaxon.ric.RicMobCleanup;
import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public class SpawnFromItemStackMixin {

    @Inject(method = "create(Lnet/minecraft/world/level/storage/ValueInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;)Ljava/util/Optional;", at = @At("RETURN"))
    private static void ric$markFreshNbtSpawn(ValueInput input, Level level, EntitySpawnRequest request,
                                            CallbackInfoReturnable<Optional<Entity>> cir) {
        if (request.reason() != EntitySpawnReason.LOAD) cir.getReturnValue().ifPresent(entity ->
                ((RicCleanupTracked) entity).ric$setCleanupEpoch(RicMobCleanup.currentEpoch()));
    }

    @Inject(method = "create(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/storage/ValueInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Ljava/util/Optional;", at = @At("RETURN"))
    private static void ric$markFreshTypedNbtSpawn(EntityType<?> type, ValueInput input, Level level, EntitySpawnReason reason,
                                                 CallbackInfoReturnable<Optional<Entity>> cir) {
        if (reason != EntitySpawnReason.LOAD) cir.getReturnValue().ifPresent(entity ->
                ((RicCleanupTracked) entity).ric$setCleanupEpoch(RicMobCleanup.currentEpoch()));
    }

    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"))
    private void ric$afterSpawnFromItemStack(
            ServerLevel world,
            @Nullable ItemStack stack,
            @Nullable LivingEntity spawner,
            BlockPos pos,
            EntitySpawnReason spawnReason,
            boolean alignPosition,
            boolean invertY,
            CallbackInfoReturnable<Entity> cir
    ) {
        Entity spawned = cir.getReturnValue();
        if (spawned == null) return;
        if (stack == null || !(stack.getItem() instanceof net.minecraft.world.item.SpawnEggItem)) return;
        SpawnEggHooks.onSpawnedFromItemStack(world, stack, spawner, spawned);
    }

    @Mixin(Entity.class)
    public interface EntityAddPassengerInvoker {
        @Invoker("addPassenger")
        void ric$addPassenger(Entity passenger);
    }
}
