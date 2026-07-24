package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class InventorySorterTest {
	static {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
	}

	@Test
	void sortHandlerLeavesNoSortSlotsUntouched() {
		InventoryHandler inventoryHandler = initInventoryHandler(7,
				Map.of(0, stack(Items.IRON_NUGGET, 100), 1, stack(Items.IRON_INGOT, 10), 5, stack(Items.IRON_NUGGET, 10), 6, stack(Items.COBBLESTONE, 1)));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of(0, 1, 2, 3, 4));

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 100);
		assertStack(inventoryHandler, 1, Items.IRON_INGOT, 10);
		Assertions.assertTrue(inventoryHandler.getInternalStack(2).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(3).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(4).isEmpty());
		assertStack(inventoryHandler, 5, Items.IRON_NUGGET, 10);
		assertStack(inventoryHandler, 6, Items.COBBLESTONE, 1);
	}

	@Test
	void sortHandlerLeavesVisibleNoSortSlotsUntouched() {
		Map<Integer, ItemStack> visibleStacks = new HashMap<>(Map.of(0, stack(Items.IRON_NUGGET, 100)));
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(3,
				Map.of(0, stack(Items.IRON_NUGGET, 5), 1, stack(Items.COBBLESTONE, 1), 2, stack(Items.IRON_NUGGET, 10)), visibleStacks);

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
		Assertions.assertTrue(inventoryHandler.getInternalStack(2).isEmpty());
	}

	@Test
	void sortHandlerDoesNotMergeStacksIntoIgnoredSlots() {
		InventoryHandler inventoryHandler = initInventoryHandler(7,
				Map.of(0, stack(Items.IRON_NUGGET, 100), 1, stack(Items.IRON_INGOT, 10), 5, stack(Items.IRON_NUGGET, 10), 6, stack(Items.COBBLESTONE, 1)));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of(), Set.of(0, 1, 2, 3, 4));

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 100);
		assertStack(inventoryHandler, 1, Items.IRON_INGOT, 10);
		Assertions.assertTrue(inventoryHandler.getInternalStack(2).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(3).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(4).isEmpty());
		assertStack(inventoryHandler, 5, Items.IRON_NUGGET, 10);
		assertStack(inventoryHandler, 6, Items.COBBLESTONE, 1);
	}

	@Test
	void sortHandlerDoesNotMoveStacksIntoInaccessibleSlots() {
		InventoryHandler inventoryHandler = initInventoryHandler(7,
				Map.of(0, stack(Items.IRON_NUGGET, 100), 5, stack(Items.COBBLESTONE, 1), 6, stack(Items.IRON_INGOT, 10)), Set.of(0, 1, 2, 3, 4));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of());

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 100);
		Assertions.assertTrue(inventoryHandler.getInternalStack(1).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(2).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(3).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(4).isEmpty());
		assertStack(inventoryHandler, 5, Items.IRON_INGOT, 10);
		assertStack(inventoryHandler, 6, Items.COBBLESTONE, 1);
	}

	@Test
	void sortHandlerFillsMatchingMemorizedSlotsBeforeOtherSlots() {
		InventoryHandler inventoryHandler = initInventoryHandler(4,
				Map.of(0, stack(Items.COBBLESTONE, 100), 1, stack(Items.COBBLESTONE, 100), 2, stack(Items.IRON_INGOT, 1), 3, stack(Items.COBBLESTONE, 100)));
		MemorySettingsCategory memorySettings = initMemorySettings(Map.of(0, stack(Items.COBBLESTONE, 1), 1, stack(Items.COBBLESTONE, 1)), false);

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(), memorySettings.getSlotIndexes(), memorySettings::matchesFilter);

		assertStack(inventoryHandler, 0, Items.COBBLESTONE, 256);
		assertStack(inventoryHandler, 1, Items.COBBLESTONE, 44);
		assertStack(inventoryHandler, 2, Items.IRON_INGOT, 1);
		Assertions.assertTrue(inventoryHandler.getInternalStack(3).isEmpty());
	}

	@Test
	void sortHandlerMovesNonMatchingMemorizedContentsToOtherSlots() {
		InventoryHandler inventoryHandler = initInventoryHandler(3,
				Map.of(0, stack(Items.IRON_INGOT, 5), 1, stack(Items.COBBLESTONE, 10), 2, stack(Items.DIRT, 1)));
		MemorySettingsCategory memorySettings = initMemorySettings(Map.of(0, stack(Items.COBBLESTONE, 1)), false);

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(), memorySettings.getSlotIndexes(), memorySettings::matchesFilter);

		assertStack(inventoryHandler, 0, Items.COBBLESTONE, 10);
		assertStack(inventoryHandler, 1, Items.DIRT, 1);
		assertStack(inventoryHandler, 2, Items.IRON_INGOT, 5);
	}

	@Test
	void sortHandlerPrioritizesNoSortOverMemorizedSlots() {
		InventoryHandler inventoryHandler = initInventoryHandler(3,
				Map.of(0, stack(Items.COBBLESTONE, 10), 1, stack(Items.IRON_INGOT, 5), 2, stack(Items.DIRT, 1)));
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
		InventoryHandler inventoryHandler = initInventoryHandler(2, Map.of(0, firstSword, 1, secondSword));
		MemorySettingsCategory memorySettings = initMemorySettings(Map.of(0, stack(Items.DIAMOND_SWORD, 1), 1, stack(Items.DIAMOND_SWORD, 1)), true);

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_NAME, Set.of(), memorySettings.getSlotIndexes(), memorySettings::matchesFilter);

		ItemStack sortedFirstSword = inventoryHandler.getInternalStack(0);
		ItemStack sortedSecondSword = inventoryHandler.getInternalStack(1);
		Assertions.assertEquals(Items.DIAMOND_SWORD, sortedFirstSword.getItem());
		Assertions.assertEquals(Items.DIAMOND_SWORD, sortedSecondSword.getItem());
		Assertions.assertFalse(ItemStack.isSameItemSameComponents(sortedFirstSword, sortedSecondSword));
	}

	private static InventoryHandler initInventoryHandler(int slots, Map<Integer, ItemStack> initialState) {
		return initInventoryHandler(slots, initialState, Set.of());
	}

	private static InventoryHandler initInventoryHandler(int slots, Map<Integer, ItemStack> initialState, Set<Integer> inaccessibleSlots) {
		StackUpgradeConfig stackUpgradeConfigMock = Mockito.mock(StackUpgradeConfig.class);
		when(stackUpgradeConfigMock.canStackItem(any(Item.class))).thenReturn(true);
		return new InventoryHandler(slots, NoopStorageWrapper.INSTANCE, getContainerContents(slots, initialState), () -> {
		}, 256, stackUpgradeConfigMock) {
			@Override
			protected boolean isAllowed(ItemResource resource) {
				return true;
			}

			@Override
			public boolean isSlotAccessible(int slot) {
				return !inaccessibleSlots.contains(slot);
			}
		};
	}

	private static InventoryHandler initInventoryHandlerWithVisibleStacks(int slots, Map<Integer, ItemStack> initialState,
			Map<Integer, ItemStack> visibleStacks) {
		return initInventoryHandlerWithVisibleStacks(slots, initialState, visibleStacks, Set.of());
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

	private static InventoryHandler initInventoryHandlerWithVisibleStacks(int slots, Map<Integer, ItemStack> initialState,
			Map<Integer, ItemStack> visibleStacks, Set<Integer> infiniteSlots) {
		StackUpgradeConfig stackUpgradeConfigMock = Mockito.mock(StackUpgradeConfig.class);
		when(stackUpgradeConfigMock.canStackItem(any(Item.class))).thenReturn(true);
		return new InventoryHandler(slots, NoopStorageWrapper.INSTANCE, getContainerContents(slots, initialState), () -> {
		}, 256, stackUpgradeConfigMock) {
			@Override
			protected boolean isAllowed(ItemResource resource) {
				return true;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return visibleStacks.getOrDefault(slot, super.getStackInSlot(slot));
			}

			@Override
			public long getCapacityAsLong(int index, ItemResource resource) {
				return visibleStacks.containsKey(index) ? 256 : super.getCapacityAsLong(index, resource);
			}

			@Override
			public boolean isInfinite(int slot) {
				return infiniteSlots.contains(slot);
			}

			@Override
			public void setStackInSlot(int slot, ItemStack stack) {
				if (visibleStacks.containsKey(slot) && !infiniteSlots.contains(slot)) {
					visibleStacks.put(slot, stack.copy());
					return;
				}
				if (infiniteSlots.contains(slot) && !getInternalStack(slot).isEmpty()) {
					return;
				}
				super.setStackInSlot(slot, stack);
			}
		};
	}

	private static ContainerContents getContainerContents(int slots, Map<Integer, ItemStack> initialState) {
		NonNullList<ItemStack> stacks = NonNullList.withSize(slots, ItemStack.EMPTY);
		initialState.forEach(stacks::set);
		ContainerContents containerContents = new ContainerContents();
		containerContents.inventory().reloadFrom(new ContainerContents.InventoryData(stacks));
		return containerContents;
	}

	private static ItemStack stack(Item item, int count) {
		return new ItemStack(item, count);
	}

	private static void assertStack(InventoryHandler inventoryHandler, int slot, Item item, int count) {
		ItemStack stack = inventoryHandler.getInternalStack(slot);
		Assertions.assertEquals(item, stack.getItem());
		Assertions.assertEquals(count, stack.getCount());
	}

	private static void assertStack(Map<Integer, ItemStack> stacks, int slot, Item item, int count) {
		ItemStack stack = stacks.get(slot);
		Assertions.assertEquals(item, stack.getItem());
		Assertions.assertEquals(count, stack.getCount());
	}

}
