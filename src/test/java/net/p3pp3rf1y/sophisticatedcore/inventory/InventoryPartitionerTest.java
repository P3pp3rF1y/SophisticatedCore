package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.Mockito.when;

class InventoryPartitionerTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	public void testSetup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
	}

	private InventoryHandler getInventoryHandler(int slots) {
		InventoryHandler inventoryHandler = Mockito.mock(InventoryHandler.class);
		when(inventoryHandler.getSlots()).thenReturn(slots);
		return inventoryHandler;
	}

	@Test
	void defaultPartDoesNotRenderInaccessibleOverlay() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		Assertions.assertFalse(partitioner.shouldRenderInaccessibleSlotOverlay(0));
	}

	@Test
	void inaccessibleOverlayIsDelegatedToCurrentPart() {
		InventoryHandler invHandler = getInventoryHandler(81);
		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);
		IInventoryPartHandler partHandler = new IInventoryPartHandler() {
			@Override
			public String getName() {
				return "dummy";
			}

			@Override
			public boolean shouldRenderInaccessibleSlotOverlay(int slot) {
				return slot == 3;
			}
		};

		partitioner.addInventoryPart(0, 9, partHandler);

		Assertions.assertTrue(partitioner.shouldRenderInaccessibleSlotOverlay(3));
		Assertions.assertFalse(partitioner.shouldRenderInaccessibleSlotOverlay(9));
	}

	@Test
	void addInventoryPartWithMaxIntSlotsAddsItForAllSlotsFromTheBaseIndex() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, Integer.MAX_VALUE, dummyPartHandler);

		Assertions.assertEquals(dummyPartHandler, partitioner.getPartBySlot(0));
		Assertions.assertEquals(dummyPartHandler, partitioner.getPartBySlot(80));
	}

	@Test
	void addInventoryPartAtTheBeginningProperlyUpdatesParts() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);

		Assertions.assertEquals(partitioner.getPartBySlot(0), dummyPartHandler);
		Assertions.assertTrue(partitioner.getPartBySlot(9) instanceof IInventoryPartHandler.Default);
	}

	@Test
	void addTwoAndRemoveInventoryPartProperlyUpdatesParts() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);
		partitioner.addInventoryPart(9, 9, () -> "dummy2");
		partitioner.removeInventoryPart(9);

		Assertions.assertEquals(dummyPartHandler, partitioner.getPartBySlot(0));
		Assertions.assertTrue(partitioner.getPartBySlot(9) instanceof IInventoryPartHandler.Default);
		Assertions.assertEquals(partitioner.getPartBySlot(9), partitioner.getPartBySlot(80));
		Assertions.assertEquals(72, partitioner.getPartBySlot(9).getSlots());
	}

	@Test
	void AddTwoAndRemoveOneProperlyShowsSlotAfterFirstAsReplaceable() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);
		partitioner.addInventoryPart(9, 9, () -> "dummy2");
		partitioner.removeInventoryPart(9);

		Optional<SlotRange> firstSpace = partitioner.getFirstSpace(9);
		Assertions.assertTrue(firstSpace.isPresent());
		Assertions.assertEquals(firstSpace.get().firstSlot(), 9);
	}

	@Test
	void addTwoAndRemoveFirstProperlyUpdatesFirstsSlots() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);
		partitioner.addInventoryPart(9, 9, () -> "dummy2");
		partitioner.removeInventoryPart(0);

		Assertions.assertEquals(9, partitioner.getPartBySlot(0).getSlots());
	}

	@Test
	void addTwoThanRemoveFirstAndThenSecondShowsAllSlotsAsReplaceable() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);
		partitioner.addInventoryPart(9, 9, () -> "dummy2");
		partitioner.removeInventoryPart(0);
		partitioner.removeInventoryPart(9);

		Optional<SlotRange> firstSpace = partitioner.getFirstSpace(9);
		Assertions.assertTrue(firstSpace.isPresent());
		Assertions.assertEquals(firstSpace.get().firstSlot(), 0);
		Assertions.assertEquals(partitioner.getPartBySlot(0), partitioner.getPartBySlot(80));
		Assertions.assertEquals(81, partitioner.getPartBySlot(0).getSlots());
	}

	@Test
	void addPartReplacingAllSlotsAndRemovingThatProperlyUpdatesToDefault() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 81, dummyPartHandler);
		partitioner.removeInventoryPart(0);

		Assertions.assertTrue(partitioner.getPartBySlot(0) instanceof IInventoryPartHandler.Default);
		Assertions.assertTrue(partitioner.getPartBySlot(80) instanceof IInventoryPartHandler.Default);
		Assertions.assertEquals(partitioner.getPartBySlot(0), partitioner.getPartBySlot(80));
		Assertions.assertEquals(81, partitioner.getPartBySlot(0).getSlots());
	}

	@Test
	void addPartReplacingAllSlotsReturnsEmptyPartFromMaxSlotPlusOne() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 81, dummyPartHandler);

		Assertions.assertEquals(IInventoryPartHandler.EMPTY, partitioner.getPartBySlot(81));
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3, 4 })
	void getFirstSpaceReturnsCorrectRangeForSmallInventories(int slots) {
		InventoryHandler invHandler = getInventoryHandler(slots);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		Optional<SlotRange> firstSpace = partitioner.getFirstSpace(9);
		Assertions.assertTrue(firstSpace.isPresent());
		Assertions.assertEquals(slots, firstSpace.get().numberOfSlots());
		Assertions.assertEquals(0, firstSpace.get().firstSlot());
	}
}
