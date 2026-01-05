package jaxon.ric;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public record WeightedItem(List<String> items, int weight) {

    public static WeightedItem selectRandom(List<WeightedItem> items, UUID gamerUuid) {
        try {
            Random random = Random_Item_Challenge.SafeRandom.getUniqueRandom();
            int totalWeight = items.stream().mapToInt(WeightedItem::weight).sum();
            if (items.isEmpty() || totalWeight <= 0) {
                Gamerz.fillOrRefillItemsList(gamerUuid);
                return null;
            }
            int index = random.nextInt(totalWeight);
            int sum = 0;
            for (WeightedItem item : items) {
                sum += item.weight();
                if (index < sum) {
                    return item;
                }
            }
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Error in selectRandom: {}", e.getMessage());
        }
        return null;
    }
    public List<String> getItems() {
        return items;
    }

    public int getWeight() {
        return weight;
    }

}
