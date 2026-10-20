package jaxon.ric;

import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import jaxon.ric.ai.RicMobTargeting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class SpawnEggHooks {
    private SpawnEggHooks() {
    }

    public static void onSpawnedBySpawnEgg(ServerLevel world, ItemStack stack, Player player, Entity spawned) {
        onSpawnedFromItemStack(world, stack, player, spawned);
    }

    public static void onPufferfishBucketSpawn(ServerLevel world, LivingEntity owner, Entity spawned) {
        if (!(spawned instanceof Mob mob)
                || !BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString().equals("minecraft:pufferfish")) return;

        RicMobCleanup.markPlaced(mob);
        if (!(owner instanceof ServerPlayer serverPlayer)) return;

        boolean matchActive = StopCommand.isRunning || GoCommand.testMode;
        PlayerTeam ownerTeam = matchActive ? ensureOwnerTeam(world, serverPlayer) : null;
        if (ownerTeam != null) {
            world.getScoreboard().addPlayerToTeam(mob.getStringUUID(), ownerTeam);
        }
    }

    public static void onSpawnedFromItemStack(ServerLevel world, ItemStack stack, LivingEntity spawner, Entity spawned) {
        if (!(spawned instanceof Mob mob)) return;
        if (!mob.addTag("ric_spawn_egg_handled")) return;
        RicMobCleanup.markPlaced(mob);

        // Creakings normally have one health, so give egg-spawned ones a full
        // 20 health pool instead of letting any hit defeat them.
        if (BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString().equals("minecraft:creaking")) {
            var maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(20.0);
                mob.setHealth(20.0F);
            }
        }

        ServerPlayer owner = spawner instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        MinecraftServer server = world.getServer();

        if (owner == null) {
            Random_Item_Challenge.LOGGER.warn(
                    "Could not identify the player who spawned spawn-egg mob type={} uuid={} spawner={} stack={}",
                    spawned.getType(), spawned.getUUID(), spawner, stack.getItem()
            );
        }

        boolean mobFriendlyFire = GoCommand.enableMobFriendlyFire;
        boolean matchActive = StopCommand.isRunning || GoCommand.testMode;
        PlayerTeam ownerTeam = matchActive ? ensureOwnerTeam(world, owner) : null;
        if (matchActive && ownerTeam != null) {
            Scoreboard scoreboard = world.getScoreboard();
            scoreboard.addPlayerToTeam(mob.getStringUUID(), ownerTeam);
        }

        // Spawn-egg piglins and hoglins must survive in every dimension.
        if (mob instanceof Piglin piglin) piglin.setImmuneToZombification(true);
        if (mob instanceof Hoglin hoglin) hoglin.setImmuneToZombification(true);

        RicJockeySpawns.tryApply(world, stack, spawned);
        if (ownerTeam != null) assignPassengerTeams(world.getScoreboard(), ownerTeam, spawned);

        if (matchActive && isRicPlayerTargetMob(mob)) {
            mob.addTag("ric_retarget_players");
        }

        // Wolves stay neutral until a player tames them. Their vanilla follow-owner
        // behavior keeps them nearby after taming instead of letting them roam away.
        if (mob instanceof Wolf || server == null) return;

        // Keep the retargeter enabled even when there is no valid opponent at the
        // instant the egg is used. A target can become available later, and mobs
        // such as endermites do not acquire player targets through vanilla AI.
        mob.addTag("ric_retarget_players");

        ServerPlayer firstTarget = RicMobTargeting.findNearest(mob);

        if (firstTarget == null) return;
        if (mob instanceof Bee) {
            mob.addTag("ric_forced_bee_aggro");
            mob.addTag("ric_bee_target_" + firstTarget.getStringUUID());

            EntityType<?> beeType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:bee"));
            for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
                if (candidate == firstTarget || candidate.isSpectator() || !candidate.isAlive()
                        || candidate.level() != world || candidate == owner) continue;
                if (!Random_Item_Challenge.participantsWithoutRemovals.contains(candidate.getUUID())) continue;
                if (Random_Item_Challenge.participantsWhoDied.contains(candidate.getUUID())) continue;
                if (!mobFriendlyFire && ownerTeam != null && ownerTeam.equals(candidate.getTeam())) continue;
                if (!RicMobTargeting.canAcquire(mob, candidate)) continue;

                Entity extra = beeType.create(world, EntitySpawnReason.SPAWN_ITEM_USE);
                if (!(extra instanceof Bee extraBee)) continue;
                extraBee.snapTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
                extraBee.addTag("ric_spawn_egg_handled");
                extraBee.addTag("ric_forced_bee_aggro");
                extraBee.addTag("ric_retarget_players");
                extraBee.addTag("ric_bee_target_" + candidate.getStringUUID());
                RicMobCleanup.markPlaced(extraBee);
                if (ownerTeam != null) {
                    world.getScoreboard().addPlayerToTeam(extraBee.getStringUUID(), ownerTeam);
                }
                world.addFreshEntity(extraBee);
            }
        }

        mob.setTarget(firstTarget);
        if (mob instanceof NeutralMob anger) {
            anger.setLastHurtByMob(firstTarget);
            anger.setPersistentAngerTarget(EntityReference.of(firstTarget.getUUID()));
            anger.startPersistentAngerTimer();
        }
    }

    private static PlayerTeam ensureOwnerTeam(ServerLevel world, ServerPlayer owner) {
        if (owner == null) return null;

        Scoreboard scoreboard = world.getScoreboard();

        if (GoCommand.teamMode) {
            // In real team mode, mobs belong to the player's actual colored team.
            return owner.getTeam();
        }

        // Solo/test matches do not create player teams, but mobs still need a
        // scoreboard team so vanilla targeting treats the owner as an ally. Do
        // not reuse a team from a previous team-mode match: that would make
        // the mob inherit a team color and could put it on the wrong team.
        PlayerTeam previousTeam = owner.getTeam();
        if (previousTeam != null) {
            scoreboard.removePlayerFromTeam(owner.getName().getString(), previousTeam);
        }

        String id = "ric_mob_" + Integer.toUnsignedString(owner.getUUID().hashCode(), 36);
        PlayerTeam mobTeam = scoreboard.getPlayerTeam(id);
        if (mobTeam == null) {
            mobTeam = scoreboard.addPlayerTeam(id);
        }
        mobTeam.setDisplayName(net.minecraft.network.chat.Component.literal("Mob Team"));
        mobTeam.setColor(java.util.Optional.empty());
        mobTeam.setAllowFriendlyFire(false);
        scoreboard.addPlayerToTeam(owner.getName().getString(), mobTeam);
        return mobTeam;
    }

    private static void assignPassengerTeams(Scoreboard scoreboard, PlayerTeam team, Entity entity) {
        scoreboard.addPlayerToTeam(entity.getStringUUID(), team);
        RicMobCleanup.markPlaced(entity);
        for (Entity passenger : entity.getPassengers()) assignPassengerTeams(scoreboard, team, passenger);
    }

    public static boolean isRicPlayerTargetMob(Mob mob) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        return id.equals("minecraft:endermite")
                || id.equals("minecraft:iron_golem")
                || id.equals("minecraft:enderman");
    }
}
