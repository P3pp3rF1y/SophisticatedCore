package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.neoforged.neoforge.transfer.item.ItemResource;

@FunctionalInterface
public interface ISlotResourceMutator {
	void set(int slot, ItemResource res, int amount);
}
