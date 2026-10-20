package jaxon.ric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Optional integration with the Tracker Compass mod. */
public final class TrackerCompassIntegration {
    private static final String TRACKER_MOD_ID = "compass-tracker";

    private TrackerCompassIntegration() {}

    public static void giveToMatchParticipants() {
        if (!FabricLoader.getInstance().isModLoaded(TRACKER_MOD_ID)) return;

        try {
            Class<?> compassLogic = Class.forName(
                    "jaxon.tracker.CompassLogic", false, TrackerCompassIntegration.class.getClassLoader());
            Method makeTracker = compassLogic.getMethod("makeTracker", ItemStack.class);

            for (Gamerz gamer : Gamerz.gamersList) {
                ServerPlayer player = Random_Item_Challenge.getPlayerByUuid(gamer.uuid);
                if (player == null || !player.isAlive() || player.isSpectator()) continue;

                ItemStack compass = new ItemStack(Items.COMPASS);
                makeTracker.invoke(null, compass);
                player.getInventory().setItem(8, compass);
                player.getInventory().setChanged();
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError e) {
            Random_Item_Challenge.LOGGER.error("Failed to give match participants tracker compasses", e);
        }
    }
}
