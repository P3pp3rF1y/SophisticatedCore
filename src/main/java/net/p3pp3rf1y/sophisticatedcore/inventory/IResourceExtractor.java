package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public interface IResourceExtractor {
	int extract(int slot, ItemResource resource, int amount, TransactionContext transaction);
}
