package net.p3pp3rf1y.sophisticatedcore.compat.curios;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class CuriosCompat implements ICompat {
	@Override
	public void setup() {
		addInventoryItemHandler();
	}

	private void addInventoryItemHandler() {
		/*
		 * TODO readd with updated curios API InventoryHelper.registerPlayerInventoryProvider(player ->
		 * CuriosApi.getCuriosInventory(player).<ResourceHandler<ItemResource>>map(ICuriosItemHandler::getEquippedCurios).orElse(EmptyItemHandler.INSTANCE));
		 * InventoryHelper.registerEquipmentInventoryProvider(player ->
		 * CuriosApi.getCuriosInventory(player).<ResourceHandler<ItemResource>>map(ICuriosItemHandler::getEquippedCurios).orElse(EmptyItemHandler.INSTANCE));
		 */
	}
}
