package jaxon.ric.mixin;

import jaxon.ric.command.GoCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.SkeletonHorseTrapTriggerGoal;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SkeletonHorseTrapTriggerGoal.class)
public class SkeletonHorseTrapTriggerGoalMixin {

    @Shadow @Final private SkeletonHorseEntity skeletonHorse;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V"
            )
    )
    private void ric$spawnEntityAndPassengersWithTeam(ServerWorld world, Entity entity) {
        if (!GoCommand.enableMobFriendlyFire) {
            Scoreboard sb = world.getScoreboard();
            Team t = sb.getScoreHolderTeam(this.skeletonHorse.getUuidAsString());
            if (t != null) {
                applyTeamRecursive(sb, t, entity);
            }
        }
        world.spawnEntityAndPassengers(entity);
    }

    @Unique
    private static void applyTeamRecursive(Scoreboard sb, Team team, Entity e) {
        sb.addScoreHolderToTeam(e.getUuidAsString(), team);
        for (Entity p : e.getPassengerList()) {
            applyTeamRecursive(sb, team, p);
        }
    }
}
