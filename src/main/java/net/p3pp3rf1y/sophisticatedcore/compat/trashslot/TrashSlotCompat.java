package net.p3pp3rf1y.sophisticatedcore.compat.trashslot;

import net.blay09.mods.trashslot.api.TrashSlotAPI;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class TrashSlotCompat implements ICompat {
	@Override
	public void setup() {
		TrashSlotScreenRegistry.getRegisteredScreens()
				.forEach(screenClass -> TrashSlotAPI.registerLayout(screenClass, SophisticatedContainerLayout.INSTANCE));
	}
}
