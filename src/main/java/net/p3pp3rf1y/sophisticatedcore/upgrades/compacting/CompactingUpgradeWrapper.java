package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper.CompactingShape;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class CompactingUpgradeWrapper extends UpgradeWrapperBase<CompactingUpgradeWrapper, CompactingUpgradeItem>
		implements
			IInsertResponseUpgrade,
			IFilteredUpgrade,
			ISlotChangeResponseUpgrade,
			ITickableUpgrade,
			IExtractResponseUpgrade {
	private final FilterLogic filterLogic;
	private final Set<Integer> slotsToCompact = new HashSet<>();
	private final Set<Integer> slotsToCompactAfterCurrent = new HashSet<>();
	private boolean fullSlotsCalculated = false;
	private boolean compacting = false;
	private final Map<Item, Integer> fullSlotsToCompactLater = new HashMap<>();

	public CompactingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), this::canCompact, ModCoreDataComponents.FILTER_ATTRIBUTES);

		FilterLogic.ObservableFilterItemStackHandler filterHandler = filterLogic.getFilterHandler();
		filterHandler.setOnSlotChange(s -> resetFullSlotInfo());
	}

	@Override
	public void onAfterInsert(IItemHandlerSimpleInserter inventoryHandler, int slot) {
		if (compacting) {
			slotsToCompactAfterCurrent.add(slot);
			return;
		}

		compactSlotAndQueued(inventoryHandler, slot);
	}

	@Override
	public void onAfterExtract(IItemHandlerSimpleInserter inventoryHandler, int slot, ItemStack originalContents) {
		if (fullSlotsToCompactLater.containsKey(originalContents.getItem())) {
			int fullSlot = fullSlotsToCompactLater.get(originalContents.getItem());
			slotsToCompact.add(fullSlot);
		}
	}

	private void compactSlot(IItemHandlerSimpleInserter handler, int slot) {
		ItemStack slotStack = handler.getStackInSlot(slot);

		if (slotStack.isEmpty() || !filterLogic.matchesFilter(slotStack)) {
			return;
		}

		getCompactingDefinition(slotStack).ifPresent(compactingDefinition -> tryCompacting(handler, slot, slotStack, compactingDefinition));
	}

	private void compactSlotAndQueued(IItemHandlerSimpleInserter handler, int slot) {
		compacting = true;
		compactSlot(handler, slot);
		while (!slotsToCompactAfterCurrent.isEmpty()) {
			Set<Integer> slotsToCompactNext = new HashSet<>(slotsToCompactAfterCurrent);
			slotsToCompactAfterCurrent.clear();
			slotsToCompactNext.forEach(s -> compactSlot(handler, s));
		}
		slotsToCompactAfterCurrent.clear();
		compacting = false;
	}

	private void tryCompacting(IItemHandlerSimpleInserter handler, int slot, ItemStack stack, CompactingDefinition compactingDefinition) {
		int totalCount = compactingDefinition.count();
		RecipeHelper.CompactingResult compactingResult = compactingDefinition.result();
		if (!compactingResult.getResult().isEmpty()) {
			ItemStack extractedStack = InventoryHelper.extractFromInventory(stack.copyWithCount(totalCount), handler, true);
			if (extractedStack.getCount() != totalCount) {
				return;
			}

			while (extractedStack.getCount() == totalCount) {
				ItemStack resultCopy = compactingResult.getResult().copy();
				List<ItemStack> remainingItemsCopy = compactingResult.getRemainingItems().isEmpty()
						? Collections.emptyList()
						: compactingResult.getRemainingItems().stream().map(ItemStack::copy).toList();

				if (!fitsResultAndRemainingItems(handler, remainingItemsCopy, resultCopy)) {
					if (handler instanceof InventoryHandler inventoryHandler
							&& inventoryHandler.getStackInSlot(slot).getCount() >= inventoryHandler.getStackLimit(slot, stack)) {
						fullSlotsToCompactLater.put(resultCopy.getItem(), slot);
					}
					return;
				}
				fullSlotsToCompactLater.remove(resultCopy.getItem());
				InventoryHelper.extractFromInventory(stack.copyWithCount(totalCount), handler, false);
				handler.insertItem(resultCopy, false);
				InventoryHelper.insertIntoInventory(remainingItemsCopy, handler, false);
				extractedStack = InventoryHelper.extractFromInventory(stack.copyWithCount(totalCount), handler, true);
			}
		}
	}

	private boolean fitsResultAndRemainingItems(IItemHandler inventoryHandler, List<ItemStack> remainingItems, ItemStack result) {
		if (!remainingItems.isEmpty()) {
			IItemHandler clonedHandler = InventoryHelper.cloneInventory(inventoryHandler);
			return InventoryHelper.insertIntoInventory(result, clonedHandler, false).isEmpty()
					&& InventoryHelper.insertIntoInventory(remainingItems, clonedHandler, false).isEmpty();
		}
		return InventoryHelper.insertIntoInventory(result, inventoryHandler, true).isEmpty();
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public boolean shouldCompactNonUncraftable() {
		return upgrade.getOrDefault(ModCoreDataComponents.COMPACT_NON_UNCRAFTABLE, false);
	}

	private boolean canCompact(ItemStack stack) {
		return getCompactingDefinition(stack).isPresent();
	}

	private Optional<CompactingDefinition> getCompactingDefinition(ItemStack stack) {
		return getCompactingDefinition(stack, upgradeItem, shouldCompactNonUncraftable());
	}

	static Optional<CompactingDefinition> getCompactingDefinition(ItemStack stack, CompactingUpgradeItem upgradeItem, boolean shouldCompactNonUncraftable) {
		Set<CompactingShape> shapes = RecipeHelper.getItemCompactingShapes(stack);

		if (upgradeItem.shouldCompactThreeByThree() && (shapes.contains(CompactingShape.THREE_BY_THREE_UNCRAFTABLE)
				|| (shouldCompactNonUncraftable && shapes.contains(CompactingShape.THREE_BY_THREE)))) {
			return getVanillaCompactingDefinition(stack, 3, 3);
		} else if (shapes.contains(CompactingShape.TWO_BY_TWO_UNCRAFTABLE) || (shouldCompactNonUncraftable && shapes.contains(CompactingShape.TWO_BY_TWO))) {
			return getVanillaCompactingDefinition(stack, 2, 2);
		}

		int maxShapeSize = upgradeItem.shouldCompactThreeByThree() ? 3 : 2;
		return upgradeItem.getConfiguredCompactingResult(stack, maxShapeSize, maxShapeSize)
				.map(compactingDefinition -> new CompactingDefinition(compactingDefinition.result(), compactingDefinition.count()));
	}

	private static Optional<CompactingDefinition> getVanillaCompactingDefinition(ItemStack stack, int width, int height) {
		RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(stack, width, height);
		return compactingResult.getResult().isEmpty() ? Optional.empty() : Optional.of(new CompactingDefinition(compactingResult, width * height));
	}

	public void setCompactNonUncraftable(boolean shouldCompactNonUncraftable) {
		upgrade.set(ModCoreDataComponents.COMPACT_NON_UNCRAFTABLE, shouldCompactNonUncraftable);
		save();
	}

	@Override
	public void onSlotChange(IItemHandler handler, int slot) {
		if (shouldWorkInGUI()) {
			slotsToCompact.add(slot);
		} else if (handler instanceof InventoryHandler inventoryHandler) {
			calculateFullSlot(inventoryHandler, slot);
		}
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		upgrade.set(ModCoreDataComponents.SHOULD_WORK_IN_GUI, shouldWorkdInGUI);
		save();
	}

	public boolean shouldWorkInGUI() {
		return upgrade.getOrDefault(ModCoreDataComponents.SHOULD_WORK_IN_GUI, false);
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (!fullSlotsCalculated) {
			calculateFullSlots();
			fullSlotsCalculated = true;
		}
		if (slotsToCompact.isEmpty()) {
			return;
		}

		for (int slot : slotsToCompact) {
			compactSlotAndQueued(storageWrapper.getInventoryHandler(), slot);
		}

		slotsToCompact.clear();
	}

	private void calculateFullSlots() {
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		for (int slot = 0; slot < inventoryHandler.getSlots(); slot++) {
			calculateFullSlot(inventoryHandler, slot);
		}
	}

	private void calculateFullSlot(InventoryHandler inventoryHandler, int slot) {
		ItemStack slotStack = inventoryHandler.getStackInSlot(slot);

		if (slotStack.isEmpty() || !filterLogic.matchesFilter(slotStack) || slotStack.getCount() < inventoryHandler.getStackLimit(slot, slotStack)) {
			return;
		}

		Optional<CompactingDefinition> compactingDefinition = getCompactingDefinition(slotStack);
		if (compactingDefinition.isPresent() && slotStack.getCount() >= slotStack.getMaxStackSize()) {
			// try compacting with simulation to see if it would work
			RecipeHelper.CompactingResult compactingResult = compactingDefinition.get().result();
			if (!compactingResult.getResult().isEmpty()) {
				ItemStack resultCopy = compactingResult.getResult().copy();
				List<ItemStack> remainingItemsCopy = compactingResult.getRemainingItems().isEmpty()
						? Collections.emptyList()
						: compactingResult.getRemainingItems().stream().map(ItemStack::copy).toList();

				if (!fitsResultAndRemainingItems(inventoryHandler, remainingItemsCopy, resultCopy)) {
					fullSlotsToCompactLater.put(resultCopy.getItem(), slot);
				}
			}
		}
	}

	public void resetFullSlotInfo() {
		fullSlotsCalculated = false;
		fullSlotsToCompactLater.clear();
	}

	record CompactingDefinition(RecipeHelper.CompactingResult result, int count) {
	}
}
