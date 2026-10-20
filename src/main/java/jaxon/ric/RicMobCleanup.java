package jaxon.ric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Persists cleanup across unloaded chunks and server restarts without loading the world. */
public final class RicMobCleanup {
    public static final String PLACED_TAG = "ric_player_placed";
    private static volatile long epoch;
    private static Path epochFile;
    private static final ConcurrentLinkedQueue<Entity> pending = new ConcurrentLinkedQueue<>();

    private RicMobCleanup() {}

    public static long currentEpoch() { return epoch; }

    public static void register() {
        // Process at the end of the tick, after spawn-egg hooks have assigned the
        // owner/team and generation to newly created mobs and their passengers.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> pending.add(entity));
    }

    public static void start(MinecraftServer server) {
        pending.clear();
        epoch = 0;
        epochFile = server.getWorldPath(LevelResource.ROOT).resolve("data/ric-mob-cleanup-generation.txt");
        if (Files.exists(epochFile)) {
            try {
                epoch = Long.parseLong(Files.readString(epochFile).trim());
            } catch (IOException | NumberFormatException e) {
                throw new IllegalStateException("Cannot read RIC mob cleanup generation", e);
            }
        }
    }

    public static void markPlaced(Entity entity) {
        entity.addTag(PLACED_TAG);
        ((RicCleanupTracked) entity).ric$setCleanupEpoch(epoch);
        for (Entity passenger : entity.getPassengers()) markPlaced(passenger);
    }

    public static void beginCleanup(MinecraftServer server) {
        long next = epoch + 1;
        try {
            Files.createDirectories(epochFile.getParent());
            Path temporary = epochFile.resolveSibling(epochFile.getFileName() + ".tmp");
            Files.writeString(temporary, Long.toString(next));
            Files.move(temporary, epochFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save RIC mob cleanup generation", e);
        }
        epoch = next;
        for (ServerLevel level : server.getAllLevels()) {
            var snapshot = new ArrayList<Entity>();
            level.getAllEntities().forEach(snapshot::add);
            for (Entity entity : snapshot) removeIfStale(entity);
        }
    }

    public static void tick() {
        Entity entity;
        while ((entity = pending.poll()) != null) removeIfStale(entity);
    }

    private static void removeIfStale(Entity entity) {
        if (entity.isRemoved() || !(entity instanceof RicCleanupTracked tracked)
                || tracked.ric$getCleanupEpoch() >= epoch) return;
        boolean placed = entity.entityTags().contains(PLACED_TAG)
                || entity.entityTags().contains("ric_spawn_egg_handled")
                || entity.entityTags().contains("ric_jockey");
        boolean hostile = entity.getType().getCategory() == MobCategory.MONSTER;
        if (!placed && !hostile) return;
        // Use the dragon's kill lifecycle, which also creates the End exit
        // fountain; discarding the dragon would leave the fight unfinished.
        if (entity instanceof EnderDragon dragon && entity.level() instanceof ServerLevel level) dragon.kill(level);
        else entity.discard();
    }
}
