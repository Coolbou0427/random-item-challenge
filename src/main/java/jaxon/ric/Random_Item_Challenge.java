package jaxon.ric;

import jaxon.ric.command.GoCommand;
import jaxon.ric.command.StopCommand;
import jaxon.ric.command.TeamsCommand;
import jaxon.ric.command.SettingsCommands;
import jaxon.ric.command.NewSpawn;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import static net.minecraft.world.GameMode.SPECTATOR;

public class Random_Item_Challenge implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Random_Item_Challenge.class);
	public static Map<String, List<String>> randomItems = new ConcurrentHashMap<>(Items.randomItems);
	public static MinecraftServer server;
	public static Thread mainThread;
	private static ConfigManager configManager;
	public static ConfigManager getConfigManager() { return configManager; }
	public static volatile double delay;

	@Override
	public void onInitialize() {
		configManager = new ConfigManager();
		delay = configManager.delay;

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			GoCommand.register(dispatcher);
			StopCommand.register(dispatcher);
			TeamsCommand.register(dispatcher);
			SettingsCommands.register(dispatcher);
			NewSpawn.register(dispatcher);
		});

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			Random_Item_Challenge.server = server;

			Scoreboard scoreboard = server.getScoreboard();
			for (Team team : new ArrayList<>(scoreboard.getTeams())) {
				scoreboard.removeTeam(team);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			StopCommand.isRunning = false;
			configManager.delay                 = delay;
			configManager.enablemobfriendlyfire = GoCommand.enableMobFriendlyFire;
			configManager.enabletntautoexplode  = GoCommand.enableTntAutoExplode;
			configManager.saveConfig();

			if (mainThread != null && mainThread.isAlive()) mainThread.interrupt();

			Scoreboard sb = server.getScoreboard();
			for (Team t : new ArrayList<>(sb.getTeams())) sb.removeTeam(t);
		});

		LOGGER.info("Random Item Challenge mod initialized.");
	}

	public static class SafeRandom {
		private static final ThreadLocal<ThreadLocalRandom> threadLocalRandom = ThreadLocal.withInitial(ThreadLocalRandom::current);
		public static int getNextInt(int bound) {
			try {
				return threadLocalRandom.get().nextInt(bound);
			} catch (Exception e) {
				LOGGER.error("Random generation error: " + e.getMessage());
				return 0;
			}
		}
		public static ThreadLocalRandom getUniqueRandom() {
			try {
				return threadLocalRandom.get();
			} catch (Exception e) {
				LOGGER.error("Random generation error: " + e.getMessage());
				return ThreadLocalRandom.current();
			}
		}
	}

	public static void mainFunc() {
		if (Gamerz.gamersList.size() > 1) {
			GoCommand.testMode = true;
		}
		GoCommand.centerWorldBorderAndSetSpawnpoint();
		executeStartupCommands(GoCommand.resumeMode ? configManager.resumeCommands : configManager.regularCommands);
		Gamerz.maxHealthAndMaxHunger();
		Items.dayChanceAndRainChance();
		StopCommand.isRunning = true;
		mainThread = new Thread(() -> {
			while (StopCommand.isRunning) {
				if (server.isPaused()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break;
					}
					continue;
				}

				try {
					server.execute(() -> {
						List<String> gamerNamesSnapshot = new ArrayList<>(Gamerz.getAllPlayerNames());
						for (String gamerName : gamerNamesSnapshot) {
							ServerPlayerEntity playerEntity = server.getPlayerManager().getPlayer(gamerName);
							if (playerEntity == null || !playerEntity.isAlive()) {
								continue;
							}

							Gamerz gamer = Gamerz.findByName(gamerName);
							if (gamer == null) continue;

							synchronized (gamer) {
								if (gamer.getPlayerItemsList().isEmpty()) {
									Gamerz.fillOrRefillItemsList(gamerName);
								}

								WeightedItem selectedItem = WeightedItem.selectRandom(gamer.getPlayerItemsList(), gamerName);
								if (selectedItem == null) continue;

								for (String givenItem : selectedItem.getItems()) {
									ServerPlayerEntity p = server.getPlayerManager().getPlayer(gamerName);
									if (p == null || p.interactionManager.getGameMode() == SPECTATOR) continue;

									sendCommand(String.format("give %s %s", gamerName, givenItem), () -> {
										LOGGER.warn("failed to give item " + givenItem + ". please check the spelling and that it exists");
										WeightedItem fallback = WeightedItem.selectRandom(gamer.getPlayerItemsList(), gamerName);
										if (fallback != null) {
											sendCommand(String.format("give %s %s", gamerName, fallback.getItems().get(0)));
											gamer.removeItem(fallback);
										}
									});

									if (randomItems.containsKey(givenItem)) {
										List<String> opts = randomItems.get(givenItem);
										if (!opts.isEmpty()) {
											String rand = opts.get(SafeRandom.getNextInt(opts.size()));
											sendCommand(String.format("give %s %s", gamerName, rand), () -> {
												LOGGER.warn("failed to give item " + rand + ". please check the spelling and that it exists");
												WeightedItem fallback = WeightedItem.selectRandom(gamer.getPlayerItemsList(), gamerName);
												if (fallback != null) {
													sendCommand(String.format("give %s %s", gamerName, fallback.getItems().get(0)));
													gamer.removeItem(fallback);
												}
											});
										}
									}
								}

								gamer.removeItem(selectedItem);
							}
						}
					});

					double localDelay = delay;
					int sleepInterval = 100;
					int elapsedTime = 0;
					while (elapsedTime < localDelay * 1000 && StopCommand.isRunning) {
						if (Thread.interrupted()) break;
						Thread.sleep(sleepInterval);
						elapsedTime += sleepInterval;
						if (localDelay != delay) {
							break;
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					LOGGER.error("Error in main loop", e);
				}
			}
		});
		mainThread.start();
	}

	public static void sendCommand(String command) {
		server.execute(() -> {
			try {
				server.getCommandManager().getDispatcher().execute(
						server.getCommandManager().getDispatcher().parse(command, server.getCommandSource().withLevel(4).withSilent())
				);
			} catch (Exception e) {
				LOGGER.error("Unexpected exception during command execution", e);
				LOGGER.info("Command was: " + command);
			}
		});
	}

	public static void sendCommand(String command, Runnable onFailure) {
		server.execute(() -> {
			try {
				server.getCommandManager().getDispatcher().execute(
						server.getCommandManager().getDispatcher().parse(command, server.getCommandSource().withLevel(4).withSilent())
				);
			} catch (Exception e) {
				LOGGER.warn("failed to give item via command: " + command + ". please check the spelling and that it exists");
				if (onFailure != null) onFailure.run();
			}
		});
	}

	public static void executeStartupCommands(List<String> commands) {
		commands.forEach(command -> {
			if (command.contains("%s")) {
				command = String.format(command, GoCommand.playerName);
			}
			sendCommand(command);
		});
	}
}
