package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

public interface IControllableStorage extends IControllerBoundable {

	IStorageWrapper getStorageWrapper();

	default boolean canBeConnected() {
		return getControllerPos().isEmpty();
	}

	@Override
	default boolean canConnectStorages() {
		return true;
	}

	default void tryToAddToController() {
		addToAdjacentController();
	}

	default void removeFromController() {
		Level level = getStorageBlockLevel();
		if (!level.isClientSide()) {
			getControllerPos().flatMap(p -> WorldHelper.getBlockEntity(level, p, ControllerBlockEntityBase.class)).ifPresent(c -> c.removeStorage(getStorageBlockPos()));
			removeControllerPos();
		}
	}

	private void addToAdjacentController() {
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

	private void addToController(Level level, BlockPos pos, BlockPos controllerPos) {
		WorldHelper.getBlockEntity(level, controllerPos, ControllerBlockEntityBase.class).ifPresent(c -> c.addStorage(pos));
	}

	default void registerController(ControllerBlockEntityBase controllerBlockEntity) {
		setControllerPos(controllerBlockEntity.getBlockPos());
		if (controllerBlockEntity.getLevel() != null && !controllerBlockEntity.getLevel().isClientSide()) {
			registerListeners();
		}
	}

	default void unregisterController() {
		removeControllerPos();
		getStorageWrapper().getInventoryForInputOutput().unregisterStackKeyListeners();
		getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).unregisterListeners();
		getStorageWrapper().getInventoryHandler().unregisterFilterItemsChangeListener();
	}

	private void registerListeners() {
		registerInventoryStackListeners();
		getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).registerListeners(
				i -> runOnController(getStorageBlockLevel(), controller -> controller.addStorageMemorizedItem(getStorageBlockPos(), i)),
				i -> runOnController(getStorageBlockLevel(), controller -> controller.removeStorageMemorizedItem(getStorageBlockPos(), i)),
				i -> runOnController(getStorageBlockLevel(), controller -> controller.addStorageMemorizedStack(getStorageBlockPos(), i)),
				i -> runOnController(getStorageBlockLevel(), controller -> controller.removeStorageMemorizedStack(getStorageBlockPos(), i))
		);
		getStorageWrapper().getInventoryHandler().registerFilterItemsChangeListener(
				items -> runOnController(getStorageBlockLevel(), controller -> controller.setStorageFilterItems(getStorageBlockPos(), items))
		);
	}

	default void registerInventoryStackListeners() {
		getStorageWrapper().getInventoryForInputOutput().registerTrackingListeners(
				isk -> runOnController(getStorageBlockLevel(), controller -> controller.addStorageStack(getStorageBlockPos(), isk)),
				isk -> runOnController(getStorageBlockLevel(), controller -> controller.removeStorageStack(getStorageBlockPos(), isk)),
				() -> runOnController(getStorageBlockLevel(), controller -> controller.addStorageWithEmptySlots(getStorageBlockPos())),
				() -> runOnController(getStorageBlockLevel(), controller -> controller.removeStorageWithEmptySlots(getStorageBlockPos()))
		);
	}

	default void registerWithControllerOnLoad() {
		getControllerPos().ifPresent(controllerPos -> {
			Level level = getStorageBlockLevel();
			if (!level.isClientSide()) {
				WorldHelper.getLoadedBlockEntity(level, controllerPos, ControllerBlockEntityBase.class)
						.ifPresent(controller -> controller.addStorageStacksAndRegisterListeners(getStorageBlockPos()));
			}
		});
	}

	default void changeSlots(int newSlots) {
		getControllerPos().ifPresent(controllerPos -> {
			Level level = getStorageBlockLevel();
			if (!level.isClientSide()) {
				WorldHelper.getLoadedBlockEntity(level, controllerPos, ControllerBlockEntityBase.class)
						.ifPresent(controller -> controller.changeSlots(getStorageBlockPos(), newSlots, getStorageWrapper().getInventoryForInputOutput().hasEmptySlots()));
			}
		});
	}

	default void updateEmptySlots() {
		getControllerPos().ifPresent(controllerPos -> {
			Level level = getStorageBlockLevel();
			if (!level.isClientSide()) {
				WorldHelper.getLoadedBlockEntity(level, controllerPos, ControllerBlockEntityBase.class)
						.ifPresent(controller -> controller.updateEmptySlots(getStorageBlockPos(), getStorageWrapper().getInventoryForInputOutput().hasEmptySlots()));
			}
		});
	}
}