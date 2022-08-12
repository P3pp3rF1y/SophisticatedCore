package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
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
}
