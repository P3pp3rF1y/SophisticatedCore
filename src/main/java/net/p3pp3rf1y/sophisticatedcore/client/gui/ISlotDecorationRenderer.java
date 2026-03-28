package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;

public interface ISlotDecorationRenderer {
	void renderDecoration(GuiGraphicsExtractor guiGraphics, Slot slot);
}
