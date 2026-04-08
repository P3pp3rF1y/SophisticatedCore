package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockHighlightGroups {
	private static final Comparator<BlockPos> BLOCK_POS_COMPARATOR = Comparator.comparingLong(BlockPos::asLong);

	private BlockHighlightGroups() {
	}

	public static List<List<BlockPos>> getHighlightGroups(Level level, Collection<BlockPos> positions) {
		Map<BlockPos, List<BlockPos>> groups = new LinkedHashMap<>();
		positions.forEach(pos -> groups.putIfAbsent(getCanonicalHighlightPos(level, pos), getHighlightPositions(level, pos)));
		return new ArrayList<>(groups.values());
	}

	public static BlockPos getCanonicalHighlightPos(Level level, BlockPos pos) {
		return getHighlightPositions(level, pos).stream().min(BLOCK_POS_COMPARATOR).orElse(pos);
	}

	public static List<BlockPos> getHighlightPositions(Level level, BlockPos pos) {
		if (!level.isLoaded(pos) || level.isEmptyBlock(pos)) {
			return List.of(pos);
		}

		BlockState state = level.getBlockState(pos);
		Set<BlockPos> positions = new LinkedHashSet<>();
		positions.add(pos);

		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			Direction connectedDir = ChestBlock.getConnectedDirection(state);
			BlockPos otherPos = pos.relative(connectedDir);
			if (level.isLoaded(otherPos) && !level.isEmptyBlock(otherPos)) {
				positions.add(otherPos);
			}
		}

		return positions.stream().sorted(BLOCK_POS_COMPARATOR).toList();
	}
}
