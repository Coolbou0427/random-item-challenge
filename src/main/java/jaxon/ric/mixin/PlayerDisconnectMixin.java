package jaxon.ric.mixin;

import jaxon.ric.DisconnectedPlayers;
import jaxon.ric.Gamerz;
import jaxon.ric.Winner;
import jaxon.ric.command.GoCommand;
import jaxon.ric.command.TeamsCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import jaxon.ric.command.StopCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(PlayerList.class)
public class PlayerDisconnectMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDisconnectMixin.class);

    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerRemove(ServerPlayer player, CallbackInfo ci) {
        try {
            if (player == null) return;
            if (!StopCommand.isRunning) return;

            String playerName = player.getName().getString();

            Gamerz gamerToRemove = null;
            for (Gamerz g : Gamerz.gamersList) {
                if (g.name.equals(playerName)) {
                    gamerToRemove = g;
                    break;
                }
            }
            if (gamerToRemove == null) return;

            String teamKey = null;
            if (GoCommand.teamMode && TeamsCommand.teams != null) {
                for (var entry : TeamsCommand.teams.entrySet()) {
                    List<String> members = entry.getValue();
                    if (members != null && members.contains(gamerToRemove.name)) {
                        teamKey = entry.getKey();
                        break;
                    }
                }
            }

            DisconnectedPlayers.disconnected.put(gamerToRemove.uuid, new DisconnectedPlayers.Entry(gamerToRemove, teamKey));

            if (GoCommand.teamMode && TeamsCommand.teams != null) {
                ArrayList<String> keys = new ArrayList<>(TeamsCommand.teams.keySet());
                for (String team : keys) {
                    List<String> members = TeamsCommand.teams.get(team);
                    if (members != null) {
                        members.remove(gamerToRemove.name);
                        if (members.isEmpty()) {
                            TeamsCommand.teams.remove(team);
                        }
                    }
                }
                Gamerz.gamersList.remove(gamerToRemove);
                if (TeamsCommand.teams.size() == 1) {
                    Winner.Declare();
                }
            } else {
                Gamerz.gamersList.remove(gamerToRemove);
                if (Gamerz.gamersList.size() == 1) {
                    Winner.Declare();
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to update RIC match state while disconnecting player {}",
                    player == null ? "<unknown>" : player.getName().getString(), exception);
        }
    }
}
