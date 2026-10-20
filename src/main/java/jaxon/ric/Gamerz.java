package jaxon.ric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;

public class Gamerz {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gamerz.class);

    public static final CopyOnWriteArrayList<Gamerz> gamersList = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<WeightedItem> playerItemsList = new CopyOnWriteArrayList<>();

    public final UUID uuid;
    public final String name;

    public Gamerz(ServerPlayer player) {
        this.uuid = player.getUUID();
        this.name = player.getName().getString();
        refillItems();
        gamersList.add(this);
    }

    public static Gamerz findByUuid(UUID uuid) {
        for (Gamerz g : gamersList) {
            if (g.uuid.equals(uuid)) return g;
        }
        return null;
    }

    public WeightedItem getItem() {
        if (playerItemsList.isEmpty()) refillItems();
        return WeightedItem.selectRandom(playerItemsList);
    }

    public List<WeightedItem> getItems() {
        return playerItemsList;
    }

    public void removeItem(WeightedItem item) {
        if (item == null) return;
        playerItemsList.remove(item);
    }

    public void refillItems() {
        playerItemsList.clear();
        if (Items.itemsAndNumbers != null && !Items.itemsAndNumbers.isEmpty()) {
            playerItemsList.addAll(Items.itemsAndNumbers);
        } else {
            LOGGER.warn("Items.itemsAndNumbers was empty when refilling items list");
        }
    }

    public static void maxHealthAndMaxHunger() {
        for (Gamerz gamer : gamersList) {
            setHunger(gamer, 20);
            setHealth(gamer, 20.0f);
        }
    }

    public static void setHunger(Gamerz gamer, int hungerLevelToSet) {
        ServerPlayer gamerObj = Random_Item_Challenge.getPlayerByUuid(gamer.uuid);
        if (gamerObj == null) return;
        gamerObj.getFoodData().setFoodLevel(hungerLevelToSet);
        gamerObj.getFoodData().setSaturation(20.0f);
    }

    public static void setHealth(Gamerz gamer, float healthLevelToSet) {
        ServerPlayer gamerObj = Random_Item_Challenge.getPlayerByUuid(gamer.uuid);
        if (gamerObj != null) gamerObj.setHealth(healthLevelToSet);
    }
}
