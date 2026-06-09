package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Set;

public abstract class UpgradeInventoryControlBase {
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// noop by default
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
		return false;
	}

	public void renderErrorOverlay(GuiGraphics guiGraphics) {
		// noop by default
	}

	public void renderErrorOverlay(GuiGraphics guiGraphics, Set<Integer> errorInventorySlots) {
		// noop by default
	}

	public void renderTooltip(StorageScreenBase<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// noop by default
	}

	public boolean replacesSlotRender(int slot) {
		return false;
	}
}
