package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CommonPayloads;

public class JeiCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		modBus.addListener(this::registerPackets);
	}

	@Override
	public void setup() {
		// noop
	}

	private void registerPackets(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(SophisticatedCore.MOD_ID).versioned(SophisticatedCore.getNetworkProtocolVersion());
		registrar.optional().playToServer(JeiTransferRecipePayload.TYPE, JeiTransferRecipePayload.STREAM_CODEC, JeiTransferRecipePayload::handlePayload);

		CommonPayloads.registerPackets(registrar);
	}
}
