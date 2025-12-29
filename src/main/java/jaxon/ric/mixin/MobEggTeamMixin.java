// src/main/java/jaxon/ric/mixin/MobEggTeamMixin.java
package jaxon.ric.mixin;

import jaxon.ric.command.GoCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(SpawnEggItem.class)
public class MobEggTeamMixin {

    @Redirect(
            method = "useOnBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityType;" +
                            "spawnFromItemStack(" +
                            "Lnet/minecraft/server/world/ServerWorld;" +
                            "Lnet/minecraft/item/ItemStack;" +
                            "Lnet/minecraft/entity/LivingEntity;" +
                            "Lnet/minecraft/util/math/BlockPos;" +
                            "Lnet/minecraft/entity/SpawnReason;" +
                            "ZZ)" +
                            "Lnet/minecraft/entity/Entity;"
            )
    )
    private Entity ric$spawnOnBlock(
            EntityType<?> type,
            ServerWorld world,
            ItemStack stack,
            LivingEntity living,
            BlockPos pos,
            SpawnReason reason,
            boolean align,
            boolean invert
    ) {
        Entity mob = type.spawnFromItemStack(world, stack, living, pos, reason, align, invert);

        if (mob instanceof MobEntity && living instanceof PlayerEntity player) {
            Scoreboard sb = world.getScoreboard();
            Team team = null;

            if (!GoCommand.enableMobFriendlyFire) {
                String name = player.getName().getString();
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

            MinecraftServer srv = world.getServer();
            Team owner = player.getScoreboardTeam();
            for (ServerPlayerEntity target : srv.getPlayerManager().getPlayerList()) {
                if (target == player || target.isSpectator()) continue;
                if (!GoCommand.enableMobFriendlyFire && owner != null && owner.equals(target.getScoreboardTeam()))
                    continue;

                if (mob instanceof BeeEntity bee) {
                    bee.setAngerTime(9999);
                    bee.setAngryAt(target.getUuid());
                    bee.setTarget(target);
                } else if (mob instanceof EndermanEntity enderman) {
                    enderman.setAngerTime(9999);
                    enderman.setAngryAt(target.getUuid());
                    enderman.setTarget(target);
                } else if (mob instanceof IronGolemEntity golem) {
                    golem.setAngerTime(9999);
                    golem.setAngryAt(target.getUuid());
                    golem.setTarget(target);
                    golem.setAttacking(true);
                } else if (mob instanceof SpiderEntity spider) {
                    spider.setTarget(target);
                } else if (mob instanceof CaveSpiderEntity cave) {
                    cave.setTarget(target);
                }
            }
        }

        return mob;
    }

    @Inject(method = "spawnBaby", at = @At("RETURN"))
    private void ric$afterSpawnBaby(
            PlayerEntity user,
            MobEntity parent,
            EntityType<? extends MobEntity> type,
            ServerWorld world,
            Vec3d pos,
            ItemStack stack,
            CallbackInfoReturnable<Optional<MobEntity>> cir
    ) {
        if (GoCommand.enableMobFriendlyFire || user == null) return;
        Optional<MobEntity> res = cir.getReturnValue();
        if (res.isEmpty()) return;

        MobEntity baby = res.get();
        Scoreboard sb = world.getScoreboard();
        String name = user.getName().getString();
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
        sb.addScoreHolderToTeam(baby.getUuidAsString(), team);
    }
}
