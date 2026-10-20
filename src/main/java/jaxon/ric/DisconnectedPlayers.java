package jaxon.ric;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DisconnectedPlayers {
    private DisconnectedPlayers() {
    }

    public static final Map<UUID, Entry> disconnected = new HashMap<>();

    public static final class Entry {
        public final Gamerz gamer;
        public final String teamKey;

        public Entry(Gamerz gamer, String teamKey) {
            this.gamer = gamer;
            this.teamKey = teamKey;
        }
    }
}
