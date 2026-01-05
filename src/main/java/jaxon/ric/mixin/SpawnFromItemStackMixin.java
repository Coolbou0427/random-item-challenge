package jaxon.ric.mixin;

import jaxon.ric.RicJockeySpawns;
import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public class SpawnFromItemStackMixin {

    @Inject(method = "spawnFromItemStack", at = @At("RETURN"))
    private void ric$afterSpawnFromItemStack(
            ServerWorld world,
            @Nullable ItemStack stack,
            @Nullable LivingEntity spawner,
            BlockPos pos,
            SpawnReason spawnReason,
            boolean alignPosition,
            boolean invertY,
            CallbackInfoReturnable<Entity> cir
    ) {
        Entity spawned = cir.getReturnValue();
        if (spawned == null) return;
        if (!(spawned instanceof MobEntity mob)) return;
        if (stack == null || !(stack.getItem() instanceof net.minecraft.item.SpawnEggItem)) return;

        boolean mobFF = GoCommand.enableMobFriendlyFire;

        ServerPlayerEntity ownerPlayer = null;
        if (spawner instanceof PlayerEntity p && p instanceof ServerPlayerEntity sp) {
            ownerPlayer = sp;
        } else {
            MinecraftServer srv = world.getServer();
            if (srv != null) {
                double bestDist = Double.MAX_VALUE;
                for (ServerPlayerEntity candidate : srv.getPlayerManager().getPlayerList()) {
                    if (candidate.isSpectator()) continue;
                    if (candidate.getEntityWorld() != world) continue;
                    ItemStack main = candidate.getMainHandStack();
                    if (main == null) continue;
                    if (main.getItem() != stack.getItem()) continue;
                    double d = candidate.squaredDistanceTo(spawned);
                    if (d < bestDist) {
                        bestDist = d;
                        ownerPlayer = candidate;
                    }
                }
                if (ownerPlayer == null) {
                    ServerPlayerEntity sole = null;
                    int count = 0;
                    for (ServerPlayerEntity candidate : srv.getPlayerManager().getPlayerList()) {
                        if (candidate.isSpectator()) continue;
                        if (candidate.getEntityWorld() != world) continue;
                        sole = candidate;
                        count++;
                    }
                    if (count == 1) ownerPlayer = sole;
                }
            }
        }

        if ((StopCommand.isRunning || GoCommand.testMode) && !mobFF) {
            Scoreboard sb = world.getScoreboard();
            if (ownerPlayer != null) {
                Team ownerTeam = ownerPlayer.getScoreboardTeam();
                if (GoCommand.teamMode && ownerTeam != null) {
                    sb.addScoreHolderToTeam(mob.getUuidAsString(), ownerTeam);
                } else {
                    String ownerShort = ownerPlayer.getUuid().toString().replace("-", "");
                    ownerShort = ownerShort.length() > 8 ? ownerShort.substring(0, 8) : ownerShort;
                    String teamId = "ric_mob_" + ownerShort;

                    Team team = sb.getTeam(teamId);
                    if (team == null) {
                        team = sb.addTeam(teamId);
                        sb.addScoreHolderToTeam(ownerPlayer.getName().getString(), team);
                        team.setFriendlyFireAllowed(false);
                    }

                    sb.addScoreHolderToTeam(mob.getUuidAsString(), team);
                }

                if (!(spawner instanceof PlayerEntity)) {
                    if (!ownerPlayer.isCreative()) {
                        ItemStack main = ownerPlayer.getMainHandStack();
                        if (main != null && main.getItem() == stack.getItem() && main.getCount() > 0) {
                            main.decrement(1);
                        }
                    }
                }
            }
        }

        RicJockeySpawns.tryApply(world, stack, spawned);
    }

    @Mixin(Entity.class)
    public interface EntityAddPassengerInvoker {
        @Invoker("addPassenger")
        void ric$addPassenger(Entity passenger);
    }
}
