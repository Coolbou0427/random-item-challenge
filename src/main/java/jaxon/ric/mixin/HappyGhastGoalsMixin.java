package jaxon.ric.mixin;

import jaxon.ric.ai.HappyGhastHuntGoal;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HappyGhast.class)
public class HappyGhastGoalsMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ric$addHuntGoal(CallbackInfo ci) {
        HappyGhast self = (HappyGhast) (Object) this;
        ((MobEntityGoalSelectorAccessor) self).ric$getGoalSelector().addGoal(2, new HappyGhastHuntGoal(self));
    }
}
