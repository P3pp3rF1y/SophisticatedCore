package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper.CompactingShape;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CompactingUpgradeWrapper extends UpgradeWrapperBase<CompactingUpgradeWrapper, CompactingUpgradeItem>
		implements IInsertResponseUpgrade, IFilteredUpgrade, ISlotChangeResponseUpgrade, ITickableUpgrade {
	private final FilterLogic filterLogic;
	private final Set<Integer> slotsToCompact = new HashSet<>();

	public CompactingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(),
				stack -> RecipeHelper.getItemCompactingShapes(stack).stream().anyMatch(shape -> shape != CompactingShape.NONE),
				ModCoreDataComponents.FILTER_ATTRIBUTES);
	}

	@Override
	public ItemStack onBeforeInsert(IItemHandlerSimpleInserter inventoryHandler, int slot, ItemStack stack, boolean simulate) {
		return stack;
	}

	@Override
	public void onAfterInsert(IItemHandlerSimpleInserter inventoryHandler, int slot) {
		compactSlot(inventoryHandler, slot);
	}

	private void compactSlot(IItemHandlerSimpleInserter inventoryHandler, int slot) {
		ItemStack slotStack = inventoryHandler.getStackInSlot(slot);

		if (slotStack.isEmpty() || !filterLogic.matchesFilter(slotStack)) {
			return;
		}

		Set<CompactingShape> shapes = RecipeHelper.getItemCompactingShapes(slotStack);

		if (upgradeItem.shouldCompactThreeByThree() && (shapes.contains(CompactingShape.THREE_BY_THREE_UNCRAFTABLE) || (shouldCompactNonUncraftable() && shapes.contains(CompactingShape.THREE_BY_THREE)))) {
			tryCompacting(inventoryHandler, slotStack, 3, 3);
		} else if (shapes.contains(CompactingShape.TWO_BY_TWO_UNCRAFTABLE) || (shouldCompactNonUncraftable() && shapes.contains(CompactingShape.TWO_BY_TWO))) {
			tryCompacting(inventoryHandler, slotStack, 2, 2);
		}
	}

	private void tryCompacting(IItemHandlerSimpleInserter inventoryHandler, ItemStack stack, int width, int height) {
		int totalCount = width * height;
		RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(stack, width, height);
		if (!compactingResult.getResult().isEmpty()) {
			ItemStack extractedStack = InventoryHelper.extractFromInventory(stack.copyWithCount(totalCount), inventoryHandler, true);
			if (extractedStack.getCount() != totalCount) {
				return;
			}

			while (extractedStack.getCount() == totalCount) {
				ItemStack resultCopy = compactingResult.getResult().copy();
				List<ItemStack> remainingItemsCopy = compactingResult.getRemainingItems().isEmpty() ? Collections.emptyList() : compactingResult.getRemainingItems().stream().map(ItemStack::copy).toList();

				if (!fitsResultAndRemainingItems(inventoryHandler, remainingItemsCopy, resultCopy)) {
					break;
				}
				InventoryHelper.extractFromInventory(stack.copyWithCount(totalCount), inventoryHandler, false);
				inventoryHandler.insertItem(resultCopy, false);
				InventoryHelper.insertIntoInventory(remainingItemsCopy, inventoryHandler, false);
				extractedStack = InventoryHelper.extractFromInventory(stack.copyWithCount(totalCount), inventoryHandler, true);
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

	public void setCompactNonUncraftable(boolean shouldCompactNonUncraftable) {
		upgrade.set(ModCoreDataComponents.COMPACT_NON_UNCRAFTABLE, shouldCompactNonUncraftable);
		save();
	}

	@Override
	public void onSlotChange(IItemHandler inventoryHandler, int slot) {
		if (shouldWorkInGUI()) {
			slotsToCompact.add(slot);
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
		if (slotsToCompact.isEmpty()) {
			return;
		}

		for (int slot : slotsToCompact) {
			compactSlot(storageWrapper.getInventoryHandler(), slot);
		}

		slotsToCompact.clear();
	}
}
