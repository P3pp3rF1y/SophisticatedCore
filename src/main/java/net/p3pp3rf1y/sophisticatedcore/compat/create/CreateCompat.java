package net.p3pp3rf1y.sophisticatedcore.compat.create;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class CreateCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		modBus.addListener(this::registerPayloads);
	}

	@Override
	public void setup() {

	}

	public void registerPayloads(final RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(SophisticatedCore.MOD_ID).versioned("1.0");
		registrar.playToClient(MountedStorageContentsPayload.TYPE, MountedStorageContentsPayload.STREAM_CODEC, MountedStorageContentsPayload::handlePayload);
		registrar.playToClient(MountedStorageSettingsPayload.TYPE, MountedStorageSettingsPayload.STREAM_CODEC, MountedStorageSettingsPayload::handlePayload);
		registrar.playToClient(MountedStorageUpdatePayload.TYPE, MountedStorageUpdatePayload.STREAM_CODEC, MountedStorageUpdatePayload::handlePayload);
	}
}
