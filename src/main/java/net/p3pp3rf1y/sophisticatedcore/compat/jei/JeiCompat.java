package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class JeiCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		modBus.addListener(this::registerPackets);
	}

	@Override
	public void setup() {
		//noop
	}

	private void registerPackets(final RegisterPayloadHandlerEvent event) {
		final IPayloadRegistrar registrar = event.registrar(SophisticatedCore.MOD_ID).versioned("1.0");
		registrar.play(TransferRecipeMessage.ID, TransferRecipeMessage::new, play -> play.server(TransferRecipeMessage::handle));
		registrar.play(SetGhostSlotMessage.ID, SetGhostSlotMessage::new, play -> play.server(SetGhostSlotMessage::handle));
		registrar.play(SetMemorySlotMessage.ID, SetMemorySlotMessage::new, play -> play.server(SetMemorySlotMessage::handle));
	}
}
