package net.p3pp3rf1y.sophisticatedcore.init;

import net.p3pp3rf1y.sophisticatedcore.compat.CompatInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticatedcore.compat.create.CreateCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.curios.CuriosCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.ftbchunks.FTBChunksCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.inventorysorter.InventorySorterCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.itemborders.ItemBordersCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.mousetweaks.MouseTweaksCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.openpartiesandclaims.OpenPACCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.reliquary.ReliquaryCompat;

public class ModCompat {
	private ModCompat() {
	}

	public static void register() {
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.JEI), () -> modBus -> new JeiCompat());
		//TODO reenable when emi updates, also uncomment in Chipped and Sawmill compats, also reenable in all other mods
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.EMI), () -> modBus -> new EmiCompat());
		//TODO reenable when rei updates, also uncomment in Chipped and Sawmill compats, also reenable in all other mods
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.REI), () -> modBus -> new ReiCompat());
		// Disabled during 26.1 port until upstream API stabilizes.
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CRAFTING_TWEAKS), () -> modBus -> new CraftingTweaksCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.INVENTORY_SORTER), () -> modBus -> new InventorySorterCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.ITEM_BORDERS), () -> mobBus -> new ItemBordersCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CURIOS), () -> mobBus -> new CuriosCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CREATE), () -> mobBus -> new CreateCompat());
		// Disabled during 26.1 port until upstream API stabilizes.
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.TRASH_SLOT), () -> mobBus -> new TrashSlotCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.RELIQUARY), () -> mobBus -> new ReliquaryCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.MOUSE_TWEAKS), () -> mobBus -> new MouseTweaksCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.FTB_CHUNKS), () -> mobBus -> new FTBChunksCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.OPEN_PARTIES_AND_CLAIMS_CHUNKS), () -> mobBus -> new OpenPACCompat());
		// Disabled during 26.1 port until upstream API stabilizes.
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.ACCESSORIES), () -> mobBus -> new AccessoriesCompat());
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.QUARK, null), QuarkCompat::new); //TODO readd quark compat
	}
}
