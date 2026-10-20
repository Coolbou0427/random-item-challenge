package jaxon.ric;

import jaxon.ric.command.GoCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/** Server-only operations and the position-scoped context used while mining. */
public final class PlacedBlockTracking {
	private record MiningContext(Level level, Set<Long> dropPositions, boolean eligible, MiningContext previous) {
	}

	private record EmissionContext(Level level, long pos, EmissionContext previous) {
	}

	private record SupportBreakContext(LevelAccessor level, long pos, SupportBreakContext previous) {
	}

	private static final ThreadLocal<MiningContext> MINING = new ThreadLocal<>();
	private static final ThreadLocal<EmissionContext> EMISSION = new ThreadLocal<>();
	private static final ThreadLocal<SupportBreakContext> SUPPORT_BREAK = new ThreadLocal<>();

	private PlacedBlockTracking() {
	}

	public static PlacedBlockSavedData data(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(PlacedBlockSavedData.TYPE);
	}

	public static void recordPlacement(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir()) return;
		data(level).put(pos.asLong(), BuiltInRegistries.BLOCK.getKey(state.getBlock()));
	}

	public static boolean isTracked(ServerLevel level, BlockPos pos, BlockState state) {
		var saved = data(level);
		var trackedBlock = saved.get(pos.asLong());
		if (trackedBlock == null) return false;
		if (!trackedBlock.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
			saved.remove(pos.asLong());
			return false;
		}
		return true;
	}

	public static void clearIfReplaced(Level level, BlockPos pos, BlockState newState) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		var saved = data(serverLevel);
		var trackedBlock = saved.get(pos.asLong());
		if (trackedBlock != null && !trackedBlock.equals(BuiltInRegistries.BLOCK.getKey(newState.getBlock()))) {
			saved.remove(pos.asLong());
		}
	}

	public static void beginMining(Level level, BlockPos pos, boolean eligible) {
		Set<Long> positions = new HashSet<>();
		positions.add(pos.asLong());
		if (eligible) {
			BlockState state = level.getBlockState(pos);
			BlockPos other = null;
			if (state.getBlock() instanceof DoorBlock && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
				other = pos.relative(state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF).getDirectionToOther());
			} else if (state.getBlock() instanceof BedBlock
					&& state.hasProperty(BlockStateProperties.BED_PART)
					&& state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
				var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
				other = pos.relative(state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT
						? facing : facing.getOpposite());
			}
			if (other != null && level.getBlockState(other).getBlock() == state.getBlock()
					&& level instanceof ServerLevel serverLevel
					&& isTracked(serverLevel, other, level.getBlockState(other))) {
				positions.add(other.asLong());
			}
		}
		MINING.set(new MiningContext(level, Set.copyOf(positions), eligible, MINING.get()));
	}

	public static void endMining() {
		MiningContext current = MINING.get();
		if (current == null || current.previous() == null) MINING.remove();
		else MINING.set(current.previous());
	}

	public static void beginEmission(Level level, BlockPos pos) {
		EMISSION.set(new EmissionContext(level, pos.asLong(), EMISSION.get()));
	}

	public static void endEmission() {
		EmissionContext current = EMISSION.get();
		if (current == null || current.previous() == null) EMISSION.remove();
		else EMISSION.set(current.previous());
	}

	public static void beginSupportBreak(LevelAccessor level, BlockPos pos) {
		SUPPORT_BREAK.set(new SupportBreakContext(level, pos.asLong(), SUPPORT_BREAK.get()));
	}

	public static void endSupportBreak() {
		SupportBreakContext current = SUPPORT_BREAK.get();
		if (current == null || current.previous() == null) SUPPORT_BREAK.remove();
		else SUPPORT_BREAK.set(current.previous());
	}

	public static boolean shouldAllowBlockDrop() {
		if (!GoCommand.enablePlayerPlacedBlockDrops) return false;
		MiningContext mining = MINING.get();
		EmissionContext emission = EMISSION.get();
		if (emission == null) return false;
		boolean playerMining = mining != null && mining.eligible()
				&& emission.level() == mining.level()
				&& mining.dropPositions().contains(emission.pos());
		SupportBreakContext supportBreak = SUPPORT_BREAK.get();
		boolean supportLost = supportBreak != null
				&& emission.level() == supportBreak.level()
				&& emission.pos() == supportBreak.pos();
		return playerMining || supportLost;
	}
}
