package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UpgradeGuiManager {
	private UpgradeGuiManager() {
	}

	private static final Map<UpgradeContainerType<?, ?>, IUpgradeSettingsFactory<?, ?>> UPGRADE_TABS = new HashMap<>();
	private static final Map<UpgradeContainerType<?, ?>, IUpgradeInventoryControlFactory<?, ?>> UPGRADE_INVENTORY_CONTROLS = new HashMap<>();
	private static final Map<UpgradeType<?>, IStorageUpgradeInventoryControlFactory<?>> STORAGE_UPGRADE_INVENTORY_CONTROLS = new HashMap<>();

	public static <W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>, S extends UpgradeSettingsTab<C>> void registerTab(
			UpgradeContainerType<W, C> containerType, IUpgradeSettingsFactory<C, S> upgradeSettingsFactory) {
		UPGRADE_TABS.put(containerType, upgradeSettingsFactory);
	}

	public static <W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>, I extends UpgradeInventoryControlBase> void registerInventoryControl(
			UpgradeContainerType<W, C> containerType, IUpgradeInventoryControlFactory<C, I> factory) {
		UPGRADE_INVENTORY_CONTROLS.put(containerType, factory);
	}

	public static <W extends IUpgradeWrapper, I extends UpgradeInventoryControlBase> void registerInventoryControl(UpgradeType<W> upgradeType,
			IStorageUpgradeInventoryControlFactory<I> factory) {
		STORAGE_UPGRADE_INVENTORY_CONTROLS.put(upgradeType, factory);
	}

	public static <C extends UpgradeContainerBase<?, ?>> UpgradeSettingsTab<C> getTab(C container, Position position, StorageScreenBase<?> screen) {
		return getTabFactory(container).create(container, position, screen);
	}

	public static <C extends UpgradeContainerBase<?, ?>> Optional<UpgradeInventoryControlBase> getInventoryControl(int upgradeSlot, C container,
			Position position, int height, StorageScreenBase<?> screen) {
		return getInventoryControlFactory(container).map(f -> f.create(upgradeSlot, container, position, height, screen));
	}

	public static Map<UpgradeType<?>, UpgradeInventoryControlBase> getStorageUpgradeInventoryControls(StorageScreenBase<?> screen) {
		Map<UpgradeType<?>, UpgradeInventoryControlBase> controls = new HashMap<>();
		STORAGE_UPGRADE_INVENTORY_CONTROLS.forEach((upgradeType, factory) -> {
			factory.create(screen).ifPresent(control -> controls.put(upgradeType, control));
		});
		return controls;
	}

	@SuppressWarnings("unchecked")
	private static <C extends UpgradeContainerBase<?, ?>, S extends UpgradeSettingsTab<C>> IUpgradeSettingsFactory<C, S> getTabFactory(C container) {
		return (IUpgradeSettingsFactory<C, S>) getTabFactory(container.getType());
	}

	@SuppressWarnings("unchecked")
	private static <W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>, S extends UpgradeSettingsTab<C>> IUpgradeSettingsFactory<C, S> getTabFactory(
			UpgradeContainerType<W, C> containerType) {
		return (IUpgradeSettingsFactory<C, S>) UPGRADE_TABS.get(containerType);
	}

	@SuppressWarnings("unchecked")
	private static <C extends UpgradeContainerBase<?, ?>, I extends UpgradeInventoryControlBase> Optional<IUpgradeInventoryControlFactory<C, I>> getInventoryControlFactory(
			C container) {
		if (!UPGRADE_INVENTORY_CONTROLS.containsKey(container.getType())) {
			return Optional.empty();
		}

		return Optional.of((IUpgradeInventoryControlFactory<C, I>) getInventoryControlFactory(container.getType()));
	}

	@SuppressWarnings("unchecked")
	private static <W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>, I extends UpgradeInventoryControlBase> IUpgradeInventoryControlFactory<C, I> getInventoryControlFactory(
			UpgradeContainerType<W, C> containerType) {
		return (IUpgradeInventoryControlFactory<C, I>) UPGRADE_INVENTORY_CONTROLS.get(containerType);
	}

	public interface IUpgradeSettingsFactory<C extends UpgradeContainerBase<?, ?>, S extends UpgradeSettingsTab<C>> {
		S create(C container, Position position, StorageScreenBase<?> screen);
	}

	public interface IUpgradeInventoryControlFactory<C extends UpgradeContainerBase<?, ?>, I extends UpgradeInventoryControlBase> {
		I create(int upgradeSlot, C container, Position position, int height, StorageScreenBase<?> screen);
	}

	public interface IStorageUpgradeInventoryControlFactory<I extends UpgradeInventoryControlBase> {
		Optional<I> create(StorageScreenBase<?> screen);
	}
}
