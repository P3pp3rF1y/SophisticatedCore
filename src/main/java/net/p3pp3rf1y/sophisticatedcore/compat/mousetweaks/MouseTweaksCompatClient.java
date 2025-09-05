package net.p3pp3rf1y.sophisticatedcore.compat.mousetweaks;

import net.p3pp3rf1y.sophisticatedcore.Config;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.InventoryScrollPanel;

public class MouseTweaksCompatClient {
	public static void restrictSophisticatedScrollInteraction() {
		if (Config.CLIENT.mouseTweaksScrollEnabled.get()) {
			InventoryScrollPanel.setRestrictScrollToScrollbar(true);
		}
	}
}
