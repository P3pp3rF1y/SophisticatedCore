package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class InventoryPartitioner {
	private IInventoryPartHandler[] inventoryPartHandlers;

	private ContainerContents.PartitionerData partitionerData;
	private final InventoryHandler parent;

	public InventoryPartitioner(ContainerContents.PartitionerData partitionerData, InventoryHandler parent, Supplier<MemorySettingsCategory> getMemorySettings) {
		this.parent = parent;
		this.partitionerData = partitionerData;
		initPartHandlers(partitionerData, getMemorySettings);
	}

	private int getIndexForSlot(int slot) {
		if (slot < 0) {return -1;}

		if (partitionerData.baseIndexes().length == 1) {
			return 0;
		}

		int i = 0;
		for (; i < partitionerData.baseIndexes().length; i++) {
			if (slot - partitionerData.baseIndexes()[i] < 0) {
				return i - 1;
			}
		}
		return i - 1;
	}

	public IInventoryPartHandler getPartBySlot(int slot) {
		if (slot < 0 || slot >= parent.size()) {
			return IInventoryPartHandler.EMPTY;
		}
		int index = getIndexForSlot(slot);
		if (index < 0 || index >= inventoryPartHandlers.length) {
			return IInventoryPartHandler.EMPTY;
		}
		return inventoryPartHandlers[index];
	}

	@Nullable
	public Identifier getNoItemIcon(int slot) {
		return getPartBySlot(slot).getNoItemIcon(slot);
	}

	public void onSlotLimitChange() {
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			inventoryPartHandler.onSlotLimitChange();
		}
	}

	public Set<Integer> getNoSortSlots() {
		Set<Integer> noSortSlots = new HashSet<>();
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			noSortSlots.addAll(inventoryPartHandler.getNoSortSlots());
		}
		return noSortSlots;
	}

	public boolean isFilterItem(Item item) {
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			if (inventoryPartHandler.isFilterItem(item)) {
				return true;
			}
		}

		return false;
	}

	public Map<Item, Set<Integer>> getFilterItems() {
		Map<Item, Set<Integer>> filterItems = new HashMap<>();
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			for (Map.Entry<Item, Set<Integer>> entry : inventoryPartHandler.getFilterItems().entrySet()) {
				filterItems.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
			}
		}
		return filterItems;
	}

	public void onInit() {
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			inventoryPartHandler.onInit();
		}
	}

	public Optional<SlotRange> getFirstSpace(int maxNumberOfSlots) {
		for (int partIndex = 0; partIndex < inventoryPartHandlers.length; partIndex++) {
			if (inventoryPartHandlers[partIndex].canBeReplaced()) {
				int firstSlot = partitionerData.baseIndexes()[partIndex];
				int numberOfSlots = partitionerData.baseIndexes().length > partIndex + 1 ? partitionerData.baseIndexes()[partIndex + 1] - firstSlot : parent.size() - firstSlot;
				numberOfSlots = Math.min(numberOfSlots, maxNumberOfSlots);
				return numberOfSlots > 0 ? Optional.of(new SlotRange(partitionerData.baseIndexes()[partIndex], numberOfSlots)) : Optional.empty();
			}
		}
		return Optional.empty();
	}

	public void addInventoryPart(int inventorySlot, int numberOfSlots, IInventoryPartHandler inventoryPartHandler) {
		int index = getIndexForSlot(inventorySlot);
		if (index < 0 || index >= inventoryPartHandlers.length || partitionerData.baseIndexes()[index] != inventorySlot) {
			return;
		}

		List<IInventoryPartHandler> newParts = new ArrayList<>();
		List<Integer> newBaseIndexes = new ArrayList<>();

		for (int i = 0; i < index; i++) {
			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(partitionerData.baseIndexes()[i]);
		}

		newParts.add(inventoryPartHandler);
		newBaseIndexes.add(inventorySlot);

		int newNextSlot = inventorySlot + numberOfSlots;
		if (inventoryPartHandlers[index].size() > newNextSlot) {
			newParts.add(new IInventoryPartHandler.Default(parent, parent.size() - newNextSlot));
			newBaseIndexes.add(newNextSlot);
		}

		for (int i = index + 1; i < inventoryPartHandlers.length; i++) {
			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(partitionerData.baseIndexes()[i]);
		}

		updatePartsAndIndexesFromLists(newParts, newBaseIndexes);

		inventoryPartHandler.onInit();
		parent.onFilterItemsChanged();
	}

	public void removeInventoryPart(int inventorySlot) {
		int index = getIndexForSlot(inventorySlot);

		if (index < 0 || index >= inventoryPartHandlers.length || partitionerData.baseIndexes()[index] != inventorySlot) {
			return;
		}

		if (inventoryPartHandlers.length == 1) {
			updatePartsAndIndexesFromLists(List.of(new IInventoryPartHandler.Default(parent, parent.size())), List.of(0));
			parent.onFilterItemsChanged();
			return;
		}

		int slotsAtPartIndex = (partitionerData.baseIndexes().length > index + 1 ? partitionerData.baseIndexes()[index + 1] : parent.size()) - partitionerData.baseIndexes()[index];

		List<IInventoryPartHandler> newParts = new ArrayList<>();
		List<Integer> newBaseIndexes = new ArrayList<>();

		boolean replacedNext = false;
		for (int i = 0; i < index; i++) {
			if (i == index - 1 && inventoryPartHandlers[i] instanceof IInventoryPartHandler.Default && partitionerData.baseIndexes().length > index + 1 && inventoryPartHandlers[index + 1] instanceof IInventoryPartHandler.Default) {
				newParts.add(new IInventoryPartHandler.Default(parent, inventoryPartHandlers[i].size() + inventoryPartHandlers[index + 1].size() + slotsAtPartIndex));
				newBaseIndexes.add(partitionerData.baseIndexes()[i]);
				replacedNext = true;
				continue;
			}

			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(partitionerData.baseIndexes()[i]);
		}

		if (!replacedNext && partitionerData.baseIndexes().length > index + 1) {
			if (inventoryPartHandlers[index + 1] instanceof IInventoryPartHandler.Default) {
				newParts.add(new IInventoryPartHandler.Default(parent, inventoryPartHandlers[index + 1].size() + slotsAtPartIndex));
				newBaseIndexes.add(inventorySlot);
			} else {
				newParts.add(new IInventoryPartHandler.Default(parent, slotsAtPartIndex));
				newBaseIndexes.add(inventorySlot);
				newParts.add(inventoryPartHandlers[index + 1]);
				newBaseIndexes.add(partitionerData.baseIndexes()[index + 1]);
			}
		}

		for (int i = index + 2; i < inventoryPartHandlers.length; i++) {
			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(partitionerData.baseIndexes()[i]);
		}

		updatePartsAndIndexesFromLists(newParts, newBaseIndexes);

		parent.onFilterItemsChanged();
	}

	private void updatePartsAndIndexesFromLists(List<IInventoryPartHandler> newParts, List<Integer> newBaseIndexes) {
		inventoryPartHandlers = newParts.toArray(new IInventoryPartHandler[0]);
		int[] baseIndexes = newBaseIndexes.stream().mapToInt(i -> i).toArray();
		List<String> partNames = new ArrayList<>();
		for (int i = 0; i < newBaseIndexes.size(); i++) {
			partNames.add(inventoryPartHandlers[i].getName());
		}
		partitionerData.setPartBaseIndexesAndNames(baseIndexes, partNames);
		parent.saveInventory();
	}

	private void initPartHandlers(ContainerContents.PartitionerData partitionerData, Supplier<MemorySettingsCategory> getMemorySettings) {
		inventoryPartHandlers = new IInventoryPartHandler[partitionerData.baseIndexes().length];
		int i = 0;
		for (String partName : partitionerData.partNames()) {
			SlotRange slotRange = new SlotRange(partitionerData.baseIndexes()[i], (i + 1 < partitionerData.baseIndexes().length ? partitionerData.baseIndexes()[i + 1] : parent.size()) - partitionerData.baseIndexes()[i]);
			int finalI = i;
			inventoryPartHandlers[finalI] = InventoryPartRegistry.instantiatePart(partName, parent, slotRange, getMemorySettings);
			i++;
		}
	}

	public boolean isInfinite(int slot) {
		return getPartBySlot(slot).isInfinite(slot);
	}
}
