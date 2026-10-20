package jaxon.ric;

import jaxon.ric.command.GoCommand;
import jaxon.ric.ai.RicMobTargeting;
import jaxon.ric.mixin.SpawnFromItemStackMixin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import java.util.Optional;

public final class RicJockeySpawns {
    private static final String RIC_JOCKEY_TAG = "ric_jockey";

    private static void tagJockey(Entity e) {
        if (e == null) return;
        e.addTag(RIC_JOCKEY_TAG);
        RicMobCleanup.markPlaced(e);
    }


    public static void tryApply(ServerLevel world, ItemStack stack, Entity spawned) {
        String type = getRicSpawnType(stack);
        if (type == null) return;

        stripNameTag(spawned);
        tagJockey(spawned);


        switch (type) {
            case "zombie_horseman" -> applyZombieHorseman(world, spawned);
            case "camel_husk_jockey" -> applyCamelHuskJockey(world, spawned);
            case "ravager_jockey" -> applyRavagerJockey(world, spawned);
            case "zombie_nautilus_jockey" -> applyZombieNautilusJockey(world, spawned);
            case "hoglin_jockey" -> applyHoglinJockey(world, spawned);
            case "happy_ghast_mount" -> applyHappyGhastMount(world, spawned);
        }
    }

    private static void applyZombieHorseman(ServerLevel world, Entity spawned) {
        if (!(spawned instanceof ZombieHorse horse)) return;

        saddleIfPossible(horse);

        horse.addTag("ric_no_burn");

        Mob zombieMob = createMob(world, "minecraft:zombie");
        if (!(zombieMob instanceof Zombie zombie)) return;

        zombie.snapTo(horse.getX(), horse.getY(), horse.getZ(), horse.getYRot(), 0.0F);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
        stripNameTag(zombie);
        zombie.addTag("ric_jockey");

        zombie.addTag("ric_no_burn");

        if (!world.addFreshEntity(zombie)) return;

        horse.clearFire();
        zombie.clearFire();

        copyTeam(world, horse, zombie);
        zombie.startRiding(horse);
    }


    private static void applyCamelHuskJockey(ServerLevel world, Entity spawned) {
        if (!(spawned instanceof Camel) && !matchesEntityId(spawned, "minecraft:camel_husk")) return;

        saddleIfPossible(spawned);

        Mob husk = createMob(world, "minecraft:husk");
        if (husk instanceof Husk) {
            husk.snapTo(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYRot(), 0.0F);
            husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
            stripNameTag(husk);
            husk.addTag("ric_jockey");

            if (!world.addFreshEntity(husk)) return;
            copyTeam(world, spawned, husk);
            husk.startRiding(spawned);
        }

        Mob parched = createMob(world, "minecraft:parched");
        if (parched != null) {
            parched.snapTo(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYRot(), 0.0F);
            parched.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            stripNameTag(parched);
            parched.addTag("ric_jockey");

            if (!world.addFreshEntity(parched)) return;
            copyTeam(world, spawned, parched);
            parched.startRiding(spawned);
        }
    }

    private static void applyRavagerJockey(ServerLevel world, Entity spawned) {
        if (!matchesEntityId(spawned, "minecraft:ravager")) return;

        Mob pillagerMob = createMob(world, "minecraft:pillager");
        if (!(pillagerMob instanceof Pillager pillager)) return;

        pillager.snapTo(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYRot(), 0.0F);
        pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        stripNameTag(pillager);
        pillager.addTag("ric_jockey");


        if (!world.addFreshEntity(pillager)) return;

        copyTeam(world, spawned, pillager);
        pillager.startRiding(spawned);
    }

    private static void applyZombieNautilusJockey(ServerLevel world, Entity spawned) {
        if (!matchesEntityId(spawned, "minecraft:zombie_nautilus")) return;

        saddleIfPossible(spawned);

        spawned.addTag("ric_no_burn");
        spawned.clearFire();

        Mob drownedMob = createMob(world, "minecraft:drowned");
        if (!(drownedMob instanceof Drowned drowned)) return;

        drowned.snapTo(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYRot(), 0.0F);
        drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        stripNameTag(drowned);
        drowned.addTag("ric_jockey");


        drowned.addTag("ric_no_burn");
        drowned.clearFire();

        if (!world.addFreshEntity(drowned)) return;

        copyTeam(world, spawned, drowned);

        ServerPlayer target = RicMobTargeting.findNearest(drowned);

        if (target != null && !target.isSpectator()) {
            drowned.setTarget(target);
            if (spawned instanceof Mob mobMount) {
                mobMount.setTarget(target);
            }
        }

        drowned.startRiding(spawned);
    }


