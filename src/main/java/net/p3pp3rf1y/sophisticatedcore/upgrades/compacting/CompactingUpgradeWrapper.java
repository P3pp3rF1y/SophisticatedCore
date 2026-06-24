package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
	public void onAfterInsert(InventoryHandler inventoryHandler, int slot, TransactionContext tx) {
		if (compacting) {
			slotsToCompactAfterCurrent.add(slot);
			return;
		}

		compactSlotAndQueued(inventoryHandler, slot, tx);
	}

	@Override
	public void onAfterExtract(InventoryHandler inventoryHandler, int slot, ItemResource originalResource) {
		if (fullSlotsToCompactLater.containsKey(originalResource.getItem())) {
			int fullSlot = fullSlotsToCompactLater.get(originalResource.getItem());
			slotsToCompact.add(fullSlot);
		}
	}

	private void compactSlot(ITrackedContentsItemResourceHandler inventoryHandler, int slot, TransactionContext tx) {
		ItemStack stack = inventoryHandler.getStackInSlot(slot);

		if (stack.isEmpty() || !filterLogic.matchesFilter(stack)) {
			return;
		}

		getCompactingDefinition(stack).ifPresent(compactingDefinition -> tryCompacting(inventoryHandler, slot, stack, compactingDefinition, tx));
	}

	private void compactSlotAndQueued(ITrackedContentsItemResourceHandler inventoryHandler, int slot, TransactionContext tx) {
		compacting = true;
		compactSlot(inventoryHandler, slot, tx);
		while (!slotsToCompactAfterCurrent.isEmpty()) {
			Set<Integer> slotsToCompactNext = new HashSet<>(slotsToCompactAfterCurrent);
			slotsToCompactAfterCurrent.clear();
			slotsToCompactNext.forEach(s -> compactSlot(inventoryHandler, s, tx));
		}
		slotsToCompactAfterCurrent.clear();
		compacting = false;
	}

	private void tryCompacting(ITrackedContentsItemResourceHandler inventoryHandler, int slotBeingCompacted, ItemStack stack,
			CompactingDefinition compactingDefinition, TransactionContext tx) {
		int totalCount = compactingDefinition.count();
		RecipeHelper.CompactingResult compactingResult = compactingDefinition.result();
		if (!compactingResult.getResult().isEmpty()) {
			try (Transaction childTx = Transaction.open(tx)) {
				ItemResource resource = ItemResource.of(stack);
				int extracted = inventoryHandler.extract(resource, totalCount, childTx);
				boolean hasCompacted = false;
				int insertBackIntoSlot = -1;
				while (extracted == totalCount) {
					ItemStack resultCopy = compactingResult.getResult().copy();
					List<ItemStack> remainingItemsCopy = compactingResult.getRemainingItems().isEmpty()
							? Collections.emptyList()
							: compactingResult.getRemainingItems().stream().map(ItemStack::copy).toList();

					if (inventoryHandler.insert(ItemResource.of(resultCopy), resultCopy.getCount(), childTx) != resultCopy.getCount()
							|| !InventoryHelper.insertIntoInventory(remainingItemsCopy, inventoryHandler, childTx).isEmpty()) {
						if (inventoryHandler.getAmountAsLong(slotBeingCompacted) + extracted >= inventoryHandler.getCapacityAsLong(slotBeingCompacted,
								resource)) {
							fullSlotsToCompactLater.put(resultCopy.getItem(), slotBeingCompacted);
						}
						break;
					}
					fullSlotsToCompactLater.remove(resultCopy.getItem());
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
	public void onSlotChange(InventoryHandler inventoryHandler, int slot) {
		if (shouldWorkInGUI()) {
			slotsToCompact.add(slot);
		} else {
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

		try (Transaction tx = Transaction.openRoot()) {
			for (int slot : slotsToCompact) {
				compactSlotAndQueued(storageWrapper.getInventoryForUpgradeProcessing(), slot, tx);
			}
			tx.commit();
		}

		slotsToCompact.clear();
	}

	private void calculateFullSlots() {
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		for (int slot = 0; slot < inventoryHandler.size(); slot++) {
			calculateFullSlot(inventoryHandler, slot);
		}
	}

	private void calculateFullSlot(InventoryHandler inventoryHandler, int slot) {
		ItemStack slotStack = inventoryHandler.getStackInSlot(slot);

		if (slotStack.isEmpty() || !filterLogic.matchesFilter(slotStack)
				|| slotStack.getCount() < inventoryHandler.getCapacityAsLong(slot, ItemResource.of(slotStack))) {
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

				try (Transaction tx = Transaction.openRoot()) {
					if (inventoryHandler.insert(ItemResource.of(resultCopy), resultCopy.getCount(), tx) != resultCopy.getCount()
							|| !InventoryHelper.insertIntoInventory(remainingItemsCopy, inventoryHandler, tx).isEmpty()) {
						fullSlotsToCompactLater.put(resultCopy.getItem(), slot);
					}
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
