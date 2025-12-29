package jaxon.ric;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Gamerz {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gamerz.class);
    private final List<WeightedItem> playerItemsList = new CopyOnWriteArrayList<>();
    public String name;
    public static List<Gamerz> gamersList = new CopyOnWriteArrayList<>();

    public Gamerz(ServerPlayerEntity gamer) {
        try {
            this.name = gamer.getName().getString();
            gamersList.add(this);
            for (WeightedItem item : Items.itemsAndNumbers) {
                this.playerItemsList.add(new WeightedItem(item.getItems(), item.getWeight()));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void fillOrRefillItemsList(String gamerName) {
        try {
            Gamerz gamer = findByName(gamerName);
            if (gamer != null) {
                gamer.getPlayerItemsList().clear();
                for (WeightedItem item : Items.itemsAndNumbers) {
                    gamer.playerItemsList.add(new WeightedItem(item.getItems(), item.getWeight()));
                }
            }
            LOGGER.info("Gave every single item. Now giving duplicate items...");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.warn("Tried to fill or refill lists but failed.");
        }
    }

    public static List<String> getAllPlayerNames() {
        try {
            return gamersList.stream().map(gamer -> gamer.name).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return new ArrayList<>();
    }

    public static float getHealth(String gamer) {
        MinecraftServer server = Random_Item_Challenge.server;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(gamer);
        return (player != null) ? player.getHealth() : -1;
    }

    public static Gamerz findByName(String name) {
        try {
            for (Gamerz gamer : gamersList) {
                if (gamer.name.equals(name)) {
                    return gamer;
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    public void removeItem(WeightedItem item) {
        try {
            playerItemsList.remove(item);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public List<WeightedItem> getPlayerItemsList() {
        return playerItemsList;
    }

    public static void setHunger(Gamerz gamer, float hungerLevelToSet) {
        ServerPlayerEntity gamerObj = Random_Item_Challenge.server.getPlayerManager().getPlayer(gamer.name);
        if (gamerObj != null) {
            gamerObj.getHungerManager().setFoodLevel((int) hungerLevelToSet);
        }
    }

    public static void maxHealthAndMaxHunger() {
        for (Gamerz gamer : gamersList) {
            setHunger(gamer, 20);
            setHealth(gamer, 20);
        }
    }

    public static void setHealth(Gamerz gamer, float healthLevelToSet) {
        ServerPlayerEntity gamerObj = Random_Item_Challenge.server.getPlayerManager().getPlayer(gamer.name);
        if (gamerObj != null) {
            gamerObj.setHealth(healthLevelToSet);
        }
    }
}
