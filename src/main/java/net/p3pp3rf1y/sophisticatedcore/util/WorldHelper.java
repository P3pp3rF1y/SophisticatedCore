package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

public class WorldHelper {
	private WorldHelper() {
	}

	private static final List<BiPredicate<Player, BlockPos>> ADDITIONAL_INTERACTION_CHECKS = new ArrayList<>();

	public static void addAdditionalInteractionCheck(BiPredicate<Player, BlockPos> check) {
		ADDITIONAL_INTERACTION_CHECKS.add(check);
	}

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

	public static int getFuelBurnTime(ItemStack fuel) {
		CookingFuel cookingFuel = fuel.get(DataComponents.COOKING_FUEL);
		if (cookingFuel == null) {
			return 0;
		}
		if (ServerLifecycleHooks.getCurrentServer() == null) {
			// Client-side filter previews cannot resolve server-owned context providers.
			return cookingFuel.burnTime() instanceof ResolvableInt.Constant constant ? constant.value() : 1;
		}
		ServerLevel level = ServerLifecycleHooks.getCurrentServer().overworld();
		LootContext context = new LootContext.Builder(
				new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST)).create(Optional.empty());
		return cookingFuel.burnTime().get(context, 0);
	}

	public static List<BlockEntity> getBlockEntitiesInRange(Level level, BlockPos origin, int range) {
		return getBlockEntitiesInRange(level, origin, range, BlockEntity.class);
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

		long maxDist = (long) range * (long) range;

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
					if (y < minY || y > maxY)
						continue;

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

	public static boolean playerMayInteract(Player player, BlockPos pos) {
		return !(player.level() instanceof ServerLevel serverLevel)
				|| (player.mayInteract(serverLevel, pos) && ADDITIONAL_INTERACTION_CHECKS.stream().allMatch(check -> check.test(player, pos)));
	}
}
