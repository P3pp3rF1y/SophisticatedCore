package net.p3pp3rf1y.sophisticatedcore.settings.main;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;

import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.GUI_CONTROLS;

public class ContextButton extends ButtonBase {
	public static final TextureBlitData LEFT_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(47, 0), new Dimension(16, 18));
	public static final TextureBlitData LEFT_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(29, 0), new Dimension(16, 18));
	public static final TextureBlitData MIDDLE_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(49, 0), new Dimension(14, 18));
	public static final TextureBlitData MIDDLE_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(31, 0), new Dimension(14, 18));
	public static final TextureBlitData RIGHT_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(49, 0), new Dimension(16, 18));
	public static final TextureBlitData RIGHT_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(31, 0), new Dimension(16, 18));
	private final Supplier<Component> getTitle;
	private final Supplier<List<Component>> getTooltipKey;

	protected ContextButton(Position position, IntConsumer onClick, Supplier<Component> getTitle, Supplier<List<Component>> getTooltipKey) {
		super(position, new Dimension(62, 18), onClick);
		this.getTitle = getTitle;
		this.getTooltipKey = getTooltipKey;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		if (isMouseOver(mouseX, mouseY)) {
			renderBackground(guiGraphics, LEFT_BUTTON_HOVERED_BACKGROUND, MIDDLE_BUTTON_HOVERED_BACKGROUND, RIGHT_BUTTON_HOVERED_BACKGROUND);
		} else {
			renderBackground(guiGraphics, LEFT_BUTTON_BACKGROUND, MIDDLE_BUTTON_BACKGROUND, RIGHT_BUTTON_BACKGROUND);
		}
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		if (isMouseOver(mouseX, mouseY)) {
			guiGraphics.renderTooltip(minecraft.font, getTooltipKey.get(), Optional.empty(), mouseX, mouseY);
		}
	}

	private void renderBackground(GuiGraphics guiGraphics, TextureBlitData leftButtonHoveredBackground, TextureBlitData middleButtonHoveredBackground, TextureBlitData rightButtonHoveredBackground) {
		int left = x;
		GuiHelper.blit(guiGraphics, left, y, leftButtonHoveredBackground);
		left += leftButtonHoveredBackground.getWidth();
		GuiHelper.blit(guiGraphics, left, y, middleButtonHoveredBackground);
		left += middleButtonHoveredBackground.getWidth();
		GuiHelper.blit(guiGraphics, left, y, middleButtonHoveredBackground);
		left += middleButtonHoveredBackground.getWidth();
		GuiHelper.blit(guiGraphics, left, y, rightButtonHoveredBackground);
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		guiGraphics.drawCenteredString(minecraft.font, getTitle.get(), x + getWidth() / 2, y - 4 + getHeight() / 2, 16777215 | (255 << 24));
	}

	@Override
	public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
		pNarrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.sophisticatedcore.narrate.context_button", getTitle.get()));
		pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("gui.sophisticatedcore.narrate.context_button.usage"));
	}
}