    private static void applyHoglinJockey(ServerLevel world, Entity spawned) {
        if (!(spawned instanceof Hoglin) && !matchesEntityId(spawned, "minecraft:hoglin")) return;

        if (spawned instanceof Hoglin h) {
            h.setImmuneToZombification(true);
        }

        Mob piglinMob = createMob(world, "minecraft:piglin");
        if (!(piglinMob instanceof Piglin piglin)) return;

        piglin.setImmuneToZombification(true);

        piglin.snapTo(spawned.getX(), spawned.getY(), spawned.getZ(), spawned.getYRot(), 0.0F);
        piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SPEAR));
        stripNameTag(piglin);
        piglin.addTag("ric_jockey");


        if (!world.addFreshEntity(piglin)) return;

        copyTeam(world, spawned, piglin);
        piglin.startRiding(spawned);
    }

    private static void applyHappyGhastMount(ServerLevel world, Entity spawned) {
        if (!matchesEntityId(spawned, "minecraft:happy_ghast")) return;

        AbstractBoat boat = createBoat(world);
        if (boat == null) return;

        int x = net.minecraft.util.Mth.floor(spawned.getX());
        int z = net.minecraft.util.Mth.floor(spawned.getZ());
        int topY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);

        double desiredGhastY = topY + 10.0;
        if (spawned.getY() < desiredGhastY) {
            spawned.snapTo(spawned.getX(), desiredGhastY, spawned.getZ(), spawned.getYRot(), 0.0F);
        }

        double boatY = Math.max(spawned.getY() - 3.0, topY + 7.0);

        boat.snapTo(spawned.getX(), boatY, spawned.getZ(), spawned.getYRot(), 0.0F);
        stripNameTag(boat);
        boat.addTag("ric_jockey");


        if (!world.addFreshEntity(boat)) return;

        copyTeam(world, spawned, boat);
        boat.setLeashedTo(spawned, true);

        for (int i = 0; i < 2; i++) {
            Mob piglinMob = createMob(world, "minecraft:piglin");
            if (!(piglinMob instanceof Piglin piglin)) continue;

            piglin.setImmuneToZombification(true);
            piglin.snapTo(boat.getX(), boat.getY(), boat.getZ(), boat.getYRot(), 0.0F);
            piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            stripNameTag(piglin);
            piglin.addTag("ric_jockey");


            if (!world.addFreshEntity(piglin)) continue;

            copyTeam(world, spawned, piglin);
            // Acquire from the rider's own position and follow range. The Ghast
            // may be much higher than the player, which can reject a target that
            // the piglin itself could reach.
            ServerPlayer target = RicMobTargeting.findNearest(piglin);
            if (target != null) RicMobTargeting.setTarget(piglin, target);

            boolean ok = piglin.startRiding(boat);
            if (!ok) {
                ((SpawnFromItemStackMixin.EntityAddPassengerInvoker) boat).ric$addPassenger(piglin);
            }
        }
    }

    private static void copyTeam(ServerLevel world, Entity from, Entity to) {
        RicMobCleanup.markPlaced(to);
        Scoreboard sb = world.getScoreboard();
        PlayerTeam t = sb.getPlayersTeam(from.getStringUUID());
        if (t == null) return;
        sb.addPlayerToTeam(to.getStringUUID(), t);
    }

    private static AbstractBoat createBoat(ServerLevel world) {
        Identifier[] candidates = new Identifier[]{
                Identifier.parse("minecraft:oak_boat"),
                Identifier.parse("minecraft:boat")
        };

        for (Identifier id : candidates) {
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            Entity e = type.create(world, EntitySpawnReason.SPAWN_ITEM_USE);
            if (e instanceof AbstractBoat boat) return boat;
        }

        return null;
    }

    private static void saddleIfPossible(Entity mount) {
        if (mount instanceof AbstractHorse horse) {
            horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            return;
        }
        if (mount instanceof Camel camel) {
            camel.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        }
    }

    private static Mob createMob(ServerLevel world, String id) {
        Identifier ident = Identifier.parse(id);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ident)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(ident);
        Entity e = type.create(world, EntitySpawnReason.JOCKEY);
        if (!(e instanceof Mob mob)) return null;
        return mob;
    }

    private static boolean matchesEntityId(Entity entity, String id) {
        Identifier actual = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return actual.toString().equals(id);
    }

    private static String getRicSpawnType(ItemStack stack) {
        CompoundTag nbt = null;

        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null && !custom.isEmpty()) {
            nbt = custom.copyTag();
        } else {
            TypedEntityData<EntityType<?>> entityData = stack.get(DataComponents.ENTITY_DATA);
            if (entityData != null) {
                nbt = entityData.copyTagWithoutId();
            }
        }

        if (nbt == null) return null;
        if (!nbt.contains("ric_spawn")) return null;

        Optional<String> v = nbt.getString("ric_spawn");
        return v.filter(s -> !s.isEmpty()).orElse(null);
    }

    private static void stripNameTag(Entity e) {
        e.setCustomName(null);
        e.setCustomNameVisible(false);
    }
}
