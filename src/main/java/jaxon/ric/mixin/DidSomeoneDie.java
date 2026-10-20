package jaxon.ric.mixin;

import jaxon.ric.DisconnectedPlayers;
import jaxon.ric.Gamerz;
import jaxon.ric.Random_Item_Challenge;
import jaxon.ric.Winner;
import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import jaxon.ric.command.TeamsCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.LevelData;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class DidSomeoneDie {

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "die", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource source, CallbackInfo info) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        UUID uuid = player.getUUID();

        boolean matchParticipant = Random_Item_Challenge.participantsWithoutRemovals.contains(uuid);
        boolean alreadyEliminated = Random_Item_Challenge.participantsWhoDied.contains(uuid);
        boolean freshMatchDeath = Random_Item_Challenge.shouldHandleMatchDeath(uuid);
        if (!StopCommand.isRunning && !freshMatchDeath) return;

        if (StopCommand.isRunning && matchParticipant
                && !Random_Item_Challenge.pendingDeathRespawns.containsKey(uuid)) {
            Random_Item_Challenge.pendingDeathRespawns.put(uuid,
                    new Random_Item_Challenge.PendingDeathRespawn(player.level().dimension(), player.position(),
                            player.getYRot(), player.getXRot(), player.getDeltaMovement(), player.getRespawnConfig(),
                            !(alreadyEliminated && player.level().dimension() == net.minecraft.world.level.Level.END)));
        }
        if (freshMatchDeath) Random_Item_Challenge.participantsWhoDied.add(uuid);

        String playerName = player.getName().getString();
        LOGGER.info("{} was killed", playerName);

        // In solo test mode, death still means the player should respawn as a
        // spectator at the death location. Handle it by UUID before looking up
        // the lobby's mutable gamer list, which may already be empty/stale.
        if (GoCommand.testMode && matchParticipant) {
            DisconnectedPlayers.disconnected.remove(uuid);
            Random_Item_Challenge.participantsWhoDied.add(uuid);
            ServerPlayer.RespawnConfig original = player.getRespawnConfig();
            if (original != null) Random_Item_Challenge.originalRespawns.put(uuid, original);
            try {
                if (alreadyEliminated && player.level().dimension() == net.minecraft.world.level.Level.END) {
                    // Once the player has already returned as a spectator,
                    // another End death uses vanilla's Overworld spawn fallback.
                    player.setRespawnPosition(null, false);
                    return;
                }
                BlockPos deathPos = player.blockPosition();
                GlobalPos gp = GlobalPos.of(player.level().dimension(), deathPos);
                LevelData.RespawnData respawn = new LevelData.RespawnData(gp, player.getYRot(), player.getXRot());
                player.setRespawnPosition(new ServerPlayer.RespawnConfig(respawn, true), false);
            } catch (Exception e) {
                LOGGER.error("Failed to set solo test death respawn for {}", playerName, e);
            }
            return;
        }

        Gamerz gamerToRemove = null;
        for (Gamerz gamer : Gamerz.gamersList) {
            if (gamer.name.equals(playerName)) {
                gamerToRemove = gamer;
                break;
            }
        }
        if (gamerToRemove == null) return;

        try {
            player.removeAllEffects();
        } catch (Exception e) {
            LOGGER.error("Failed to clear status effects for {}", playerName, e);
        }


        uuid = gamerToRemove.uuid;
        DisconnectedPlayers.disconnected.remove(uuid);

        ServerPlayer.RespawnConfig original = player.getRespawnConfig();
        if (original != null) {
            Random_Item_Challenge.originalRespawns.put(uuid, original);
        }

        try {
            BlockPos deathPos = player.blockPosition();
            GlobalPos gp = GlobalPos.of(player.level().dimension(), deathPos);
            LevelData.RespawnData sp = new LevelData.RespawnData(gp, player.getYRot(), player.getXRot());
            ServerPlayer.RespawnConfig deathRespawn = new ServerPlayer.RespawnConfig(sp, true);
            player.setRespawnPosition(deathRespawn, false);
        } catch (Exception e) {
            LOGGER.error("Failed to set death respawn for {}", playerName, e);
        }

        Random_Item_Challenge.participantsWhoDied.add(uuid);

        if (GoCommand.teamMode && TeamsCommand.teams != null) {
            TeamsCommand.teamsWithoutRemovals = copyTeams(TeamsCommand.teams);
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
        } else {
            Gamerz.gamersList.remove(gamerToRemove);
        }

        Random_Item_Challenge.queueWinnerEvaluation();
    }

    @Unique
    private static HashMap<String, List<String>> copyTeams(HashMap<String, List<String>> source) {
        HashMap<String, List<String>> copy = new HashMap<>();
        for (var entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }
}
