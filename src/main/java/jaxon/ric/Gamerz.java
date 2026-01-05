package jaxon.ric;

import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Gamerz {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gamerz.class);

    public static final CopyOnWriteArrayList<Gamerz> gamersList = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<WeightedItem> playerItemsList = new CopyOnWriteArrayList<>();

    public final UUID uuid;
    public final String name;

    public Gamerz(ServerPlayerEntity player) {
        this.uuid = player.getUuid();
        this.name = player.getName().getString();
        fillOrRefillItemsList(this.uuid);
        gamersList.add(this);
    }

    public static Gamerz findByUuid(UUID uuid) {
        for (Gamerz g : gamersList) {
            if (g.uuid.equals(uuid)) return g;
        }
        return null;
    }

    public WeightedItem getItem() {
        if (playerItemsList.isEmpty()) fillOrRefillItemsList(uuid);
        WeightedItem wi = WeightedItem.selectRandom(playerItemsList, uuid);
        if (wi == null) {
            fillOrRefillItemsList(uuid);
            wi = WeightedItem.selectRandom(playerItemsList, uuid);
        }
        return wi;
    }

    public List<WeightedItem> getItems() {
        return playerItemsList;
    }

    public void removeItem(WeightedItem item) {
        if (item == null) return;
        playerItemsList.remove(item);
    }

    public static void fillOrRefillItemsList(UUID gamerUuid) {
        Gamerz gamer = findByUuid(gamerUuid);
        if (gamer == null) return;

        gamer.playerItemsList.clear();
        if (Items.itemsAndNumbers != null && !Items.itemsAndNumbers.isEmpty()) {
            gamer.playerItemsList.addAll(Items.itemsAndNumbers);
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
        ServerPlayerEntity gamerObj = Random_Item_Challenge.getPlayerByUuid(gamer.uuid);
        if (gamerObj == null) return;
        gamerObj.getHungerManager().setFoodLevel(hungerLevelToSet);
        gamerObj.getHungerManager().setSaturationLevel(20.0f);
    }

    public static void setHealth(Gamerz gamer, float healthLevelToSet) {
        ServerPlayerEntity gamerObj = Random_Item_Challenge.getPlayerByUuid(gamer.uuid);
        if (gamerObj != null) gamerObj.setHealth(healthLevelToSet);
    }
}
