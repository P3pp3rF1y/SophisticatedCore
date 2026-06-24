package net.p3pp3rf1y.sophisticatedcore.compat.trashslot;

import net.blay09.mods.trashslot.api.TrashSlotAPI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class TrashSlotCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			modBus.addListener(this::onLoadComplete);
		}
	}

	@Override
	public void setup() {
		// noop
	}

	private void onLoadComplete(FMLLoadCompleteEvent event) {
		event.enqueueWork(() -> TrashSlotScreenRegistry.getRegisteredScreens()
				.forEach(screenClass -> TrashSlotAPI.registerLayout(screenClass, SophisticatedContainerLayout.INSTANCE)));
	}
}
