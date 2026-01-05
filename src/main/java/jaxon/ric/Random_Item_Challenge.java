package jaxon.ric;

import jaxon.ric.command.GoCommand;
import jaxon.ric.command.NewSpawn;
import jaxon.ric.command.SettingsCommands;
import jaxon.ric.command.StopCommand;
import jaxon.ric.command.TeamsCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Random_Item_Challenge implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Random_Item_Challenge.class);

	public static MinecraftServer server;

	private static ConfigManager configManager;
	public static volatile double delay;

	private static int lastDelayTicks;
	private static int ticksUntilNextGive;

	private static final Set<String> ricTeamNames = ConcurrentHashMap.newKeySet();
	private static final String RIC_TEAM_PREFIX = "ric_";

	public static java.util.Set<java.util.UUID> participantsWithoutRemovals = new java.util.HashSet<>();
	public static java.util.Set<java.util.UUID> participantsWhoDied = new java.util.HashSet<>();

	private static final CommandOutput NO_FEEDBACK_OUTPUT = new CommandOutput() {
		@Override
		public void sendMessage(Text message) {
		}

		@Override
		public boolean shouldReceiveFeedback() {
			return false;
		}

		@Override
		public boolean shouldTrackOutput() {
			return false;
		}

		@Override
		public boolean shouldBroadcastConsoleToOps() {
			return false;
		}
	};

	public static ConfigManager getConfigManager() {
		return configManager;
	}

	@Override
	public void onInitialize() {
		configManager = new ConfigManager();
		delay = configManager.delay;
		GoCommand.enableMobFriendlyFire = configManager.enablemobfriendlyfire;
		GoCommand.enableTntAutoExplode = configManager.enabletntautoexplode;

		AdvancementResetter.init();

		ServerTickEvents.END_SERVER_TICK.register(Random_Item_Challenge::onEndServerTick);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			GoCommand.register(dispatcher);
			StopCommand.register(dispatcher);
			TeamsCommand.register(dispatcher);
			SettingsCommands.register(dispatcher);
			NewSpawn.register(dispatcher);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, srv) -> {
			ServerPlayerEntity player = handler.player;
			if (player == null) return;
			if (!StopCommand.isRunning) return;

			UUID uuid = player.getUuid();
			if (participantsWithoutRemovals != null && participantsWithoutRemovals.contains(uuid)) {
				boolean died = participantsWhoDied != null && participantsWhoDied.contains(uuid);

				if (!died) {
					if (Gamerz.findByUuid(uuid) == null) new Gamerz(player);
				}

				if (GoCommand.teamMode) {
					String playerName = player.getName().getString();
					for (var e : TeamsCommand.teamsWithoutRemovals.entrySet()) {
						List<String> members = e.getValue();
						if (members != null && members.contains(playerName)) {
							String teamId = e.getKey();
							TeamsCommand.teams.computeIfAbsent(teamId, k -> new java.util.ArrayList<>());
							if (!died) {
								if (!TeamsCommand.teams.get(teamId).contains(playerName)) TeamsCommand.teams.get(teamId).add(playerName);
							}
							Scoreboard sb = server.getScoreboard();
							Team t = sb.getTeam(teamId);
							if (t == null) t = sb.addTeam(teamId);
							sb.addScoreHolderToTeam(playerName, t);
							break;
						}
					}
				}
			}
		});

		ServerLifecycleEvents.SERVER_STARTING.register(srv -> {
			server = srv;
			clearRicTeams(srv.getScoreboard());
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(srv -> {
			configManager.delay = delay;
			configManager.enablemobfriendlyfire = GoCommand.enableMobFriendlyFire;
			configManager.enabletntautoexplode = GoCommand.enableTntAutoExplode;
			configManager.saveConfig();
		});
	}

	public static class SafeRandom {
		private static final ThreadLocal<ThreadLocalRandom> threadLocalRandom = ThreadLocal.withInitial(ThreadLocalRandom::current);

		public static ThreadLocalRandom getUniqueRandom() {
			return threadLocalRandom.get();
		}
	}

	public static void mainFunc() {
        if (server == null) return;

        if (Gamerz.gamersList.isEmpty()) {
            server.getPlayerManager().broadcast(Text.literal("Need at least 1 player to start."), false);
            StopCommand.isRunning = false;
            return;
        }

        if (Gamerz.gamersList.size() == 1) {
            GoCommand.testMode = true;
        }

        participantsWithoutRemovals.clear();
        for (Gamerz g : Gamerz.gamersList) participantsWithoutRemovals.add(g.uuid);

        Items.itemsAndNumbers = configManager.items;

        try {
            TitleFadeS2CPacket timings = new TitleFadeS2CPacket(10, 70, 20);
            TitleS2CPacket titlePacket = new TitleS2CPacket(Text.literal("Random Items").formatted(Formatting.BLUE));
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.networkHandler.sendPacket(timings);
                player.networkHandler.sendPacket(titlePacket);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to broadcast start title", e);
        }

        try {
            applyGamemodeToAll(net.minecraft.world.GameMode.CREATIVE);

            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                try {
                    p.setExperienceLevel(0);
                } catch (Throwable ex) {
                    LOGGER.warn("Failed to set experience level for player {}", p.getName().getString());
                }

                try {
                    p.experienceProgress = 0.0f;
                } catch (Throwable ignored) {
                }

                try {
                    java.lang.reflect.Field f = null;
                    try {
                        f = p.getClass().getDeclaredField("totalExperience");
                    } catch (NoSuchFieldException ignored) {
                        try {
                            f = p.getClass().getDeclaredField("experienceTotal");
                        } catch (NoSuchFieldException ignored2) {
                        }
                    }
                    if (f != null) {
                        f.setAccessible(true);
                        f.setInt(p, 0);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to apply gamemode/xp changes", e);
        }

        GoCommand.centerWorldBorderAndSetSpawnpoint();
        executeStartupCommands(GoCommand.resumeMode ? configManager.resumeCommands : configManager.regularCommands);
        Gamerz.maxHealthAndMaxHunger();
        Items.dayChanceAndRainChance();

        try {
            applyGamemodeToAll(net.minecraft.world.GameMode.SURVIVAL);
        } catch (Exception e) {
            LOGGER.error("Failed to set survival gamemode for all players", e);
        }

        StopCommand.isRunning = true;

        int delayTicks = Math.max(1, (int) Math.round(delay * 20.0));
        lastDelayTicks = delayTicks;
        ticksUntilNextGive = delayTicks;

        giveAllOnce();
    }

	private static void onEndServerTick(MinecraftServer srv) {
		if (!StopCommand.isRunning) return;

		if (!srv.isDedicated() && srv.isPaused()) return;

		int delayTicks = Math.max(1, (int) Math.round(delay * 20.0));
		if (delayTicks != lastDelayTicks) {
			lastDelayTicks = delayTicks;
			ticksUntilNextGive = delayTicks;
		}

		if (ticksUntilNextGive > 0) ticksUntilNextGive--;

		if (ticksUntilNextGive <= 0) {
			giveAllOnce();
			ticksUntilNextGive = lastDelayTicks;
		}
	}

	private static void giveAllOnce() {
		if (server == null) return;

		server.execute(() -> {
			for (Gamerz gamer : new ArrayList<>(Gamerz.gamersList)) {
				if (!StopCommand.isRunning) return;

				ServerPlayerEntity p = getPlayerByUuid(gamer.uuid);
				if (p == null || !p.isAlive()) continue;
				if (p.isSpectator()) continue;

				WeightedItem wi = gamer.getItem();
				if (wi == null) continue;

				List<String> stackArgs = wi.items();
				if (stackArgs == null || stackArgs.isEmpty()) continue;

				boolean allOk = true;

				for (String itemArg : stackArgs) {
					if (itemArg == null || itemArg.trim().isEmpty()) continue;
					boolean ok = executeCommandNow("give " + p.getName().getString() + " " + itemArg.trim());
					if (!ok) allOk = false;
				}

				if (allOk) {
					gamer.removeItem(wi);
				} else {
					List<WeightedItem> fallback = gamer.getItems();
					if (fallback != null && !fallback.isEmpty()) {
						WeightedItem fb = fallback.getFirst();
						if (fb != null && fb.items() != null) {
							boolean fbOk = true;
							for (String itemArg : fb.items()) {
								if (itemArg == null || itemArg.trim().isEmpty()) continue;
								boolean ok = executeCommandNow("give " + p.getName().getString() + " " + itemArg.trim());
								if (!ok) fbOk = false;
							}
							if (fbOk) gamer.removeItem(fb);
						}
					}
				}
			}
		});
	}

	private static boolean executeCommandNow(String rawCommand) {
		try {
			if (server == null) return false;

			String command = rawCommand == null ? "" : rawCommand.trim();
			if (command.isEmpty()) return false;
			if (command.startsWith("/")) command = command.substring(1);

			ServerCommandSource src = server.getCommandSource().withOutput(NO_FEEDBACK_OUTPUT);
			var dispatcher = server.getCommandManager().getDispatcher();
			var parseResults = dispatcher.parse(command, src);
			int result = dispatcher.execute(parseResults);

			return result > 0;
		} catch (Exception e) {
			LOGGER.error("Command failed: {}", rawCommand, e);
			return false;
		}
	}

	private static void applyGamemodeToAll(net.minecraft.world.GameMode mode) {
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			p.changeGameMode(mode);
		}
	}

	private static void clearEffectsAllIfAny() {
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			Collection<StatusEffectInstance> effects = p.getStatusEffects();
			if (effects == null || effects.isEmpty()) continue;
			p.clearStatusEffects();
		}

	}

	private static void clearInventoryAllIfAny() {
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (p == null) continue;
			if (p.getInventory() == null) continue;
			if (p.getInventory().isEmpty()) continue;
			p.getInventory().clear();
		}
	}


	public static void executeStartupCommands(List<String> commands) {
		if (server == null) return;
		if (commands == null || commands.isEmpty()) return;

		for (String cmd : commands) {
			if (cmd == null) continue;
			String out = cmd.trim();
			if (out.isEmpty()) continue;

			switch (out) {
				case "gamemode spectator @a" -> applyGamemodeToAll(net.minecraft.world.GameMode.SPECTATOR);
				case "gamemode creative @a" -> applyGamemodeToAll(net.minecraft.world.GameMode.CREATIVE);
				case "gamemode survival @a" -> applyGamemodeToAll(net.minecraft.world.GameMode.SURVIVAL);
				case "effect clear @a" -> clearEffectsAllIfAny();
				case "clear @a" -> clearInventoryAllIfAny();
				default -> executeCommandNow(out);
			}
		}
	}


	public static void resetGameState() {
		StopCommand.isRunning = false;
		ticksUntilNextGive = 0;
		lastDelayTicks = 0;
		participantsWithoutRemovals.clear();
		participantsWhoDied.clear();
	}

	public static void clearRicTeams(Scoreboard scoreboard) {
		if (scoreboard == null) return;

		Set<String> names = new HashSet<>(ricTeamNames);
		ricTeamNames.clear();

		for (String name : names) {
			net.minecraft.scoreboard.Team t = scoreboard.getTeam(name);
			if (t != null) scoreboard.removeTeam(t);
		}

		for (net.minecraft.scoreboard.Team team : new ArrayList<>(scoreboard.getTeams())) {
			String n = team.getName();
			if (n != null && n.startsWith(RIC_TEAM_PREFIX)) scoreboard.removeTeam(team);
		}
	}

	public static ServerPlayerEntity getPlayerByUuid(java.util.UUID uuid) {
		if (server == null) return null;
		return server.getPlayerManager().getPlayer(uuid);
	}
}
