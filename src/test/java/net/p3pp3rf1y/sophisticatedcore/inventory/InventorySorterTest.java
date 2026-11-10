package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class InventorySorterTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void sortHandlerLeavesNoSortSlotsUntouched() {
		Map<Integer, ItemStack> visibleStacks = new HashMap<>(Map.of(0, stack(Items.IRON_NUGGET, 100)));
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(3,
				Map.of(0, stack(Items.IRON_NUGGET, 5), 1, stack(Items.COBBLESTONE, 1), 2, stack(Items.IRON_NUGGET, 10)), visibleStacks, Set.of());

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(0));

		assertStack(visibleStacks, 0, Items.IRON_NUGGET, 100);
		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 5);
		assertStack(inventoryHandler, 1, Items.COBBLESTONE, 1);
		assertStack(inventoryHandler, 2, Items.IRON_NUGGET, 10);
	}

	@Test
	void sortHandlerLeavesInfiniteNoSortSlotsUntouched() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(3,
				Map.of(0, stack(Items.IRON_NUGGET, 5), 1, stack(Items.COBBLESTONE, 1), 2, stack(Items.IRON_NUGGET, 10)),
				new HashMap<>(Map.of(0, stack(Items.IRON_NUGGET, Integer.MAX_VALUE))), Set.of(0));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(0));

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 5);
		assertStack(inventoryHandler, 1, Items.COBBLESTONE, 1);
		assertStack(inventoryHandler, 2, Items.IRON_NUGGET, 10);
	}

	@Test
	void sortHandlerSortsInfiniteSlotsWithoutLeavingOriginalStacks() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(3, Map.of(0, stack(Items.COBBLESTONE, 1), 1, stack(Items.IRON_NUGGET, 10)),
				new HashMap<>(Map.of(0, stack(Items.COBBLESTONE, Integer.MAX_VALUE), 1, stack(Items.IRON_NUGGET, Integer.MAX_VALUE))), Set.of(0, 1));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of());

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 10);
		assertStack(inventoryHandler, 1, Items.COBBLESTONE, 1);
		Assertions.assertTrue(inventoryHandler.getSlotStack(2).isEmpty());
	}

	@Test
	void sortHandlerDoesNotMoveStacksIntoInaccessibleSlots() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(7,
				Map.of(0, stack(Items.IRON_NUGGET, 100), 5, stack(Items.COBBLESTONE, 1), 6, stack(Items.IRON_INGOT, 10)), new HashMap<>(), Set.of(),
				Set.of(0, 1, 2, 3, 4));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of());

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 100);
		Assertions.assertTrue(inventoryHandler.getSlotStack(1).isEmpty());
		Assertions.assertTrue(inventoryHandler.getSlotStack(2).isEmpty());
		Assertions.assertTrue(inventoryHandler.getSlotStack(3).isEmpty());
		Assertions.assertTrue(inventoryHandler.getSlotStack(4).isEmpty());
		assertStack(inventoryHandler, 5, Items.IRON_INGOT, 10);
		assertStack(inventoryHandler, 6, Items.COBBLESTONE, 1);
	}

	@Test
	void sortHandlerFillsMatchingMemorizedSlotsBeforeOtherSlots() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(4,
				Map.of(0, stack(Items.COBBLESTONE, 100), 1, stack(Items.COBBLESTONE, 100), 2, stack(Items.IRON_INGOT, 1), 3, stack(Items.COBBLESTONE, 100)),
				new HashMap<>(), Set.of());
		MemorySettingsCategory memorySettings = initMemorySettings(Map.of(0, stack(Items.COBBLESTONE, 1), 1, stack(Items.COBBLESTONE, 1)), false);

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(), memorySettings.getSlotIndexes(), memorySettings::matchesFilter);

		assertStack(inventoryHandler, 0, Items.COBBLESTONE, 256);
		assertStack(inventoryHandler, 1, Items.COBBLESTONE, 44);
		assertStack(inventoryHandler, 2, Items.IRON_INGOT, 1);
		Assertions.assertTrue(inventoryHandler.getSlotStack(3).isEmpty());
	}

	@Test
	void sortHandlerMovesNonMatchingMemorizedContentsToOtherSlots() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(3,
				Map.of(0, stack(Items.IRON_INGOT, 5), 1, stack(Items.COBBLESTONE, 10), 2, stack(Items.DIRT, 1)), new HashMap<>(), Set.of());
		MemorySettingsCategory memorySettings = initMemorySettings(Map.of(0, stack(Items.COBBLESTONE, 1)), false);

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(), memorySettings.getSlotIndexes(), memorySettings::matchesFilter);

		assertStack(inventoryHandler, 0, Items.COBBLESTONE, 10);
		assertStack(inventoryHandler, 1, Items.DIRT, 1);
		assertStack(inventoryHandler, 2, Items.IRON_INGOT, 5);
	}

	@Test
	void sortHandlerPrioritizesNoSortOverMemorizedSlots() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(3,
				Map.of(0, stack(Items.COBBLESTONE, 10), 1, stack(Items.IRON_INGOT, 5), 2, stack(Items.DIRT, 1)), new HashMap<>(), Set.of());
		MemorySettingsCategory memorySettings = initMemorySettings(Map.of(0, stack(Items.COBBLESTONE, 1)), false);

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(0), memorySettings.getSlotIndexes(), memorySettings::matchesFilter);

		assertStack(inventoryHandler, 0, Items.COBBLESTONE, 10);
		assertStack(inventoryHandler, 1, Items.DIRT, 1);
		assertStack(inventoryHandler, 2, Items.IRON_INGOT, 5);
	}

	@Test
	void sortHandlerKeepsComponentDistinctStacksInSeparateMemorizedSlots() {
		ItemStack firstSword = stack(Items.DIAMOND_SWORD, 1);
		firstSword.setDamageValue(1);
		ItemStack secondSword = stack(Items.DIAMOND_SWORD, 1);
		secondSword.setDamageValue(2);
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(2, Map.of(0, firstSword, 1, secondSword), new HashMap<>(), Set.of());
		MemorySettingsCategory memorySettings = initMemorySettings(Map.of(0, stack(Items.DIAMOND_SWORD, 1), 1, stack(Items.DIAMOND_SWORD, 1)), true);

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(), memorySettings.getSlotIndexes(), memorySettings::matchesFilter);

		ItemStack sortedFirstSword = inventoryHandler.getSlotStack(0);
		ItemStack sortedSecondSword = inventoryHandler.getSlotStack(1);
		Assertions.assertEquals(Items.DIAMOND_SWORD, sortedFirstSword.getItem());
		Assertions.assertEquals(Items.DIAMOND_SWORD, sortedSecondSword.getItem());
		Assertions.assertFalse(ItemStack.isSameItemSameComponents(sortedFirstSword, sortedSecondSword));
	}

	@Test
	void sortHandlerIgnoresOutOfRangeMemorizedSlotsWithoutChangingCallerSet() {
		ItemStack firstSword = stack(Items.DIAMOND_SWORD, 1);
		firstSword.setDamageValue(1);
		ItemStack secondSword = stack(Items.DIAMOND_SWORD, 1);
		secondSword.setDamageValue(2);
		ItemStackHandler handler = new ItemStackHandler(3);
		handler.setStackInSlot(0, firstSword);
		handler.setStackInSlot(1, stack(Items.DIRT, 2));
		handler.setStackInSlot(2, secondSword);
		Set<Integer> memorizedSlots = new HashSet<>(Arrays.asList(-1, 1, 3));
		Map<ItemStackKey, Integer> expectedContents = getContents(handler);

		InventorySorter.sortHandler(handler, InventorySorter.BY_NAME, Set.of(), memorizedSlots, (slot, stack) -> slot == 1 && stack.is(Items.DIRT));

		assertStack(handler, 1, Items.DIRT, 2);
		Assertions.assertEquals(expectedContents, getContents(handler));
		Assertions.assertEquals(Set.of(-1, 1, 3), memorizedSlots);
	}

	@Test
	void sortHandlerUsesRetainedMemoryAfterCapacityIsRestored() {
		AtomicInteger slots = new AtomicInteger(3);
		InventoryHandler settingsInventory = Mockito.mock(InventoryHandler.class);
		when(settingsInventory.getSlots()).thenAnswer(invocation -> slots.get());
		when(settingsInventory.getStackInSlot(2)).thenReturn(stack(Items.COBBLESTONE, 1));
		CompoundTag memoryNbt = new CompoundTag();
		MemorySettingsCategory memorySettings = new MemorySettingsCategory(() -> settingsInventory, memoryNbt, tag -> {
		});
		memorySettings.selectSlot(2);
		slots.set(2);
		CompoundTag savedMemoryNbt = memoryNbt.copy();
		CompoundTag reloadedMemoryNbt = memoryNbt.copy();
		MemorySettingsCategory reloadedMemorySettings = new MemorySettingsCategory(() -> settingsInventory, reloadedMemoryNbt, tag -> {
		});
		ItemStackHandler reducedHandler = new ItemStackHandler(2);
		ItemStack reducedSword = stack(Items.DIAMOND_SWORD, 1);
		reducedSword.setDamageValue(1);
		reducedHandler.setStackInSlot(0, stack(Items.COBBLESTONE, 2));
		reducedHandler.setStackInSlot(1, reducedSword);
		Map<ItemStackKey, Integer> reducedContents = getContents(reducedHandler);

		InventorySorter.sortHandler(reducedHandler, InventorySorter.BY_NAME, Set.of(), reloadedMemorySettings.getSlotIndexes(),
				reloadedMemorySettings::matchesFilter);

		Assertions.assertEquals(Set.of(2), reloadedMemorySettings.getSlotIndexes());
		Assertions.assertTrue(reloadedMemorySettings.matchesFilter(2, stack(Items.COBBLESTONE, 1)));
		Assertions.assertEquals(savedMemoryNbt, reloadedMemoryNbt);
		Assertions.assertEquals(reducedContents, getContents(reducedHandler));
		slots.set(3);
		ItemStackHandler restoredHandler = new ItemStackHandler(3);
		ItemStack restoredSword = stack(Items.DIAMOND_SWORD, 1);
		restoredSword.setDamageValue(2);
		restoredHandler.setStackInSlot(0, stack(Items.COBBLESTONE, 1));
		restoredHandler.setStackInSlot(1, restoredSword);
		restoredHandler.setStackInSlot(2, stack(Items.DIRT, 1));
		Map<ItemStackKey, Integer> restoredContents = getContents(restoredHandler);

		InventorySorter.sortHandler(restoredHandler, InventorySorter.BY_NAME, Set.of(), reloadedMemorySettings.getSlotIndexes(),
				reloadedMemorySettings::matchesFilter);

		assertStack(restoredHandler, 2, Items.COBBLESTONE, 1);
		Assertions.assertEquals(restoredContents, getContents(restoredHandler));
	}

	@Test
	void sortHandlerIgnoresOutOfRangeMemorizedSlotsForInventoryHandler() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(3,
				Map.of(0, stack(Items.IRON_INGOT, 5), 1, stack(Items.DIRT, 2), 2, stack(Items.COBBLESTONE, 10)), new HashMap<>(), Set.of(), Set.of(2));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(0), Set.of(-1, 0, 1, 2, 3),
				(slot, stack) -> slot == 1 && stack.is(Items.DIRT));

		assertStack(inventoryHandler, 0, Items.IRON_INGOT, 5);
		assertStack(inventoryHandler, 1, Items.DIRT, 2);
		assertStack(inventoryHandler, 2, Items.COBBLESTONE, 10);
	}

	private static InventoryHandler initInventoryHandlerWithVisibleStacks(int slots, Map<Integer, ItemStack> initialState,
			Map<Integer, ItemStack> visibleStacks, Set<Integer> infiniteSlots) {
		return initInventoryHandlerWithVisibleStacks(slots, initialState, visibleStacks, infiniteSlots, Set.of());
	}

	private static InventoryHandler initInventoryHandlerWithVisibleStacks(int slots, Map<Integer, ItemStack> initialState,
			Map<Integer, ItemStack> visibleStacks, Set<Integer> infiniteSlots, Set<Integer> inaccessibleSlots) {
		Map<Integer, ItemStack> internalStacks = new HashMap<>();
		initialState.forEach((slot, stack) -> internalStacks.put(slot, stack.copy()));

		InventoryHandler inventoryHandler = Mockito.mock(InventoryHandler.class);
		when(inventoryHandler.getSlots()).thenReturn(slots);
		when(inventoryHandler.isSlotAccessible(anyInt()))
				.thenAnswer(invocation -> !inaccessibleSlots.contains(getBoundedSlot(invocation.getArgument(0), slots)));
		when(inventoryHandler.getBaseStackLimit(any(ItemStack.class))).thenReturn(256);
		when(inventoryHandler.getStackLimit(anyInt(), any(ItemStack.class))).thenAnswer(invocation -> {
			getBoundedSlot(invocation.getArgument(0), slots);
			return 256;
		});
		when(inventoryHandler.getSlotLimit(anyInt())).thenAnswer(invocation -> {
			getBoundedSlot(invocation.getArgument(0), slots);
			return 256;
		});
		when(inventoryHandler.isInfinite(anyInt())).thenAnswer(invocation -> infiniteSlots.contains(getBoundedSlot(invocation.getArgument(0), slots)));
		when(inventoryHandler.getSlotStack(anyInt()))
				.thenAnswer(invocation -> internalStacks.getOrDefault(getBoundedSlot(invocation.getArgument(0), slots), ItemStack.EMPTY));
		when(inventoryHandler.getStackInSlot(anyInt())).thenAnswer(invocation -> {
			int slot = getBoundedSlot(invocation.getArgument(0), slots);
			return visibleStacks.getOrDefault(slot, internalStacks.getOrDefault(slot, ItemStack.EMPTY));
		});
		Mockito.doAnswer(invocation -> {
			internalStacks.put(getBoundedSlot(invocation.getArgument(0), slots), invocation.getArgument(1));
			return null;
		}).when(inventoryHandler).setSlotStack(anyInt(), any(ItemStack.class));
		Mockito.doAnswer(invocation -> {
			int slot = getBoundedSlot(invocation.getArgument(0), slots);
			ItemStack stack = invocation.getArgument(1);
			if (visibleStacks.containsKey(slot) && !infiniteSlots.contains(slot)) {
				visibleStacks.put(slot, stack.copy());
			} else if (!infiniteSlots.contains(slot) || internalStacks.getOrDefault(slot, ItemStack.EMPTY).isEmpty()) {
				internalStacks.put(slot, stack.copy());
			}
			return null;
		}).when(inventoryHandler).setStackInSlot(anyInt(), any(ItemStack.class));

		return inventoryHandler;
	}

	private static int getBoundedSlot(int slot, int slots) {
		if (slot < 0 || slot >= slots) {
			throw new IndexOutOfBoundsException("Slot " + slot + " is outside handler bounds");
		}
		return slot;
	}

	private static MemorySettingsCategory initMemorySettings(Map<Integer, ItemStack> filters, boolean ignoreComponents) {
		MemorySettingsCategory memorySettings = Mockito.mock(MemorySettingsCategory.class);
		when(memorySettings.getSlotIndexes()).thenReturn(filters.keySet());
		when(memorySettings.matchesFilter(anyInt(), any(ItemStack.class))).thenAnswer(invocation -> {
			ItemStack filter = filters.get(invocation.getArgument(0));
			ItemStack stack = invocation.getArgument(1);
			return ignoreComponents ? filter.getItem() == stack.getItem() : ItemStack.isSameItemSameComponents(filter, stack);
		});
		return memorySettings;
	}

	private static ItemStack stack(Item item, int count) {
		return new ItemStack(item, count);
	}

	private static Map<ItemStackKey, Integer> getContents(ItemStackHandler handler) {
		Map<ItemStackKey, Integer> contents = new HashMap<>();
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (!stack.isEmpty()) {
				contents.merge(ItemStackKey.of(stack), stack.getCount(), Integer::sum);
			}
		}
		return contents;
	}

	private static void assertStack(InventoryHandler inventoryHandler, int slot, Item item, int count) {
		assertStack(Map.of(slot, inventoryHandler.getSlotStack(slot)), slot, item, count);
	}

	private static void assertStack(ItemStackHandler inventoryHandler, int slot, Item item, int count) {
		assertStack(Map.of(slot, inventoryHandler.getStackInSlot(slot)), slot, item, count);
	}

	private static void assertStack(Map<Integer, ItemStack> stacks, int slot, Item item, int count) {
		ItemStack stack = stacks.get(slot);
		Assertions.assertEquals(item, stack.getItem());
		Assertions.assertEquals(count, stack.getCount());
	}
}
