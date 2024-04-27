package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class Label extends WidgetBase {
	private static final int DEFAULT_GUI_TEXT_COLOR = 4210752;
	private final Component labelText;
	private final int color;

	public Label(Position position, Component labelText) {
		this(position, labelText, DEFAULT_GUI_TEXT_COLOR);
	}

	public Label(Position position, Component labelText, int color) {
		super(position, new Dimension(Minecraft.getInstance().font.width(labelText), 8));
		this.labelText = labelText;
		this.color = color;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		guiGraphics.drawString(minecraft.font, labelText, x, y, color, false);
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		//TODO add narration
	}
}
