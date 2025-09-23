package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorldHelper {
	private WorldHelper() {}

	public static Optional<BlockEntity> getBlockEntity(@Nullable BlockGetter level, BlockPos pos) {
		return getBlockEntity(level, pos, BlockEntity.class);
	}

	public static <T> Optional<T> getLoadedBlockEntity(@Nullable Level level, BlockPos pos, Class<T> teClass) {
		if (level != null && level.isLoaded(pos)) {
			return getBlockEntity(level, pos, teClass);
		}
		return Optional.empty();
	}

	public static <T> Optional<T> getBlockEntity(@Nullable BlockGetter level, BlockPos pos, Class<T> teClass) {
		if (level == null) {
			return Optional.empty();
		}

		BlockEntity be = level.getBlockEntity(pos);

		if (teClass.isInstance(be)) {
			return Optional.of(teClass.cast(be));
		}

		return Optional.empty();
	}

	public static void notifyBlockUpdate(BlockEntity tile) {
		Level level = tile.getLevel();
		if (level == null) {
			return;
		}
		level.sendBlockUpdated(tile.getBlockPos(), tile.getBlockState(), tile.getBlockState(), 3);
	}

	public static <T> List<T> getBlockEntitiesInRange(Level level, BlockPos origin, int range, Class<T> beClass) {
		List<T> out = new ArrayList<>();

		int minX = origin.getX() - range;
		int maxX = origin.getX() + range;
		int minZ = origin.getZ() - range;
		int maxZ = origin.getZ() + range;

		DimensionType dim = level.dimensionType();
		int minY = Math.max(origin.getY() - range, dim.minY());
		int maxY = Math.min(origin.getY() + range, dim.minY() + dim.height() - 1);

		int minCx = minX >> 4;
		int maxCx = maxX >> 4;
		int minCz = minZ >> 4;
		int maxCz = maxZ >> 4;

		long maxDist = (long) range * (long) range; // use squared distance

		for (int cz = minCz; cz <= maxCz; cz++) {
			for (int cx = minCx; cx <= maxCx; cx++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}

				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be == null || be.isRemoved() || !beClass.isInstance(be)) {
						continue;
					}
					BlockPos pos = be.getBlockPos();
					int y = pos.getY();
					if (y < minY || y > maxY) continue;

					long dx = (long) pos.getX() - origin.getX();
					long dz = (long) pos.getZ() - origin.getZ();
					long beDist = dx * dx + dz * dz;
					if (beDist <= maxDist) {
						out.add(beClass.cast(be));
					}
				}
			}
		}
		return out;
	}
}
