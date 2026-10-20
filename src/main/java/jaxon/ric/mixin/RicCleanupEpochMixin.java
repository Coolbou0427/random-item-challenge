package jaxon.ric.mixin;

import jaxon.ric.RicCleanupTracked;
import jaxon.ric.RicMobCleanup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class RicCleanupEpochMixin implements RicCleanupTracked {
    @Unique private long ric$cleanupEpoch = RicMobCleanup.currentEpoch();

    @Override
    public long ric$getCleanupEpoch() { return ric$cleanupEpoch; }

    @Override
    public void ric$setCleanupEpoch(long epoch) { ric$cleanupEpoch = epoch; }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void ric$saveCleanupEpoch(ValueOutput output, CallbackInfo ci) {
        output.putLong("ric_cleanup_epoch", ric$cleanupEpoch);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void ric$loadCleanupEpoch(ValueInput input, CallbackInfo ci) {
        // Entities saved before this feature belong to generation zero.
        ric$cleanupEpoch = input.getLongOr("ric_cleanup_epoch", 0L);
    }
}
