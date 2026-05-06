package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class InventoryHandlerTest {
	@BeforeEach
	public void testSetup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
	}

	private InventoryHandler initInventoryHandler(Map<Integer, ItemStack> initialState, int slotLimit) {
		return initInventoryHandler(initialState, slotLimit, Collections.emptySet());
	}

	private InventoryHandler initInventoryHandler(Map<Integer, ItemStack> initialState, int slotLimit, Set<Integer> inaccessibleSlots) {
		ContainerContents containerContents = new ContainerContents();
		containerContents.inventory().reloadFrom(
				new ContainerContents.InventoryData(initialState.entrySet().stream()
						.sorted(Map.Entry.comparingByKey())
						.map(Map.Entry::getValue)
						.collect(Collectors.toCollection(NonNullList::create))));

		StackUpgradeConfig stackUpgradeConfigMock = Mockito.mock(StackUpgradeConfig.class);
		when(stackUpgradeConfigMock.canStackItem(any(Item.class))).thenReturn(true);
		return new InventoryHandler(
				initialState.size(), NoopStorageWrapper.INSTANCE, containerContents, () -> {}, slotLimit,
				stackUpgradeConfigMock) {
			@Override
			protected boolean isAllowed(ItemResource resource) {
				return true;
			}
		};
	}

	@Test
	void insertHandlesPartialSlotsBecomingEmptyMidLoop() throws Exception {
		InventoryHandler inventoryHandler = initInventoryHandler(Map.of(0, new ItemStack(Items.DIAMOND, 60)), 64);
		setSlotTracker(inventoryHandler, new StalePartialSlotTracker(0, ItemStack.EMPTY));

		try (Transaction tx = Transaction.openRoot()) {
			Assertions.assertDoesNotThrow(() -> inventoryHandler.insert(ItemResource.of(new ItemStack(Items.DIAMOND, 4)), 4, tx));
		}
	}

	@Test
	void extractHandlesTrackedSlotsBecomingEmptyMidLoop() throws Exception {
		InventoryHandler inventoryHandler = initInventoryHandler(Map.of(0, new ItemStack(Items.DIAMOND, 64)), 64);
		setSlotTracker(inventoryHandler, new StaleFullSlotTracker(0, ItemStack.EMPTY));

		try (Transaction tx = Transaction.openRoot()) {
			Assertions.assertDoesNotThrow(() -> inventoryHandler.extract(ItemResource.of(new ItemStack(Items.DIAMOND, 64)), 64, tx));
		}
	}

	private static void setSlotTracker(InventoryHandler inventoryHandler, ISlotTracker slotTracker) throws NoSuchFieldException, IllegalAccessException {
		Field slotTrackerField = InventoryHandler.class.getDeclaredField("slotTracker");
		slotTrackerField.setAccessible(true);
		slotTrackerField.set(inventoryHandler, slotTracker);
	}


	@ParameterizedTest
	@MethodSource("realInsertAffectsTrackedStacksData")
	void realInsertAffectsTrackedStacks(RealInsertAffectsTrackedStacksParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker slotTracker = inventoryHandler.getSlotTracker();

		ItemStack toInsert = params.stackBeingInserted().copy();
		int insertedAmount;
		SlotTrackerSnapshot snapshotBefore = new SlotTrackerSnapshot(slotTracker);
		try (Transaction tx = Transaction.openRoot()) {
			insertedAmount = inventoryHandler.insert(ItemResource.of(toInsert), toInsert.getCount(), tx);
			tx.commit();
		}
		SlotTrackerSnapshot snapshotAfter = new SlotTrackerSnapshot(slotTracker);

		Assertions.assertEquals(params.expectedInsertedAmount(), insertedAmount, "Inserted amount does not match expected amount");
		Assertions.assertNotEquals(snapshotBefore, snapshotAfter, "Slot tracker state didn't change after real insert");
	}

	private record RealInsertAffectsTrackedStacksParams(int slotLimit,
														Map<Integer, ItemStack> initialState,
														ItemStack stackBeingInserted,
														int expectedInsertedAmount) {
	}

	private static List<RealInsertAffectsTrackedStacksParams> realInsertAffectsTrackedStacksData() {
		return List.of(
				new RealInsertAffectsTrackedStacksParams(
						64,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.GOLD_INGOT, 5)),
						new ItemStack(Items.DIAMOND, 5),
						5
				),
				new RealInsertAffectsTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 60), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						new ItemStack(Items.DIAMOND, 4),
						4
				)
		);
	}

	@ParameterizedTest
	@MethodSource("realSpecificSlotInsertAffectsTrackedStacksData")
	void realSpecificSlotInsertAffectsTrackedStacks(RealSpecificSlotInsertAffectsTrackedStacksParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker slotTracker = inventoryHandler.getSlotTracker();

		int insertedAmount;
		SlotTrackerSnapshot snapshotBefore = new SlotTrackerSnapshot(slotTracker);
		try (Transaction tx = Transaction.openRoot()) {
			insertedAmount = inventoryHandler.insert(params.slotToInsertInto(), ItemResource.of(params.stackBeingInserted()), params.stackBeingInserted().getCount(), tx);
			tx.commit();
		}
		SlotTrackerSnapshot snapshotAfter = new SlotTrackerSnapshot(slotTracker);

		Assertions.assertEquals(params.expectedInsertedAmount(), insertedAmount, "Inserted amount does not match expected amount");
		Assertions.assertNotEquals(snapshotBefore, snapshotAfter, "Slot tracker state didn't change after real insert");
	}

	private record RealSpecificSlotInsertAffectsTrackedStacksParams(int slotLimit,
																	Map<Integer, ItemStack> initialState,
																	int slotToInsertInto,
																	ItemStack stackBeingInserted,
																	int expectedInsertedAmount) {
	}

	private static List<RealSpecificSlotInsertAffectsTrackedStacksParams> realSpecificSlotInsertAffectsTrackedStacksData() {
		return List.of(
				new RealSpecificSlotInsertAffectsTrackedStacksParams(
						64,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.GOLD_INGOT, 5)),
						0,
						new ItemStack(Items.DIAMOND, 10),
						10
				),
				new RealSpecificSlotInsertAffectsTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 60), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						0,
						new ItemStack(Items.DIAMOND, 4),
						4
				)
		);
	}

	@ParameterizedTest
	@MethodSource("realExtractAffectsTrackedStacksData")
	void realExtractAffectsTrackedStacks(RealExtractAffectsTrackedStacksParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker slotTracker = inventoryHandler.getSlotTracker();

		int result;
		SlotTrackerSnapshot snapshotBefore = new SlotTrackerSnapshot(slotTracker);
		try (Transaction tx = Transaction.openRoot()) {
			result = inventoryHandler.extract(ItemResource.of(params.stackBeingExtracted()), params.stackBeingExtracted().getCount(), tx);
			tx.commit();
		}
		SlotTrackerSnapshot snapshotAfter = new SlotTrackerSnapshot(slotTracker);

		Assertions.assertEquals(params.expectedResult(), result, "Extracted amount does not match expected amount");
		Assertions.assertNotEquals(snapshotBefore, snapshotAfter, "Slot tracker state didn't change after real extract");
	}

	private record RealExtractAffectsTrackedStacksParams(int slotLimit,
															Map<Integer, ItemStack> initialState,
															ItemStack stackBeingExtracted,
															int expectedResult) {
	}

	private static List<RealExtractAffectsTrackedStacksParams> realExtractAffectsTrackedStacksData() {
		return List.of(
				new RealExtractAffectsTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 10), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						new ItemStack(Items.DIAMOND, 10),
						10
				),
				new RealExtractAffectsTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 10), 1, new ItemStack(Items.GOLD_INGOT, 64)),
						new ItemStack(Items.GOLD_INGOT, 64),
						64
				)
		);
	}

	@ParameterizedTest
	@MethodSource("realSpecificSlotExtractAffectsTrackedStacksData")
	void realSpecificSlotExtractAffectsTrackedStacks(RealSpecificSlotExtractAffectsTrackedStacksParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker slotTracker = inventoryHandler.getSlotTracker();

		int result;
		SlotTrackerSnapshot snapshotBefore = new SlotTrackerSnapshot(slotTracker);
		try (Transaction tx = Transaction.openRoot()) {
			result = inventoryHandler.extract(params.slotToExtractFrom(), ItemResource.of(params.stackBeingExtracted()), params.stackBeingExtracted().getCount(), tx);
			tx.commit();
		}
		SlotTrackerSnapshot snapshotAfter = new SlotTrackerSnapshot(slotTracker);

		Assertions.assertEquals(params.expectedResult(), result, "Extracted amount does not match expected amount");
		Assertions.assertNotEquals(snapshotBefore, snapshotAfter, "Slot tracker state didn't change after real extract");
	}

	private record RealSpecificSlotExtractAffectsTrackedStacksParams(int slotLimit,
																		Map<Integer, ItemStack> initialState,
																		int slotToExtractFrom,
																		ItemStack stackBeingExtracted,
																		int expectedResult) {
	}

	private static List<RealSpecificSlotExtractAffectsTrackedStacksParams> realSpecificSlotExtractAffectsTrackedStacksData() {
		return List.of(
				new RealSpecificSlotExtractAffectsTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 10), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						0,
						new ItemStack(Items.DIAMOND, 10),
						10
				),
				new RealSpecificSlotExtractAffectsTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 10), 1, new ItemStack(Items.GOLD_INGOT, 64)),
						1,
						new ItemStack(Items.GOLD_INGOT, 64),
						64
				)
		);
	}

	@ParameterizedTest
	@MethodSource("simulatedInsertDoesntAffectTrackedStacksData")
	void simulatedInsertDoesntAffectTrackedStacks(SimulatedInsertDoesntAffectTrackedStacksParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker slotTracker = inventoryHandler.getSlotTracker();

		ItemStack toInsert = params.stackBeingInserted().copy();
		int insertedAmount;
		SlotTrackerSnapshot snapshotBefore = new SlotTrackerSnapshot(slotTracker);
		try (Transaction tx = Transaction.openRoot()) {
			insertedAmount = inventoryHandler.insert(ItemResource.of(toInsert), toInsert.getCount(), tx);
		}
		SlotTrackerSnapshot snapshotAfter = new SlotTrackerSnapshot(slotTracker);

		Assertions.assertEquals(params.expectedInsertedAmount(), insertedAmount, "Inserted amount does not match expected amount");
		Assertions.assertEquals(snapshotBefore, snapshotAfter, "Slot tracker state changed after simulated insert");
	}

	private record SimulatedInsertDoesntAffectTrackedStacksParams(int slotLimit,
																   Map<Integer, ItemStack> initialState,
																   ItemStack stackBeingInserted,
																   int expectedInsertedAmount) {
	}

	private static List<SimulatedInsertDoesntAffectTrackedStacksParams> simulatedInsertDoesntAffectTrackedStacksData() {
		return List.of(
				new SimulatedInsertDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.GOLD_INGOT, 5)),
						new ItemStack(Items.DIAMOND, 5),
						5
				),
				new SimulatedInsertDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 60), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						new ItemStack(Items.DIAMOND, 10),
						4
				),
				new SimulatedInsertDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 64), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						new ItemStack(Items.DIAMOND, 1),
						0
				)
		);
	}

	@ParameterizedTest
	@MethodSource("simulatedSpecificSlotInsertDoesntAffectTrackedStacksData")
	void simulatedSpecificSlotInsertDoesntAffectTrackedStacks(SimulatedSpecificSlotInsertDoesntAffectTrackedStacksParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker slotTracker = inventoryHandler.getSlotTracker();

		int insertedAmount;
		SlotTrackerSnapshot snapshotBefore = new SlotTrackerSnapshot(slotTracker);
		try (Transaction tx = Transaction.openRoot()) {
			insertedAmount = inventoryHandler.insert(params.slotToInsertInto(), ItemResource.of(params.stackBeingInserted()), params.stackBeingInserted().getCount(), tx);
		}
		SlotTrackerSnapshot snapshotAfter = new SlotTrackerSnapshot(slotTracker);

		Assertions.assertEquals(params.expectedInsertedAmount(), insertedAmount, "Inserted amount does not match expected amount");
		Assertions.assertEquals(snapshotBefore, snapshotAfter, "Slot tracker state changed after simulated insert");
	}

	private record SimulatedSpecificSlotInsertDoesntAffectTrackedStacksParams(int slotLimit,
																			   Map<Integer, ItemStack> initialState,
																			   int slotToInsertInto,
																			   ItemStack stackBeingInserted,
																			   int expectedInsertedAmount) {
	}

	private static List<SimulatedSpecificSlotInsertDoesntAffectTrackedStacksParams> simulatedSpecificSlotInsertDoesntAffectTrackedStacksData() {
		return List.of(
				new SimulatedSpecificSlotInsertDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.GOLD_INGOT, 5)),
						0,
						new ItemStack(Items.DIAMOND, 10),
						10
				),
				new SimulatedSpecificSlotInsertDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 60), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						0,
						new ItemStack(Items.DIAMOND, 10),
						4
				),
				new SimulatedSpecificSlotInsertDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 32), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						1,
						new ItemStack(Items.DIAMOND, 1),
						0
				)
		);
	}

	@ParameterizedTest
	@MethodSource("simulatedExtractDoesntAffectTrackedStacksData")
	void simulatedExtractDoesntAffectTrackedStacks(SimulatedExtractDoesntAffectTrackedStacksParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker slotTracker = inventoryHandler.getSlotTracker();

		int result;
		SlotTrackerSnapshot snapshotBefore = new SlotTrackerSnapshot(slotTracker);
		try (Transaction tx = Transaction.openRoot()) {
			result = inventoryHandler.extract(ItemResource.of(params.stackBeingExtracted()), params.stackBeingExtracted().getCount(), tx);
		}
		SlotTrackerSnapshot snapshotAfter = new SlotTrackerSnapshot(slotTracker);

		Assertions.assertEquals(params.expectedResult(), result, "Extracted amount does not match expected amount");
		Assertions.assertEquals(snapshotBefore, snapshotAfter, "Slot tracker state changed after simulated extract");
	}

	private record SimulatedExtractDoesntAffectTrackedStacksParams(int slotLimit,
																   Map<Integer, ItemStack> initialState,
																   ItemStack stackBeingExtracted,
																   int expectedResult) {
	}

	private static List<SimulatedExtractDoesntAffectTrackedStacksParams> simulatedExtractDoesntAffectTrackedStacksData() {
		return List.of(
				new SimulatedExtractDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 10), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						new ItemStack(Items.DIAMOND, 10),
						10
				),
				new SimulatedExtractDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 10), 1, new ItemStack(Items.GOLD_INGOT, 64)),
						new ItemStack(Items.GOLD_INGOT, 64),
						64
				),
				new SimulatedExtractDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.GOLD_INGOT, 5)),
						new ItemStack(Items.DIAMOND, 1),
						0
				)
		);
	}

	@ParameterizedTest
	@MethodSource("simulatedSpecificSlotExtractDoesntAffectTrackedStacksData")
	void simulatedSpecificSlotExtractDoesntAffectTrackedStacks(SimulatedSpecificSlotExtractDoesntAffectTrackedStacksParams params) {
		InventoryHandler inventoryHandler = initInventoryHandler(params.initialState(), params.slotLimit());
		ISlotTracker slotTracker = inventoryHandler.getSlotTracker();

		int result;
		SlotTrackerSnapshot snapshotBefore = new SlotTrackerSnapshot(slotTracker);
		try (Transaction tx = Transaction.openRoot()) {
			result = inventoryHandler.extract(params.slotToExtractFrom(), ItemResource.of(params.stackBeingExtracted()), params.stackBeingExtracted().getCount(), tx);
		}
		SlotTrackerSnapshot snapshotAfter = new SlotTrackerSnapshot(slotTracker);

		Assertions.assertEquals(params.expectedResult(), result, "Extracted amount does not match expected amount");
		Assertions.assertEquals(snapshotBefore, snapshotAfter, "Slot tracker state changed after simulated extract");
	}

	private record SimulatedSpecificSlotExtractDoesntAffectTrackedStacksParams(int slotLimit,
																			   Map<Integer, ItemStack> initialState,
																			   int slotToExtractFrom,
																			   ItemStack stackBeingExtracted,
																			   int expectedResult) {
	}

	private record SlotTrackerSnapshot(Set<Integer> emptySlots, Map<ItemStackKey, Set<Integer>> partialStacks, Map<ItemStackKey, Set<Integer>> fullStacks) {
		public SlotTrackerSnapshot(ISlotTracker slotTracker) {
			this(new HashSet<>(slotTracker.getEmptySlots()), copyPartialStacks(slotTracker), copyFullStacks(slotTracker));
		}

		private static Map<ItemStackKey, Set<Integer>> copyFullStacks(ISlotTracker slotTracker) {
			return slotTracker.getFullStacks().stream()
					.map(key -> Map.entry(key, new HashSet<>(slotTracker.getFullSlots(key))))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		private static Map<ItemStackKey, Set<Integer>> copyPartialStacks(ISlotTracker slotTracker) {
			return slotTracker.getPartialStacks().stream()
					.map(key -> Map.entry(key, new HashSet<>(slotTracker.getPartialSlots(key))))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof SlotTrackerSnapshot(
					Set<Integer> otherEmptySlots, Map<ItemStackKey, Set<Integer>> otherPartialStacks, Map<ItemStackKey, Set<Integer>> otherFullStacks
			))) {
				return false;
			}
			return Objects.equals(emptySlots, otherEmptySlots) && Objects.equals(partialStacks, otherPartialStacks) && Objects.equals(fullStacks, otherFullStacks);
		}

		@Override
		public int hashCode() {
			return Objects.hash(emptySlots, partialStacks, fullStacks);
		}
	}

	private static List<SimulatedSpecificSlotExtractDoesntAffectTrackedStacksParams> simulatedSpecificSlotExtractDoesntAffectTrackedStacksData() {
		return List.of(
				new SimulatedSpecificSlotExtractDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 10), 1, new ItemStack(Items.GOLD_INGOT, 5)),
						0,
						new ItemStack(Items.DIAMOND, 10),
						10
				),
				new SimulatedSpecificSlotExtractDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, new ItemStack(Items.DIAMOND, 10), 1, new ItemStack(Items.GOLD_INGOT, 64)),
						1,
						new ItemStack(Items.GOLD_INGOT, 64),
						64
				),
				new SimulatedSpecificSlotExtractDoesntAffectTrackedStacksParams(
						64,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.GOLD_INGOT, 5)),
						0,
						new ItemStack(Items.DIAMOND, 1),
						0
				)
		);
	}

	private static void assertSlotTrackerSnapshotEquals(SlotTrackerSnapshot expected, ISlotTracker slotTracker) {
		SlotTrackerSnapshot actual = new SlotTrackerSnapshot(slotTracker);

		if (!expected.equals(actual)) {
			assertionFailure().message("Slot tracker snapshot doesn't equal")
					.expected(expected)
					.actual(actual)
					.buildAndThrow();
		}
	}

	private static class StalePartialSlotTracker extends ISlotTracker.Noop {
		private final Set<Integer> partialSlots;
		private final int slotToRemove;
		private final ItemStack nextStack;

		private StalePartialSlotTracker(int slotToRemove, ItemStack nextStack) {
			partialSlots = new LinkedHashSet<>(Set.of(slotToRemove));
			this.slotToRemove = slotToRemove;
			this.nextStack = nextStack;
		}

		@Override
		public Set<Integer> getPartialSlots(ItemStackKey key) {
			return partialSlots;
		}

		@Override
		public void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack) {
			if (slot == slotToRemove) {
				partialSlots.clear();
				inventoryHandler.setStackInSlot(slot, nextStack);
			}
		}

		@Override
		public Set<Integer> getEmptySlots() {
			return Collections.emptySet();
		}
	}

	private static class StaleFullSlotTracker extends ISlotTracker.Noop {
		private final Set<Integer> fullSlots;
		private final int slotToRemove;
		private final ItemStack nextStack;

		private StaleFullSlotTracker(int slotToRemove, ItemStack nextStack) {
			fullSlots = new LinkedHashSet<>(Set.of(slotToRemove));
			this.slotToRemove = slotToRemove;
			this.nextStack = nextStack;
		}

		@Override
		public Set<Integer> getFullSlots(ItemStackKey key) {
			return fullSlots;
		}

		@Override
		public void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack) {
			if (slot == slotToRemove) {
				fullSlots.clear();
				inventoryHandler.setStackInSlot(slot, nextStack);
			}
		}
	}
}
