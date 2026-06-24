package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInventoryLayoutContributor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInventorySlotBlocker;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.ITintable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IStorageWrapper extends ITintable {
	String SETTINGS = "settings";

	void setContentsChangeHandler(Runnable contentsChangeHandler);

	default void setInventorySlotChangeHandler(Runnable slotChangeHandler) {
		// noop
	}

	ITrackedContentsItemResourceHandler getInventoryForUpgradeProcessing();

	InventoryHandler getInventoryHandler();

	ITrackedContentsItemResourceHandler getInventoryForInputOutput();

	default void setUpgradeCachesInvalidatedHandler(Runnable handler) {
		// noop
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

	void onContentsUpdated();

	void refreshInventoryForUpgradeProcessing();

	void refreshInventoryForInputOutput();

	void setPersistent(boolean persistent);

	void fillWithLoot(Player playerEntity);

	RenderDataHandler getRenderDataHandler();

	void setColumnsTaken(int columnsTaken, boolean hasChanged);

	int getColumnsTaken();

	default int getNumberOfSlotRows() {
		return 0;
	}

	default List<InventoryLayoutPart> getInventoryLayoutParts(int columns) {
		return getInventoryLayoutParts(columns, columns);
	}

	default List<InventoryLayoutPart> getInventoryLayoutParts(int columns, int targetColumns) {
		InventoryHandler inventoryHandler = getInventoryHandler();
		Set<Integer> fixedSlots = getFixedLayoutSlots();
		UpgradeHandler upgradeHandler = getUpgradeHandler();
		List<IInventoryLayoutContributor> layoutContributors = upgradeHandler.getWrappersThatImplement(IInventoryLayoutContributor.class);
		List<InventoryLayoutPart> parts = new ArrayList<>();
		for (int slot = 0; slot < inventoryHandler.size(); slot++) {
			boolean handledByContributor = false;
			for (IInventoryLayoutContributor layoutContributor : layoutContributors) {
				if (layoutContributor.isInventoryLayoutSlotHandled(slot, columns)) {
					handledByContributor = true;
					layoutContributor.getInventoryLayoutPart(slot, columns, targetColumns).ifPresent(parts::add);
					break;
				}
			}
			if (handledByContributor) {
				continue;
			}

			ItemStack stack = inventoryHandler.getStackInSlot(slot);
			if (!stack.isEmpty() && fixedSlots.contains(slot)) {
				parts.add(new InventoryLayoutPart("fixed:" + slot, slot, 1, 1, Set.of(slot)));
			} else if (!stack.isEmpty() && inventoryHandler.isSlotAccessible(slot)) {
				parts.add(new InventoryLayoutPart("stack:" + slot, slot, 1, 1, Set.of(slot)));
			} else if (!stack.isEmpty() || !inventoryHandler.isSlotAccessible(slot)) {
				parts.add(new InventoryLayoutPart("fixed:" + slot, slot, 1, 1, Set.of(slot)));
			}
		}
		return parts;
	}

	private Set<Integer> getFixedLayoutSlots() {
		SettingsHandler settingsHandler = getSettingsHandler();

		Set<Integer> fixedSlots = new HashSet<>();
		NoSortSettingsCategory noSortSettings = settingsHandler.getTypeCategory(NoSortSettingsCategory.class);
		if (noSortSettings != null) {
			fixedSlots.addAll(noSortSettings.getNoSortSlots());
		}
		MemorySettingsCategory memorySettings = settingsHandler.getTypeCategory(MemorySettingsCategory.class);
		if (memorySettings != null) {
			fixedSlots.addAll(memorySettings.getSlotIndexes());
		}
		return fixedSlots;
	}

	default void applyInventoryLayout(InventoryLayoutFitResult fitResult, int columns) {
		InventoryHandler inventoryHandler = getInventoryHandler();
		List<StackMove> stackMoves = new ArrayList<>();
		Set<Integer> sourceSlots = new HashSet<>();
		for (Map.Entry<String, Integer> fittedSlot : fitResult.fittedSlots().entrySet()) {
			String partId = fittedSlot.getKey();
			if (!partId.startsWith("stack:")) {
				continue;
			}
			int sourceSlot = Integer.parseInt(partId.substring("stack:".length()));
			int targetSlot = fittedSlot.getValue();
			if (sourceSlot == targetSlot || sourceSlot >= inventoryHandler.size() || targetSlot >= inventoryHandler.size()) {
				continue;
			}
			ItemStack stack = inventoryHandler.getStackInSlot(sourceSlot);
			if (!stack.isEmpty()) {
				stackMoves.add(new StackMove(sourceSlot, targetSlot, stack.copy()));
				sourceSlots.add(sourceSlot);
			}
		}

		if (!stackMoves.isEmpty()) {
			Set<Integer> targetSlots = new HashSet<>();
			stackMoves.forEach(move -> targetSlots.add(move.targetSlot()));
			for (int targetSlot : targetSlots) {
				if (!inventoryHandler.getStackInSlot(targetSlot).isEmpty() && !sourceSlots.contains(targetSlot)) {
					return;
				}
			}

			stackMoves.forEach(move -> inventoryHandler.setStackInSlot(move.sourceSlot(), ItemStack.EMPTY));
			stackMoves.forEach(move -> inventoryHandler.setStackInSlot(move.targetSlot(), move.stack()));
		}

		UpgradeHandler upgradeHandler = getUpgradeHandler();
		upgradeHandler.getWrappersThatImplement(IInventoryLayoutContributor.class).forEach(contributor -> contributor.applyInventoryLayout(fitResult, columns));
	}

	default void attachInventorySlotBlockers() {
		UpgradeHandler upgradeHandler = getUpgradeHandler();
		List<IInventorySlotBlocker> slotBlockers = upgradeHandler.getWrappersThatImplementFromMainStorage(IInventorySlotBlocker.class);
		getInventoryHandler().setSlotBlockedPredicate(slot -> slotBlockers.stream().anyMatch(slotBlocker -> slotBlocker.isSlotBlocked(slot)),
				slot -> slotBlockers.stream().anyMatch(slotBlocker -> slotBlocker.isSlotBlocked(slot) && slotBlocker.shouldRenderBlockedSlotOverlay(slot)));
	}

	record StackMove(int sourceSlot, int targetSlot, ItemStack stack) {
	}

	default Optional<IStorageFluidHandler> getFluidHandler() {
		return Optional.empty();
	}

	default Optional<EnergyHandler> getEnergyHandler() {
		return Optional.empty();
	}

	default ItemStack getWrappedStorageStack() {
		return ItemStack.EMPTY;
	}

	default int getBaseStackSizeMultiplier() {
		return 1;
	}

	default void onInit(Level level) {
		getInventoryHandler().onInit();
	}

	String getStorageType();

	Component getDisplayName();

	default boolean isUpgradeRunnable(ItemStack upgrade) {
		return true;
	}

	default void registerOnInventoryInputOutputHandlerRefreshListener(Runnable onInventoryForInputOutputHandlerRefresh) {
		// noop
	}
}
