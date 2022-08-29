package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.Optional;
import java.util.Set;

public interface ILinkable extends IControllerBoundable {
	default void linkToController(BlockPos controllerPos) {
		if (!canBeLinked() || isLinked()) {
			return;
		}

		Level level = getStorageBlockLevel();

		if (!level.isClientSide() && WorldHelper.getLoadedBlockEntity(level, controllerPos, ControllerBlockEntityBase.class).isPresent()) {
			boolean hadControllerPos = getControllerPos().isPresent();
			if (!hadControllerPos) {
				setControllerPos(controllerPos);
			}
			runOnController(level, controller -> {
				if (!controller.addLinkedBlock(getStorageBlockPos()) && !hadControllerPos) {
					removeControllerPos();
				}
			});
		}
	}

	default void unlinkFromController() {
		Level level = getStorageBlockLevel();

		if (!level.isClientSide()) {
			runOnController(level, controller -> controller.removeLinkedBlock(getStorageBlockPos()));
			removeControllerPos();
			setNotLinked();
		}
	}
	default void setNotLinked() {
		//noop by default
	}
	Set<BlockPos> getConnectablePositions();
	boolean connectLinkedSelf();
	void setControllerPos(BlockPos controllerPos);
	Optional<BlockPos> getControllerPos();
	boolean isLinked();
	default boolean canBeLinked() {
		return true;
	}
}
