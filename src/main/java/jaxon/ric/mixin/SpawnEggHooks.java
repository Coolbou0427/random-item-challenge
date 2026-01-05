package jaxon.ric.mixin;

import jaxon.ric.RicJockeySpawns;
import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class SpawnEggHooks {
    private SpawnEggHooks() {
    }

    public static void onSpawnedBySpawnEgg(ServerWorld world, ItemStack stack, PlayerEntity player, Entity spawned) {
        onSpawnedByPlayerItem(world, stack, player, spawned, true);
    }

    public static void onSpawnedByBucket(ServerWorld world, ItemStack stack, PlayerEntity player, Entity spawned) {
        onSpawnedByPlayerItem(world, stack, player, spawned, false);
    }

    private static void onSpawnedByPlayerItem(ServerWorld world, ItemStack stack, PlayerEntity player, Entity spawned, boolean applyJockey) {
        MobEntity mob = spawned instanceof MobEntity ? (MobEntity) spawned : null;

        boolean mobFF = GoCommand.enableMobFriendlyFire;

        if ((StopCommand.isRunning || GoCommand.testMode) && !mobFF) {
            Scoreboard sb = world.getScoreboard();
            String ownerShort = player.getUuid().toString().replace("-", "");
            ownerShort = ownerShort.length() > 8 ? ownerShort.substring(0, 8) : ownerShort;
            String teamId = (GoCommand.teamMode ? "ric_mob_" : "ric_ffa_") + ownerShort;

            Team team = sb.getTeam(teamId);
            if (team == null) {
                team = sb.addTeam(teamId);
                sb.addScoreHolderToTeam(player.getName().getString(), team);
                team.setFriendlyFireAllowed(false);
            }

            sb.addScoreHolderToTeam(spawned.getUuidAsString(), team);
        }

        if (applyJockey) {
            RicJockeySpawns.tryApply(world, stack, spawned);
        }

        if (mob == null) return;
        if (!StopCommand.isRunning) return;

        MinecraftServer srv = world.getServer();
        if (srv == null) return;

        Team owner = world.getScoreboard().getScoreHolderTeam(spawned.getUuidAsString());

        ServerPlayerEntity bestTarget = null;
        double bestDistSq = Double.MAX_VALUE;

        for (ServerPlayerEntity target : srv.getPlayerManager().getPlayerList()) {
            if (target == player || target.isSpectator()) continue;
            if (target.getEntityWorld() != world) continue;

            if (!mobFF && owner != null) {
                Team targetTeam = target.getScoreboardTeam();
                if (targetTeam != null && owner.getName().equals(targetTeam.getName())) continue;
            }

            double d = mob.squaredDistanceTo(target);
            if (d < bestDistSq) {
                bestDistSq = d;
                bestTarget = target;
            }
        }

        if (bestTarget == null) return;

        mob.getType().toString();
        mob.setTarget(bestTarget);

        if (mob instanceof Angerable anger) {
            anger.setAngryAt(LazyEntityReference.ofUUID(bestTarget.getUuid()));
        }
    }
}
