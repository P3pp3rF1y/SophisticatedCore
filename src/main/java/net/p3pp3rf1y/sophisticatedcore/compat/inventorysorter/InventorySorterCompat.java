package net.p3pp3rf1y.sophisticatedcore.compat.inventorysorter;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.FilterSlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;

public class InventorySorterCompat implements ICompat {
	private static final String SLOTBLACKLIST = "slotblacklist";

	@Override
	public void init(IEventBus modBus) {
		ICompat.super.init(modBus);
		modBus.addListener(this::sendImc);
	}

	private void sendImc(InterModEnqueueEvent evt) {
		evt.enqueueWork(() -> {
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, StorageContainerMenuBase.StorageUpgradeSlot.class::getName);
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, FilterSlotItemHandler.class::getName);
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, FilterLogicContainer.FilterLogicSlot.class::getName);
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, SlotSuppliedHandler.class::getName);
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, StorageInventorySlot.class::getName);
		});
	}

	@Override
	public void setup() {
		//noop
	}
}
