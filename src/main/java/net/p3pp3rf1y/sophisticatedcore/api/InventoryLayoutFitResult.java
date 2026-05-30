package net.p3pp3rf1y.sophisticatedcore.api;

import java.util.Map;
import java.util.Set;

public record InventoryLayoutFitResult(boolean fits, Set<Integer> errorSlots, Map<String, Integer> fittedSlots) {
	public InventoryLayoutFitResult {
		errorSlots = Set.copyOf(errorSlots);
		fittedSlots = Map.copyOf(fittedSlots);
	}

	public static InventoryLayoutFitResult fit(Map<String, Integer> fittedSlots) {
		return new InventoryLayoutFitResult(true, Set.of(), fittedSlots);
	}

	public static InventoryLayoutFitResult fail(Set<Integer> errorSlots, Map<String, Integer> fittedSlots) {
		return new InventoryLayoutFitResult(false, errorSlots, fittedSlots);
	}
}
