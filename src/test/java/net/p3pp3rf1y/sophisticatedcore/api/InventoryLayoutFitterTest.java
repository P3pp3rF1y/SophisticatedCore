package net.p3pp3rf1y.sophisticatedcore.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryLayoutFitterTest {
	@Test
	void movesLargerPartForwardWhenItWouldCrossTargetRow() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(
				List.of(new InventoryLayoutPart("large:one", 7, 2, 2, Set.of(7, 8, 16, 17)), new InventoryLayoutPart("stack:18", 18, 1, 1, Set.of(18))), 24, 8);

		assertTrue(result.fits());
		assertEquals(8, result.fittedSlots().get("large:one"));
		assertEquals(18, result.fittedSlots().get("stack:18"));
	}

	@Test
	void keepsLargerPartAtOriginalSlotWhenItStillFits() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(
				List.of(new InventoryLayoutPart("large:one", 10, 2, 2, Set.of(10, 11, 18, 19)), new InventoryLayoutPart("stack:21", 21, 1, 1, Set.of(21))), 24,
				8);

		assertTrue(result.fits());
		assertEquals(10, result.fittedSlots().get("large:one"));
		assertEquals(21, result.fittedSlots().get("stack:21"));
	}

	@Test
	void movesTailStackIntoNextOrderedAvailableSlot() {
		InventoryLayoutFitResult result = InventoryLayoutFitter
				.fit(List.of(new InventoryLayoutPart("stack:0", 0, 1, 1, Set.of(0)), new InventoryLayoutPart("stack:26", 26, 1, 1, Set.of(26))), 24, 8);

		assertTrue(result.fits());
		assertEquals(Map.of("stack:0", 0, "stack:26", 1), result.fittedSlots());
	}

	@Test
	void movesTailStackAfterMovedLargerPart() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("stack:0", 0, 1, 1, Set.of(0)),
				new InventoryLayoutPart("large:one", 7, 2, 2, Set.of(7, 8, 16, 17)), new InventoryLayoutPart("stack:26", 26, 1, 1, Set.of(26))), 24, 8);

		assertTrue(result.fits());
		assertEquals(Map.of("stack:0", 0, "large:one", 8, "stack:26", 10), result.fittedSlots());
	}

	@Test
	void movesLaterTallPartPastEarlierLargerPartFootprint() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("large:one", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26)),
				new InventoryLayoutPart("narrow:one", 8, 1, 3, Set.of(8, 17, 26))), 36, 6);

		assertTrue(result.fits());
		assertEquals(Map.of("large:one", 7, "narrow:one", 21), result.fittedSlots());
	}

	@Test
	void movesLaterLargerPartPastEarlierTallPartFootprint() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("narrow:one", 8, 1, 3, Set.of(8, 17, 26)),
				new InventoryLayoutPart("large:one", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26))), 36, 6);

		assertTrue(result.fits());
		assertEquals(Map.of("narrow:one", 8, "large:one", 21), result.fittedSlots());
	}

	@Test
	void movesLastTopRowRectanglesIntoLowerRowsWhenThreeColumnsAreRemovedFromIronBackpackLayout() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 9, 10, 18, 19)),
				new InventoryLayoutPart("large:chicken", 2, 1, 2, Set.of(2, 11)), new InventoryLayoutPart("large:cow", 3, 2, 3, Set.of(3, 4, 12, 13, 21, 22)),
				new InventoryLayoutPart("large:pig", 5, 2, 3, Set.of(5, 6, 14, 15, 23, 24)),
				new InventoryLayoutPart("large:fox", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26))), 36, 6);

		assertTrue(result.fits());
		assertEquals(Map.of("large:sheep", 0, "large:chicken", 2, "large:cow", 3, "large:pig", 18, "large:fox", 20), result.fittedSlots());
	}

	@Test
	void movesLastTopRowRectangleIntoLowerRowsWhenTwoColumnsAreRemovedFromIronBackpackLayout() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 9, 10, 18, 19)),
				new InventoryLayoutPart("large:chicken", 2, 1, 2, Set.of(2, 11)), new InventoryLayoutPart("large:cow", 3, 2, 3, Set.of(3, 4, 12, 13, 21, 22)),
				new InventoryLayoutPart("large:wolf", 5, 2, 3, Set.of(5, 6, 14, 15, 23, 24)),
				new InventoryLayoutPart("large:horse", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26))), 42, 7);

		assertTrue(result.fits());
		assertEquals(Map.of("large:sheep", 0, "large:chicken", 2, "large:cow", 3, "large:wolf", 5, "large:horse", 21), result.fittedSlots());
	}

	@Test
	void compactsInOrderWhenPreservingCurrentSlotsWouldFail() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 9, 10, 18, 19)),
				new InventoryLayoutPart("large:horse", 5, 2, 3, Set.of(5, 6, 14, 15, 23, 24))), 24, 6);

		assertTrue(result.fits());
		assertEquals(Map.of("large:sheep", 0, "large:horse", 2), result.fittedSlots());
	}

	@Test
	void keepsMiddleStackInPlaceWhenCompactingLargerPartsAroundIt() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 9, 10, 18, 19)),
				new InventoryLayoutPart("large:chicken", 2, 1, 2, Set.of(2, 11)), new InventoryLayoutPart("stack:20", 20, 1, 1, Set.of(20)),
				new InventoryLayoutPart("large:cow", 3, 2, 3, Set.of(3, 4, 12, 13, 21, 22)),
				new InventoryLayoutPart("large:wolf", 5, 2, 3, Set.of(5, 6, 14, 15, 23, 24)),
				new InventoryLayoutPart("large:horse", 7, 2, 3, Set.of(7, 8, 16, 17, 25, 26))), 42, 7);

		assertTrue(result.fits());
		assertEquals(20, result.fittedSlots().get("stack:20"));
	}

	@Test
	void movesLastTopRowRectanglesBackWhenColumnsAreRestoredOnIronBackpackLayout() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 6, 7, 12, 13)),
				new InventoryLayoutPart("large:chicken", 2, 1, 2, Set.of(2, 8)), new InventoryLayoutPart("large:cow", 3, 2, 3, Set.of(3, 4, 9, 10, 15, 16)),
				new InventoryLayoutPart("large:pig", 18, 2, 3, Set.of(18, 19, 24, 25, 30, 31)),
				new InventoryLayoutPart("large:fox", 20, 2, 3, Set.of(20, 21, 26, 27, 32, 33))), 54, 9, true);

		assertTrue(result.fits());
		assertEquals(Map.of("large:sheep", 0, "large:chicken", 2, "large:cow", 3, "large:pig", 5, "large:fox", 7), result.fittedSlots());
	}

	@Test
	void compactingKeepsOneByOneStackAtCurrentSlotWhenItFits() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("large:sheep", 0, 2, 3, Set.of(0, 1, 6, 7, 12, 13)),
				new InventoryLayoutPart("stack:12", 12, 1, 1, Set.of(12)), new InventoryLayoutPart("large:pig", 18, 2, 3, Set.of(18, 19, 24, 25, 30, 31))), 54,
				9, true);

		assertTrue(result.fits());
		assertEquals(12, result.fittedSlots().get("stack:12"));
	}

	@Test
	void compactingKeepsMobPartAtCurrentSlotWhenItFits() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("mob:pig", 5, 2, 2, Set.of(5, 6, 12, 13))), 18, 9, true);

		assertTrue(result.fits());
		assertEquals(0, result.fittedSlots().get("mob:pig"));
	}

	@Test
	void compactingMovesMobPartsBackWhenColumnsAreRestored() {
		List<InventoryLayoutPart> parts = List.of(mobPart("pig", 0, 2, 3, 7, 63), mobPart("cow", 21, 2, 3, 7, 63), mobPart("sheep", 42, 2, 3, 7, 63));

		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(parts, 81, 9, true);

		assertTrue(result.fits());
		assertEquals(Map.of("mob:pig", 0, "mob:cow", 2, "mob:sheep", 4), result.fittedSlots());
		assertValidLayout(result, parts, 81, 9);
	}

	@Test
	void compactingKeepsAllStacksWhenMobPartsMoveBackAfterColumnsAreRestored() {
		List<InventoryLayoutPart> parts = tankLayoutPartsWithStacks(
				List.of(mobPart("pig", 0, 2, 3, 7, 63), mobPart("cow", 21, 2, 3, 7, 63), mobPart("sheep", 42, 2, 3, 7, 63)), 63, 7);

		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(parts, 81, 9, true);

		assertTrue(result.fits());
		assertEquals(0, result.fittedSlots().get("mob:pig"));
		assertEquals(parts.size(), result.fittedSlots().size());
		assertValidLayout(result, parts, 81, 9);
	}

	@Test
	void compactingKeepsAllStacksWithMixedMobSizesAfterColumnsAreRestored() {
		List<InventoryLayoutPart> parts = tankLayoutPartsWithStacks(
				List.of(mobPart("chicken", 0, 1, 2, 7, 63), mobPart("pig", 8, 2, 3, 7, 63), mobPart("horse", 35, 3, 4, 7, 63)), 63, 7);

		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(parts, 81, 9, true);

		assertTrue(result.fits());
		assertEquals(parts.size(), result.fittedSlots().size());
		assertValidLayout(result, parts, 81, 9);
	}

	@Test
	void compactingKeepsAllStacksWithWideMobPartsAfterColumnsAreRestored() {
		List<InventoryLayoutPart> parts = tankLayoutPartsWithStacks(
				List.of(mobPart("top", 0, 4, 3, 7, 63), mobPart("middle", 24, 4, 3, 7, 63), mobPart("bottom", 42, 4, 3, 7, 63)), 63, 7);

		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(parts, 81, 9, true);

		assertTrue(result.fits());
		assertEquals(parts.size(), result.fittedSlots().size());
		assertValidLayout(result, parts, 81, 9);
	}

	@Test
	void compactingDoesNotKeepWideMobPartsAtTankColumnSlotsWhenColumnsAreRestored() {
		List<InventoryLayoutPart> parts = tankLayoutPartsWithStacks(
				List.of(mobPart("top", 0, 4, 3, 7, 63), mobPart("middle", 24, 4, 3, 7, 63), mobPart("bottom", 42, 4, 3, 7, 63)), 63, 7);

		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(parts, 81, 9, true);

		assertTrue(result.fits());
		assertEquals(0, result.fittedSlots().get("mob:top"));
		assertFalse(result.fittedSlots().get("mob:middle") == 24, "middle mob stayed at a 7-column slot that crosses a 9-column row");
		assertFalse(result.fittedSlots().get("mob:bottom") == 42, "bottom mob stayed at a 7-column slot that crosses a 9-column row");
		assertEquals(parts.size(), result.fittedSlots().size());
		assertValidLayout(result, parts, 81, 9);
	}

	@Test
	void compactingKeepsAllStacksWithWideMobPartsAfterMobTargetSlotRemapping() {
		List<InventoryLayoutPart> parts = tankLayoutPartsWithStacks(List.of(mobPart("top", targetSlot(0, 4, 3, 7, 9, 63), 0, 4, 3, 7, 63),
				mobPart("middle", targetSlot(24, 4, 3, 7, 9, 63), 24, 4, 3, 7, 63), mobPart("bottom", targetSlot(42, 4, 3, 7, 9, 63), 42, 4, 3, 7, 63)), 63, 7);

		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(parts, 81, 9, true);

		assertTrue(result.fits());
		assertEquals(parts.size(), result.fittedSlots().size());
		assertValidLayout(result, parts, 81, 9);
	}

	@Test
	void compactingKeepsAllStacksForDeterministicMobPartCombinationsAfterColumnsAreRestored() {
		List<List<InventoryLayoutPart>> mobPartCombinations = List.of(
				List.of(mobPart("a", targetSlot(0, 2, 2, 7, 9, 63), 0, 2, 2, 7, 63), mobPart("b", targetSlot(10, 3, 2, 7, 9, 63), 10, 3, 2, 7, 63),
						mobPart("c", targetSlot(35, 2, 3, 7, 9, 63), 35, 2, 3, 7, 63)),
				List.of(mobPart("a", targetSlot(0, 4, 3, 7, 9, 63), 0, 4, 3, 7, 63), mobPart("b", targetSlot(24, 4, 3, 7, 9, 63), 24, 4, 3, 7, 63),
						mobPart("c", targetSlot(42, 4, 3, 7, 9, 63), 42, 4, 3, 7, 63)),
				List.of(mobPart("a", targetSlot(0, 1, 4, 7, 9, 63), 0, 1, 4, 7, 63), mobPart("b", targetSlot(8, 3, 3, 7, 9, 63), 8, 3, 3, 7, 63),
						mobPart("c", targetSlot(37, 3, 2, 7, 9, 63), 37, 3, 2, 7, 63)),
				List.of(mobPart("a", targetSlot(2, 2, 4, 7, 9, 63), 2, 2, 4, 7, 63), mobPart("b", targetSlot(21, 4, 2, 7, 9, 63), 21, 4, 2, 7, 63),
						mobPart("c", targetSlot(39, 2, 3, 7, 9, 63), 39, 2, 3, 7, 63)),
				List.of(mobPart("a", targetSlot(0, 5, 2, 7, 9, 63), 0, 5, 2, 7, 63), mobPart("b", targetSlot(21, 2, 5, 7, 9, 63), 21, 2, 5, 7, 63),
						mobPart("c", targetSlot(40, 2, 2, 7, 9, 63), 40, 2, 2, 7, 63)));

		for (List<InventoryLayoutPart> mobParts : mobPartCombinations) {
			List<InventoryLayoutPart> parts = tankLayoutPartsWithStacks(mobParts, 63, 7);
			InventoryLayoutFitResult result = InventoryLayoutFitter.fit(parts, 81, 9, true);

			assertTrue(result.fits(), mobParts.toString());
			assertEquals(parts.size(), result.fittedSlots().size(), mobParts.toString());
			assertValidLayout(result, parts, 81, 9);
		}
	}

	@Test
	void compactingKeepsAllStacksForGeneratedMobPartCombinationsAfterColumnsAreRestored() {
		Random random = new Random(12345L);
		for (int scenario = 0; scenario < 30; scenario++) {
			List<InventoryLayoutPart> mobParts = generatedMobParts(random, scenario, 63, 7, 9);
			List<InventoryLayoutPart> parts = tankLayoutPartsWithStacks(mobParts, 63, 7);

			InventoryLayoutFitResult result = InventoryLayoutFitter.fit(parts, 81, 9, true);

			assertTrue(result.fits(), "scenario " + scenario + ": " + mobParts);
			assertEquals(parts.size(), result.fittedSlots().size(), "scenario " + scenario + ": " + mobParts);
			assertValidLayout(result, parts, 81, 9);
		}
	}

	private static InventoryLayoutPart mobPart(String mobName, int firstSlot, int width, int height, int columns, int slots) {
		return new InventoryLayoutPart("mob:" + mobName, firstSlot, width, height, occupiedSlots(firstSlot, width, height, columns, slots));
	}

	private static InventoryLayoutPart mobPart(String mobName, int targetSlot, int sourceSlot, int width, int height, int columns, int slots) {
		return new InventoryLayoutPart("mob:" + mobName, targetSlot, width, height, occupiedSlots(sourceSlot, width, height, columns, slots));
	}

	private static int targetSlot(int sourceSlot, int width, int height, int columns, int targetColumns, int inventorySlots) {
		int rows = Math.max(1, (int) Math.ceil((double) inventorySlots / columns));
		int targetX = Math.min(sourceSlot % columns, Math.max(0, targetColumns - width));
		int targetY = Math.min(sourceSlot / columns, Math.max(0, rows - height));
		return targetY * targetColumns + targetX;
	}

	private static List<InventoryLayoutPart> generatedMobParts(Random random, int scenario, int slots, int columns, int targetColumns) {
		Set<Integer> occupiedSlots = new HashSet<>();
		List<InventoryLayoutPart> mobParts = new ArrayList<>();
		int mobCount = 2 + scenario % 4;
		for (int mob = 0; mob < mobCount; mob++) {
			int width = 1 + random.nextInt(3);
			int height = 1 + random.nextInt(3);
			int sourceSlot = findGeneratedMobSlot(random, slots, columns, width, height, occupiedSlots);
			occupiedSlots.addAll(occupiedSlots(sourceSlot, width, height, columns, slots));
			mobParts.add(mobPart("generated" + scenario + "_" + mob, targetSlot(sourceSlot, width, height, columns, targetColumns, slots), sourceSlot, width,
					height, columns, slots));
		}
		return mobParts;
	}

	private static int findGeneratedMobSlot(Random random, int slots, int columns, int width, int height, Set<Integer> occupiedSlots) {
		for (int attempt = 0; attempt < slots * 2; attempt++) {
			int slot = random.nextInt(slots);
			if (rectangleFits(slot, width, height, columns, slots, occupiedSlots)) {
				return slot;
			}
		}

		for (int slot = 0; slot < slots; slot++) {
			if (rectangleFits(slot, width, height, columns, slots, occupiedSlots)) {
				return slot;
			}
		}
		throw new IllegalStateException("Could not place generated mob part");
	}

	private static boolean rectangleFits(int firstSlot, int width, int height, int columns, int slots, Set<Integer> occupiedSlots) {
		if (firstSlot % columns + width > columns) {
			return false;
		}
		for (int slot : occupiedSlots(firstSlot, width, height, columns, slots)) {
			if (occupiedSlots.contains(slot)) {
				return false;
			}
		}
		return occupiedSlots(firstSlot, width, height, columns, slots).size() == width * height;
	}

	private static List<InventoryLayoutPart> tankLayoutPartsWithStacks(List<InventoryLayoutPart> mobParts, int slots, int columns) {
		Set<Integer> mobSlots = new HashSet<>();
		mobParts.forEach(part -> mobSlots.addAll(part.sourceSlots()));

		List<InventoryLayoutPart> parts = new ArrayList<>();
		for (int slot = 0; slot < slots; slot++) {
			int finalSlot = slot;
			mobParts.stream().filter(part -> part.sourceSlots().stream().min(Integer::compareTo).orElse(-1) == finalSlot).findFirst().ifPresent(parts::add);
			if (!mobSlots.contains(slot)) {
				parts.add(new InventoryLayoutPart("stack:" + slot, slot, 1, 1, Set.of(slot)));
			}
		}
		return parts;
	}

	private static Set<Integer> occupiedSlots(int firstSlot, int width, int height, int columns, int slots) {
		Set<Integer> occupiedSlots = new HashSet<>();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int slot = firstSlot + y * columns + x;
				if (slot < slots) {
					occupiedSlots.add(slot);
				}
			}
		}
		return occupiedSlots;
	}

	private static void assertValidLayout(InventoryLayoutFitResult result, List<InventoryLayoutPart> parts, int targetSlots, int targetColumns) {
		Set<Integer> occupiedSlots = new HashSet<>();
		for (InventoryLayoutPart part : parts) {
			int firstSlot = result.fittedSlots().get(part.id());
			assertTrue(firstSlot % targetColumns + part.width() <= targetColumns, part.id() + " crosses target row at slot " + firstSlot);
			for (int y = 0; y < part.height(); y++) {
				for (int x = 0; x < part.width(); x++) {
					int slot = firstSlot + y * targetColumns + x;
					assertTrue(slot < targetSlots, part.id() + " exceeds target inventory at slot " + slot);
					assertTrue(occupiedSlots.add(slot), part.id() + " overlaps at slot " + slot);
				}
			}
		}
	}

	@Test
	void fixedPartMustRemainAtOriginalSlot() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("fixed:8", 8, 1, 1, Set.of(8))), 8, 8);

		assertFalse(result.fits());
		assertEquals(Set.of(8), result.errorSlots());
	}

	@Test
	void failingPartMarksItselfAndRemainingPartsAsErrors() {
		InventoryLayoutFitResult result = InventoryLayoutFitter.fit(List.of(new InventoryLayoutPart("stack:0", 0, 1, 1, Set.of(0)),
				new InventoryLayoutPart("large:one", 7, 2, 2, Set.of(7, 8, 16, 17)), new InventoryLayoutPart("stack:26", 26, 1, 1, Set.of(26))), 8, 8);

		assertFalse(result.fits());
		assertEquals(Set.of(7, 8, 16, 17, 26), result.errorSlots());
		assertEquals(Map.of("stack:0", 0), result.fittedSlots());
	}
}
