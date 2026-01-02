package jaxon.ric.mixin;

import jaxon.ric.ai.HappyGhastHuntGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HappyGhastEntity.class)
public class HappyGhastGoalsMixin {

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void ric$addHuntGoal(CallbackInfo ci) {
        HappyGhastEntity self = (HappyGhastEntity) (Object) this;
        ((MobEntityGoalSelectorAccessor) (MobEntity) self).ric$getGoalSelector().add(2, new HappyGhastHuntGoal(self));
    }
}
