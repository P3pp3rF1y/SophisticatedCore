package net.p3pp3rf1y.sophisticatedcore.settings.memory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MemorySettingsCategory implements ISettingsCategory {
	public static final String NAME = "memory";
	private static final String SLOT_FILTER_ITEMS_TAG = "slotFilterItems";
	private static final String SLOT_FILTER_STACKS_TAG = "slotFilterStacks";
	private static final String IGNORE_NBT_TAG = "ignoreNbt";
	private final Supplier<InventoryHandler> inventoryHandlerSupplier;
	private CompoundTag categoryNbt;
	private final Consumer<CompoundTag> saveNbt;
	private final Map<Integer, Item> slotFilterItems = new TreeMap<>();
	private final Map<Integer, ItemStackKey> slotFilterStacks = new TreeMap<>();
	private final Map<Item, Set<Integer>> filterItemSlots = new HashMap<>();

	private final Map<Integer, Set<Integer>> filterStackSlots = new HashMap<>();

	private boolean ignoreNbt = true;
	private Consumer<Item> onItemAdded = i -> {};

	private Consumer<Integer> onStackAdded = i -> {};
	private Consumer<Item> onItemRemoved = i -> {};
	private Consumer<Integer> onStackRemoved = i -> {};
	public MemorySettingsCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, CompoundTag categoryNbt, Consumer<CompoundTag> saveNbt) {
		this.inventoryHandlerSupplier = inventoryHandlerSupplier;
		this.categoryNbt = categoryNbt;
		this.saveNbt = saveNbt;

		deserialize();
	}

	private void deserialize() {
		NBTHelper.getMap(categoryNbt, SLOT_FILTER_ITEMS_TAG,
						Integer::valueOf,
						(k, v) -> Optional.ofNullable(ForgeRegistries.ITEMS.getValue(new ResourceLocation(v.getAsString()))))
				.ifPresent(map -> map.forEach(this::addSlotItem));

		NBTHelper.getMap(categoryNbt, SLOT_FILTER_STACKS_TAG,
						Integer::valueOf,
						(k, v) -> v instanceof CompoundTag tag ? Optional.of(ItemStack.of(tag)) : Optional.empty())
				.ifPresent(map -> map.forEach(this::addSlotStack));
		ignoreNbt = NBTHelper.getBoolean(categoryNbt, IGNORE_NBT_TAG).orElse(true);
	}

	public boolean matchesFilter(int slotNumber, ItemStack stack) {
		if (slotFilterItems.containsKey(slotNumber)) {
			return !stack.isEmpty() && stack.getItem() == slotFilterItems.get(slotNumber);
		}
		if (slotFilterStacks.containsKey(slotNumber)) {
			return !stack.isEmpty() && slotFilterStacks.get(slotNumber).matches(stack);
		}

		return true;
	}

	public Optional<ItemStack> getSlotFilterStack(int slotNumber, boolean copy) {
		if (slotFilterItems.containsKey(slotNumber)) {
			return Optional.of(new ItemStack(slotFilterItems.get(slotNumber)));
		}
		if (slotFilterStacks.containsKey(slotNumber)) {
			ItemStack filterStack = slotFilterStacks.get(slotNumber).getStack();
			return Optional.of(copy ? filterStack.copy() : filterStack);
		}

		return Optional.empty();
	}

	public boolean isSlotSelected(int slotNumber) {
		return slotFilterItems.containsKey(slotNumber) || slotFilterStacks.containsKey(slotNumber);
	}

	public void unselectAllSlots() {
		unselectAllFilterItemSlots();
		unselectAllFilteStackSlots();

		serializeFilterItems();
	}

	private void unselectAllFilteStackSlots() {
		filterStackSlots.keySet().forEach(i -> onStackRemoved.accept(i));
		slotFilterStacks.clear();
		filterStackSlots.clear();
	}

	private void unselectAllFilterItemSlots() {
		filterItemSlots.keySet().forEach(i -> onItemRemoved.accept(i));
		slotFilterItems.clear();
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
			if (slot < inventoryHandler.getSlots()) {
				ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
				if (!stackInSlot.isEmpty()) {
					if (ignoreNbt) {
						Item item = stackInSlot.getItem();
						addSlotItem(slot, item);
					} else {
						addSlotStack(slot, stackInSlot);
					}
				}
			}
		}
		serializeFilterItems();
	}

	private InventoryHandler getInventoryHandler() {
		return inventoryHandlerSupplier.get();
	}

	private void addSlotItem(int slot, Item item) {
		slotFilterItems.put(slot, item);
		filterItemSlots.computeIfAbsent(item, k -> {
			onItemAdded.accept(k);
			return new TreeSet<>();
		}).add(slot);
	}

	private void addSlotStack(int slot, ItemStack stack) {
		ItemStackKey isk = new ItemStackKey(stack);
		slotFilterStacks.put(slot, isk);
		int stackHash = isk.hashCode();
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
		serializeFilterItems();
	}

	private void unselectFilterItemSlot(int slotNumber) {
		Item item = slotFilterItems.remove(slotNumber);
		Set<Integer> itemSlots = filterItemSlots.get(item);
		itemSlots.remove(slotNumber);
		if (itemSlots.isEmpty()) {
			filterItemSlots.remove(item);
			onItemRemoved.accept(item);
		}
	}

	private void unselectFilterStackSlot(int slotNumber) {
		ItemStackKey isk = slotFilterStacks.remove(slotNumber);
		int stackHash = isk.hashCode();
		Set<Integer> stackSlots = filterStackSlots.get(stackHash);
		stackSlots.remove(slotNumber);
		if (stackSlots.isEmpty()) {
			filterStackSlots.remove(stackHash);
			onStackRemoved.accept(stackHash);
		}
	}

	public boolean ignoresNbt() {
		return ignoreNbt;
	}

	public void setIgnoreNbt(boolean ignoreNbt) {
		if (this.ignoreNbt == ignoreNbt) {
			return;
		}

		Set<Integer> slotIndexes = getSlotIndexes();
		if (this.ignoreNbt && !ignoreNbt) {
			slotFilterItems.forEach((slot, item) -> {
				ItemStack stack = inventoryHandlerSupplier.get().getStackInSlot(slot);
				if (stack.isEmpty()) {
					stack = new ItemStack(item);
				}
				addSlotStack(slot, stack);
			});
			unselectAllFilterItemSlots();
		} else {
			slotFilterStacks.forEach((slot, isk) -> addSlotItem(slot, isk.getStack().getItem()));
			unselectAllFilteStackSlots();
		}
		serializeFilterItems();

		this.ignoreNbt = ignoreNbt;
		categoryNbt.putBoolean(IGNORE_NBT_TAG, ignoreNbt);
		saveNbt.accept(categoryNbt);
		slotIndexes.forEach(this::selectSlot);
	}

	private void serializeFilterItems() {
		//noinspection ConstantConditions - item registry name exists in this content otherwise player wouldn't be able to work with it
		NBTHelper.putMap(categoryNbt, SLOT_FILTER_ITEMS_TAG, slotFilterItems, String::valueOf, i -> StringTag.valueOf(ForgeRegistries.ITEMS.getKey(i).toString()));
		NBTHelper.putMap(categoryNbt, SLOT_FILTER_STACKS_TAG, slotFilterStacks, String::valueOf, isk -> isk.stack().save(new CompoundTag()));
		saveNbt.accept(categoryNbt);
	}

	@Override
	public void reloadFrom(CompoundTag categoryNbt) {
		this.categoryNbt = categoryNbt;
		slotFilterItems.clear();
		filterItemSlots.clear();
		slotFilterStacks.clear();
		filterStackSlots.clear();
		deserialize();
	}

	public Set<Integer> getSlotIndexes() {
		HashSet<Integer> slots = new HashSet<>(slotFilterItems.keySet());
		slots.addAll(slotFilterStacks.keySet());
		return slots;
	}

	public Map<Item, Set<Integer>> getFilterItemSlots() {
		return filterItemSlots;
	}

	public Map<Integer, Set<Integer>> getFilterStackSlots() {
		return filterStackSlots;
	}

	public boolean matchesFilter(ItemStack stack) {
		return filterItemSlots.containsKey(stack.getItem()) || (!filterStackSlots.isEmpty() && filterStackSlots.containsKey(ItemStackKey.getHashCode(stack)));
	}

	public void registerListeners(Consumer<Item> onItemAdded, Consumer<Item> onItemRemoved, Consumer<Integer> onStackAdded, Consumer<Integer> onStackRemoved) {
		this.onItemAdded = onItemAdded;
		this.onItemRemoved = onItemRemoved;
		this.onStackAdded = onStackAdded;
		this.onStackRemoved = onStackRemoved;
	}

	public void unregisterListeners() {
		onItemAdded = i -> {};
		onItemRemoved = i -> {};
		onStackAdded = i -> {};
		onStackRemoved = i -> {};
	}
}
