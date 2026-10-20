package jaxon.ric.mixin;

import jaxon.ric.SpawnEggHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(SpawnEggItem.class)
public class SpawnEggBabyMixin {

    @Inject(method = "spawnOffspringFromSpawnEgg", at = @At("RETURN"))
    private static void ric$afterSpawnBaby(
            Player user,
            Mob entity,
            EntityType<? extends Mob> entityType,
            ServerLevel world,
            Vec3 pos,
            ItemStack stack,
            CallbackInfoReturnable<Optional<Mob>> cir
    ) {
        Optional<Mob> babyOpt = cir.getReturnValue();
        if (babyOpt == null || babyOpt.isEmpty()) return;

        SpawnEggHooks.onSpawnedBySpawnEgg(world, stack, user, babyOpt.get());
    }
}
