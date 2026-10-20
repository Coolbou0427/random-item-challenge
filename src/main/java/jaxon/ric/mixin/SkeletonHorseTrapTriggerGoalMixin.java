package jaxon.ric.mixin;

import jaxon.ric.RicMobCleanup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.SkeletonTrapGoal;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkeletonTrapGoal.class)
public class SkeletonHorseTrapTriggerGoalMixin {

    @Shadow @Final private SkeletonHorse horse;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"
            )
    )
    private void ric$spawnEntityAndPassengersWithTeam(ServerLevel world, Entity entity) {
        ric$inheritHorseOwnership(entity);
        world.addFreshEntityWithPassengers(entity);
    }

    // Also cover the first rider on the existing trap horse, before it is added
    // to the world, and every extra horse/rider created by the lightning trap.
    @Inject(method = {"createHorse", "createSkeleton"}, at = @At("RETURN"))
    private void ric$teamTrapMember(CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() != null) ric$inheritHorseOwnership(cir.getReturnValue());
    }

    @Unique
    private void ric$inheritHorseOwnership(Entity entity) {
        if (horse.entityTags().contains("ric_spawn_egg_handled")
                || horse.entityTags().contains(RicMobCleanup.PLACED_TAG)) RicMobCleanup.markPlaced(entity);
        Scoreboard sb = horse.level().getScoreboard();
        PlayerTeam team = sb.getPlayersTeam(horse.getStringUUID());
        if (team != null) applyTeamRecursive(sb, team, entity);
    }

    @Unique
    private static void applyTeamRecursive(Scoreboard sb, PlayerTeam team, Entity e) {
        sb.addPlayerToTeam(e.getStringUUID(), team);
        if (e instanceof Mob mob) {
            mob.addTag("ric_retarget_players");
        }
        for (Entity p : e.getPassengers()) {
            applyTeamRecursive(sb, team, p);
        }
    }
}
