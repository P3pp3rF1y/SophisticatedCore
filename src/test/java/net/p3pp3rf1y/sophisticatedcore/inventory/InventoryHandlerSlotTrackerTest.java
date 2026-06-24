package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.HelperAssertions;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.SlotValueMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class InventoryHandlerSlotTrackerTest {

	@BeforeEach
	public void testSetup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
	}

	private InventoryHandler initInventoryHandler(Map<Integer, ItemStack> initialState, int slotLimit) {
		return initInventoryHandler(initialState, slotLimit, Collections.emptySet());
	}

	private InventoryHandler initInventoryHandler(Map<Integer, ItemStack> initialState, int slotLimit, Set<Integer> inaccessibleSlots) {
		InventoryHandler inventoryHandler = Mockito.mock(InventoryHandler.class);
		when(inventoryHandler.getSlots()).thenReturn(initialState.size());
		when(inventoryHandler.getStackInSlot(anyInt())).thenAnswer(invocation -> {
			int slot = invocation.getArgument(0);
			return initialState.get(slot);
		});
		when(inventoryHandler.getStackLimit(anyInt(), any(ItemStack.class))).thenAnswer(invocation -> {
			ItemStack stack = invocation.getArgument(1);
			int limitMultiplier = slotLimit / 64;
			return stack.getMaxStackSize() * limitMultiplier;
		});
		when(inventoryHandler.isSlotAccessible(anyInt())).thenAnswer(invocation -> {
			int slot = invocation.getArgument(0);
			return !inaccessibleSlots.contains(slot);
		});
		return inventoryHandler;
	}

	private InventoryHandlerSlotTracker initSlotTracker(InventoryHandler inventoryHandler) {
		return initSlotTracker(inventoryHandler, Map.of(), false, new SlotValueMap<>(), true);
	}

	private InventoryHandlerSlotTracker initSlotTracker(InventoryHandler inventoryHandler, Map<Integer, ItemStack> memoryFilterStacks, boolean memoryIgnoresNbt,
			SlotValueMap<Item> filterItemSlots, boolean shouldInsertIntoEmpty) {
		MemorySettingsCategory memorySettings = new MemorySettingsCategory(() -> inventoryHandler, new CompoundTag(), tag -> {
		});
		memorySettings.setIgnoreNbt(memoryIgnoresNbt);
		memoryFilterStacks.forEach(memorySettings::setFilter);

		InventoryHandlerSlotTracker slotTracker = new InventoryHandlerSlotTracker(memorySettings, filterItemSlots);
		slotTracker.refreshSlotIndexesFrom(inventoryHandler);
		slotTracker.setShouldInsertIntoEmpty(() -> shouldInsertIntoEmpty);
		return slotTracker;
	}

	private ISlotTracker.IItemHandlerInserter initInserter(Map<Integer, ItemStack> initialState, int slotLimit) {
		ISlotTracker.IItemHandlerInserter inserter = Mockito.mock(ISlotTracker.IItemHandlerInserter.class);
		when(inserter.insertItem(anyInt(), any(ItemStack.class), anyBoolean())).thenAnswer(invocation -> {
			int slot = invocation.getArgument(0);
			ItemStack stack = invocation.getArgument(1);
			ItemStack slotStack = initialState.get(slot);
			if (slotStack.isEmpty()) {
				return stack.getCount() <= slotLimit ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - slotLimit);
			} else if (ItemStack.isSameItemSameComponents(stack, slotStack)) {
				int remainingSpace = Math.min(stack.getMaxStackSize(), slotLimit) - slotStack.getCount();
				return stack.getCount() <= remainingSpace ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - remainingSpace);
			}
			return stack;
		});
		return inserter;
	}

	@ParameterizedTest
	@MethodSource("simulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlot")
	void simulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlot(SimulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlotParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker.IItemHandlerInserter inserter = initInserter(params.initialState(), params.slotLimit());
		InventoryHandlerSlotTracker slotTracker = initSlotTracker(inventoryHandler);

		ItemStack result = slotTracker.insertItemIntoHandler(inventoryHandler, (stack, simulate) -> stack, inserter, stack -> stack, stack -> stack,
				params.slotInsertedInto(), params.stackBeingInserted(), true);

		HelperAssertions.assertStackEquals(params.expectedResult(), result, "Resulting stack does not match expected stack");
	}

	private record SimulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlotParams(int slotLimit, Map<Integer, ItemStack> initialState, int slotInsertedInto,
			ItemStack stackBeingInserted, ItemStack expectedResult) {
	}

	private static List<SimulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlotParams> simulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlot() {
		return List.of(
				new SimulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlotParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 0,
						new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.GOLD_INGOT, 1)),
				new SimulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlotParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), ItemStack.EMPTY),
				new SimulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlotParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 0,
						new ItemStack(Items.DIAMOND, 1), ItemStack.EMPTY),
				new SimulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlotParams(64, Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.DIAMOND, 1)), 1,
						new ItemStack(Items.STICK, 1), new ItemStack(Items.STICK, 1)),
				new SimulatedInsertItemIntoHandlerOnlyConsidersSpecifiedSlotParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 0,
						new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.DIAMOND, 1)));
	}

	@ParameterizedTest
	@MethodSource("simulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlot")
	void simulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlot(SimulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlotParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker.IItemHandlerInserter inserter = initInserter(params.initialState(), params.slotLimit());
		InventoryHandlerSlotTracker slotTracker = initSlotTracker(inventoryHandler);

		ItemStack result = slotTracker.insertItemIntoHandler(inventoryHandler, (stack, simulate) -> stack, inserter,
				stack -> params.stackVoided.test(stack) ? ItemStack.EMPTY : stack, stack -> params.stackVoided.test(stack) ? ItemStack.EMPTY : stack,
				params.slotInsertedInto(), params.stackBeingInserted(), true);

		HelperAssertions.assertStackEquals(params.expectedResult(), result, "Resulting stack does not match expected stack");
	}

	private record SimulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlotParams(int slotLimit, Map<Integer, ItemStack> initialState,
			int slotInsertedInto, ItemStack stackBeingInserted, ItemStack expectedResult, Predicate<ItemStack> stackVoided) {
	}

	private static List<SimulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlotParams> simulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlot() {
		return List.of(
				new SimulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlotParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY),
						0, new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.GOLD_INGOT, 1), stack -> stack.getItem() == Items.DIAMOND),
				new SimulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlotParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY),
						0, new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.GOLD_INGOT, 1), stack -> stack.getItem() == Items.GOLD_INGOT),
				new SimulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlotParams(64,
						Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, new ItemStack(Items.GOLD_INGOT, 64)), 0, new ItemStack(Items.GOLD_INGOT, 1),
						ItemStack.EMPTY, stack -> stack.getItem() == Items.GOLD_INGOT),
				new SimulatedInsertItemIntoHandlerConsidersOverflowEvenForSpecificSlotParams(64,
						Map.of(0, new ItemStack(Items.DIAMOND, 60), 1, ItemStack.EMPTY), 0, new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY,
						stack -> stack.getItem() == Items.DIAMOND));
	}

	@ParameterizedTest
	@MethodSource("simulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlots")
	void simulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlots(SimulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlotsParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker.IItemHandlerInserter inserter = initInserter(params.initialState(), params.slotLimit());
		InventoryHandlerSlotTracker slotTracker = initSlotTracker(inventoryHandler);

		ItemStack result = insertItemStackedEquivalent(inventoryHandler, params.stackBeingInserted(), true,
				(slot, stack, simulate) -> slotTracker.insertItemIntoHandler(inventoryHandler, (s, sim) -> s, inserter,
						s -> params.stackVoided.test(s) ? ItemStack.EMPTY : s, s -> params.stackVoided.test(s) ? ItemStack.EMPTY : s, slot, stack, simulate));

		HelperAssertions.assertStackEquals(params.expectedResult(), result, "Resulting stack does not match expected stack");
	}

	private record SimulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlotsParams(int slotLimit, Map<Integer, ItemStack> initialState,
			ItemStack stackBeingInserted, ItemStack expectedResult, Predicate<ItemStack> stackVoided) {
	}

	private static List<SimulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlotsParams> simulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlots() {
		return List.of(
				new SimulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlotsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY),
						new ItemStack(Items.GOLD_INGOT, 1), ItemStack.EMPTY, stack -> stack.getItem() == Items.DIAMOND),
				new SimulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlotsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY),
						new ItemStack(Items.GOLD_INGOT, 64), ItemStack.EMPTY, stack -> stack.getItem() == Items.DIAMOND),
				new SimulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlotsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY),
						new ItemStack(Items.DIAMOND, 127), ItemStack.EMPTY, stack -> stack.getItem() == Items.GOLD_INGOT),
				new SimulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlotsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY),
						new ItemStack(Items.DIAMOND, 128), new ItemStack(Items.DIAMOND, 1), stack -> stack.getItem() == Items.GOLD_INGOT),
				new SimulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlotsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY),
						new ItemStack(Items.DIAMOND, 128), ItemStack.EMPTY, stack -> stack.getItem() == Items.DIAMOND),
				new SimulatedInsertItemStackedEquivalentConsidersOnlySpecifiedSlotsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY),
						new ItemStack(Items.GOLD_INGOT, 128), new ItemStack(Items.GOLD_INGOT, 64), stack -> stack.getItem() == Items.DIAMOND));
	}

	@ParameterizedTest
	@MethodSource("simulatedBulkInsertOnlyWorksWithSpecifiedSlots")
	void simulatedBulkInsertOnlyWorksWithSpecifiedSlots(SimulatedBulkInsertOnlyWorksWithSpecifiedSlotsParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker.IItemHandlerInserter inserter = initInserter(params.initialState(), params.slotLimit());
		InventoryHandlerSlotTracker slotTracker = initSlotTracker(inventoryHandler);

		List<ItemStack> result = insertItemsBulk(inventoryHandler, List.of(params.stacks().toArray(new ItemStack[0])), true,
				(slot, stack, simulate) -> slotTracker.insertItemIntoHandler(inventoryHandler, (s, sim) -> s, inserter,
						s -> params.stackVoided.test(s) ? ItemStack.EMPTY : s, s -> params.stackVoided.test(s) ? ItemStack.EMPTY : s, slot, stack, simulate),
				params.stackVoided());

		Assertions.assertEquals(params.expectedResult().size(), result.size(), "Resulting stacks size does not match expected stacks size");
		for (int i = 0; i < params.expectedResult().size(); i++) {
			HelperAssertions.assertStackEquals(params.expectedResult().get(i), result.get(i),
					"Resulting stack in slot " + i + " does not match expected stack");
		}
	}

	private record SimulatedBulkInsertOnlyWorksWithSpecifiedSlotsParams(int slotLimit, Map<Integer, ItemStack> initialState, List<ItemStack> stacks,
			List<ItemStack> expectedResult, Predicate<ItemStack> stackVoided) {
	}

	private static List<SimulatedBulkInsertOnlyWorksWithSpecifiedSlotsParams> simulatedBulkInsertOnlyWorksWithSpecifiedSlots() {
		return List.of(
				new SimulatedBulkInsertOnlyWorksWithSpecifiedSlotsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						List.of(new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.DIAMOND, 64)), List.of(), stack -> false),
				new SimulatedBulkInsertOnlyWorksWithSpecifiedSlotsParams(64,
						Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, new ItemStack(Items.DIAMOND, 64), 2, new ItemStack(Items.DIAMOND, 1)),
						List.of(new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.DIAMOND, 64)), List.of(new ItemStack(Items.GOLD_INGOT, 1)),
						stack -> false),
				new SimulatedBulkInsertOnlyWorksWithSpecifiedSlotsParams(64,
						Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, new ItemStack(Items.DIAMOND, 1), 2, new ItemStack(Items.GOLD_INGOT, 64)),
						List.of(new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.DIAMOND, 64)), List.of(), stack -> stack.getItem() == Items.GOLD_INGOT),
				new SimulatedBulkInsertOnlyWorksWithSpecifiedSlotsParams(64,
						Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, new ItemStack(Items.DIAMOND, 1), 2, ItemStack.EMPTY),
						List.of(new ItemStack(Items.IRON_INGOT, 32), new ItemStack(Items.IRON_INGOT, 17), new ItemStack(Items.IRON_INGOT, 15)), List.of(),
						stack -> stack.getItem() == Items.GOLD_INGOT),
				new SimulatedBulkInsertOnlyWorksWithSpecifiedSlotsParams(64,
						Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, new ItemStack(Items.DIAMOND, 32), 2, ItemStack.EMPTY),
						List.of(new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.DIAMOND, 64)),
						List.of(new ItemStack(Items.DIAMOND, 33)), stack -> stack.getItem() == Items.GOLD_INGOT),
				new SimulatedBulkInsertOnlyWorksWithSpecifiedSlotsParams(64,
						Map.of(0, new ItemStack(Items.DIAMOND, 35), 1, new ItemStack(Items.DIAMOND, 32), 2, ItemStack.EMPTY),
						List.of(new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.DIAMOND, 64)), List.of(),
						stack -> stack.getItem() == Items.DIAMOND));
	}

	@ParameterizedTest
	@MethodSource("simulatedInsertRespectsMemorizedAndFilterItems")
	void simulatedInsertRespectsMemorizedAndFilterItems(SimulatedInsertRespectsMemorizedAndFilterItemsParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker.IItemHandlerInserter inserter = initInserter(params.initialState(), params.slotLimit());
		InventoryHandlerSlotTracker slotTracker = initSlotTracker(inventoryHandler, params.memorizedItems(), params.memoryIgnoresNbt(), params.filterItems(),
				params.shouldInsertIntoEmpty());

		ItemStack result = slotTracker.insertItemIntoHandler(inventoryHandler, (stack, simulate) -> stack, inserter,
				stack -> params.stackVoided.test(stack) ? ItemStack.EMPTY : stack, stack -> params.stackVoided.test(stack) ? ItemStack.EMPTY : stack,
				params.slotInsertedInto(), params.stackBeingInserted(), true);

		HelperAssertions.assertStackEquals(params.expectedResult(), result, "Resulting stack does not match expected stack");
	}

	private record SimulatedInsertRespectsMemorizedAndFilterItemsParams(int slotLimit, Map<Integer, ItemStack> initialState, int slotInsertedInto,
			ItemStack stackBeingInserted, ItemStack expectedResult, Predicate<ItemStack> stackVoided, Map<Integer, ItemStack> memorizedItems,
			boolean memoryIgnoresNbt, SlotValueMap<Item> filterItems, boolean shouldInsertIntoEmpty) {
	}

	private static List<SimulatedInsertRespectsMemorizedAndFilterItemsParams> simulatedInsertRespectsMemorizedAndFilterItems() {
		return List.of(
				new SimulatedInsertRespectsMemorizedAndFilterItemsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.GOLD_INGOT, 1), stack -> false, Map.of(), true,
						SlotValueMap.of(1, Items.IRON_INGOT), true),
				new SimulatedInsertRespectsMemorizedAndFilterItemsParams(64, Map.of(0, new ItemStack(Items.GOLD_INGOT, 64), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), ItemStack.EMPTY, stack -> stack.getItem() == Items.GOLD_INGOT, Map.of(), true,
						SlotValueMap.of(1, Items.IRON_INGOT), true),
				new SimulatedInsertRespectsMemorizedAndFilterItemsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), ItemStack.EMPTY, stack -> false, Map.of(), true, SlotValueMap.of(1, Items.GOLD_INGOT), true),
				new SimulatedInsertRespectsMemorizedAndFilterItemsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), ItemStack.EMPTY, stack -> false, Map.of(1, new ItemStack(Items.GOLD_INGOT, 1)), true,
						SlotValueMap.of(), true),
				new SimulatedInsertRespectsMemorizedAndFilterItemsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.GOLD_INGOT, 1), stack -> false, Map.of(1, new ItemStack(Items.IRON_INGOT, 1)),
						true, SlotValueMap.of(), true),
				new SimulatedInsertRespectsMemorizedAndFilterItemsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.GOLD_INGOT, 1), stack -> false,
						Map.of(1, customizeName(new ItemStack(Items.GOLD_INGOT, 1), "test")), false, SlotValueMap.of(), true),
				new SimulatedInsertRespectsMemorizedAndFilterItemsParams(64, Map.of(0, new ItemStack(Items.GOLD_INGOT, 64), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), ItemStack.EMPTY, stack -> stack.getItem() == Items.GOLD_INGOT,
						Map.of(1, customizeName(new ItemStack(Items.GOLD_INGOT, 1), "test")), false, SlotValueMap.of(), true),
				new SimulatedInsertRespectsMemorizedAndFilterItemsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), ItemStack.EMPTY, stack -> false, Map.of(1, new ItemStack(Items.GOLD_INGOT, 1)), true,
						SlotValueMap.of(), false),
				new SimulatedInsertRespectsMemorizedAndFilterItemsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), ItemStack.EMPTY, stack -> false, Map.of(), true, SlotValueMap.of(1, Items.GOLD_INGOT), false));
	}

	@ParameterizedTest
	@MethodSource("simulatedInsertConsidersInaccessibleSlots")
	void simulatedInsertConsidersInaccessibleSlots(SimulatedInsertConsidersInaccessibleSlotsParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit(), params.inaccessibleSlots());
		ISlotTracker.IItemHandlerInserter inserter = initInserter(params.initialState(), params.slotLimit());
		InventoryHandlerSlotTracker slotTracker = initSlotTracker(inventoryHandler);

		ItemStack result = slotTracker.insertItemIntoHandler(inventoryHandler, (stack, simulate) -> stack, inserter,
				stack -> params.stackVoided.test(stack) ? ItemStack.EMPTY : stack, stack -> params.stackVoided.test(stack) ? ItemStack.EMPTY : stack,
				params.slotInsertedInto(), params.stackBeingInserted(), true);

		HelperAssertions.assertStackEquals(params.expectedResult(), result, "Resulting stack does not match expected stack");
	}

	private record SimulatedInsertConsidersInaccessibleSlotsParams(int slotLimit, Map<Integer, ItemStack> initialState, int slotInsertedInto,
			ItemStack stackBeingInserted, ItemStack expectedResult, Predicate<ItemStack> stackVoided, Set<Integer> inaccessibleSlots) {
	}

	private static List<SimulatedInsertConsidersInaccessibleSlotsParams> simulatedInsertConsidersInaccessibleSlots() {
		return List.of(
				new SimulatedInsertConsidersInaccessibleSlotsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.GOLD_INGOT, 1), stack -> false, Set.of(1)),
				new SimulatedInsertConsidersInaccessibleSlotsParams(64, Map.of(0, new ItemStack(Items.DIAMOND, 1), 1, ItemStack.EMPTY), 1,
						new ItemStack(Items.GOLD_INGOT, 1), ItemStack.EMPTY, stack -> false, Set.of()));
	}

	private static ItemStack customizeName(ItemStack stack, String customName) {
		ItemStack result = stack.copy();
		result.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
		return result;
	}

	public static List<ItemStack> insertItemsBulk(InventoryHandler inventoryHandler, List<ItemStack> stacks, boolean simulate,
			IItemHandlerSlottedInserter slottedInserter, Predicate<ItemStack> stackVoided) {
		List<ItemStack> remainingStacks = new ArrayList<>(stacks);

		for (int slot = 0; slot < inventoryHandler.getSlots(); slot++) {
			ItemStack slotStack = inventoryHandler.getStackInSlot(slot);
			if (slotStack.getCount() >= inventoryHandler.getStackLimit(slot, slotStack)) {
				continue;
			}

			ItemStack inProgressStack = slotStack.isEmpty() ? ItemStack.EMPTY : slotStack.copy();
			for (Iterator<ItemStack> iterator = remainingStacks.iterator(); iterator.hasNext();) {
				ItemStack stack = iterator.next();
				if (inProgressStack.isEmpty() || ItemStack.isSameItemSameComponents(inProgressStack, stack) || stackVoided.test(stack)) {
					int stackLimit = inventoryHandler.getStackLimit(slot, stack);
					int remainingSpace = stackLimit - inProgressStack.getCount();
					ItemStack toInsert;
					if (stackVoided.test(stack)) {
						toInsert = stack;
					} else {
						toInsert = stack.getCount() <= remainingSpace ? stack : stack.copyWithCount(remainingSpace);
					}
					ItemStack result = slottedInserter.insertItem(slot, toInsert, simulate);
					int numberInserted = toInsert.getCount() - result.getCount();
					if (numberInserted > 0) {
						if (inProgressStack.isEmpty()) {
							inProgressStack = toInsert.copyWithCount(numberInserted);
						} else {
							inProgressStack.grow(numberInserted);
						}
						stack.shrink(numberInserted);
					}
					if (stack.isEmpty()) {
						iterator.remove();
						if (remainingStacks.isEmpty()) {
							return Collections.emptyList();
						}
					}
				}
			}
		}

		return remainingStacks;
	}

	public static ItemStack insertItemStackedEquivalent(InventoryHandler inventoryHandler, ItemStack stack, boolean simulate,
			IItemHandlerSlottedInserter slottedInserter) {
		if (!stack.isEmpty()) {
			if (!stack.isStackable()) {
				return insertItem(inventoryHandler, stack, simulate, slottedInserter);
			} else {
				int sizeInventory = inventoryHandler.getSlots();

				int i;
				for (i = 0; i < sizeInventory; ++i) {
					ItemStack slot = inventoryHandler.getStackInSlot(i);
					if (ItemStack.isSameItemSameComponents(slot, stack)) {
						stack = slottedInserter.insertItem(i, stack, simulate);
						if (stack.isEmpty()) {
							break;
						}
					}
				}

				if (!stack.isEmpty()) {
					for (i = 0; i < sizeInventory; ++i) {
						if (inventoryHandler.getStackInSlot(i).isEmpty()) {
							stack = slottedInserter.insertItem(i, stack, simulate);
							if (stack.isEmpty()) {
								break;
							}
						}
					}
				}

				return stack;
			}
		} else {
			return stack;
		}
	}

	public static ItemStack insertItem(InventoryHandler inventoryHandler, ItemStack stack, boolean simulate, IItemHandlerSlottedInserter inserter) {
		if (!stack.isEmpty()) {
			for (int slot = 0; slot < inventoryHandler.getSlots(); ++slot) {
				stack = inserter.insertItem(slot, stack, simulate);
				if (stack.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}

		}
		return stack;
	}

	public interface IItemHandlerSlottedInserter {
		ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
	}
}
