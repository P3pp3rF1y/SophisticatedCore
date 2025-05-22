package net.p3pp3rf1y.sophisticatedcore.compat.mousetweaks;

import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.InventoryScrollPanel;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class MouseTweaksCompat implements ICompat {
	@Override
	public void setup() {
		InventoryScrollPanel.setRestrictScrollToScrollbar(true);
	}
}
