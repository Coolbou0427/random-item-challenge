package jaxon.ric.ai;

import jaxon.ric.command.StopCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/** Gives normally neutral RIC mobs vanilla player acquisition and target memory. */
public final class RicPlayerTargetGoal extends NearestAttackableTargetGoal<ServerPlayer> {
    public RicPlayerTargetGoal(Mob mob) {
        super(mob, ServerPlayer.class, 10, true, false,
                (candidate, level) -> candidate instanceof ServerPlayer player && RicMobTargeting.isEligible(mob, player));
    }

    @Override
    public boolean canUse() {
        return StopCommand.isRunning && mob.entityTags().contains("ric_retarget_players") && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return StopCommand.isRunning && super.canContinueToUse();
    }
}
