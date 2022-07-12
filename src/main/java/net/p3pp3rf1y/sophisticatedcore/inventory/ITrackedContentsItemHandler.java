package net.p3pp3rf1y.sophisticatedcore.inventory;

import java.util.Set;
import java.util.function.Consumer;

public interface ITrackedContentsItemHandler extends IItemHandlerSimpleInserter {

	Set<ItemStackKey> getTrackedStacks();

	void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot);

	void unregisterStackKeyListeners();

	boolean hasEmptySlots();
}
