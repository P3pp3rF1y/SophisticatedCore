package net.p3pp3rf1y.sophisticatedcore.init;

import net.p3pp3rf1y.sophisticatedcore.compat.CompatInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks.CraftingTweaksCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.create.CreateCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.curios.CuriosCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.EmiCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.inventorysorter.InventorySorterCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.itemborders.ItemBordersCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.reliquary.ReliquaryCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei.ReiCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.trashslot.TrashSlotCompat;

public class ModCompat {
	private ModCompat() {
	}

	public static void register() {
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.JEI), () -> modBus -> new JeiCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.EMI), () -> modBus -> new EmiCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.REI), () -> modBus -> new ReiCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CRAFTING_TWEAKS), () -> modBus -> new CraftingTweaksCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.INVENTORY_SORTER), () -> modBus -> new InventorySorterCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.ITEM_BORDERS), () -> mobBus -> new ItemBordersCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CURIOS), () -> mobBus -> new CuriosCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CREATE), () -> mobBus -> new CreateCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.TRASH_SLOT), () -> mobBus -> new TrashSlotCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.RELIQUARY), () -> mobBus -> new ReliquaryCompat());
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.QUARK, null), QuarkCompat::new); //TODO readd quark compat
	}
}
