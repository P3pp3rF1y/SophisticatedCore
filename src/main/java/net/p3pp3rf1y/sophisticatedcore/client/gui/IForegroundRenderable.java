package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public interface IForegroundRenderable {
	void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);
}
