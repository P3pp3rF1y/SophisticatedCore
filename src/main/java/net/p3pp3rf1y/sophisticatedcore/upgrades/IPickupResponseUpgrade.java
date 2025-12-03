package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public interface IPickupResponseUpgrade {
	int pickup(Level level, ItemResource resource, int amount, TransactionContext tx);
}
