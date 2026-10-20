package jaxon.ric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** World-saved positions of blocks that were placed by a player. */
public final class PlacedBlockSavedData extends SavedData {
	private record SavedBlock(long pos, Identifier block) {
	}

	private static final Codec<SavedBlock> BLOCK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("pos").forGetter(SavedBlock::pos),
			Identifier.CODEC.fieldOf("block").forGetter(SavedBlock::block)
	).apply(instance, SavedBlock::new));

	private static final Codec<PlacedBlockSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BLOCK_CODEC.listOf()
					.fieldOf("placed_blocks")
					.forGetter(PlacedBlockSavedData::savedBlocks)
	).apply(instance, PlacedBlockSavedData::new));

	public static final SavedDataType<PlacedBlockSavedData> TYPE = new SavedDataType<>(
			Identifier.withDefaultNamespace("ric_placed_blocks"),
			PlacedBlockSavedData::new,
			CODEC,
			DataFixTypes.LEVEL
	);

	private final Long2ObjectOpenHashMap<Identifier> blocks = new Long2ObjectOpenHashMap<>();

	public PlacedBlockSavedData() {
	}

	private PlacedBlockSavedData(List<SavedBlock> loaded) {
		for (SavedBlock block : loaded) blocks.put(block.pos(), block.block());
	}

	public Identifier get(long packedPos) {
		return blocks.get(packedPos);
	}

	public boolean put(long packedPos, Identifier blockId) {
		Identifier previous = blocks.put(packedPos, blockId);
		if (blockId.equals(previous)) return false;
		setDirty();
		return true;
	}

	public boolean remove(long packedPos) {
		if (blocks.remove(packedPos) == null) return false;
		setDirty();
		return true;
	}

	public List<Long> positions() {
		List<Long> result = new ArrayList<>(blocks.size());
		for (long pos : blocks.keySet()) result.add(pos);
		return result;
	}

	private List<SavedBlock> savedBlocks() {
		List<SavedBlock> result = new ArrayList<>(blocks.size());
		for (var entry : blocks.long2ObjectEntrySet()) {
			result.add(new SavedBlock(entry.getLongKey(), entry.getValue()));
		}
		return result;
	}
}
