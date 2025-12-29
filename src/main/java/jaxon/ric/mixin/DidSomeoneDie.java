package jaxon.ric.mixin;

import jaxon.ric.Gamerz;
import jaxon.ric.Winner;
import jaxon.ric.command.GoCommand;
import jaxon.ric.command.TeamsCommand;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mixin(ServerPlayerEntity.class)
public abstract class DidSomeoneDie {

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource source, CallbackInfo info) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        String playerName = player.getName().getString();
        LOGGER.info(String.format("%s was killed", playerName));

        Gamerz gamerToRemove = null;
        for (Gamerz gamer : Gamerz.gamersList) {
            if (gamer.name.equals(playerName)) {
                gamerToRemove = gamer;
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
    }
}
