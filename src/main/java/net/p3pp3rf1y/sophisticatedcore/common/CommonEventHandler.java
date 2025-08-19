package net.p3pp3rf1y.sophisticatedcore.common;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.CoreFakePlayer;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

public class CommonEventHandler {
	public void registerHandlers() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModFluids.registerHandlers(modBus);
		ModParticles.registerParticles(modBus);
		ModRecipes.registerHandlers(modBus);
		IEventBus eventBus = MinecraftForge.EVENT_BUS;
		eventBus.addListener(ItemStackKey::clearCacheOnTickEnd);
		eventBus.addListener(RecipeHelper::onDataPackSync);
		eventBus.addListener(RecipeHelper::onRecipesUpdated);
		eventBus.addListener(MagnetUpgradeWrapper::globalPostTick);
		eventBus.addListener(MagnetUpgradeWrapper::onWorldUnload);
		eventBus.addListener(CoreFakePlayer::onDimensionUnload);
	}
}
