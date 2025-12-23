package net.p3pp3rf1y.sophisticatedcore.common;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.init.*;
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
		ModTags.registerTags();
		modBus.addListener(ModPayloads::registerPayloads);
		IEventBus eventBus = NeoForge.EVENT_BUS;

		eventBus.addListener(ItemStackKey::clearCacheOnTickEnd);
		eventBus.addListener(RecipeHelper::onDataPackSync);
		eventBus.addListener(RecipeHelper::onRecipesUpdated);
		eventBus.addListener(ServerStorageSoundHandler::onWorldUnload);
		eventBus.addListener(ServerStorageSoundHandler::tick);
		eventBus.addListener(MagnetUpgradeWrapper::globalPostTick);
		eventBus.addListener(MagnetUpgradeWrapper::onWorldUnload);
		eventBus.addListener(CoreFakePlayer::onDimensionUnload);
	}
}
