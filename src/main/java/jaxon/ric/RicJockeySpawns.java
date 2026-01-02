package jaxon.ric;

import jaxon.ric.command.GoCommand;
import jaxon.ric.mixin.SpawnFromItemStackMixin;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class RicJockeySpawns {
    private RicJockeySpawns() {
    }

    public static void tryApply(ServerWorld world, ItemStack stack, Entity spawned) {
        String type = getRicSpawnType(stack);
        if (type == null) return;

        stripNameTag(spawned);

        switch (type) {
            case "zombie_horseman" -> applyZombieHorseman(world, spawned);
            case "camel_husk_jockey" -> applyCamelHuskJockey(world, spawned);
            case "ravager_jockey" -> applyRavagerJockey(world, spawned);
            case "zombie_nautilus_jockey" -> applyZombieNautilusJockey(world, spawned);
            case "hoglin_jockey" -> applyHoglinJockey(world, spawned);
            case "happy_ghast_mount" -> applyHappyGhastMount(world, spawned);
        }
    }

    private static void applyZombieHorseman(ServerWorld world, Entity spawned) {
        if (!(spawned instanceof ZombieHorseEntity horse)) return;

        saddleIfPossible(horse);

        ZombieEntity zombie = EntityType.ZOMBIE.create(world, SpawnReason.JOCKEY);
        if (zombie == null) return;

        zombie.refreshPositionAndAngles(horse.getX(), horse.getY(), horse.getZ(), horse.getYaw(), 0.0F);
        zombie.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
        stripNameTag(zombie);

        if (!world.spawnEntity(zombie)) return;

        copyTeam(world, horse, zombie);
        zombie.startRiding(horse);
    }

    private static void applyCamelHuskJockey(ServerWorld world, Entity spawned) {
        if (!(spawned instanceof CamelEntity)) {
            if (!isEntityId(spawned, "minecraft:camel_husk")) return;
            if (!(spawned instanceof MobEntity)) return;
        }

        saddleIfPossible(spawned);

        MobEntity husk = createMob(world, "minecraft:husk");
        if (husk instanceof HuskEntity) {
            husk.refreshPositionAndAngles(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYaw(), 0.0F);
            husk.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
            stripNameTag(husk);
            if (!world.spawnEntity(husk)) return;
            copyTeam(world, spawned, husk);
            husk.startRiding(spawned);
        }

        MobEntity parched = createMob(world, "minecraft:parched");
        if (parched != null) {
            parched.refreshPositionAndAngles(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYaw(), 0.0F);
            parched.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            stripNameTag(parched);
            if (!world.spawnEntity(parched)) return;
            copyTeam(world, spawned, parched);
            parched.startRiding(spawned);
        }
    }

    private static void applyRavagerJockey(ServerWorld world, Entity spawned) {
        if (!isEntityId(spawned, "minecraft:ravager")) return;

        PillagerEntity pillager = EntityType.PILLAGER.create(world, SpawnReason.JOCKEY);
        if (pillager == null) return;

        pillager.refreshPositionAndAngles(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYaw(), 0.0F);
        pillager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        stripNameTag(pillager);

        if (!world.spawnEntity(pillager)) return;

        copyTeam(world, spawned, pillager);
        pillager.startRiding(spawned);
    }

    private static void applyZombieNautilusJockey(ServerWorld world, Entity spawned) {
        if (!isEntityId(spawned, "minecraft:zombie_nautilus")) return;

        saddleIfPossible(spawned);

        DrownedEntity drowned = EntityType.DROWNED.create(world, SpawnReason.JOCKEY);
        if (drowned == null) return;

        drowned.refreshPositionAndAngles(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYaw(), 0.0F);
        drowned.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        stripNameTag(drowned);

        if (!world.spawnEntity(drowned)) return;

        copyTeam(world, spawned, drowned);
        drowned.startRiding(spawned);
    }

    private static void applyHoglinJockey(ServerWorld world, Entity spawned) {
        if (!(spawned instanceof HoglinEntity)) {
            if (!isEntityId(spawned, "minecraft:hoglin")) return;
            if (!(spawned instanceof MobEntity)) return;
        }

        if (spawned instanceof HoglinEntity h) {
            h.setImmuneToZombification(true);
        }

        PiglinEntity piglin = EntityType.PIGLIN.create(world, SpawnReason.JOCKEY);
        if (piglin == null) return;

        piglin.setImmuneToZombification(true);

        piglin.refreshPositionAndAngles(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYaw(), 0.0F);
        piglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SPEAR));
        stripNameTag(piglin);

        if (!world.spawnEntity(piglin)) return;

        copyTeam(world, spawned, piglin);
        piglin.startRiding(spawned);
    }

    private static void applyHappyGhastMount(ServerWorld world, Entity spawned) {
        if (!isEntityId(spawned, "minecraft:happy_ghast")) return;

        AbstractBoatEntity boat = createBoat(world);
        if (boat == null) return;

        int x = net.minecraft.util.math.MathHelper.floor(spawned.getX());
        int z = net.minecraft.util.math.MathHelper.floor(spawned.getZ());
        int topY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

        double desiredGhastY = topY + 6.0;
        if (spawned.getY() < desiredGhastY) {
            spawned.refreshPositionAndAngles(spawned.getX(), desiredGhastY, spawned.getZ(), spawned.getYaw(), 0.0F);
        }

        double boatY = Math.max(spawned.getY() - 3.0, topY + 1.0);

        boat.refreshPositionAndAngles(spawned.getX(), boatY, spawned.getZ(), spawned.getYaw(), 0.0F);
        stripNameTag(boat);

        if (!world.spawnEntity(boat)) return;

        copyTeam(world, spawned, boat);
        boat.attachLeash(spawned, true);

        Team ghastTeam = getTeamForEntity(world, spawned);
        ServerPlayerEntity target = pickTarget(world, ghastTeam);

        for (int i = 0; i < 2; i++) {
            PiglinEntity piglin = EntityType.PIGLIN.create(world, SpawnReason.JOCKEY);
            if (piglin == null) continue;

            piglin.setImmuneToZombification(true);
            piglin.refreshPositionAndAngles(boat.getX(), boat.getY(), boat.getZ(), boat.getYaw(), 0.0F);
            piglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            stripNameTag(piglin);

            if (!world.spawnEntity(piglin)) continue;

            copyTeam(world, spawned, piglin);

            if (target != null && !target.isSpectator()) {
                piglin.setTarget(target);
            }

            boolean ok = piglin.startRiding(boat);
            if (!ok) {
                ((SpawnFromItemStackMixin.EntityAddPassengerInvoker) boat).ric$addPassenger(piglin);
            }
        }
    }

    private static void copyTeam(ServerWorld world, Entity from, Entity to) {
        if (GoCommand.enableMobFriendlyFire) return;
        Scoreboard sb = world.getScoreboard();
        Team t = sb.getScoreHolderTeam(from.getUuidAsString());
        if (t == null) return;
        sb.addScoreHolderToTeam(to.getUuidAsString(), t);
    }

    private static AbstractBoatEntity createBoat(ServerWorld world) {
        Identifier[] candidates = new Identifier[]{
                Identifier.of("minecraft:oak_boat"),
                Identifier.of("minecraft:boat")
        };

        for (Identifier id : candidates) {
            if (!Registries.ENTITY_TYPE.containsId(id)) continue;
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            Entity e = type.create(world, SpawnReason.SPAWN_ITEM_USE);
            if (e instanceof AbstractBoatEntity boat) return boat;
        }

        return null;
    }

    private static Team getTeamForEntity(ServerWorld world, Entity entity) {
        Scoreboard sb = world.getScoreboard();
        return sb.getScoreHolderTeam(entity.getUuidAsString());
    }

    private static ServerPlayerEntity pickTarget(ServerWorld world, Team mobTeam) {
        MinecraftServer srv = world.getServer();
        if (srv == null) return null;

        for (ServerPlayerEntity p : srv.getPlayerManager().getPlayerList()) {
            if (p.isSpectator()) continue;
            if (p.isCreative()) continue;
            if (!GoCommand.enableMobFriendlyFire && mobTeam != null && mobTeam.equals(p.getScoreboardTeam())) continue;
            return p;
        }

        return null;
    }

    private static void saddleIfPossible(Entity mount) {
        if (mount instanceof AbstractHorseEntity horse) {
            horse.equipStack(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            return;
        }
        if (mount instanceof CamelEntity camel) {
            camel.equipStack(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        }
    }

    private static MobEntity createMob(ServerWorld world, String id) {
        EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(id));
        Entity e = type.create(world, SpawnReason.JOCKEY);
        if (!(e instanceof MobEntity mob)) return null;
        return mob;
    }

    private static boolean isEntityId(Entity entity, String id) {
        Identifier actual = Registries.ENTITY_TYPE.getId(entity.getType());
        return actual.toString().equals(id);
    }

    private static String getRicSpawnType(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || custom.isEmpty()) return null;

        NbtCompound nbt = custom.copyNbt();
        if (!nbt.contains("ric_spawn")) return null;

        String v = nbt.getString("ric_spawn").orElse("");
        return v.isEmpty() ? null : v;
    }

    private static void stripNameTag(Entity e) {
        e.setCustomName(null);
        e.setCustomNameVisible(false);
    }
}
