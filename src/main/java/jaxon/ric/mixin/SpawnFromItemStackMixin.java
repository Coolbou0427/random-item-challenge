package jaxon.ric.mixin;

import jaxon.ric.RicJockeySpawns;
import jaxon.ric.command.GoCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public class SpawnFromItemStackMixin {

    @Inject(method = "spawnFromItemStack", at = @At("RETURN"))
    private void ric$afterSpawnFromItemStack(
            ServerWorld world,
            @Nullable ItemStack stack,
            @Nullable LivingEntity spawner,
            BlockPos pos,
            SpawnReason spawnReason,
            boolean alignPosition,
            boolean invertY,
            CallbackInfoReturnable<Entity> cir
    ) {
        Entity spawned = cir.getReturnValue();
        if (spawned == null) return;
        if (!(spawned instanceof MobEntity mob)) return;
        if (!(spawner instanceof PlayerEntity player)) return;
        if (stack == null || !(stack.getItem() instanceof net.minecraft.item.SpawnEggItem)) return;

        if (!GoCommand.enableMobFriendlyFire) {
            Scoreboard sb = world.getScoreboard();
            String name = player.getName().getString();
            Team team;

            if (GoCommand.teamMode) {
                team = sb.getScoreHolderTeam(name);
                if (team == null) {
                    team = sb.addTeam("mobteam-" + name);
                    sb.addScoreHolderToTeam(name, team);
                    team.setFriendlyFireAllowed(false);
                }
            } else {
                String tName = "ffa-mob-" + name;
                team = sb.getTeam(tName);
                if (team == null) {
                    team = sb.addTeam(tName);
                    sb.addScoreHolderToTeam(name, team);
                    team.setFriendlyFireAllowed(false);
                }
            }

            sb.addScoreHolderToTeam(mob.getUuidAsString(), team);
        }

        RicJockeySpawns.tryApply(world, stack, spawned);

        MinecraftServer srv = world.getServer();
        if (srv == null) return;

        Team owner = world.getScoreboard().getScoreHolderTeam(mob.getUuidAsString());


        for (ServerPlayerEntity target : srv.getPlayerManager().getPlayerList()) {
            if (target == player || target.isSpectator()) continue;
            if (!GoCommand.enableMobFriendlyFire && owner != null && owner.equals(target.getScoreboardTeam())) continue;

            switch (mob) {
                case BeeEntity bee -> bee.setTarget(target);
                case IronGolemEntity golem -> golem.setTarget(target);
                case CaveSpiderEntity cave -> cave.setTarget(target);
                case SpiderEntity spider -> spider.setTarget(target);
                default -> mob.setTarget(target);
            }

            if (mob instanceof Angerable anger) {
                anger.setAngryAt(LazyEntityReference.ofUUID(target.getUuid()));
            }
            break;
        }
    }

    @Mixin(Entity.class)
    public interface EntityAddPassengerInvoker {
        @Invoker("addPassenger")
        void ric$addPassenger(Entity passenger);
    }
}
