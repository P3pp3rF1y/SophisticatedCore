package net.p3pp3rf1y.sophisticatedcore.client.gui.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.p3pp3rf1y.sophisticatedcore.Config;

public final class GuiSoundHelper {
	private GuiSoundHelper() {
	}

	public static void playButtonClickSound() {
		if (Boolean.TRUE.equals(Config.CLIENT.playButtonSound.get())) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
	}
}
