package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Set;

public abstract class UpgradeInventoryControlBase {
	public void extract(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		// noop by default
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean handleMouseReleased(MouseButtonEvent event) {
		return false;
	}

	public void extractErrorOverlay(GuiGraphicsExtractor guiGraphics) {
		// noop by default
	}

	public void extractErrorOverlay(GuiGraphicsExtractor guiGraphics, Set<Integer> errorInventorySlots) {
		// noop by default
	}

	public void extractTooltip(StorageScreenBase<?> screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		// noop by default
	}

	public boolean replacesSlotRender(int slot) {
		return false;
	}
}
