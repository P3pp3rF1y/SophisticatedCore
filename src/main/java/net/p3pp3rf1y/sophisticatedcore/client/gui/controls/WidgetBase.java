package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public abstract class WidgetBase implements Renderable, GuiEventListener, NarratableEntry {
	protected int x;

	protected int y;
	protected final Minecraft minecraft;
	protected final Font font;
	private int height;
	private int width;
	protected boolean isHovered;
	protected boolean visible = true;
	private boolean focused = false;
	private boolean renderInDefaultPass = true;

	protected WidgetBase(Position position, Dimension dimension) {
		x = position.x();
		y = position.y();
		width = dimension.width();
		height = dimension.height();
		minecraft = Minecraft.getInstance();
		font = minecraft.font;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		if (!visible || !renderInDefaultPass) {
			return;
		}
		actuallyExtractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
	}

	public void extractRenderStateInLatePass(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}
		actuallyExtractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
	}

	protected void actuallyExtractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		extractBg(guiGraphics, minecraft, mouseX, mouseY);
		extractWidget(guiGraphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public NarrationPriority narrationPriority() {
		return isHovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
	}

	protected abstract void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY);

	protected abstract void extractWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks);

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	protected void updateDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + getWidth() && mouseY >= y && mouseY < y + getHeight();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setPosition(Position position) {
		x = position.x();
		y = position.y();
	}

	protected int getCenteredX(int elementWidth) {
		return (getWidth() - elementWidth) / 2;
	}

	public void extractTooltip(Screen screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		// noop
	}

	public void setRenderInDefaultPass(boolean renderInDefaultPass) {
		this.renderInDefaultPass = renderInDefaultPass;
	}

	@Override
	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	@Override
	public boolean isFocused() {
		return focused;
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		// noop by default
	}
}
