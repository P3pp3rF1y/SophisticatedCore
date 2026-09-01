package net.p3pp3rf1y.sophisticatedcore.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedcore.Config;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;

import java.util.function.Supplier;

public final class ModItems {
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, SophisticatedCore.MOD_ID);
	public static final DeferredHolder<Item, EnderLinkerItem> ENDER_LINKER = ITEMS.register("ender_linker", EnderLinkerItem::new);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB.identifier(),
			SophisticatedCore.MOD_ID);
	public static final Supplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("main",
			() -> CreativeModeTab.builder().icon(() -> new ItemStack(ModFluids.XP_BUCKET.get())).title(Component.translatable("itemGroup.sophisticatedcore"))
					.displayItems((featureFlags, output) -> {
						output.accept(new ItemStack(ModFluids.XP_BUCKET.get()));
						if (Config.COMMON.enabledItems.isItemEnabled(ENDER_LINKER.get())
								&& (ModList.get().isLoaded("sophisticatedbackpacks") || ModList.get().isLoaded("sophisticatedstorage"))) {
							output.accept(new ItemStack(ENDER_LINKER.get()));
						}
					}).build());

	private ModItems() {
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		CREATIVE_MODE_TABS.register(modBus);
	}
}
