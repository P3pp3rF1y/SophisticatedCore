package net.p3pp3rf1y.sophisticatedcore.common;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;

public class CommonEventHandler {
	public void registerHandlers() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModFluids.registerHandlers(modBus);
		ModParticles.registerParticles(modBus);
		ModRecipes.registerHandlers(modBus);
	}
}
