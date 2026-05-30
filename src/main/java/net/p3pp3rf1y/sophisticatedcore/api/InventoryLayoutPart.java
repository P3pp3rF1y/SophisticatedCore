package net.p3pp3rf1y.sophisticatedcore.api;

import java.util.Set;

public record InventoryLayoutPart(String id, int firstSlot, int width, int height, Set<Integer> sourceSlots) {
	public InventoryLayoutPart {
		sourceSlots = Set.copyOf(sourceSlots);
	}
}
