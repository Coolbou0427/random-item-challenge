package jaxon.ric.mixin;

import jaxon.ric.ai.ZombieHorseAvoidLowCeilingGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieHorse.class)
public abstract class ZombieHorseLowCeilingGoalMixin {

    @Inject(method = "addBehaviourGoals", at = @At("TAIL"))
    private void ric$addLowCeilingEscapeGoal(CallbackInfo ci) {
        ZombieHorse self = (ZombieHorse) (Object) this;
        GoalSelector goals = ((MobEntityGoalSelectorAccessor) self).ric$getGoalSelector();
        goals.addGoal(1, new ZombieHorseAvoidLowCeilingGoal(self, 1.25, 8, 3));
    }
}
