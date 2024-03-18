package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;

public class Button extends ButtonBase {
	private final TextureBlitData backgroundTexture;
	@Nullable
	private final TextureBlitData hoveredBackgroundTexture;
	@Nullable
	private final TextureBlitData foregroundTexture;
	private List<Component> tooltip;
	private boolean hovered = false;

	public Button(Position position, ButtonDefinition buttonDefinition, IntConsumer onClick) {
		super(position, buttonDefinition.getDimension(), onClick);
		backgroundTexture = buttonDefinition.getBackgroundTexture();
		foregroundTexture = buttonDefinition.getForegroundTexture();
		hoveredBackgroundTexture = buttonDefinition.getHoveredBackgroundTexture();
		tooltip = buttonDefinition.getTooltip();
	}

	public boolean isHovered() {
		return hovered;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		if (isMouseOver(mouseX, mouseY)) {
			hovered = true;
			if (hoveredBackgroundTexture != null) {
				GuiHelper.blit(guiGraphics, x, y, hoveredBackgroundTexture);
			}
		} else {
			hovered = false;
			GuiHelper.blit(guiGraphics, x, y, backgroundTexture);
		}
	}

	protected List<Component> getTooltip() {
		return tooltip;
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		if (foregroundTexture != null) {
			GuiHelper.blit(guiGraphics, x, y, foregroundTexture);
		}
	}

	@Override
	public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
		pNarrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", getTooltip()));
		pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
	}

	public void setTooltip(List<Component> tooltip) {
		this.tooltip = tooltip;
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		if (visible && isMouseOver(mouseX, mouseY)) {
			guiGraphics.renderTooltip(screen.font, getTooltip(), Optional.empty(), mouseX, mouseY);
		}
	}
}
