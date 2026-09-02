package net.p3pp3rf1y.sophisticatedcore.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;

public final class ModItems {
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SophisticatedCore.MOD_ID);
	public static final RegistryObject<EnderLinkerItem> ENDER_LINKER = ITEMS.register("ender_linker", EnderLinkerItem::new);

	private ModItems() {
	}

	public static void registerHandlers(IEventBus modBus) {
		ITEMS.register(modBus);
	}
}
