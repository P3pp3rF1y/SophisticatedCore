package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CommonPayloads;

public class JeiCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		modBus.addListener(this::registerPackets);
		if (FMLEnvironment.dist == Dist.CLIENT) {
			JeiClientCompat.init();
		}
	}

	@Override
	public void setup() {
		//noop
	}

	private void registerPackets(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(SophisticatedCore.MOD_ID).versioned("1.0");
		registrar.optional().playToServer(JeiTransferRecipePayload.TYPE, JeiTransferRecipePayload.STREAM_CODEC, JeiTransferRecipePayload::handlePayload);
		
		CommonPayloads.registerPackets(registrar);
	}
}
