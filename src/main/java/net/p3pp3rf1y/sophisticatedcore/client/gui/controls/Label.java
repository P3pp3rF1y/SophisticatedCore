package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;

public class Label extends WidgetBase {
	private static final int DEFAULT_GUI_TEXT_COLOR = 4210752;
	private static final String ELLIPSIS = "...";
	private final Component labelText;
	private final int color;
	@Nullable
	private final IntSupplier maxWidthSupplier;

	public Label(Position position, Component labelText) {
		this(position, labelText, DEFAULT_GUI_TEXT_COLOR);
	}

	public Label(Position position, Component labelText, int color) {
		super(position, new Dimension(Minecraft.getInstance().font.width(labelText), 8));
		this.labelText = labelText;
		this.color = color;
		maxWidthSupplier = null;
	}

	public Label(Position position, Component labelText, IntSupplier maxWidthSupplier) {
		this(position, labelText, DEFAULT_GUI_TEXT_COLOR, maxWidthSupplier);
	}

	public Label(Position position, Component labelText, int color, IntSupplier maxWidthSupplier) {
		super(position, new Dimension(maxWidthSupplier.getAsInt(), 8));
		this.labelText = labelText;
		this.color = color;
		this.maxWidthSupplier = maxWidthSupplier;
	}

	@Override
	public int getWidth() {
		return maxWidthSupplier == null ? super.getWidth() : maxWidthSupplier.getAsInt();
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		if (getWidth() <= 0) {
			return;
		}

		if (isTextTruncated()) {
			guiGraphics.drawString(minecraft.font, getTruncatedText(), x, y, ARGB.opaque(color), false);
		} else {
			guiGraphics.drawString(minecraft.font, labelText, x, y, ARGB.opaque(color), false);
		}
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if (isTextTruncated() && isMouseOver(mouseX, mouseY)) {
			guiGraphics.setTooltipForNextFrame(screen.getFont(), List.of(labelText), Optional.empty(), mouseX, mouseY);
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + getWidth() && mouseY >= y && mouseY < y + font.lineHeight;
	}

	private boolean isTextTruncated() {
		return maxWidthSupplier != null && isTextTruncated(minecraft.font, labelText, getWidth());
	}

	private String getTruncatedText() {
		return getTruncatedText(minecraft.font, labelText, getWidth());
	}

	public static boolean isTextTruncated(Font font, Component text, int maxWidth) {
		return font.width(text) > maxWidth;
	}

	public static String getTruncatedText(Font font, Component component, int maxWidth) {
		int ellipsisWidth = font.width(ELLIPSIS);
		if (maxWidth <= ellipsisWidth) {
			return font.plainSubstrByWidth(ELLIPSIS, maxWidth);
		}

		String text = font.plainSubstrByWidth(component.getString(), maxWidth - ellipsisWidth) + ELLIPSIS;
		while (!text.equals(ELLIPSIS) && font.width(text) > maxWidth) {
			text = text.substring(0, text.length() - ELLIPSIS.length() - 1) + ELLIPSIS;
		}
		return text;
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		//TODO add narration
	}
}
