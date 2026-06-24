package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class CapabilityHelper {

	public static void runOnItemHandler(Entity entity, Consumer<ResourceHandler<ItemResource>> run) {
		runOnCapability(entity, Capabilities.Item.ENTITY, null, run);
	}

	public static <T> T getFromItemHandler(Entity entity, Function<ResourceHandler<ItemResource>, T> get, T defaultValue) {
		return getFromCapability(entity, Capabilities.Item.ENTITY, null, get, defaultValue);
	}

	public static <T> T getFromItemHandler(Level level, BlockPos pos, @Nullable Direction context, Function<ResourceHandler<ItemResource>, T> get,
			T defaultValue) {
		return getFromCapability(level, pos, Capabilities.Item.BLOCK, context, get, defaultValue);
	}

	public static <T> T getFromItemHandler(Level level, BlockPos pos, Function<ResourceHandler<ItemResource>, T> get, T defaultValue) {
		return getFromItemHandler(level, pos, null, get, defaultValue);
	}

	public static <T, C> void runOnCapability(Entity entity, EntityCapability<T, C> capability, @Nullable C context, Consumer<T> run) {
		runOnCapability(run, entity.getCapability(capability, context));
	}

	public static <T> void runOnCapability(ItemAccess itemAccess, ItemCapability<T, ItemAccess> capability, Consumer<T> run) {
		runOnCapability(run, itemAccess.getCapability(capability));
	}

	private static <T> void runOnCapability(Consumer<T> run, @Nullable T t) {
		if (t != null) {
			run.accept(t);
		}
	}

	public static <T, U> U getFromCapability(ItemAccess itemAccess, ItemCapability<T, ItemAccess> capability, Function<T, U> get, U defaultValue) {
		T t = itemAccess.getCapability(capability);
		if (t == null) {
			return defaultValue;
		}
		return get.apply(t);
	}

	public static <T, C, U> U getFromCapability(Level level, BlockPos pos, BlockCapability<T, C> capability, @Nullable C context, Function<T, U> get,
			U defaultValue) {
		return getFromCapability(level, pos, null, null, capability, context, get, defaultValue);
	}

	public static <T, C, U> U getFromCapability(BlockEntity blockEntity, BlockCapability<T, C> capability, @Nullable C context, Function<T, U> get,
			U defaultValue) {
		if (blockEntity.getLevel() == null) {
			return defaultValue;
		}

		return getFromCapability(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, capability, context, get,
				defaultValue);
	}

	public static <T, C, U> U getFromCapability(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity,
			BlockCapability<T, C> capability, @Nullable C context, Function<T, U> get, U defaultValue) {
		T t = level.getCapability(capability, pos, state, blockEntity, context);
		if (t == null) {
			return defaultValue;
		}
		return get.apply(t);
	}

	public static <T, C, U> U getFromCapability(Entity entity, EntityCapability<T, C> capability, @Nullable C context, Function<T, U> get, U defaultValue) {
		T t = entity.getCapability(capability, context);
		if (t == null) {
			return defaultValue;
		}
		return get.apply(t);
	}

	public static <T> T getFromFluidHandler(BlockEntity be, Direction side, Function<ResourceHandler<FluidResource>, T> get, T defaultValue) {
		return getFromCapability(be, Capabilities.Fluid.BLOCK, side, get, defaultValue);
	}

	public static <T> T getFromFluidHandler(ItemAccess itemAccess, Function<ResourceHandler<FluidResource>, T> get, T defaultValue) {
		return getFromCapability(itemAccess, Capabilities.Fluid.ITEM, get, defaultValue);
	}
}
