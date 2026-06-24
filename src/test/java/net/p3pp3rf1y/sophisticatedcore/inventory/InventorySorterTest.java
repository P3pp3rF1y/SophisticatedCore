package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

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
	void sortHandlerTopsUpNoSortSlotsUsingVisibleCount() {
		Map<Integer, ItemStack> visibleStacks = new HashMap<>(Map.of(0, stack(Items.IRON_NUGGET, 100)));
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(7,
				Map.of(0, stack(Items.IRON_NUGGET, 5), 5, stack(Items.IRON_NUGGET, 10), 6, stack(Items.COBBLESTONE, 1)), visibleStacks, Set.of());

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of(0, 1, 2, 3, 4));

		assertStack(visibleStacks, 0, Items.IRON_NUGGET, 110);
		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 5);
		assertStack(inventoryHandler, 5, Items.COBBLESTONE, 1);
		Assertions.assertTrue(inventoryHandler.getSlotStack(6).isEmpty());
	}

	@Test
	void sortHandlerTopsUpInfiniteNoSortSlotsUsingInternalCount() {
		InventoryHandler inventoryHandler = initInventoryHandlerWithVisibleStacks(7,
				Map.of(0, stack(Items.IRON_NUGGET, 5), 5, stack(Items.IRON_NUGGET, 10), 6, stack(Items.COBBLESTONE, 1)),
				new HashMap<>(Map.of(0, stack(Items.IRON_NUGGET, Integer.MAX_VALUE))), Set.of(0));

		InventorySorter.sortHandler(inventoryHandler, InventorySorter.BY_COUNT, Set.of(0, 1, 2, 3, 4));

		assertStack(inventoryHandler, 0, Items.IRON_NUGGET, 15);
		assertStack(inventoryHandler, 5, Items.COBBLESTONE, 1);
		Assertions.assertTrue(inventoryHandler.getSlotStack(6).isEmpty());
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
		when(inventoryHandler.isSlotAccessible(anyInt())).thenAnswer(invocation -> !inaccessibleSlots.contains((int) invocation.getArgument(0)));
		when(inventoryHandler.getBaseStackLimit(any(ItemStack.class))).thenReturn(256);
		when(inventoryHandler.getStackLimit(anyInt(), any(ItemStack.class))).thenReturn(256);
		when(inventoryHandler.getSlotLimit(anyInt())).thenReturn(256);
		when(inventoryHandler.isInfinite(anyInt())).thenAnswer(invocation -> infiniteSlots.contains((int) invocation.getArgument(0)));
		when(inventoryHandler.getSlotStack(anyInt())).thenAnswer(invocation -> internalStacks.getOrDefault((int) invocation.getArgument(0), ItemStack.EMPTY));
		when(inventoryHandler.getStackInSlot(anyInt())).thenAnswer(invocation -> {
			int slot = invocation.getArgument(0);
			return visibleStacks.getOrDefault(slot, internalStacks.getOrDefault(slot, ItemStack.EMPTY));
		});
		Mockito.doAnswer(invocation -> {
			internalStacks.put(invocation.getArgument(0), invocation.getArgument(1));
			return null;
		}).when(inventoryHandler).setSlotStack(anyInt(), any(ItemStack.class));
		Mockito.doAnswer(invocation -> {
			int slot = invocation.getArgument(0);
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

	private static ItemStack stack(Item item, int count) {
		return new ItemStack(item, count);
	}

	private static void assertStack(InventoryHandler inventoryHandler, int slot, Item item, int count) {
		assertStack(Map.of(slot, inventoryHandler.getSlotStack(slot)), slot, item, count);
	}

	private static void assertStack(Map<Integer, ItemStack> stacks, int slot, Item item, int count) {
		ItemStack stack = stacks.get(slot);
		Assertions.assertEquals(item, stack.getItem());
		Assertions.assertEquals(count, stack.getCount());
	}
}
