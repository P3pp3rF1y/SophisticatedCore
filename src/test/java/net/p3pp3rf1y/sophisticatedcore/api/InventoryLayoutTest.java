package net.p3pp3rf1y.sophisticatedcore.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryLayoutTest {
	@Test
	void getBaseColumnsUsesNineColumnsThroughEightyOneSlots() {
		int columnsForEightySlots = InventoryLayout.getBaseColumns(80);
		int columnsForEightyOneSlots = InventoryLayout.getBaseColumns(81);

		assertEquals(9, columnsForEightySlots);
		assertEquals(9, columnsForEightyOneSlots);
	}

	@Test
	void getBaseColumnsUsesTwelveColumnsAboveEightyOneSlots() {
		int columns = InventoryLayout.getBaseColumns(82);

		assertEquals(12, columns);
	}

	@Test
	void getEffectiveColumnsSubtractsTakenColumns() {
		int columns = InventoryLayout.getEffectiveColumns(82, 3);

		assertEquals(9, columns);
	}

	@Test
	void getNumberOfRowsUsesBaseColumnsOnBothSidesOfThreshold() {
		int rowsForEightyOneSlots = InventoryLayout.getNumberOfRows(81);
		int rowsForEightyTwoSlots = InventoryLayout.getNumberOfRows(82);

		assertEquals(9, rowsForEightyOneSlots);
		assertEquals(7, rowsForEightyTwoSlots);
	}

	@Test
	void getUsableInventorySlotsRemovesTakenColumnsFromEachRowOnBothSidesOfThreshold() {
		int usableSlotsForEightyOneSlots = InventoryLayout.getUsableInventorySlots(81, 2);
		int usableSlotsForEightyTwoSlots = InventoryLayout.getUsableInventorySlots(82, 2);

		assertEquals(63, usableSlotsForEightyOneSlots);
		assertEquals(68, usableSlotsForEightyTwoSlots);
	}
}
