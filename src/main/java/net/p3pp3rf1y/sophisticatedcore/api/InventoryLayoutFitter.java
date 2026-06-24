package net.p3pp3rf1y.sophisticatedcore.api;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InventoryLayoutFitter {
	private InventoryLayoutFitter() {
	}

	public static InventoryLayoutFitResult fit(List<InventoryLayoutPart> parts, int targetSlots, int targetColumns) {
		return fit(parts, targetSlots, targetColumns, false);
	}

	public static InventoryLayoutFitResult fit(List<InventoryLayoutPart> parts, int targetSlots, int targetColumns, boolean compact) {
		InventoryLayoutFitResult result = fitInternal(parts, targetSlots, targetColumns, compact, compact);
		if (compact || result.fits()) {
			return result;
		}

		InventoryLayoutFitResult compactResult = fitInternal(parts, targetSlots, targetColumns, true, false);
		if (compactResult.fits()) {
			return compactResult;
		}

		InventoryLayoutFitResult reorderedCompactResult = fitInternal(orderPartsForCompaction(parts), targetSlots, targetColumns, true, false, true);
		return reorderedCompactResult.fits() ? reorderedCompactResult : compactResult;
	}

	private static List<InventoryLayoutPart> orderPartsForCompaction(List<InventoryLayoutPart> parts) {
		return parts.stream()
				.sorted(Comparator.comparingInt(InventoryLayoutFitter::compactionPriority)
						.thenComparing(Comparator.comparingInt((InventoryLayoutPart part) -> part.width() * part.height()).reversed())
						.thenComparingInt(InventoryLayoutPart::firstSlot))
				.toList();
	}

	private static int compactionPriority(InventoryLayoutPart part) {
		if (part.id().startsWith("fixed:")) {
			return 0;
		}
		return part.id().startsWith("stack:") ? 2 : 1;
	}

	private static InventoryLayoutFitResult fitInternal(List<InventoryLayoutPart> parts, int targetSlots, int targetColumns, boolean compact,
			boolean preserveStacks) {
		return fitInternal(parts, targetSlots, targetColumns, compact, preserveStacks, false);
	}

	private static InventoryLayoutFitResult fitInternal(List<InventoryLayoutPart> parts, int targetSlots, int targetColumns, boolean compact,
			boolean preserveStacks, boolean fillGapsWithStacks) {
		Set<Integer> occupiedSlots = new HashSet<>();
		Map<String, Integer> fittedSlots = new HashMap<>();
		Set<Integer> errorSlots = new HashSet<>();
		int nextSlot = 0;

		for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
			InventoryLayoutPart part = parts.get(partIndex);
			int fittedSlot = findNextFit(part, fillGapsWithStacks && part.id().startsWith("stack:") ? 0 : nextSlot, targetSlots, targetColumns, occupiedSlots,
					compact, preserveStacks);
			if (fittedSlot < 0) {
				for (int remainingPartIndex = partIndex; remainingPartIndex < parts.size(); remainingPartIndex++) {
					errorSlots.addAll(parts.get(remainingPartIndex).sourceSlots());
				}
				return InventoryLayoutFitResult.fail(errorSlots, fittedSlots);
			}

			occupy(part, fittedSlot, targetColumns, occupiedSlots);
			fittedSlots.put(part.id(), fittedSlot);
			nextSlot = !compact && fittedSlot == part.firstSlot()
					? Math.max(nextSlot, getSlotAfterPart(part, fittedSlot, targetColumns))
					: fittedSlot + part.width();
		}

		return InventoryLayoutFitResult.fit(fittedSlots);
	}

	private static int findNextFit(InventoryLayoutPart part, int nextSlot, int targetSlots, int targetColumns, Set<Integer> occupiedSlots, boolean compact,
			boolean preserveStacks) {
		if (part.id().startsWith("fixed:")) {
			return fits(part, part.firstSlot(), targetSlots, targetColumns, occupiedSlots) ? part.firstSlot() : -1;
		}
		if (shouldPreserveFirstSlot(part, compact, preserveStacks) && part.firstSlot() < targetSlots
				&& fits(part, part.firstSlot(), targetSlots, targetColumns, occupiedSlots)) {
			return part.firstSlot();
		}

		int startSlot = compact || part.firstSlot() >= targetSlots ? nextSlot : Math.max(nextSlot, part.firstSlot());
		for (int slot = startSlot; slot < targetSlots; slot++) {
			if (fits(part, slot, targetSlots, targetColumns, occupiedSlots)) {
				return slot;
			}
		}
		return -1;
	}

	private static boolean shouldPreserveFirstSlot(InventoryLayoutPart part, boolean compact, boolean preserveStacks) {
		return !compact || preserveStacks && part.id().startsWith("stack:");
	}

	private static boolean fits(InventoryLayoutPart part, int slot, int targetSlots, int targetColumns, Set<Integer> occupiedSlots) {
		int x = slot % targetColumns;
		if (x + part.width() > targetColumns) {
			return false;
		}

		for (int y = 0; y < part.height(); y++) {
			for (int partX = 0; partX < part.width(); partX++) {
				int checkedSlot = slot + y * targetColumns + partX;
				if (checkedSlot >= targetSlots || occupiedSlots.contains(checkedSlot)) {
					return false;
				}
			}
		}
		return true;
	}

	private static void occupy(InventoryLayoutPart part, int slot, int targetColumns, Set<Integer> occupiedSlots) {
		for (int y = 0; y < part.height(); y++) {
			for (int x = 0; x < part.width(); x++) {
				occupiedSlots.add(slot + y * targetColumns + x);
			}
		}
	}

	private static int getSlotAfterPart(InventoryLayoutPart part, int slot, int targetColumns) {
		return slot + (part.height() - 1) * targetColumns + part.width();
	}
}
