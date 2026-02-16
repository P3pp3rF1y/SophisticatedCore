package net.p3pp3rf1y.sophisticatedcore.common;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.init.ModPayloads;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.CoreFakePlayer;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

public class CommonEventHandler {
	public void registerHandlers(IEventBus modBus) {
		ModFluids.registerHandlers(modBus);
		ModParticles.registerParticles(modBus);
		ModRecipes.registerHandlers(modBus);
		modBus.addListener(ModPayloads::registerPayloads);
		IEventBus eventBus = NeoForge.EVENT_BUS;

		eventBus.addListener(CommonEventHandler::onTickEnd);
		eventBus.addListener(RecipeHelper::onDataPackSync);
		eventBus.addListener(RecipeHelper::onRecipesUpdated);
		eventBus.addListener(ServerStorageSoundHandler::onWorldUnload);
		eventBus.addListener(ServerStorageSoundHandler::tick);
		eventBus.addListener(MagnetUpgradeWrapper::globalPostTick);
		eventBus.addListener(MagnetUpgradeWrapper::onWorldUnload);
		eventBus.addListener(CoreFakePlayer::onDimensionUnload);
	}

	public static void onTickEnd(ServerTickEvent.Post event) {
		ItemStackKey.clearCache();
	}
}
