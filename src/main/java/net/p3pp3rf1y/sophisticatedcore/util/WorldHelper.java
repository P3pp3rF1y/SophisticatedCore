package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
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

	public static FuelValues getFuelValues() {
		if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER && FMLEnvironment.dist.isClient()) {
			return ClientLevelHelper.getFuelValues();
		}
		MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
		if (currentServer == null) {
			throw new IllegalArgumentException("Cannot get fuel values without a server instance.");
		}
		return currentServer.fuelValues();
	}
}
