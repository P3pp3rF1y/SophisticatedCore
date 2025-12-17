package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper.CompactingShape;

import org.jspecify.annotations.Nullable;
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
	public int onBeforeInsert(InventoryHandler inventoryHandler, int slot, ItemResource resource, int amount) {
		return 0;
	}

	@Override
	public void onAfterInsert(InventoryHandler inventoryHandler, int slot, TransactionContext tx) {
		compactSlot(inventoryHandler, slot, tx);
	}

	private void compactSlot(ITrackedContentsItemResourceHandler inventoryHandler, int slot, TransactionContext tx) {
		ItemStack stack = inventoryHandler.getStackInSlot(slot);

		if (stack.isEmpty() || !filterLogic.matchesFilter(stack)) {
			return;
		}

		Set<CompactingShape> shapes = RecipeHelper.getItemCompactingShapes(stack);

		if (upgradeItem.shouldCompactThreeByThree() && (shapes.contains(CompactingShape.THREE_BY_THREE_UNCRAFTABLE) || (shouldCompactNonUncraftable() && shapes.contains(CompactingShape.THREE_BY_THREE)))) {
			tryCompacting(inventoryHandler, stack, 3, 3, tx);
		} else if (shapes.contains(CompactingShape.TWO_BY_TWO_UNCRAFTABLE) || (shouldCompactNonUncraftable() && shapes.contains(CompactingShape.TWO_BY_TWO))) {
			tryCompacting(inventoryHandler, stack, 2, 2, tx);
		}
	}

	private void tryCompacting(ResourceHandler<ItemResource> inventoryHandler, ItemStack stack, int width, int height, TransactionContext tx) {
		int totalCount = width * height;
		RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(stack, width, height);
		if (!compactingResult.getResult().isEmpty()) {
			try (Transaction childTx = Transaction.open(tx)) {
				ItemResource resource = ItemResource.of(stack);
				int extracted = inventoryHandler.extract(resource, totalCount, childTx);
				boolean hasCompacted = false;
				int insertBackIntoSlot = -1;
				while (extracted == totalCount) {
					ItemStack resultCopy = compactingResult.getResult().copy();
					List<ItemStack> remainingItemsCopy = compactingResult.getRemainingItems().isEmpty() ? Collections.emptyList() : compactingResult.getRemainingItems().stream().map(ItemStack::copy).toList();

					if (inventoryHandler.insert(ItemResource.of(resultCopy), resultCopy.getCount(), childTx) != resultCopy.getCount() || !InventoryHelper.insertIntoInventory(remainingItemsCopy, inventoryHandler, childTx).isEmpty()) {
						return;
					}
					hasCompacted = true;

					extracted = 0;
					for (int slot = 0; slot < inventoryHandler.size(); slot++) {
						extracted += inventoryHandler.extract(slot, resource, totalCount, childTx);
						if (extracted > 0) {
							if (insertBackIntoSlot == -1) {
								insertBackIntoSlot = slot;
							}
							if (extracted == totalCount) {
								break;
							}
						}
					}
				}
				if (hasCompacted) {
					if (extracted > 0) {
						inventoryHandler.insert(insertBackIntoSlot, resource, extracted, childTx);
					}
					childTx.commit();
				}
			}
		}
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
	public void onSlotChange(InventoryHandler inventoryHandler, int slot) {
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

		try (Transaction tx = Transaction.openRoot()) {
			for (int slot : slotsToCompact) {
				compactSlot(storageWrapper.getInventoryForUpgradeProcessing(), slot, tx);
			}
			tx.commit();
		}

		slotsToCompact.clear();
	}
}
