package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.Optional;
import java.util.function.Consumer;

public interface IControllerBoundable {
	String CONTROLLER_POS_TAG = "controllerPos";

	void setControllerPos(BlockPos controllerPos);

	Optional<BlockPos> getControllerPos();

	void removeControllerPos();

	BlockPos getStorageBlockPos();

	Level getStorageBlockLevel();

	default boolean canBeConnected() {
		return getControllerPos().isEmpty();
	}

	void registerController(ControllerBlockEntityBase controllerBlockEntity);
	void unregisterController();

	boolean canConnectStorages();

	default void runOnController(Level level, Consumer<ControllerBlockEntityBase> toRun) {
		getControllerPos().flatMap(pos -> WorldHelper.getLoadedBlockEntity(level, pos, ControllerBlockEntityBase.class)).ifPresent(toRun);
	}

	default void saveControllerPos(CompoundTag tag) {
		getControllerPos().ifPresent(p -> tag.putLong(IControllerBoundable.CONTROLLER_POS_TAG, p.asLong()));
	}

	default void loadControllerPos(CompoundTag tag) {
		NBTHelper.getLong(tag, IControllerBoundable.CONTROLLER_POS_TAG).ifPresent(value -> {
			BlockPos controllerPos = BlockPos.of(value);
			setControllerPos(controllerPos);
		});
	}

	default void addToController(Level level, BlockPos pos, BlockPos controllerPos) {
		//noop by default
	}

	default void addToAdjacentController() {
		Level level = getStorageBlockLevel();
		if (!level.isClientSide()) {
			BlockPos pos = getStorageBlockPos();
			for (Direction dir : Direction.values()) {
				BlockPos offsetPos = pos.offset(dir.getNormal());
				WorldHelper.getBlockEntity(level, offsetPos, IControllerBoundable.class).ifPresentOrElse(
						s -> {
							if (s.canConnectStorages()) {
								s.getControllerPos().ifPresent(controllerPos -> addToController(level, pos, controllerPos));
							}
						},
						() -> addToController(level, pos, offsetPos)
				);
				if (getControllerPos().isPresent()) {
					break;
				}
			}
		}
	}
}
