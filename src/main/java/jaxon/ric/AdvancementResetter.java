package jaxon.ric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

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

                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player == null) continue;

                resetPlayer(server, player);
            }
        });
    }

    private static void resetPlayer(MinecraftServer server, ServerPlayer player) {
        PlayerAdvancements tracker = player.getAdvancements();

        for (AdvancementHolder entry : server.getAdvancements().getAllAdvancements()) {
            AdvancementProgress progress = tracker.getOrStartProgress(entry);
            if (!progress.hasProgress()) continue;

            for (String criterion : progress.getCompletedCriteria()) {
                tracker.revoke(entry, criterion);
            }
        }

        tracker.flushDirty(player, true);

    }
}
