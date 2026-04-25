package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InventorySorterTest {
	static {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
	}

	@Test
	void sortHandlerTopsUpNoSortSlots() {
		InventoryHandler inventoryHandler = initInventoryHandler(7, Map.of(
				0, stack(Items.IRON_NUGGET, 100),
				1, stack(Items.IRON_INGOT, 10),
				5, stack(Items.IRON_NUGGET, 10),
				6, stack(Items.COBBLESTONE, 1)
		));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of(0, 1, 2, 3, 4));

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 110);
		assertStack(inventoryHandler, 1, Items.IRON_INGOT, 10);
		Assertions.assertTrue(inventoryHandler.getInternalStack(2).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(3).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(4).isEmpty());
		assertStack(inventoryHandler, 5, Items.COBBLESTONE, 1);
		Assertions.assertTrue(inventoryHandler.getInternalStack(6).isEmpty());
	}

	@Test
	void sortHandlerTopsUpNoSortSlotsUsingVisibleCount() {
		Map<Integer, ItemStack> visibleStacks = new HashMap<>(Map.of(0, stack(Items.IRON_NUGGET, 100)));
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(7, Map.of(
				0, stack(Items.IRON_NUGGET, 5),
				5, stack(Items.IRON_NUGGET, 10),
				6, stack(Items.COBBLESTONE, 1)
		), visibleStacks);

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of(0, 1, 2, 3, 4));

		assertStack(visibleStacks, 0, Items.IRON_NUGGET, 110);
		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 5);
		assertStack(inventoryHandler, 5, Items.COBBLESTONE, 1);
		Assertions.assertTrue(inventoryHandler.getInternalStack(6).isEmpty());
	}

	@Test
	void sortHandlerTopsUpInfiniteNoSortSlotsUsingInternalCount() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(7, Map.of(
				0, stack(Items.IRON_NUGGET, 5),
				5, stack(Items.IRON_NUGGET, 10),
				6, stack(Items.COBBLESTONE, 1)
		), new HashMap<>(Map.of(0, stack(Items.IRON_NUGGET, Integer.MAX_VALUE))), Set.of(0));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of(0, 1, 2, 3, 4));

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 15);
		assertStack(inventoryHandler, 5, Items.COBBLESTONE, 1);
		Assertions.assertTrue(inventoryHandler.getInternalStack(6).isEmpty());
	}

	@Test
	void sortHandlerSortsInfiniteSlotsWithoutLeavingOriginalStacks() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(3, Map.of(
				0, stack(Items.COBBLESTONE, 1),
				1, stack(Items.IRON_NUGGET, 10)
		), new HashMap<>(Map.of(
				0, stack(Items.COBBLESTONE, Integer.MAX_VALUE),
				1, stack(Items.IRON_NUGGET, Integer.MAX_VALUE)
		)), Set.of(0, 1));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of());

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 10);
		assertStack(inventoryHandler, 1, Items.COBBLESTONE, 1);
		Assertions.assertTrue(inventoryHandler.getInternalStack(2).isEmpty());
	}

	@Test
	void sortHandlerDoesNotMergeStacksIntoIgnoredSlots() {
		InventoryHandler inventoryHandler = initInventoryHandler(7, Map.of(
				0, stack(Items.IRON_NUGGET, 100),
				1, stack(Items.IRON_INGOT, 10),
				5, stack(Items.IRON_NUGGET, 10),
				6, stack(Items.COBBLESTONE, 1)
		));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of(), Set.of(0, 1, 2, 3, 4));

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 100);
		assertStack(inventoryHandler, 1, Items.IRON_INGOT, 10);
		Assertions.assertTrue(inventoryHandler.getInternalStack(2).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(3).isEmpty());
		Assertions.assertTrue(inventoryHandler.getInternalStack(4).isEmpty());
		assertStack(inventoryHandler, 5, Items.IRON_NUGGET, 10);
		assertStack(inventoryHandler, 6, Items.COBBLESTONE, 1);
	}

	private static InventoryHandler initInventoryHandler(int slots, Map<Integer, ItemStack> initialState) {
		StackUpgradeConfig stackUpgradeConfigMock = Mockito.mock(StackUpgradeConfig.class);
		when(stackUpgradeConfigMock.canStackItem(any(Item.class))).thenReturn(true);
		return new InventoryHandler(slots, NoopStorageWrapper.INSTANCE, getContainerContents(slots, initialState), () -> {}, 256, stackUpgradeConfigMock) {
			@Override
			protected boolean isAllowed(ItemResource resource) {
				return true;
			}
		};
	}

	private static InventoryHandler initInventoryHandlerWithVisibleStacks(int slots, Map<Integer, ItemStack> initialState, Map<Integer, ItemStack> visibleStacks) {
		return initInventoryHandlerWithVisibleStacks(slots, initialState, visibleStacks, Set.of());
	}

	private static InventoryHandler initInventoryHandlerWithVisibleStacks(int slots, Map<Integer, ItemStack> initialState, Map<Integer, ItemStack> visibleStacks, Set<Integer> infiniteSlots) {
		StackUpgradeConfig stackUpgradeConfigMock = Mockito.mock(StackUpgradeConfig.class);
		when(stackUpgradeConfigMock.canStackItem(any(Item.class))).thenReturn(true);
		return new InventoryHandler(slots, NoopStorageWrapper.INSTANCE, getContainerContents(slots, initialState), () -> {}, 256, stackUpgradeConfigMock) {
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
