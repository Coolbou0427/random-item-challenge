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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ServerPlayer.class)
public abstract class LethalDamageSpectator {

    @Shadow @Final private static Logger LOGGER;

    @Unique
    private static final ConcurrentHashMap<UUID, Long> ric$pendingElimTick = new ConcurrentHashMap<>();

    @ModifyArg(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            index = 2
    )

    private float ric$adjustLethalDamage(float amount) {
        if (!StopCommand.isRunning) return amount;

        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.isSpectator()) return amount;
        if (GoCommand.testMode) return amount;

        UUID uuid = player.getUUID();
        if (Random_Item_Challenge.participantsWhoDied.contains(uuid)) return amount;

        ServerLevel world = player.level();

        float health = player.getHealth();
        float absorption = player.getAbsorptionAmount();
        float effective = health + absorption;

        if (effective - amount > 0.0F) return amount;

        ric$pendingElimTick.put(uuid, world.getGameTime());

        float adjusted = effective - 0.001F;
        if (adjusted < 0.001F) adjusted = 0.001F;
        return adjusted;
    }



    @Inject(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("RETURN")
    )
    private void ric$finishElim(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!StopCommand.isRunning) return;

        ServerPlayer player = (ServerPlayer) (Object) this;
        UUID uuid = player.getUUID();

        Long markedTick = ric$pendingElimTick.remove(uuid);
        if (markedTick == null) return;
        if (markedTick != world.getGameTime()) return;
        if (!cir.getReturnValue()) return;


        String playerName = player.getName().getString();

        Gamerz gamerToRemove = null;
        for (Gamerz gamer : Gamerz.gamersList) {
            if (gamer.name.equals(playerName)) {
                gamerToRemove = gamer;
                break;
            }
        }
        if (gamerToRemove == null) return;

        DisconnectedPlayers.disconnected.remove(uuid);
        Random_Item_Challenge.participantsWhoDied.add(uuid);

        try {
            net.minecraft.server.MinecraftServer srv = Random_Item_Challenge.server;
            if (srv != null) {
                Component text = player.getCombatTracker().getDeathMessage();
                Team team = player.getTeam();

                if (team != null && team.getDeathMessageVisibility() != Team.Visibility.ALWAYS) {
                    if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
                        srv.getPlayerList().broadcastSystemToTeam(player, text);
                    } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                        srv.getPlayerList().broadcastSystemToAllExceptTeam(player, text);
                    } else {
                        srv.getPlayerList().broadcastSystemMessage(text, false);
                    }
                } else {
                    srv.getPlayerList().broadcastSystemMessage(text, false);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to broadcast vanilla death message for {}", playerName, e);
        }

        try {
            player.removeAllEffects();
        } catch (Exception e) {
            LOGGER.error("Failed to clear status effects for {}", playerName, e);
        }

        try {
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        } catch (Exception e) {
            LOGGER.error("Failed to set spectator for {}", playerName, e);
        }


        if (GoCommand.teamMode && TeamsCommand.teams != null) {
            TeamsCommand.teamsWithoutRemovals = new HashMap<>(TeamsCommand.teams);
            ArrayList<String> keys = new ArrayList<>(TeamsCommand.teams.keySet());
            for (String teamKey : keys) {
                List<String> members = TeamsCommand.teams.get(teamKey);
                if (members != null) {
                    members.remove(gamerToRemove.name);
                    if (members.isEmpty()) {
                        TeamsCommand.teams.remove(teamKey);
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

    @Inject(
            method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD")
    )
    private void ric$fixRespawnObstructedDuringMatch(DamageSource source, CallbackInfo ci) {
        if (!StopCommand.isRunning) return;
        if (GoCommand.testMode) return;


        ServerPlayer player = (ServerPlayer) (Object) this;
        UUID uuid = player.getUUID();

        if (Random_Item_Challenge.participantsWhoDied.contains(uuid)) return;

        Gamerz gamerToRemove = null;
        String playerName = player.getName().getString();
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


        try {
            ServerPlayer.RespawnConfig current = player.getRespawnConfig();
            if (current != null) {
                Random_Item_Challenge.originalRespawns.put(uuid, current);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save original respawn for {}", playerName, e);
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

        DisconnectedPlayers.disconnected.remove(uuid);
        Random_Item_Challenge.participantsWhoDied.add(uuid);

        if (GoCommand.teamMode && TeamsCommand.teams != null) {
            TeamsCommand.teamsWithoutRemovals = new HashMap<>(TeamsCommand.teams);
            ArrayList<String> keys = new ArrayList<>(TeamsCommand.teams.keySet());
            for (String teamKey : keys) {
                List<String> members = TeamsCommand.teams.get(teamKey);
                if (members != null) {
                    members.remove(gamerToRemove.name);
                    if (members.isEmpty()) {
                        TeamsCommand.teams.remove(teamKey);
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

