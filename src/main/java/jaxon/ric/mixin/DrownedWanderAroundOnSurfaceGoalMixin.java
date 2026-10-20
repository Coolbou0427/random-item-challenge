package jaxon.ric.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.zombie.Drowned$DrownedGoToWaterGoal")
public abstract class DrownedWanderAroundOnSurfaceGoalMixin extends Goal {

    @Shadow @Final private PathfinderMob mob;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void ric$noSurfaceWanderForJockey(CallbackInfoReturnable<Boolean> cir) {
        Entity e = this.mob;

        if (e.entityTags().contains("ric_no_burn") || e.isPassenger()) {
            cir.setReturnValue(false);
        }
    }
}
