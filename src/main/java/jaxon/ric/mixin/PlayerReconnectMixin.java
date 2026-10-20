package jaxon.ric.mixin;

import jaxon.ric.DisconnectedPlayers;
import jaxon.ric.Gamerz;
import jaxon.ric.Random_Item_Challenge;
import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import jaxon.ric.command.TeamsCommand;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(PlayerList.class)
public class PlayerReconnectMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerReconnectMixin.class);

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void onPlayerConnect(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        try {
            if (player == null) return;
            if (!StopCommand.isRunning) return;

            UUID uuid = player.getUUID();
            if (Random_Item_Challenge.participantsWhoDied.contains(uuid)) return;

            DisconnectedPlayers.Entry entry = DisconnectedPlayers.disconnected.remove(uuid);
            if (entry == null) return;

            boolean alreadyIn = false;
            for (Gamerz g : Gamerz.gamersList) {
                if (g.uuid.equals(uuid)) {
                    alreadyIn = true;
                    break;
                }
            }
            if (!alreadyIn) {
                Gamerz.gamersList.add(entry.gamer);
            }

            if (!GoCommand.teamMode) return;
            if (TeamsCommand.teams == null) return;

            String teamKey = entry.teamKey;
            if (teamKey == null || teamKey.isEmpty()) return;

            List<String> members = TeamsCommand.teams.computeIfAbsent(teamKey, k -> new ArrayList<>());

            String name = entry.gamer.name;
            if (!members.contains(name)) {
                members.add(name);
                var sb = player.level().getScoreboard();
                TeamsCommand.createOrUpdateTeam(sb, teamKey, TeamsCommand.teamDisplayNames.get(teamKey));
                TeamsCommand.addPlayerToTeam(sb, name, teamKey);

            }
        } catch (Exception exception) {
            LOGGER.error("Failed to restore RIC match state for reconnecting player {}",
                    player == null ? "<unknown>" : player.getName().getString(), exception);
        }
    }
}
