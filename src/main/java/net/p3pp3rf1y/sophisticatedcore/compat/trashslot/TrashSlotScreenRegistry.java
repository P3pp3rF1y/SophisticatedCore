package net.p3pp3rf1y.sophisticatedcore.compat.trashslot;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.HashSet;
import java.util.Set;

public class TrashSlotScreenRegistry {
	private static final Set<Class<? extends AbstractContainerScreen<?>>> SCREENS = new HashSet<>();

	public static void registerScreen(Class<? extends AbstractContainerScreen<?>> screenClass) {
		SCREENS.add(screenClass);
	}

	public static Set<Class<? extends AbstractContainerScreen<?>>> getRegisteredScreens() {
		return SCREENS;
	}
}
