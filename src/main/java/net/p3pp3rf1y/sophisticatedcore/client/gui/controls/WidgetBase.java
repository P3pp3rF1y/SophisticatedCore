package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public abstract class WidgetBase implements Renderable, GuiEventListener, NarratableEntry {
	protected final int x;

	protected final int y;
	protected final Minecraft minecraft;
	protected final Font font;
	private int height;
	private int width;
	protected boolean isHovered;
	protected boolean visible = true;
	private boolean focused = false;

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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}

		isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		RenderSystem.enableDepthTest();
		renderBg(guiGraphics, minecraft, mouseX, mouseY);
		renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public NarrationPriority narrationPriority() {
		return isHovered ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
	}

	protected abstract void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY);

	protected abstract void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);

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

	protected int getCenteredX(int elementWidth) {
		return (getWidth() - elementWidth) / 2;
	}

	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		//noop
	}

	@Override
	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	@Override
	public boolean isFocused() {
		return focused;
	}
}
