package net.p3pp3rf1y.sophisticatedcore.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerClearRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerEndpointRecipe;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.init.ModPayloads;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.RecentCraftedResultStorage;
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
		eventBus.addListener(CommonEventHandler::onPlayerLoggedIn);
		eventBus.addListener(CommonEventHandler::onPlayerLoggedOut);
		eventBus.addListener(CommonEventHandler::onEnderLinkCrafted);
	}

	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			RecentCraftedResultStorage.syncToPlayer(serverPlayer);
		}
	}

	private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		EnderLinkerEndpointRecipe.clearInFlightClaim(event.getEntity().getUUID());
	}

	private static void onEnderLinkCrafted(PlayerEvent.ItemCraftedEvent event) {
		if (event.getEntity().level() instanceof ServerLevel level) {
			EnderLinkerClearRecipe.clearPendingCraftClaim(level, event.getInventory());
			EnderLinkerEndpointRecipe.issueCraftClaim(event.getEntity(), event.getCrafting());
			if (EnderLinkerEndpointRecipe.completeCraft(level, event.getEntity(), event.getInventory(), event.getCrafting())) {
				EnderLinkerEndpointRecipe.finalizeDeliveredCraftResult(level, event.getCrafting(), event.getEntity().getInventory(),
						event.getEntity().containerMenu.getCarried());
			}
		}
	}

	public static void onTickEnd(ServerTickEvent.Post event) {
		ItemStackKey.clearCache();
		for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
			EnderLinkerEndpointRecipe.finalizePendingCraftResult(player.level(), player.containerMenu.getCarried());
		}
		EnderLinkerEndpointRecipe.clearInFlightClaims();
	}
}
