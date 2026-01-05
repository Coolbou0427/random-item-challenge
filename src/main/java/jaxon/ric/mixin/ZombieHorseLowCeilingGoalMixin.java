package jaxon.ric.mixin;

import jaxon.ric.ai.ZombieHorseAvoidLowCeilingGoal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.ZombieHorseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieHorseEntity.class)
public abstract class ZombieHorseLowCeilingGoalMixin {

    @Inject(method = "initCustomGoals", at = @At("TAIL"))
    private void ric$addLowCeilingEscapeGoal(CallbackInfo ci) {
        ZombieHorseEntity self = (ZombieHorseEntity) (Object) this;
        GoalSelector goals = ((MobEntityGoalSelectorAccessor) self).ric$getGoalSelector();
        goals.add(1, new ZombieHorseAvoidLowCeilingGoal(self, 1.25, 8, 3));
    }
}
