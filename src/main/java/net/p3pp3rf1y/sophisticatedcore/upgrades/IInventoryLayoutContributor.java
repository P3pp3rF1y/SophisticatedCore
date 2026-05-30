package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutFitResult;
import net.p3pp3rf1y.sophisticatedcore.api.InventoryLayoutPart;

import java.util.Optional;

public interface IInventoryLayoutContributor {
	boolean isInventoryLayoutSlotHandled(int slot, int columns);

	Optional<InventoryLayoutPart> getInventoryLayoutPart(int slot, int columns, int targetColumns);

	default void applyInventoryLayout(InventoryLayoutFitResult fitResult, int columns) {
		// noop
	}
}
