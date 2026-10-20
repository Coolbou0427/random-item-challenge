package smoke;

import com.mojang.authlib.GameProfile;
import jaxon.ric.Random_Item_Challenge;
import jaxon.ric.SpawnEggHooks;
import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.UUID;

/** Dedicated-server integration fixture. Package as a test-only Fabric mod; never ship in RIC.jar. */
public final class GhastPursuitChecks implements ModInitializer {
    private ServerPlayer player;
    private HappyGhast ghast;
    private int ticks, stageTicks, stage, shots, stationary, maximumStationary;
    private boolean started, done, diagonal;
    private Vec3 previous;
    private float startingHealth;
    private boolean firedOutsideRange;
    private int blockContactTicks;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) server.overworld().setChunkForced(x, z, true);
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (started && entity instanceof AbstractArrow arrow && arrow.getOwner() != null
                    && arrow.getOwner().entityTags().contains("ric_jockey")) {
                shots++;
                if (arrow.getOwner().position().distanceTo(player.position()) >= 8.01) firedOutsideRange = true;
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (done) return;
            try {
                if (!started) {
                    if (++ticks < 40) return;
                    initialize(server);
                    started = true;
                    begin(server, false);
                    return;
                }
                stageTicks++;
                Vec3 position = ghast.position();
                Vec3 movement = position.subtract(previous);
                previous = position;
                if (Math.abs(movement.y) > 0.015 && Math.hypot(movement.x, movement.z) > 0.015) diagonal = true;
                double range = minimumRiderDistance(server.overworld());
                if (stageTicks > 20 && range > 8 && movement.length() < 0.003) stationary++;
                else stationary = 0;
                maximumStationary = Math.max(maximumStationary, stationary);
                if (ghast.horizontalCollision || ghast.verticalCollision) blockContactTicks++;
                if (ghast.getAttributeValue(Attributes.FLYING_SPEED) != 0.05) {
                    throw new AssertionError("Pursuit changed the mob's flying speed attribute");
                }
                if (stageTicks % 40 == 0) System.out.println("GHAST_TRACE stage=" + stage + " tick=" + stageTicks
                        + " pos=" + position + " riderDistance=" + range + " speed=" + movement.length()
                        + " shots=" + shots + " damage=" + (startingHealth - player.getHealth()) + " idle=" + stationary);
                if (shots >= 2 && player.getHealth() < startingHealth) {
                    if (maximumStationary > 12) throw new AssertionError("Out-of-range mount stalled for " + maximumStationary + " ticks");
                    if (!diagonal) throw new AssertionError("Pursuit never combined vertical and horizontal movement");
                    if (firedOutsideRange) throw new AssertionError("Piglin fired outside its real crossbow range");
                    if (blockContactTicks > 8) throw new AssertionError("Mount repeatedly contacted blocks: " + blockContactTicks);
                    System.out.println("GHAST_INTEGRATION_PASS stage=" + stage + " actual crossbow shots=" + shots
                            + " damage=" + (startingHealth - player.getHealth()) + " ticks=" + stageTicks
                            + " worstIdleTicks=" + maximumStationary + " blockContactTicks=" + blockContactTicks);
                    if (stage == 0) {
                        stage = 1;
                        begin(server, true);
                    } else if (stage == 1) {
                        stage = 2;
                        player.setPos(-8.5, 101, 12.5);
                        resetCounters();
                    } else finish(server, true, null);
                } else if (stageTicks > 650) {
                    throw new AssertionError("No piglin fired after " + stageTicks + " ticks; closest rider=" + range);
                }
            } catch (Throwable error) {
                finish(server, false, error);
            }
        });
    }

    private void initialize(MinecraftServer server) {
        ServerLevel world = server.overworld();
        for (int x = -32; x <= 32; x++) for (int z = -32; z <= 32; z++) {
            world.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 3);
        }
        GameProfile profile = new GameProfile(UUID.randomUUID(), "RicGhastCheck");
        player = new ServerPlayer(server, world, profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player,
                CommonListenerCookie.createInitial(profile, false));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        player.setHealth(1000);
        player.setPos(8.5, 101, 0.5);
        world.addNewPlayer(player);
        Random_Item_Challenge.participantsWithoutRemovals.add(player.getUUID());
        GoCommand.testMode = true;
        GoCommand.enableMobFriendlyFire = true;
        StopCommand.isRunning = true;
    }

    private void begin(MinecraftServer server, boolean tree) {
        ServerLevel world = server.overworld();
        ArrayList<Entity> placed = new ArrayList<>();
        world.getAllEntities().forEach(entity -> { if (entity != player) placed.add(entity); });
        placed.forEach(Entity::discard);
        for (int x = -5; x <= 5; x++) for (int z = -6; z <= 6; z++) for (int y = 101; y <= 115; y++) {
            world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        if (tree) {
            for (int y = 101; y <= 113; y++) world.setBlock(new BlockPos(0, y, 0), Blocks.BIRCH_LOG.defaultBlockState(), 3);
            for (int x = -4; x <= 4; x++) for (int z = -5; z <= 5; z++) for (int y = 110; y <= 114; y++) {
                world.setBlock(new BlockPos(x, y, z), Blocks.BIRCH_LEAVES.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 3);
            }
        }
        player.setPos(8.5, 101, 0.5);
        ghast = (HappyGhast) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:happy_ghast"))
                .create(world, EntitySpawnReason.COMMAND);
        ghast.setPos(-10.5, 122, 0.5);
        world.addFreshEntity(ghast);
        ItemStack egg = new ItemStack(Items.HAPPY_GHAST_SPAWN_EGG);
        CompoundTag marker = new CompoundTag();
        marker.putString("ric_spawn", "happy_ghast_mount");
        egg.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        SpawnEggHooks.onSpawnedFromItemStack(world, egg, player, ghast);
        resetCounters();
    }

    private void resetCounters() {
        stageTicks = shots = stationary = maximumStationary = 0;
        diagonal = false;
        previous = ghast.position();
        startingHealth = player.getHealth();
        firedOutsideRange = false;
        blockContactTicks = 0;
    }

    private double minimumRiderDistance(ServerLevel world) {
        final double[] closest = {Double.POSITIVE_INFINITY};
        world.getAllEntities().forEach(entity -> {
            if (entity != ghast && entity.entityTags().contains("ric_jockey") && entity.isPassenger()) {
                closest[0] = Math.min(closest[0], entity.position().distanceTo(player.position()));
            }
        });
        return closest[0];
    }

    private void finish(MinecraftServer server, boolean success, Throwable error) {
        done = true;
        System.out.println(success ? "GHAST_INTEGRATION_COMPLETE" : "GHAST_INTEGRATION_FAILED");
        if (error != null) error.printStackTrace();
        StopCommand.isRunning = false;
        server.overworld().players().remove(player);
        if (player != null) player.discard();
        server.halt(false);
    }
}
