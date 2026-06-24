package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class ColorButton extends ButtonBase {
	private final Supplier<Integer> colorGetter;
	private final List<Component> tooltip;

	public ColorButton(Position position, Dimension dimension, Supplier<Integer> colorGetter, IntConsumer onClick, @Nullable Component tooltip) {
		super(position, dimension, onClick);
		this.colorGetter = colorGetter;
		this.tooltip = tooltip == null ? List.of() : List.of(tooltip);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		int color = isMouseOver(mouseX, mouseY) ? 0xFF_FFFFFF : 0xFF_CCCCCC;
		guiGraphics.fill(x, y, x + getWidth(), y + getHeight(), color);
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		int color = colorGetter.get();
		if (color == -1) {
			for (int row = 0; row < getHeight() - 2; row++) {
				for (int column = 0; column < getWidth() - 2; column++) {
					guiGraphics.fill(x + column + 1, y + row + 1, x + column + 2, y + row + 2, ((row + column) % 2 == 0) ? 0xFF_CCCCC : 0xFF_888888);
				}
			}
		} else {
			guiGraphics.fill(x + 1, y + 1, x + getWidth() - 1, y + getHeight() - 1, colorGetter.get());
		}
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		if (visible && isMouseOver(mouseX, mouseY) && !tooltip.isEmpty()) {
			guiGraphics.setTooltipForNextFrame(screen.getMinecraft().font, tooltip, Optional.empty(), mouseX, mouseY);
		}
	}
}
