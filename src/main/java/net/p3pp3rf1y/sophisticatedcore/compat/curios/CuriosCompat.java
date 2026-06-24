package net.p3pp3rf1y.sophisticatedcore.compat.curios;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

public class CuriosCompat implements ICompat {
	@Override
	public void setup() {
		addInventoryItemHandler();
	}

	private void addInventoryItemHandler() {
		InventoryHelper.registerPlayerInventoryProvider(
				player -> CuriosApi.getCuriosInventory(player).<IItemHandler>map(ICuriosItemHandler::getEquippedCurios).orElse(EmptyItemHandler.INSTANCE));
		InventoryHelper.registerEquipmentInventoryProvider(
				player -> CuriosApi.getCuriosInventory(player).<IItemHandler>map(ICuriosItemHandler::getEquippedCurios).orElse(EmptyItemHandler.INSTANCE));
	}
}
