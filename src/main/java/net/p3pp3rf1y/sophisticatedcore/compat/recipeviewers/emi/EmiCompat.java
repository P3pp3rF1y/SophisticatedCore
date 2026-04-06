package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CommonPayloads;

public class EmiCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		SettingsContainerMenu.setConstructCallback(menu -> menu.addInternalCompatibilitySlot(new EmiAnchorSlot()));
		modBus.addListener(this::registerPackets);
	}

	@Override
	public void setup() {
		//noop
	}

	private void registerPackets(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(SophisticatedCore.MOD_ID).versioned("1.0");
		registrar.optional().playToServer(EmiTransferRecipePayload.TYPE, EmiTransferRecipePayload.STREAM_CODEC, EmiTransferRecipePayload::handlePayload);

		CommonPayloads.registerPackets(registrar);
	}

	private static class EmiAnchorSlot extends Slot {
		public EmiAnchorSlot() {
			super(new SimpleContainer(1), 0, -10000, -10000);
		}

		@Override
		public boolean isActive() {
			return false;
		}
	}
}
