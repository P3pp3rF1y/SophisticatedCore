package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class InventoryPartitioner {
	public static final String BASE_INDEXES_TAG = "baseIndexes";
	private IInventoryPartHandler[] inventoryPartHandlers;

	private int[] baseIndexes;
	private final InventoryHandler parent;

	public InventoryPartitioner(CompoundTag tag, InventoryHandler parent, Supplier<MemorySettingsCategory> getMemorySettings) {
		this.parent = parent;
		deserializeNBT(tag, getMemorySettings);
	}

	private int getIndexForSlot(int slot) {
		if (slot < 0) {return -1;}

		int i = 0;
		for (; i < baseIndexes.length; i++) {
			if (slot - baseIndexes[i] < 0) {
				return i - 1;
			}
		}
		return i - 1;
	}

	public IInventoryPartHandler getPartBySlot(int slot) {
		if (slot < 0 || slot >= parent.getSlots()) {
			return IInventoryPartHandler.EMPTY;
		}
		int index = getIndexForSlot(slot);
		if (index < 0 || index >= inventoryPartHandlers.length) {
			return IInventoryPartHandler.EMPTY;
		}
		return inventoryPartHandlers[index];
	}

	@Nullable
	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon(int slot) {
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

	public record SlotRange(int firstSlot, int numberOfSlots) {
	}

	public Optional<SlotRange> getFirstSpace(int maxNumberOfSlots) {
		for (int partIndex = 0; partIndex < inventoryPartHandlers.length; partIndex++) {
			if (inventoryPartHandlers[partIndex].canBeReplaced()) {
				int firstSlot = baseIndexes[partIndex];
				int numberOfSlots = baseIndexes.length > partIndex + 1 ? baseIndexes[partIndex + 1] - firstSlot : parent.getSlots() - firstSlot;
				numberOfSlots = Math.min(numberOfSlots, maxNumberOfSlots);
				return numberOfSlots > 0 ? Optional.of(new SlotRange(baseIndexes[partIndex], numberOfSlots)) : Optional.empty();
			}
		}
		return Optional.empty();
	}

	public void addInventoryPart(int inventorySlot, int numberOfSlots, IInventoryPartHandler inventoryPartHandler) {
		int index = getIndexForSlot(inventorySlot);
		if (index < 0 || index >= inventoryPartHandlers.length || baseIndexes[index] != inventorySlot) {
			return;
		}

		List<IInventoryPartHandler> newParts = new ArrayList<>();
		List<Integer> newBaseIndexes = new ArrayList<>();

		for (int i = 0; i < index; i++) {
			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(baseIndexes[i]);
		}

		newParts.add(inventoryPartHandler);
		newBaseIndexes.add(inventorySlot);

		int newNextSlot = inventorySlot + numberOfSlots;
		if (inventoryPartHandlers[index].getSlots() > newNextSlot) {
			newParts.add(new IInventoryPartHandler.Default(parent, parent.getSlots() - newNextSlot));
			newBaseIndexes.add(newNextSlot);
		}

		for (int i = index + 1; i < inventoryPartHandlers.length; i++) {
			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(baseIndexes[i]);
		}

		updatePartsAndIndexesFromLists(newParts, newBaseIndexes);

		inventoryPartHandler.onInit();
		parent.onFilterItemsChanged();
	}

	public void removeInventoryPart(int inventorySlot) {
		int index = getIndexForSlot(inventorySlot);

		if (index < 0 || index >= inventoryPartHandlers.length || baseIndexes[index] != inventorySlot) {
			return;
		}

		if (inventoryPartHandlers.length == 1) {
			updatePartsAndIndexesFromLists(List.of(new IInventoryPartHandler.Default(parent, parent.getSlots())), List.of(0));
			parent.onFilterItemsChanged();
			return;
		}

		int slotsAtPartIndex = (baseIndexes.length > index + 1 ? baseIndexes[index + 1] : parent.getSlots()) - baseIndexes[index];

		List<IInventoryPartHandler> newParts = new ArrayList<>();
		List<Integer> newBaseIndexes = new ArrayList<>();

		boolean replacedNext = false;
		for (int i = 0; i < index; i++) {
			if (i == index - 1 && inventoryPartHandlers[i] instanceof IInventoryPartHandler.Default && baseIndexes.length > index + 1 && inventoryPartHandlers[index + 1] instanceof IInventoryPartHandler.Default) {
				newParts.add(new IInventoryPartHandler.Default(parent, inventoryPartHandlers[i].getSlots() + inventoryPartHandlers[index + 1].getSlots() + slotsAtPartIndex));
				newBaseIndexes.add(baseIndexes[i]);
				replacedNext = true;
				continue;
			}

			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(baseIndexes[i]);
		}

		if (!replacedNext && baseIndexes.length > index + 1) {
			if (inventoryPartHandlers[index + 1] instanceof IInventoryPartHandler.Default) {
				newParts.add(new IInventoryPartHandler.Default(parent, inventoryPartHandlers[index + 1].getSlots() + slotsAtPartIndex));
				newBaseIndexes.add(inventorySlot);
			} else {
				newParts.add(new IInventoryPartHandler.Default(parent, slotsAtPartIndex));
				newBaseIndexes.add(inventorySlot);
				newParts.add(inventoryPartHandlers[index + 1]);
				newBaseIndexes.add(baseIndexes[index + 1]);
			}
		}

		for (int i = index + 2; i < inventoryPartHandlers.length; i++) {
			newParts.add(inventoryPartHandlers[i]);
			newBaseIndexes.add(baseIndexes[i]);
		}

		updatePartsAndIndexesFromLists(newParts, newBaseIndexes);

		parent.onFilterItemsChanged();
	}

	private void updatePartsAndIndexesFromLists(List<IInventoryPartHandler> newParts, List<Integer> newBaseIndexes) {
		inventoryPartHandlers = newParts.toArray(new IInventoryPartHandler[0]);
		baseIndexes = new int[newBaseIndexes.size()];
		for (int i = 0; i < newBaseIndexes.size(); i++) {
			baseIndexes[i] = newBaseIndexes.get(i);
		}
		parent.saveInventory();
	}

	public CompoundTag serializeNBT() {
		CompoundTag ret = new CompoundTag();
		ret.putIntArray(BASE_INDEXES_TAG, baseIndexes);
		ListTag partNames = new ListTag();
		for (IInventoryPartHandler inventoryPartHandler : inventoryPartHandlers) {
			partNames.add(StringTag.valueOf(inventoryPartHandler.getName()));
		}
		ret.put("inventoryPartNames", partNames);
		return ret;
	}

	private void deserializeNBT(CompoundTag tag, Supplier<MemorySettingsCategory> getMemorySettings) {
		if (!tag.contains(BASE_INDEXES_TAG)) {
			this.inventoryPartHandlers = new IInventoryPartHandler[] {new IInventoryPartHandler.Default(parent, parent.getSlots())};
			this.baseIndexes = new int[] {0};
			return;
		}

		baseIndexes = tag.getIntArray(BASE_INDEXES_TAG);
		inventoryPartHandlers = new IInventoryPartHandler[baseIndexes.length];
		ListTag partNamesTag = tag.getList("inventoryPartNames", Tag.TAG_STRING);
		int i = 0;
		for (Tag t : partNamesTag) {
			SlotRange slotRange = new SlotRange(baseIndexes[i], (i + 1 < baseIndexes.length ? baseIndexes[i + 1] : parent.getSlots()) - baseIndexes[i]);
			inventoryPartHandlers[i] = InventoryPartRegistry.instantiatePart(t.getAsString(), parent, slotRange, getMemorySettings);
			i++;
		}
	}
}
