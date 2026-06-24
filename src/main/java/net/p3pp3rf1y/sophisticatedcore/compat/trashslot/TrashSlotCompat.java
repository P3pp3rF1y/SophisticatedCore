package net.p3pp3rf1y.sophisticatedcore.compat.trashslot;

import net.blay09.mods.trashslot.api.TrashSlotAPI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class TrashSlotCompat implements ICompat {
	@Override
	public void setup() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onLoadComplete);
		}
	}

	private void onLoadComplete(FMLLoadCompleteEvent event) {
		event.enqueueWork(() -> TrashSlotScreenRegistry.getRegisteredScreens()
				.forEach(screenClass -> TrashSlotAPI.registerLayout(screenClass, SophisticatedContainerLayout.INSTANCE)));
	}
}
