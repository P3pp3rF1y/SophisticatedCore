package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointProvider;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

public interface IControllableStorage extends IControllerBoundable {

	IStorageWrapper getStorageWrapper();

	default boolean canConnectStorages() {
		return true;
	}

	default boolean hasStorageData() {
		return true;
	}

	default BlockPos getControlledStorageBlockPos() {
		return getStorageBlockPos();
	}

	default ControllerStorageKey getControllerStorageKey() {
		if (this instanceof ILinkedStorageEndpointProvider endpointProvider) {
			return endpointProvider.getLinkedStorageEndpoint().map(endpoint -> new ControllerStorageKey(getControlledStorageBlockPos(), endpoint.groupId()))
					.orElseGet(() -> new ControllerStorageKey(getControlledStorageBlockPos(), null));
		}
		return new ControllerStorageKey(getControlledStorageBlockPos(), null);
	}

	default void tryToAddToController() {
		addToAdjacentController();
	}

	default void removeFromController() {
		Level level = getStorageBlockLevel();
		if (!level.isClientSide()) {
			getControllerPos().flatMap(p -> WorldHelper.getBlockEntity(level, p, ControllerBlockEntityBase.class))
					.ifPresent(c -> c.removeStorage(getStorageBlockPos()));
			removeControllerPos();
		}
	}

	default void unloadFromController() {
		Level level = getStorageBlockLevel();
		if (!level.isClientSide()) {
			getControllerPos().flatMap(pos -> WorldHelper.getLoadedBlockEntity(level, pos, ControllerBlockEntityBase.class))
					.filter(controller -> !controller.isRemoved()).ifPresent(controller -> controller.unloadStorage(getStorageBlockPos()));
		}
	}

	@Override
	default void addToController(Level level, BlockPos pos, BlockPos controllerPos) {
		WorldHelper.getBlockEntity(level, controllerPos, ControllerBlockEntityBase.class).ifPresent(c -> c.addStorage(getControlledStorageBlockPos()));
	}

	default void registerController(ControllerBlockEntityBase controllerBlockEntity) {
		setControllerPos(controllerBlockEntity.getBlockPos());
		if (hasStorageData() && controllerBlockEntity.getLevel() != null && !controllerBlockEntity.getLevel().isClientSide()) {
			registerListeners(new WeakReference<>(controllerBlockEntity));
		}
	}

	default void registerControllerMembership(ControllerBlockEntityBase controllerBlockEntity) {
		setControllerPos(controllerBlockEntity.getBlockPos());
	}

	default void unregisterController() {
		unregisterControllerMembership();
		unregisterControllerListeners();
	}

	default void unregisterControllerMembership() {
		removeControllerPos();
	}

