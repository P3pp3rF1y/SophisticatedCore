package net.p3pp3rf1y.sophisticatedcore.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryLayoutFitterTest {
	@Test
	void movesLargerPartForwardWhenItWouldCrossTargetRow() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("large:one", 7, 2, 2, Set.of(7, 8, 16, 17)),
				new InventoryLayoutPart("stack:18", 18, 1, 1, Set.of(18))
		), 24, 8);

		assertTrue(result.fits());
		assertEquals(8, result.fittedSlots().get("large:one"));
		assertEquals(18, result.fittedSlots().get("stack:18"));
	}

	@Test
	void keepsLargerPartAtOriginalSlotWhenItStillFits() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("large:one", 10, 2, 2, Set.of(10, 11, 18, 19)),
				new InventoryLayoutPart("stack:21", 21, 1, 1, Set.of(21))
		), 24, 8);

		assertTrue(result.fits());
		assertEquals(10, result.fittedSlots().get("large:one"));
		assertEquals(21, result.fittedSlots().get("stack:21"));
	}

	@Test
	void movesTailStackIntoNextOrderedAvailableSlot() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("stack:0", 0, 1, 1, Set.of(0)),
				new InventoryLayoutPart("stack:26", 26, 1, 1, Set.of(26))
		), 24, 8);

		assertTrue(result.fits());
		assertEquals(Map.of("stack:0", 0, "stack:26", 1), result.fittedSlots());
	}

	@Test
	void movesTailStackAfterMovedLargerPart() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("stack:0", 0, 1, 1, Set.of(0)),
				new InventoryLayoutPart("large:one", 7, 2, 2, Set.of(7, 8, 16, 17)),
				new InventoryLayoutPart("stack:26", 26, 1, 1, Set.of(26))
		), 24, 8);

		assertTrue(result.fits());
		assertEquals(Map.of("stack:0", 0, "large:one", 8, "stack:26", 10), result.fittedSlots());
	}

	@Test
	void movesLaterTallPartPastEarlierLargerPartFootprint() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("large:one", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26)),
				new InventoryLayoutPart("narrow:one", 8, 1, 3, Set.of(8, 17, 26))
		), 36, 6);

		assertTrue(result.fits());
		assertEquals(Map.of("large:one", 7, "narrow:one", 21), result.fittedSlots());
	}

	@Test
	void movesLaterLargerPartPastEarlierTallPartFootprint() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("narrow:one", 8, 1, 3, Set.of(8, 17, 26)),
				new InventoryLayoutPart("large:one", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26))
		), 36, 6);

		assertTrue(result.fits());
		assertEquals(Map.of("narrow:one", 8, "large:one", 21), result.fittedSlots());
	}

	@Test
	void movesLastTopRowRectanglesIntoLowerRowsWhenThreeColumnsAreRemovedFromIronBackpackLayout() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 9, 10, 18, 19)),
				new InventoryLayoutPart("large:chicken", 2, 1, 2, Set.of(2, 11)),
				new InventoryLayoutPart("large:cow", 3, 2, 3, Set.of(3, 4, 12, 13, 21, 22)),
				new InventoryLayoutPart("large:pig", 5, 2, 3, Set.of(5, 6, 14, 15, 23, 24)),
				new InventoryLayoutPart("large:fox", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26))
		), 36, 6);

		assertTrue(result.fits());
		assertEquals(Map.of(
				"large:sheep", 0,
				"large:chicken", 2,
				"large:cow", 3,
				"large:pig", 18,
				"large:fox", 20
		), result.fittedSlots());
	}

	@Test
	void movesLastTopRowRectangleIntoLowerRowsWhenTwoColumnsAreRemovedFromIronBackpackLayout() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 9, 10, 18, 19)),
				new InventoryLayoutPart("large:chicken", 2, 1, 2, Set.of(2, 11)),
				new InventoryLayoutPart("large:cow", 3, 2, 3, Set.of(3, 4, 12, 13, 21, 22)),
				new InventoryLayoutPart("large:wolf", 5, 2, 3, Set.of(5, 6, 14, 15, 23, 24)),
				new InventoryLayoutPart("large:horse", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26))
		), 42, 7);

		assertTrue(result.fits());
		assertEquals(Map.of(
				"large:sheep", 0,
				"large:chicken", 2,
				"large:cow", 3,
				"large:wolf", 5,
				"large:horse", 21
		), result.fittedSlots());
	}

	@Test
	void compactsInOrderWhenPreservingCurrentSlotsWouldFail() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 9, 10, 18, 19)),
				new InventoryLayoutPart("large:horse", 5, 2, 3, Set.of(5, 6, 14, 15, 23, 24))
		), 24, 6);

		assertTrue(result.fits());
		assertEquals(Map.of(
				"large:sheep", 0,
				"large:horse", 2
		), result.fittedSlots());
	}

	@Test
	void keepsMiddleStackInPlaceWhenCompactingLargerPartsAroundIt() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 9, 10, 18, 19)),
				new InventoryLayoutPart("large:chicken", 2, 1, 2, Set.of(2, 11)),
				new InventoryLayoutPart("stack:20", 20, 1, 1, Set.of(20)),
				new InventoryLayoutPart("large:cow", 3, 2, 3, Set.of(3, 4, 12, 13, 21, 22)),
				new InventoryLayoutPart("large:wolf", 5, 2, 3, Set.of(5, 6, 14, 15, 23, 24)),
				new InventoryLayoutPart("large:horse", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26))
		), 42, 7);

		assertTrue(result.fits());
		assertEquals(20, result.fittedSlots().get("stack:20"));
	}

	@Test
	void movesLastTopRowRectanglesBackWhenColumnsAreRestoredOnIronBackpackLayout() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 6, 7, 12, 13)),
				new InventoryLayoutPart("large:chicken", 2, 1, 2, Set.of(2, 8)),
				new InventoryLayoutPart("large:cow", 3, 2, 3, Set.of(3, 4, 9, 10, 15, 16)),
				new InventoryLayoutPart("large:pig", 18, 2, 3, Set.of(18, 19, 24, 25, 30, 31)),
				new InventoryLayoutPart("large:fox", 20, 2, 3, Set.of(20, 21, 26, 27, 32, 33))
		), 54, 9, true);

		assertTrue(result.fits());
		assertEquals(Map.of(
				"large:sheep", 0,
				"large:chicken", 2,
				"large:cow", 3,
				"large:pig", 5,
				"large:fox", 7
		), result.fittedSlots());
	}

	@Test
	void compactingKeepsOneByOneStackAtCurrentSlotWhenItFits() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 6, 7, 12, 13)),
				new InventoryLayoutPart("stack:12", 12, 1, 1, Set.of(12)),
				new InventoryLayoutPart("large:pig", 18, 2, 3, Set.of(18, 19, 24, 25, 30, 31))
		), 54, 9, true);

		assertTrue(result.fits());
		assertEquals(12, result.fittedSlots().get("stack:12"));
	}

	@Test
	void fixedPartMustRemainAtOriginalSlot() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("fixed:8", 8, 1, 1, Set.of(8))
		), 8, 8);

		assertFalse(result.fits());
		assertEquals(Set.of(8), result.errorSlots());
	}

	@Test
	void failingPartMarksItselfAndRemainingPartsAsErrors() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(
				new InventoryLayoutPart("stack:0", 0, 1, 1, Set.of(0)),
				new InventoryLayoutPart("large:one", 7, 2, 2, Set.of(7, 8, 16, 17)),
				new InventoryLayoutPart("stack:26", 26, 1, 1, Set.of(26))
		), 8, 8);

		assertFalse(result.fits());
		assertEquals(Set.of(7, 8, 16, 17, 26), result.errorSlots());
		assertEquals(Map.of("stack:0", 0), result.fittedSlots());
	}
}
