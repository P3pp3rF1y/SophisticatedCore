package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.TextBox;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;

import java.util.List;
import java.util.Optional;

class SearchBox extends TextBox {
	private static final List<Component> TOOLTIP = List.of(Component.translatable(TranslationHelper.INSTANCE.translGui("text_box.search_box")),
			Component.translatable(TranslationHelper.INSTANCE.translGui("text_box.search_box_detail")).withStyle(ChatFormatting.GRAY));
	public static final String MAGNIFYING_GLASS = "\uD83D\uDD0D";
	public static final int MIN_WIDTH = 10;
	public static final int UNFOCUSED_COLOR = 0xBBBBBB;
	private final StorageScreenBase<?> screen;
	private long lastFocusChangeTime = 0;
	private final int maximizedX;
	private final int maximizedWidth;

	public SearchBox(Position position, Dimension dimension, StorageScreenBase<?> screen) {
		super(position, dimension);
		this.screen = screen;
		setTextColor(UNFOCUSED_COLOR);
		setTextColorUneditable(UNFOCUSED_COLOR);
		setBordered(false);
		setMaxLength(50);
		setUnfocusedEmptyHint(MAGNIFYING_GLASS);
		maximizedX = position.x();
		maximizedWidth = dimension.width();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}

		if (isEditable()) {
			if (button == 0) {
				setFocused(true);
				screen.setFocused(this);
			} else if (button == 1) {
				setValue("");
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void setFocused(boolean focused) {
		if (isFocused() != focused) {
			lastFocusChangeTime = System.currentTimeMillis();
		}
		super.setFocused(focused);
		if (focused) {
			setTextColor(-1);
		} else {
			setTextColor(UNFOCUSED_COLOR);
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		int minWidth = MIN_WIDTH;
		if ((isFocused() && maximizedWidth > getWidth()) || (!isFocused() && getValue().isEmpty() && getWidth() > minWidth)) {
			float ratio = Easing.EASE_IN_OUT_CUBIC.ease(Math.min((System.currentTimeMillis() - lastFocusChangeTime) / 200f, 1));
			int currentWidth = isFocused()
					? (int) (minWidth + (maximizedWidth - minWidth) * ratio)
					: (int) (maximizedWidth - (maximizedWidth - minWidth) * ratio);
			this.setPosition(new Position(maximizedX + maximizedWidth - currentWidth, this.y));
			this.updateDimensions(currentWidth, this.getHeight());
		}

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, 0, 100);
		guiGraphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFF777777);
		guiGraphics.pose().popPose();
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		if (!isFocused() && isMouseOver(mouseX, mouseY)) {
			guiGraphics.renderTooltip(screen.getFont(), TOOLTIP, Optional.empty(), mouseX, mouseY);
		}
	}

	boolean isExpandedOrFocused() {
		return isFocused() || getWidth() > MIN_WIDTH;
	}
}
