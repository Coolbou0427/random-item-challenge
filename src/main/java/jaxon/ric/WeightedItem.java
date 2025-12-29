package jaxon.ric;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import jaxon.ric.Random_Item_Challenge.SafeRandom;

public class WeightedItem {
    private List<String> items;
    private int weight;

    public WeightedItem(List<String> items, int weight) {
        this.items = items;
        this.weight = weight;
    }

    public List<String> getItems() {
        return items;
    }

    public int getWeight() {
        return weight;
    }

    public static WeightedItem selectRandom(List<WeightedItem> items, String gamer) {
        try {
            Random random = SafeRandom.getUniqueRandom();
            int totalWeight = items.stream().mapToInt(WeightedItem::getWeight).sum();
            if (items.isEmpty() || totalWeight <= 0) {
                Gamerz.fillOrRefillItemsList(gamer);
                return null;
            }
            int index = random.nextInt(totalWeight);
            int sum = 0;
            for (WeightedItem item : items) {
                sum += item.getWeight();
                if (index < sum) {
                    return item;
                }
            }
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error in selectRandom: " + e.getMessage());
        }
        return null;
    }
}
