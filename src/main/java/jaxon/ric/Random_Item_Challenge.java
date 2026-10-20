package jaxon.ric;

import jaxon.ric.command.GoCommand;
import jaxon.ric.command.DropsCommand;
import jaxon.ric.command.NewSpawn;
import jaxon.ric.command.SettingsCommands;
import jaxon.ric.command.StopCommand;
import jaxon.ric.command.TeamsCommand;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

	private static final String RIC_TEAM_PREFIX = "ric_";

	public static java.util.Set<java.util.UUID> participantsWithoutRemovals = new java.util.HashSet<>();
	public static java.util.Set<java.util.UUID> participantsWhoDied = new java.util.HashSet<>();
	public record PendingDeathRespawn(ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch,
									   Vec3 velocity, ServerPlayer.RespawnConfig originalRespawn,
									   boolean returnToDeathLocation) {}
	public static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, PendingDeathRespawn> pendingDeathRespawns = new java.util.concurrent.ConcurrentHashMap<>();
	public static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, net.minecraft.server.level.ServerPlayer.RespawnConfig> originalRespawns = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, Integer> forceSpectatorTicks = new java.util.concurrent.ConcurrentHashMap<>();
	private static final Set<UUID> lateJoinSpectators = ConcurrentHashMap.newKeySet();
	private static volatile boolean deathHandlingActive = false;
	private static boolean winnerEvaluationQueued = false;
	private static int deferredDeathStateResetTicks = -1;

	private static volatile boolean isStarting = false;
	private static int pendingSurvivalReapplyTicks = -1;

	private static volatile boolean startWaitingForTeleports = false;
	private static int startWaitTicksLeft = -1;
	private static ResourceKey<Level> startTargetWorld;
	private static double startTargetX;
	private static double startTargetY;
	private static double startTargetZ;

	private static final CommandSource NO_FEEDBACK_OUTPUT = new CommandSource() {
		@Override
		public void sendSystemMessage(Component message) {
		}

		@Override
		public boolean acceptsSuccess() {
			return false;
		}

		@Override
		public boolean acceptsFailure() {
			return false;
		}

		@Override
		public boolean shouldInformAdmins() {
			return false;
		}
	};

	public static ConfigManager getConfigManager() {
		return configManager;
	}

	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) return;

		configManager = new ConfigManager();
		delay = configManager.delay;
		GoCommand.enableMobFriendlyFire = configManager.enablemobfriendlyfire;
		DirectEnchantingHooks.enabled = configManager.enableDirectEnchanting;
		GoCommand.enableTntAutoExplode = configManager.enabletntautoexplode;
		GoCommand.enablePlayerPlacedBlockDrops = configManager.enablePlayerPlacedBlockDrops;

		RicMobCleanup.register();

		ServerTickEvents.END_SERVER_TICK.register(Random_Item_Challenge::onEndServerTick);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			GoCommand.register(dispatcher);
			StopCommand.register(dispatcher);
			TeamsCommand.register(dispatcher);
			SettingsCommands.register(dispatcher);
			NewSpawn.register(dispatcher);
			DropsCommand.register(dispatcher);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, srv) -> {
			ServerPlayer player = handler.player;
			if (player == null) return;
			if (!StopCommand.isRunning) return;

			UUID uuid = player.getUUID();

			if (DisconnectedPlayers.disconnected.containsKey(uuid)) return;

			boolean isParticipant = participantsWithoutRemovals != null && participantsWithoutRemovals.contains(uuid);
			if (!isParticipant) {
				lateJoinSpectators.add(uuid);
				forceSpectatorTicks.put(uuid, 40);
				teleportLateJoinerToArena(srv, player);
				return;
			}

			boolean died = participantsWhoDied != null && participantsWhoDied.contains(uuid);

			if (!died) {
				if (Gamerz.findByUuid(uuid) == null) new Gamerz(player);
			}

			if (GoCommand.teamMode) {
				String playerName = player.getName().getString();

				String foundTeamId = null;

				for (var e : TeamsCommand.teamsWithoutRemovals.entrySet()) {
					List<String> members = e.getValue();
					if (members != null && members.contains(playerName)) {
						foundTeamId = e.getKey();
						break;
					}
				}

				if (foundTeamId == null) {
					int bestSize = Integer.MAX_VALUE;

					for (var e : TeamsCommand.teamsWithoutRemovals.entrySet()) {
						List<String> members = e.getValue();
						int size = members == null ? 0 : members.size();
						if (size < bestSize) {
							bestSize = size;
							foundTeamId = e.getKey();
						}
					}

					if (foundTeamId != null) {
						TeamsCommand.teamsWithoutRemovals.computeIfAbsent(foundTeamId, k -> new java.util.ArrayList<>());
						if (!TeamsCommand.teamsWithoutRemovals.get(foundTeamId).contains(playerName)) {
							TeamsCommand.teamsWithoutRemovals.get(foundTeamId).add(playerName);
						}
					}
				}

				if (foundTeamId != null) {
					Scoreboard sb = srv.getScoreboard();
					TeamsCommand.createOrUpdateTeam(sb, foundTeamId, TeamsCommand.teamDisplayNames.get(foundTeamId));
					TeamsCommand.addPlayerToTeam(sb, playerName, foundTeamId);

					if (!died) {
						TeamsCommand.teams.computeIfAbsent(foundTeamId, k -> new java.util.ArrayList<>());
						if (!TeamsCommand.teams.get(foundTeamId).contains(playerName)) {
							TeamsCommand.teams.get(foundTeamId).add(playerName);
						}
					}
				}
			}

		});

		ServerLifecycleEvents.SERVER_STARTING.register(srv -> {
			server = srv;
			RicMobCleanup.start(srv);
			CommandSourceStack src = srv.createCommandSourceStack();
			TeamsCommand.clearAllTeams(src, false);
		});


		ServerLifecycleEvents.SERVER_STOPPING.register(srv -> {
			configManager.delay = delay;
			configManager.enablemobfriendlyfire = GoCommand.enableMobFriendlyFire;
			configManager.enabletntautoexplode = GoCommand.enableTntAutoExplode;
			configManager.enablePlayerPlacedBlockDrops = GoCommand.enablePlayerPlacedBlockDrops;
			configManager.saveConfig();
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			UUID uuid = newPlayer.getUUID();
			PendingDeathRespawn pendingDeath = pendingDeathRespawns.remove(uuid);
			boolean eliminated = pendingDeath != null || participantsWhoDied.contains(uuid);
			if (eliminated) {
				forceSpectatorTicks.put(uuid, 20);
				try {
					newPlayer.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
				} catch (Exception e) {
					LOGGER.error("Failed to restore spectator mode for {}", newPlayer.getName().getString(), e);
				}

				net.minecraft.server.level.ServerPlayer.RespawnConfig original = pendingDeath != null
						? pendingDeath.originalRespawn() : null;
				if (original == null) original = originalRespawns.remove(uuid);
				if (pendingDeath != null || original != null) {
					try {
						newPlayer.setRespawnPosition(original, false);
					} catch (Exception e) {
						LOGGER.error("Failed to restore spawn point after respawn for {}", newPlayer.getName().getString(), e);
					}
				}

				// Vanilla may respawn a player in the Overworld when they die in the
				// Nether or End. Eliminated RIC players should spectate from the exact
				// place they died, rather than being moved to the match start point.
				if (pendingDeath != null && pendingDeath.returnToDeathLocation()) {
					ServerLevel deathLevel = server == null ? null : server.getLevel(pendingDeath.dimension());
					if (deathLevel != null) {
						try {
							Vec3 pos = pendingDeath.position();
							newPlayer.teleportTo(deathLevel, pos.x, pos.y, pos.z,
									java.util.Set.of(), pendingDeath.yaw(), pendingDeath.pitch(), true);
							newPlayer.setDeltaMovement(pendingDeath.velocity());
							newPlayer.connection.send(new ClientboundSetEntityMotionPacket(newPlayer));
						} catch (Exception e) {
							LOGGER.error("Failed to restore eliminated player {} to their saved death state",
									newPlayer.getName().getString(), e);
						}
					}
				} else if (pendingDeath == null && oldPlayer.level() instanceof ServerLevel deathLevel) {
					try {
						newPlayer.teleportTo(deathLevel, oldPlayer.getX(), oldPlayer.getY(), oldPlayer.getZ(),
								java.util.Set.of(), oldPlayer.getYRot(), oldPlayer.getXRot(), true);
					} catch (Exception e) {
						LOGGER.error("Failed to return eliminated player {} to their death location", newPlayer.getName().getString(), e);
					}
				}
				return;
			}

			// Players who respawn without being eliminated return to RIC's arena.
			if (!StopCommand.isRunning || !participantsWithoutRemovals.contains(uuid)
					|| startTargetWorld == null || server == null) return;

			ServerLevel arena = server.getLevel(startTargetWorld);
			if (arena == null) return;
			try {
				newPlayer.teleportTo(arena, startTargetX, startTargetY, startTargetZ,
						java.util.Set.of(), newPlayer.getYRot(), newPlayer.getXRot(), true);
			} catch (Exception e) {
				LOGGER.error("Failed to return {} to the RIC arena after respawn", newPlayer.getName().getString(), e);
			}
		});
	}

	public static void beginStartAfterTeleports(ResourceKey<Level> targetWorld, double x, double y, double z) {
		startWaitingForTeleports = true;
		startTargetWorld = targetWorld;
		startTargetX = x;
		startTargetY = y;
		startTargetZ = z;
		startWaitTicksLeft = 60;
	}

	private static void onEndServerTick(MinecraftServer srv) {
		RicMobCleanup.tick();
		if (!StopCommand.isRunning && !lateJoinSpectators.isEmpty()) {
			lateJoinSpectators.clear();
		}

		if (!StopCommand.isRunning && !deathHandlingActive && !forceSpectatorTicks.isEmpty()) {
			forceSpectatorTicks.clear();
		}

		if (startWaitingForTeleports) {
			boolean allClose = true;

			for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
				if (p == null) continue;

				if (p.level().dimension() != startTargetWorld) {
					allClose = false;
					break;
				}

				double dx = p.getX() - startTargetX;
				double dz = p.getZ() - startTargetZ;
				if ((dx * dx + dz * dz) > 9.0) {
					allClose = false;
					break;
				}

				if (Math.abs(p.getY() - startTargetY) > 6.0) {
					allClose = false;
					break;
				}
			}

			if (allClose) {
				startWaitingForTeleports = false;
				startWaitTicksLeft = -1;
				mainFunc();
			} else if (startWaitTicksLeft <= 0) {
				// A failed command teleport must never silently start the match in
				// the wrong dimension. Retry with the direct server API and keep
				// waiting until every online player reaches the starter.
				ServerLevel targetLevel = srv.getLevel(startTargetWorld);
				if (targetLevel == null) {
					LOGGER.error("Could not find match start dimension {}", startTargetWorld);
					startWaitingForTeleports = false;
					startWaitTicksLeft = -1;
					startTargetWorld = null;
					return;
				}
				for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
					if (p == null) continue;
					p.teleportTo(targetLevel, startTargetX, startTargetY, startTargetZ,
							java.util.Set.of(), p.getYRot(), p.getXRot(), true);
				}
				startWaitTicksLeft = 60;
			} else {
				startWaitTicksLeft--;
			}
			return;
		}

		if (StopCommand.isRunning) {
			pushPlayersAwayFromWorldBorder(srv);
		}

			if (pendingSurvivalReapplyTicks >= 0) {
			if (pendingSurvivalReapplyTicks == 0) {
				try {
					applyGamemodeToAll(net.minecraft.world.level.GameType.SURVIVAL);
				} catch (Exception e) {
					LOGGER.error("Failed to re-apply survival gamemode for all players", e);
				}
				pendingSurvivalReapplyTicks = -1;
			} else {
				pendingSurvivalReapplyTicks--;
			}
		}

		if (deferredDeathStateResetTicks >= 0) {
			if (deferredDeathStateResetTicks == 0) {
				resetGameState();
			} else {
				deferredDeathStateResetTicks--;
			}
		}

		if (!forceSpectatorTicks.isEmpty()) {
			for (var e : new java.util.ArrayList<>(forceSpectatorTicks.entrySet())) {
				UUID uuid = e.getKey();
				Integer left = e.getValue();
				if (left == null) {
					forceSpectatorTicks.remove(uuid);
					continue;
				}

				ServerPlayer p = srv.getPlayerList().getPlayer(uuid);
				if (p == null) {
					forceSpectatorTicks.remove(uuid);
					continue;
				}

				try {
					if (!p.isSpectator()) {
						p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
					}
				} catch (Exception ex) {
					LOGGER.error("Failed to enforce spectator for {}", p.getName().getString(), ex);
				}

				int next = left - 1;
				if (next <= 0) forceSpectatorTicks.remove(uuid);
				else forceSpectatorTicks.put(uuid, next);
			}
		}

		if (!lateJoinSpectators.isEmpty() && StopCommand.isRunning) {
			for (UUID uuid : new java.util.ArrayList<>(lateJoinSpectators)) {
				ServerPlayer p = srv.getPlayerList().getPlayer(uuid);
				if (p == null) {
					lateJoinSpectators.remove(uuid);
					continue;
				}
				try {
					if (!p.isSpectator()) {
						p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
					}
				} catch (Exception ex) {
					LOGGER.error("Failed to keep late-join player in spectator for {}", p.getName().getString(), ex);
				}
			}
		}

		if (!StopCommand.isRunning) return;

		if (!srv.isDedicatedServer() && srv.isPaused()) return;

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

	private static void pushPlayersAwayFromWorldBorder(MinecraftServer srv) {
		for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
			if (player.isSpectator() || player.isCreative()) continue;
			var border = player.level().getWorldBorder();
			double halfSize = border.getSize() * 0.5;
			double centerX = border.getCenterX();
			double centerZ = border.getCenterZ();
			double left = player.getX() - (centerX - halfSize);
			double right = (centerX + halfSize) - player.getX();
			double north = player.getZ() - (centerZ - halfSize);
			double south = (centerZ + halfSize) - player.getZ();

			double pushX = 0.0;
			double pushZ = 0.0;
			if (left < 0.0) pushX += 1.0;
			if (right < 0.0) pushX -= 1.0;
			if (north < 0.0) pushZ += 1.0;
			if (south < 0.0) pushZ -= 1.0;
			if (pushX == 0.0 && pushZ == 0.0) continue;

			double length = Math.sqrt(pushX * pushX + pushZ * pushZ);
			pushX /= length;
			pushZ /= length;
			double nearestEdge = Math.min(Math.min(left, right), Math.min(north, south));
			double strength = Math.min(1.1, 0.55 + Math.max(0.0, -nearestEdge) * 0.30);
			var velocity = player.getDeltaMovement();
			double velocityX = pushX > 0 ? Math.max(velocity.x, strength) : pushX < 0 ? Math.min(velocity.x, -strength) : velocity.x;
			double velocityZ = pushZ > 0 ? Math.max(velocity.z, strength) : pushZ < 0 ? Math.min(velocity.z, -strength) : velocity.z;
			player.setDeltaMovement(velocityX, velocity.y, velocityZ);
		}
	}

	public static void mainFunc() {
		if (server == null) return;

		lateJoinSpectators.clear();

		if (Gamerz.gamersList.isEmpty()) {
			server.getPlayerList().broadcastSystemMessage(Component.literal("Need at least 1 player to start."), false);
			StopCommand.isRunning = false;
			return;
		}

		if (Gamerz.gamersList.size() == 1) {
			GoCommand.testMode = true;
		}

		isStarting = true;

		// Start each match with fresh participant and disconnect bookkeeping.
		// Keep queued respawn records until their respawn callback consumes them.
		participantsWhoDied.clear();
		originalRespawns.clear();
		DisconnectedPlayers.disconnected.clear();

		participantsWithoutRemovals.clear();
		for (Gamerz g : Gamerz.gamersList) participantsWithoutRemovals.add(g.uuid);
		deathHandlingActive = true;
		winnerEvaluationQueued = false;
		deferredDeathStateResetTicks = -1;

		Items.itemsAndNumbers = configManager.items;

		try {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				try {
					p.setExperienceLevels(0);
				} catch (RuntimeException ex) {
					LOGGER.error("Failed to reset experience level for player {}", p.getName().getString(), ex);
				}

				try {
					p.experienceProgress = 0.0f;
				} catch (RuntimeException ex) {
					LOGGER.error("Failed to reset experience progress for player {}", p.getName().getString(), ex);
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
					} else {
						LOGGER.warn("Could not find a total experience field for player {}; total points were not reset",
								p.getName().getString());
					}
				} catch (ReflectiveOperationException | RuntimeException ex) {
					LOGGER.error("Failed to reset total experience points for player {}", p.getName().getString(), ex);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to apply xp changes", e);
		}

		GoCommand.centerWorldBorderAndSetSpawnpoint(startTargetWorld, startTargetX, startTargetY, startTargetZ);
		closeHandledScreensAll();
		executeStartupCommands(GoCommand.resumeMode ? configManager.resumeCommands : configManager.regularCommands);
		Gamerz.maxHealthAndMaxHunger();
		Items.dayChanceAndRainChance();

		try {
			applyGamemodeToAll(net.minecraft.world.level.GameType.SURVIVAL);
			pendingSurvivalReapplyTicks = 2;
		} catch (Exception e) {
			LOGGER.error("Failed to set survival gamemode for all players", e);
		}

		try {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (p == null) continue;
				p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 40, 4, false, false, true));
			}
		} catch (Exception e) {
			LOGGER.error("Failed to apply start resistance", e);
		}

		StopCommand.isRunning = true;

		try {
			ClientboundSetTitlesAnimationPacket timings = new ClientboundSetTitlesAnimationPacket(10, 70, 20);
			ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(Component.literal("Random Items").withStyle(ChatFormatting.BLUE));
			ClientboundSetSubtitleTextPacket clearSubtitle = new ClientboundSetSubtitleTextPacket(Component.empty());

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				player.connection.send(timings);
				player.connection.send(clearSubtitle);
				player.connection.send(titlePacket);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to broadcast start title", e);
		}


		lastDelayTicks = Math.max(1, (int) Math.round(delay * 20.0));

		giveAllOnce();
		server.execute(TrackerCompassIntegration::giveToMatchParticipants);
		ticksUntilNextGive = lastDelayTicks;

		isStarting = false;
	}

	private static String decorateDirectEnchantingBook(String itemArg) {
		if (!DirectEnchantingHooks.enabled || itemArg == null || !itemArg.startsWith("enchanted_book[")) return itemArg;
		if (itemArg.contains("minecraft:lore=")) return itemArg;
		String lore = "minecraft:lore=[{text:\"Right-click to apply\",color:\"aqua\",italic:false}],";
		return itemArg.replace("enchanted_book[", "enchanted_book[" + lore);
	}

	private static boolean skipAutoIgniteFlintAndSteel(List<String> bundleItems, String itemArg) {
		if (!GoCommand.enableTntAutoExplode || bundleItems == null || itemArg == null) return false;
		boolean hasTnt = bundleItems.stream().filter(java.util.Objects::nonNull)
				.anyMatch(raw -> configuredItemId(raw).equals("tnt"));
		return hasTnt && configuredItemId(itemArg).equals("flint_and_steel");
	}

	private static String configuredItemId(String raw) {
		String value = raw.trim();
		int space = value.lastIndexOf(' ');
		if (space > 0) value = value.substring(0, space).trim();
		int component = value.indexOf('[');
		if (component >= 0) value = value.substring(0, component);
		int namespace = value.indexOf(':');
		if (namespace >= 0) value = value.substring(namespace + 1);
		return value;
	}

	private record BundleDelivery(boolean complete, boolean deliveredAny) {}

	private static void giveAllOnce() {
		if (server == null) return;

		server.execute(() -> {
			for (Gamerz gamer : new ArrayList<>(Gamerz.gamersList)) {
				if (!StopCommand.isRunning) return;

				ServerPlayer player = getPlayerByUuid(gamer.uuid);
				if (player == null || !player.isAlive() || player.isSpectator()) continue;

				WeightedItem selected = gamer.getItem();
				if (selected == null || selected.items().isEmpty()) continue;

				BundleDelivery delivery = giveBundle(player, selected.items());
				if (delivery.complete() || delivery.deliveredAny()) {
					gamer.removeItem(selected);
					continue;
				}

				// Try one different configured bundle only when the selected bundle
				// failed before granting anything. Never stack a full fallback on top
				// of a partially delivered bundle.
				gamer.removeItem(selected);
				WeightedItem fallback = gamer.getItems().stream()
						.filter(item -> !item.equals(selected))
						.findFirst().orElse(null);
				if (fallback != null) {
					BundleDelivery fallbackDelivery = giveBundle(player, fallback.items());
					if (fallbackDelivery.complete() || fallbackDelivery.deliveredAny()) {
						gamer.removeItem(fallback);
					}
				}
			}
		});
	}

	private static BundleDelivery giveBundle(ServerPlayer player, List<String> bundleItems) {
		boolean directBookBundle = DirectEnchantingHooks.enabled && bundleItems.stream()
				.anyMatch(arg -> arg != null && arg.trim().startsWith("enchanted_book["));
		boolean complete = true;
		boolean deliveredAny = false;

		for (String itemArg : bundleItems) {
			if (itemArg == null) continue;
			String item = itemArg.trim();
			if (item.isEmpty() || skipAutoIgniteFlintAndSteel(bundleItems, item)) continue;
			if (directBookBundle && isEnchantingSupply(item)) continue;

			item = decorateDirectEnchantingBook(item);
			boolean given = executeCommandNow("give " + player.getName().getString() + " " + item);
			complete &= given;
			deliveredAny |= given;

			List<String> extras = Items.randomItems.get(item);
			if (extras == null || extras.isEmpty()) continue;

			String extra = extras.get(ThreadLocalRandom.current().nextInt(extras.size()));
			if (extra == null || extra.isBlank()) continue;
			String extraArgument = hasExplicitCount(extra) ? extra.trim() : extra.trim() + " 1";
			boolean extraGiven = executeCommandNow("give " + player.getName().getString() + " " + extraArgument);
			complete &= extraGiven;
			deliveredAny |= extraGiven;
		}

		return new BundleDelivery(complete, deliveredAny);
	}

	private static boolean isEnchantingSupply(String item) {
		return item.matches("(?:minecraft:)?anvil\\s+\\d+")
				|| item.matches("(?:minecraft:)?experience_bottle\\s+\\d+");
	}

	private static boolean hasExplicitCount(String item) {
		int lastSpace = item.trim().lastIndexOf(' ');
		if (lastSpace <= 0 || lastSpace == item.trim().length() - 1) return false;
		try {
			Integer.parseInt(item.trim().substring(lastSpace + 1));
			return true;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

	public static boolean executeCommandNow(String rawCommand) {
		try {
			if (server == null) return false;

			String command = rawCommand == null ? "" : rawCommand.trim();
			if (command.isEmpty()) return false;
			if (command.startsWith("/")) command = command.substring(1);

			CommandSourceStack src = server.createCommandSourceStack().withSource(NO_FEEDBACK_OUTPUT);
			var dispatcher = server.getCommands().getDispatcher();
			var parseResults = dispatcher.parse(command, src);
			int result = dispatcher.execute(parseResults);

			return result > 0;
		} catch (CommandSyntaxException e) {
			if (isNoChangeCommandFailure(e)) return true;
			LOGGER.error("Command failed: {}", rawCommand, e);
			return false;
		} catch (Exception e) {
			LOGGER.error("Command failed: {}", rawCommand, e);
			return false;
		}
	}

	private static boolean isNoChangeCommandFailure(CommandSyntaxException exception) {
		String message = (exception.getRawMessage().getString() + " " + exception.getMessage())
				.toLowerCase(Locale.ROOT);
		return message.contains("already") || message.contains("no change")
				|| message.contains("nochange") || message.contains("failed.nochange");
	}

	private static void teleportLateJoinerToArena(MinecraftServer srv, ServerPlayer player) {
		try {
			if (srv == null || player == null) return;
			ResourceKey<Level> arenaKey = startTargetWorld;
			if (arenaKey == null) return;

			ServerLevel arena = srv.getLevel(arenaKey);
			if (arena == null) return;

			double x = arena.getWorldBorder().getCenterX() + 0.5;
			double y = startTargetY;
			double z = arena.getWorldBorder().getCenterZ() + 0.5;

			String name = player.getName().getString();
			executeCommandNow("execute in " + dimensionId(arenaKey) + " run tp " + name + " " + x + " " + y + " " + z);
		} catch (Exception e) {
			LOGGER.error("Failed to teleport late-join player to arena: {}", player.getName().getString(), e);
		}
	}

	private static String dimensionId(ResourceKey<Level> key) {
		if (key == Level.OVERWORLD) return "minecraft:overworld";
		if (key == Level.NETHER) return "minecraft:the_nether";
		if (key == Level.END) return "minecraft:the_end";
		return key.identifier().toString();
	}

	private static void applyGamemodeToAll(net.minecraft.world.level.GameType mode) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.setGameMode(mode);
		}
	}

	private static void clearEffectsAllIfAny() {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Collection<MobEffectInstance> effects = p.getActiveEffects();
			if (effects == null || effects.isEmpty()) continue;
			p.removeAllEffects();
		}
	}

	private static void clearInventoryAllIfAny() {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p == null) continue;
			if (p.getInventory() == null) continue;
			if (p.getInventory().isEmpty()) continue;
			p.getInventory().clearContent();
		}
	}

	private static void closeHandledScreensAll() {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p == null) continue;
			try {
				p.closeContainer();
			} catch (Exception e) {
				LOGGER.error("Failed to close handled screen for {}", p.getName().getString(), e);
			}
		}
	}

	public static void executeStartupCommands(List<String> commands) {
		if (server == null) return;
		if (configManager != null && configManager.clearMobsOnStart) {
			RicMobCleanup.beginCleanup(server);
		}
		if (commands == null || commands.isEmpty()) return;

		for (String cmd : commands) {
			if (cmd == null) continue;
			String out = GoCommand.keepEndArrivalInsideBorderCommand(cmd.trim());
			if (out.isEmpty()) continue;

			if (isStarting && out.startsWith("gamemode ")) continue;
			if (out.startsWith("execute as @a at @s run kill @e[") || out.startsWith("kill @e[")) continue;
			if (executeNativeStartupCommand(out)) continue;

			switch (out) {
				case "effect clear @a" -> clearEffectsAllIfAny();
				case "clear @a" -> clearInventoryAllIfAny();
				default -> executeCommandNow(out);
			}
		}
	}

	/** Applies RIC's simple startup settings directly, avoiding Brigadier's
	 * failure feedback when an idempotent setting already has its requested value. */
	private static boolean executeNativeStartupCommand(String rawCommand) {
		if (server == null || rawCommand == null) return false;
		String command = rawCommand.trim();
		if (command.startsWith("/")) command = command.substring(1).trim();

		String dimension = "minecraft:overworld";
		if (command.startsWith("execute in ")) {
			int runIndex = command.indexOf(" run ");
			if (runIndex < 0) return false;
			dimension = command.substring("execute in ".length(), runIndex).trim();
			command = command.substring(runIndex + " run ".length()).trim();
		}

		String[] parts = command.split("\\s+");
		if (parts.length == 3 && parts[0].equals("gamerule")) {
			ServerLevel level = startupLevel(dimension);
			GameRule<?> rule = startupGameRule(parts[1]);
			if (level == null || rule == null) return false;
			return setStartupGameRule(level, rule, parts[2]);
		}

		if (parts.length >= 3 && parts[0].equals("worldborder") && parts[1].equals("set")) {
			ServerLevel level = startupLevel(dimension);
			if (level == null || parts.length > 4) return false;
			try {
				double size = Double.parseDouble(parts[2]);
				long duration = parts.length == 4 ? Long.parseLong(parts[3]) : 0L;
				if (!Double.isFinite(size) || size < 1.0 || size > WorldBorder.MAX_SIZE || duration < 0) return false;
				WorldBorder border = level.getWorldBorder();
				if (duration == 0L) {
					if (Math.abs(border.getSize() - size) > 1.0E-7) border.setSize(size);
				} else if (Math.abs(border.getLerpTarget() - size) > 1.0E-7
						|| border.getLerpTime() == 0L) {
					border.lerpSizeBetween(border.getSize(), size, duration, level.getGameTime());
				}
				return true;
			} catch (NumberFormatException ignored) {
				return false;
			}
		}

		if (parts.length >= 7 && parts.length <= 8 && parts[0].equals("item")
				&& parts[1].equals("replace") && parts[2].equals("entity") && parts[3].equals("@a")
				&& parts[4].startsWith("hotbar.") && parts[5].equals("with")) {
			try {
				int slot = Integer.parseInt(parts[4].substring("hotbar.".length()));
				int count = parts.length == 8 ? Integer.parseInt(parts[7]) : 1;
				if (slot < 0 || slot > 8 || count < 1) return false;
				Identifier itemId = Identifier.parse(parts[6].contains(":") ? parts[6] : "minecraft:" + parts[6]);
				var item = BuiltInRegistries.ITEM.getValue(itemId);
				if (item == null || item == net.minecraft.world.item.Items.AIR) return false;
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					player.getInventory().setItem(slot, new ItemStack(item, count));
				}
				return true;
			} catch (IllegalArgumentException ignored) {
				return false;
			}
		}
		return false;
	}

	private static ServerLevel startupLevel(String dimension) {
		return switch (dimension) {
			case "minecraft:overworld" -> server.getLevel(Level.OVERWORLD);
			case "minecraft:the_nether" -> server.getLevel(Level.NETHER);
			case "minecraft:the_end" -> server.getLevel(Level.END);
			default -> null;
		};
	}

	private static GameRule<?> startupGameRule(String id) {
		return switch (id) {
			case "mob_drops" -> GameRules.MOB_DROPS;
			case "block_drops" -> GameRules.BLOCK_DROPS;
			case "keep_inventory" -> GameRules.KEEP_INVENTORY;
			case "spectators_generate_chunks" -> GameRules.SPECTATORS_GENERATE_CHUNKS;
			case "immediate_respawn" -> GameRules.IMMEDIATE_RESPAWN;
			case "respawn_radius" -> GameRules.RESPAWN_RADIUS;
			default -> null;
		};
	}

	@SuppressWarnings("unchecked")
	private static boolean setStartupGameRule(ServerLevel level, GameRule<?> rawRule, String rawValue) {
		GameRules rules = level.getGameRules();
		if (rawRule.valueClass() == Boolean.class) {
			if (!rawValue.equalsIgnoreCase("true") && !rawValue.equalsIgnoreCase("false")) return false;
			GameRule<Boolean> rule = (GameRule<Boolean>) rawRule;
			Boolean value = Boolean.parseBoolean(rawValue);
			if (!Objects.equals(rules.get(rule), value)) rules.set(rule, value, server);
			return true;
		}
		if (rawRule.valueClass() == Integer.class) {
			try {
				GameRule<Integer> rule = (GameRule<Integer>) rawRule;
				Integer value = Integer.parseInt(rawValue);
				if (!Objects.equals(rules.get(rule), value)) rules.set(rule, value, server);
				return true;
			} catch (NumberFormatException ignored) {
				return false;
			}
		}
		return false;
	}

	public static void resetGameState() {
		StopCommand.isRunning = false;
		ticksUntilNextGive = 0;
		lastDelayTicks = 0;
		isStarting = false;
		pendingSurvivalReapplyTicks = -1;
		startWaitingForTeleports = false;
		startWaitTicksLeft = -1;
		startTargetWorld = null;
		participantsWithoutRemovals.clear();
		participantsWhoDied.clear();
		originalRespawns.clear();
		forceSpectatorTicks.clear();
		lateJoinSpectators.clear();
		deathHandlingActive = false;
		winnerEvaluationQueued = false;
		deferredDeathStateResetTicks = -1;
	}

	public static boolean shouldHandleMatchDeath(UUID uuid) {
		return deathHandlingActive
				&& uuid != null
				&& participantsWithoutRemovals.contains(uuid)
				&& !participantsWhoDied.contains(uuid);
	}

	public static void queueWinnerEvaluation() {
		if (server == null || winnerEvaluationQueued) return;
		winnerEvaluationQueued = true;
		server.execute(() -> {
			winnerEvaluationQueued = false;
			if (!deathHandlingActive) return;

			boolean oneWinner = GoCommand.teamMode
					? TeamsCommand.teams != null && TeamsCommand.teams.size() == 1
					: Gamerz.gamersList.size() == 1;
			boolean noWinners = GoCommand.teamMode
					? TeamsCommand.teams == null || TeamsCommand.teams.isEmpty()
					: Gamerz.gamersList.isEmpty();

			if (oneWinner && StopCommand.isRunning) {
				Winner.Declare();
			} else if (noWinners) {
				finishMatchPreservingDeathHandling();
			}
		});
	}

	public static void finishMatchPreservingDeathHandling() {
		StopCommand.isRunning = false;
		GoCommand.testMode = false;
		boolean allParticipantsDied = participantsWithoutRemovals.isEmpty()
				|| participantsWhoDied.containsAll(participantsWithoutRemovals);
		if (allParticipantsDied) GoCommand.teamMode = false;

		if (server != null) {
			for (ServerLevel level : server.getAllLevels()) {
				level.getWorldBorder().setSize(4.9999999E7);
			}
			clearRicTeams(server.getScoreboard());
		}

		if (allParticipantsDied) {
			deferredDeathStateResetTicks = Math.max(deferredDeathStateResetTicks, 5);
		}
	}

	public static void clearRicTeams(Scoreboard scoreboard) {
		if (scoreboard == null) return;

		for (net.minecraft.world.scores.PlayerTeam team : new ArrayList<>(scoreboard.getPlayerTeams())) {
			String n = team.getName();
			if (n != null && n.startsWith(RIC_TEAM_PREFIX)) scoreboard.removePlayerTeam(team);
		}
	}

	public static ServerPlayer getPlayerByUuid(java.util.UUID uuid) {
		if (server == null) return null;
		return server.getPlayerList().getPlayer(uuid);
	}
}
