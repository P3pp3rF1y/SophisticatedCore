package net.p3pp3rf1y.sophisticatedcore.init;

import net.p3pp3rf1y.sophisticatedcore.compat.CompatInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks.CraftingTweaksCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.inventorysorter.InventorySorterCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.itemborders.ItemBordersCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.JeiCompat;

public class ModCompat {
	private ModCompat() {
	}

	public static void register() {
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.JEI, null), () -> modBus -> new JeiCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CRAFTING_TWEAKS, null), () -> modBus -> new CraftingTweaksCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.INVENTORY_SORTER, null), () -> modBus -> new InventorySorterCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.ITEM_BORDERS, null), () -> mobBus -> new ItemBordersCompat());
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.QUARK, null), QuarkCompat::new); //TODO readd quark compat
	}
}
