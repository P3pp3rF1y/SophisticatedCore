package net.p3pp3rf1y.sophisticatedcore.common;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.init.ModPackets;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

public class CommonEventHandler {
	public void registerHandlers(IEventBus modBus) {
		ModFluids.registerHandlers(modBus);
		ModParticles.registerParticles(modBus);
		ModRecipes.registerHandlers(modBus);
		modBus.addListener(ModPackets::registerPackets);
		NeoForge.EVENT_BUS.addListener(ItemStackKey::clearCacheOnTickEnd);
		NeoForge.EVENT_BUS.addListener(RecipeHelper::onDataPackSync);
		NeoForge.EVENT_BUS.addListener(RecipeHelper::onRecipesUpdated);
	}
}
