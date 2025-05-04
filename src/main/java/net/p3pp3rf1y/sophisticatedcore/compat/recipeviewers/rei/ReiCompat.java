package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CommonPayloads;

public class ReiCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		modBus.addListener(this::registerPackets);
	}

	@Override
	public void setup() {
		//noop
	}

	private void registerPackets(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(SophisticatedCore.MOD_ID).versioned("1.0");
		registrar.optional().playToServer(TransferRecipePayload.TYPE, TransferRecipePayload.STREAM_CODEC, TransferRecipePayload::handlePayload);

		CommonPayloads.registerPackets(registrar);
	}
}
