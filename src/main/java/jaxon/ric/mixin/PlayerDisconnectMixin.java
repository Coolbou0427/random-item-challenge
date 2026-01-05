package jaxon.ric.mixin;

import jaxon.ric.Gamerz;
import jaxon.ric.Winner;
import jaxon.ric.command.GoCommand;
import jaxon.ric.command.TeamsCommand;
import jaxon.ric.command.StopCommand;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mixin(PlayerManager.class)
public class PlayerDisconnectMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerRemove(ServerPlayerEntity player, CallbackInfo ci) {
        try {
            if (player == null) return;

            String playerName = player.getName().getString();

            if (!StopCommand.isRunning) return;

            Gamerz gamerToRemove = null;
            for (Gamerz g : Gamerz.gamersList) {
                if (g.name.equals(playerName)) {
                    gamerToRemove = g;
                    break;
                }
            }
            if (gamerToRemove == null) return;

            if (GoCommand.teamMode && TeamsCommand.teams != null) {
                TeamsCommand.teamsWithoutRemovals = new HashMap<>(TeamsCommand.teams);
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
        } catch (Exception ignored) {
        }
    }
}
