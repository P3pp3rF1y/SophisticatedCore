package net.p3pp3rf1y.sophisticatedcore.settings.memory;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.ItemResourceHelper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MemorySettingsCategory implements ISettingsCategory<MemorySettingsCategory, MemorySettingsCategoryData> {
	public static final String NAME = "memory";
	private final Supplier<InventoryHandler> inventoryHandlerSupplier;
	private MemorySettingsCategoryData data;
	private final Runnable save;
	private final Map<Item, Set<Integer>> filterItemSlots = new HashMap<>();

	private final Map<Integer, Set<Integer>> filterStackSlots = new HashMap<>();

	private Consumer<Item> onItemAdded = i -> {
	};

	private Consumer<Integer> onStackAdded = i -> {
	};
	private Consumer<Item> onItemRemoved = i -> {
	};
	private Consumer<Integer> onStackRemoved = i -> {
	};

	public MemorySettingsCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, MemorySettingsCategoryData data, Runnable save) {
		this.inventoryHandlerSupplier = inventoryHandlerSupplier;
		this.data = data;
		this.save = save;

		initItemAndStackSlots();
	}

	private void initItemAndStackSlots() {
		data.slotFilterItems().forEach(this::addFilterItemSlot);
		data.slotFilterStacks().forEach(this::addFilterStackSlot);
	}

	public boolean matchesFilter(int slotNumber, ItemStack stack) {
		if (data.slotFilterItems().containsKey(slotNumber)) {
			return !stack.isEmpty() && stack.getItem() == data.slotFilterItems().get(slotNumber);
		}
		if (data.slotFilterStacks().containsKey(slotNumber)) {
			return !stack.isEmpty() && data.slotFilterStacks().get(slotNumber).matches(stack);
		}

		return true;
	}

	public boolean matchesFilter(int slotNumber, ItemResource resource) {
		if (data.slotFilterItems().containsKey(slotNumber)) {
			return resource.getItem() == data.slotFilterItems().get(slotNumber);
		}
		if (data.slotFilterStacks().containsKey(slotNumber)) {
			return resource.matches(data.slotFilterStacks().get(slotNumber).stack());
		}

		return true;
	}

	public Optional<ItemStack> getSlotFilterStack(int slotNumber, boolean copy) {
		if (data.slotFilterItems().containsKey(slotNumber)) {
			return Optional.of(new ItemStack(data.slotFilterItems().get(slotNumber)));
		}
		if (data.slotFilterStacks().containsKey(slotNumber)) {
			ItemStack filterStack = data.slotFilterStacks().get(slotNumber).stack();
			return Optional.of(copy ? filterStack.copy() : filterStack);
		}

		return Optional.empty();
	}

	public boolean isSlotSelected(int slotNumber) {
		return data.slotFilterItems().containsKey(slotNumber) || data.slotFilterStacks().containsKey(slotNumber);
	}

	public void unselectAllSlots() {
		unselectAllFilterItemSlots();
		unselectAllFilteStackSlots();

		save();
	}

	private void save() {
		save.run();
	}

	private void unselectAllFilteStackSlots() {
		filterStackSlots.keySet().forEach(i -> onStackRemoved.accept(i));
		data.clearSlotFilterStacks();
		filterStackSlots.clear();
	}

	private void unselectAllFilterItemSlots() {
		filterItemSlots.keySet().forEach(i -> onItemRemoved.accept(i));
		data.clearSlotFilterItems();
		filterItemSlots.clear();
	}

	/**
	 * Selects slots that shouldn't be sorted
	 *
	 * @param minSlot inclusive
	 * @param maxSlot exclusive
	 */

	public void selectSlots(int minSlot, int maxSlot) {
		for (int slot = minSlot; slot < maxSlot; slot++) {
			InventoryHandler inventoryHandler = getInventoryHandler();
			if (slot < inventoryHandler.size()) {
				ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
				if (!stackInSlot.isEmpty()) {
					if (data.ignoreNbt()) {
						Item item = stackInSlot.getItem();
						addSlotItem(slot, item);
					} else {
						addSlotStack(slot, stackInSlot);
					}
				} else {
					Item filterItem = inventoryHandler.getFilterItem(slot);
					if (filterItem != Items.AIR) {
						if (data.ignoreNbt()) {
							addSlotItem(slot, filterItem);
						} else {
							addSlotStack(slot, new ItemStack(filterItem));
						}
					}
				}
			}
		}
		save();
	}

	private InventoryHandler getInventoryHandler() {
		return inventoryHandlerSupplier.get();
	}

	private void addSlotItem(int slot, Item item) {
		data.slotFilterItems().put(slot, item);
		addFilterItemSlot(slot, item);
	}

	private void addFilterItemSlot(int slot, Item item) {
		filterItemSlots.computeIfAbsent(item, k -> {
			onItemAdded.accept(k);
			return new TreeSet<>();
		}).add(slot);
	}

	private void addSlotStack(int slot, ItemStack stack) {
		ItemStackKey stackKey = ItemStackKey.of(stack);
		data.addSlotStack(slot, stackKey);
		addFilterStackSlot(slot, stackKey);
	}

	private void addFilterStackSlot(int slot, ItemStackKey stackKey) {
		int stackHash = stackKey.hashCode();
		filterStackSlots.computeIfAbsent(stackHash, k -> {
			onStackAdded.accept(stackHash);
			return new TreeSet<>();
		}).add(slot);
	}

	public void selectSlot(int slotNumber) {
		selectSlots(slotNumber, slotNumber + 1);
	}

	public void unselectSlot(int slotNumber) {
		unselectFilterItemSlot(slotNumber);
		unselectFilterStackSlot(slotNumber);
		save();
	}

	private void unselectFilterItemSlot(int slotNumber) {
		if (!data.slotFilterItems().containsKey(slotNumber)) {
			return;
		}

		Item item = data.slotFilterItems().remove(slotNumber);
		Set<Integer> itemSlots = filterItemSlots.get(item);
		itemSlots.remove(slotNumber);
		if (itemSlots.isEmpty()) {
			filterItemSlots.remove(item);
			onItemRemoved.accept(item);
		}
	}

	private void unselectFilterStackSlot(int slotNumber) {
		if (!data.slotFilterStacks().containsKey(slotNumber)) {
			return;
		}

		ItemStackKey isk = data.slotFilterStacks().remove(slotNumber);
		int stackHash = isk.hashCode();
		Set<Integer> stackSlots = filterStackSlots.get(stackHash);
		stackSlots.remove(slotNumber);
		if (stackSlots.isEmpty()) {
			filterStackSlots.remove(stackHash);
			onStackRemoved.accept(stackHash);
		}
	}

	public boolean ignoresNbt() {
		return data.ignoreNbt();
	}

	public void setIgnoreNbt(boolean ignoreNbt) {
		if (data.ignoreNbt() == ignoreNbt) {
			return;
		}

		Set<Integer> slotIndexes = getSlotIndexes();
		if (data.ignoreNbt() && !ignoreNbt) {
			data.slotFilterItems().forEach((slot, item) -> {
				ItemStack stack = inventoryHandlerSupplier.get().getStackInSlot(slot);
				if (stack.isEmpty()) {
					stack = new ItemStack(item);
				}
				addSlotStack(slot, stack);
			});
			unselectAllFilterItemSlots();
		} else {
			data.slotFilterStacks().forEach((slot, isk) -> {
				addSlotItem(slot, isk.stack().getItem());
			});
			unselectAllFilteStackSlots();
		}
		data.setIgnoreNbt(ignoreNbt);
		save();
		slotIndexes.forEach(this::selectSlot);
	}

	@Override
	public void reloadFrom(MemorySettingsCategoryData data) {
		this.data = data;
	}

	@Override
	public void overwriteWith(MemorySettingsCategory otherCategory) {
		unselectAllSlots();

		data.setIgnoreNbt(otherCategory.ignoresNbt());

		if (data.ignoreNbt()) {
			overwriteFilterItems(otherCategory);
		} else {
			overwriteFilterStacks(otherCategory);
		}
		save();
	}

	private void overwriteFilterStacks(MemorySettingsCategory otherCategory) {
		InventoryHandler inventoryHandler = getInventoryHandler();
		otherCategory.data.slotFilterStacks().forEach((slot, isk) -> {
			if (slot >= inventoryHandler.size()) {
				return;
			}

			ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
			if (stackInSlot.isEmpty() || otherCategory.matchesFilter(slot, stackInSlot)) {
				addSlotStack(slot, isk.stack());
			}
		});
	}

	private void overwriteFilterItems(MemorySettingsCategory otherCategory) {
		InventoryHandler inventoryHandler = getInventoryHandler();
		otherCategory.data.slotFilterItems().forEach((slot, item) -> {
			if (slot >= inventoryHandler.size()) {
				return;
			}

			ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
			if (stackInSlot.isEmpty() || otherCategory.matchesFilter(slot, stackInSlot)) {
				addSlotItem(slot, item);
			}
		});
	}

	public Set<Integer> getSlotIndexes() {
		HashSet<Integer> slots = new HashSet<>(data.slotFilterItems().keySet());
		slots.addAll(data.slotFilterStacks().keySet());
		return slots;
	}

	public Map<Item, Set<Integer>> getFilterItemSlots() {
		return filterItemSlots;
	}

	public Map<Integer, Set<Integer>> getFilterStackSlots() {
		return filterStackSlots;
	}

	public boolean matchesFilter(ItemStack stack) {
		return matchesFilter(stack.getItem(), ItemStack.hashItemAndComponents(stack));
	}

	private boolean matchesFilter(Item item, int componentHash) {
		return filterItemSlots.containsKey(item) || !filterStackSlots.isEmpty() && filterStackSlots.containsKey(componentHash);
	}

	public boolean matchesFilter(ItemResource resource) {
		return matchesFilter(resource.getItem(), ItemResourceHelper.hashItemAndComponents(resource));
	}

	public boolean matchesFilter(Item item, DataComponentMap components) {
		if (filterItemSlots.containsKey(item)) {
			return true;
		}
		int hash = 31 + item.hashCode();
		hash = 31 * hash + components.hashCode();
		return filterStackSlots.containsKey(hash);
	}

	public void registerListeners(Consumer<Item> onItemAdded, Consumer<Item> onItemRemoved, Consumer<Integer> onStackAdded, Consumer<Integer> onStackRemoved) {
		this.onItemAdded = onItemAdded;
		this.onItemRemoved = onItemRemoved;
		this.onStackAdded = onStackAdded;
		this.onStackRemoved = onStackRemoved;
	}

	public void unregisterListeners() {
		onItemAdded = i -> {
		};
		onItemRemoved = i -> {
		};
		onStackAdded = i -> {
		};
		onStackRemoved = i -> {
		};
	}

	public void setFilter(int slot, ItemStack filter) {
		InventoryHandler inventoryHandler = getInventoryHandler();
		if (slot < inventoryHandler.size()) {
			ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
			if (stackInSlot.isEmpty()) {
				if (data.ignoreNbt()) {
					Item item = filter.getItem();
					addSlotItem(slot, item);
				} else {
					addSlotStack(slot, filter);
				}
			}
		}
		save();
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return data.slotFilterItems().keySet().stream().anyMatch(slotIndex -> slotIndex >= slots) || data.slotFilterStacks().keySet().stream().anyMatch(slotIndex -> slotIndex >= slots);
	}

	@Override
	public void copyTo(MemorySettingsCategory otherCategory, int startFromSlot, int slotOffset) {
		data.slotFilterItems().forEach((slotIndex, item) -> {
			if (slotIndex < startFromSlot) {
				return;
			}
			otherCategory.data.slotFilterItems().put(slotIndex + slotOffset, item);
		});
		data.slotFilterStacks().forEach((slotIndex, isk) -> {
			if (slotIndex < startFromSlot) {
				return;
			}
			otherCategory.data.slotFilterStacks().put(slotIndex + slotOffset, isk);
		});
		otherCategory.save();
	}

	@Override
	public void deleteSlotSettingsFrom(int slotIndex) {
		data.removeFilterItemSlot(slotIndex);
		data.removeFilterStackSlot(slotIndex);
		save();
	}
}
