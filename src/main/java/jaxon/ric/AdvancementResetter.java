package jaxon.ric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdvancementResetter {
    private static final Map<UUID, Integer> pending = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private AdvancementResetter() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (pending.isEmpty()) return;

            Iterator<Map.Entry<UUID, Integer>> it = pending.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> e = it.next();
                int left = e.getValue() - 1;
                if (left > 0) {
                    e.setValue(left);
                    continue;
                }

                UUID uuid = e.getKey();
                it.remove();

                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player == null) continue;

                resetPlayer(server, player);
            }
        });
    }

    private static void resetPlayer(MinecraftServer server, ServerPlayerEntity player) {
        PlayerAdvancementTracker tracker = player.getAdvancementTracker();

        for (AdvancementEntry entry : server.getAdvancementLoader().getAdvancements()) {
            AdvancementProgress progress = tracker.getProgress(entry);
            if (!progress.isAnyObtained()) continue;

            for (String criterion : progress.getObtainedCriteria()) {
                tracker.revokeCriterion(entry, criterion);
            }
        }

        tracker.sendUpdate(player, true);

    }
}
