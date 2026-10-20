package jaxon.ric;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public record WeightedItem(List<String> items, int weight, String categoryOverride) {

    public WeightedItem(List<String> items, int weight) {
        this(items, weight, null);
    }

    public WeightedItem {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static WeightedItem selectRandom(List<WeightedItem> items) {
        return selectRandom(items, java.util.concurrent.ThreadLocalRandom.current());
    }

    static WeightedItem selectRandom(List<WeightedItem> items, RandomGenerator random) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(random, "random");

        long totalWeight = 0;
        for (WeightedItem item : items) {
            if (item.weight() > 0) totalWeight += item.weight();
        }
        if (totalWeight <= 0) return null;

        long selectedWeight = random.nextLong(totalWeight);
        for (WeightedItem item : items) {
            if (item.weight() <= 0) continue;
            selectedWeight -= item.weight();
            if (selectedWeight < 0) return item;
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
