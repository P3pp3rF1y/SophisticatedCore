package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.ITintable;

import java.util.Optional;
import java.util.UUID;

public interface IStorageWrapper extends ITintable {
	String SETTINGS_TAG = "settings";

	void setContentsChangeHandler(Runnable contentsChangeHandler);

	default void setInventorySlotChangeHandler(Runnable slotChangeHandler) {
		//noop
	}

	ITrackedContentsItemHandler getInventoryForUpgradeProcessing();

	InventoryHandler getInventoryHandler();

	ITrackedContentsItemHandler getInventoryForInputOutput();

	default void setUpgradeCachesInvalidatedHandler(Runnable handler) {
		//noop
	}

	SettingsHandler getSettingsHandler();

	UpgradeHandler getUpgradeHandler();

	Optional<UUID> getContentsUuid();

	Optional<Integer> getOpenTabId();

	void setOpenTabId(int openTabId);

	void removeOpenTabId();

	void setSortBy(SortBy sortBy);

	SortBy getSortBy();

	void sort();

	void onContentsNbtUpdated();

	void refreshInventoryForUpgradeProcessing();

	void refreshInventoryForInputOutput();

	void setPersistent(boolean persistent);

	void fillWithLoot(Player playerEntity);

	RenderInfo getRenderInfo();

	void setColumnsTaken(int columnsTaken, boolean hasChanged);

	int getColumnsTaken();

	default int getNumberOfSlotRows() {
		return 0;
	}

	default Optional<IStorageFluidHandler> getFluidHandler() {
		return Optional.empty();
	}

	default Optional<IEnergyStorage> getEnergyStorage() {return Optional.empty();}

	default ItemStack getWrappedStorageStack() {
		return ItemStack.EMPTY;
	}

	default int getBaseStackSizeMultiplier() {
		return 1;
	}

	default void onInit() {
		getInventoryHandler().onInit();
	}

	String getStorageType();

	Component getDisplayName();

	default boolean isUpgradeRunnable(ItemStack upgrade) {
		return true;
	}

	default void registerOnInventoryInputOutputHandlerRefreshListener(Runnable onInventoryForInputOutputHandlerRefresh) {
		//noop
	}
}
