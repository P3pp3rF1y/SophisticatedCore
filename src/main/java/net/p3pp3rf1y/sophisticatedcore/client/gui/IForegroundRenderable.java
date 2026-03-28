package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface IForegroundRenderable {
	void extractForeground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks);
}