	default void unregisterControllerListeners() {
		getStorageWrapper().getInventoryForInputOutput().unregisterStackKeyListeners();
		getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).unregisterListeners();
		getStorageWrapper().getInventoryHandler().unregisterFilterItemsChangeListener();
	}

	default boolean isControllerStorageAccessible() {
		return true;
	}

	private void registerListeners(WeakReference<ControllerBlockEntityBase> controllerReference) {
		registerInventoryStackListeners(controllerReference);
		getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).registerListeners(
				i -> runOnController(getStorageBlockLevel(), controller -> controller.addStorageMemorizedItem(getStorageBlockPos(), i)),
				i -> runOnController(getStorageBlockLevel(), controller -> controller.removeStorageMemorizedItem(getStorageBlockPos(), i)),
				i -> runOnController(getStorageBlockLevel(), controller -> controller.addStorageMemorizedStack(getStorageBlockPos(), i)),
				i -> runOnController(getStorageBlockLevel(), controller -> controller.removeStorageMemorizedStack(getStorageBlockPos(), i)));
		getStorageWrapper().getInventoryHandler().registerFilterItemsChangeListener(
				items -> runOnController(getStorageBlockLevel(), controller -> controller.setStorageFilterItems(getStorageBlockPos(), items)));
		getStorageWrapper().registerOnInventoryInputOutputHandlerRefreshListener(this::onInventoryInputOutputHandlerRefresh);
	}

	private void runOnRegisteredController(WeakReference<ControllerBlockEntityBase> controllerReference, Consumer<ControllerBlockEntityBase> update) {
		// A FULL-chunk lookup can temporarily fail without unloading the registered controller.
		ControllerBlockEntityBase controller = controllerReference.get();
		if (controller != null && controller.canReceiveStorageUpdates(getStorageBlockPos())
				&& getControllerPos().filter(controller.getBlockPos()::equals).isPresent()) {
			update.accept(controller);
		}
	}

	default void onInventoryInputOutputHandlerRefresh() {
		runOnController(getStorageBlockLevel(), controller -> controller.rebindStorage(getStorageBlockPos()));
	}

	default void registerInventoryStackListeners() {
		getControllerPos().flatMap(pos -> WorldHelper.getLoadedBlockEntity(getStorageBlockLevel(), pos, ControllerBlockEntityBase.class)).ifPresentOrElse(
				controller -> registerInventoryStackListeners(new WeakReference<>(controller)),
				() -> getStorageWrapper().getInventoryForInputOutput().registerTrackingListeners(
						isk -> runOnController(getStorageBlockLevel(), controller -> controller.addStorageStack(getStorageBlockPos(), isk)),
						isk -> runOnController(getStorageBlockLevel(), controller -> controller.removeStorageStack(getStorageBlockPos(), isk)),
						() -> runOnController(getStorageBlockLevel(), controller -> controller.addStorageWithEmptySlots(getStorageBlockPos())),
						() -> runOnController(getStorageBlockLevel(), controller -> controller.removeStorageWithEmptySlots(getStorageBlockPos()))));
	}

	private void registerInventoryStackListeners(WeakReference<ControllerBlockEntityBase> controllerReference) {
		getStorageWrapper().getInventoryForInputOutput().registerTrackingListeners(
				isk -> runOnRegisteredController(controllerReference, controller -> controller.addStorageStack(getStorageBlockPos(), isk)),
				isk -> runOnRegisteredController(controllerReference, controller -> controller.removeStorageStack(getStorageBlockPos(), isk)),
				() -> runOnRegisteredController(controllerReference, controller -> controller.addStorageWithEmptySlots(getStorageBlockPos())),
				() -> runOnRegisteredController(controllerReference, controller -> controller.removeStorageWithEmptySlots(getStorageBlockPos())));
	}

	default void registerWithControllerOnLoad() {
		getControllerPos().ifPresentOrElse(controllerPos -> {
			Level level = getStorageBlockLevel();
			if (!level.isClientSide()) {
				BlockPos controlledStoragePos = getControlledStorageBlockPos();
				WorldHelper.getLoadedBlockEntity(level, controllerPos, ControllerBlockEntityBase.class).ifPresent(controller -> {
					if (controller.isStorageConnected(controlledStoragePos)) {
						if (hasStorageData()) {
							controller.addStorageStacksAndRegisterListeners(controlledStoragePos);
						}
					} else {
						removeControllerPos();
						tryToAddToController();
					}
				});
			}
		}, this::tryToAddToController);
	}

	default void reregisterWithController() {
		Level level = getStorageBlockLevel();
		if (!level.isClientSide()) {
			getControllerPos().flatMap(controllerPos -> WorldHelper.getLoadedBlockEntity(level, controllerPos, ControllerBlockEntityBase.class))
					.ifPresent(controller -> controller.reregisterStorage(this));
		}
	}

	default void changeSlots(int newSlots) {
		getControllerPos().ifPresent(controllerPos -> {
			Level level = getStorageBlockLevel();
			if (!level.isClientSide()) {
				WorldHelper.getLoadedBlockEntity(level, controllerPos, ControllerBlockEntityBase.class).ifPresent(controller -> controller
						.changeSlots(getControlledStorageBlockPos(), newSlots, getStorageWrapper().getInventoryForInputOutput().hasEmptySlots()));
			}
		});
	}

	default void updateEmptySlots() {
		getControllerPos().ifPresent(controllerPos -> {
			Level level = getStorageBlockLevel();
			if (!level.isClientSide()) {
				WorldHelper.getLoadedBlockEntity(level, controllerPos, ControllerBlockEntityBase.class).ifPresent(controller -> controller
						.updateEmptySlots(getControlledStorageBlockPos(), getStorageWrapper().getInventoryForInputOutput().hasEmptySlots()));
			}
		});
	}
}
