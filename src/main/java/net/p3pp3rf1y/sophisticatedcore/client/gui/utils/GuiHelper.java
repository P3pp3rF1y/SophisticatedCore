package net.p3pp3rf1y.sophisticatedcore.client.gui.utils;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiHelper {
	public static final Identifier GUI_CONTROLS = SophisticatedCore.getIdentifier("textures/gui/gui_controls.png");
	public static final int GUI_CONTROLS_TEXTURE_WIDTH = 256;
	public static final int GUI_CONTROLS_TEXTURE_HEIGHT = 256;
	public static final TextureBlitData BAR_BACKGROUND_BOTTOM = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 66), Dimension.SQUARE_18);
	public static final TextureBlitData BAR_BACKGROUND_MIDDLE = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 48), Dimension.SQUARE_18);
	public static final TextureBlitData BAR_BACKGROUND_TOP = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 30), Dimension.SQUARE_18);
	public static final Identifier ICONS = SophisticatedCore.getIdentifier("textures/gui/icons.png");
	public static final TextureBlitData CRAFTING_RESULT_SLOT = new TextureBlitData(GUI_CONTROLS, new UV(71, 216), new Dimension(26, 26));
	public static final TextureBlitData DEFAULT_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(47, 0), Dimension.SQUARE_18);
	public static final TextureBlitData DEFAULT_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(29, 0), Dimension.SQUARE_18);
	public static final TextureBlitData SMALL_BUTTON_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 18), Dimension.SQUARE_12);
	public static final TextureBlitData SMALL_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(41, 18), Dimension.SQUARE_12);
	public static final Identifier SLOTS_BACKGROUND = SophisticatedCore.getIdentifier("textures/gui/slots_background.png");

	private static final Map<Integer, TextureBlitData> SLOTS_BACKGROUNDS = new HashMap<>();

	private GuiHelper() {
	}

	public static void renderItemInGUI(GuiGraphics guiGraphics, Minecraft minecraft, ItemStack stack, int xPosition, int yPosition) {
		renderItemInGUI(guiGraphics, minecraft, stack, xPosition, yPosition, false);
	}

	public static void renderSlotsBackground(GuiGraphics guiGraphics, int x, int y, int slotWidth, int slotHeight) {
		for (int currentY = y, remainingSlotHeight = slotHeight; remainingSlotHeight > 0; currentY += 12 * 18, remainingSlotHeight -= Math.min(slotHeight, 12)) {
			int finalRemainingSlotHeight = remainingSlotHeight;
			int key = getSlotsBackgroundKey(slotWidth, remainingSlotHeight);
			blit(guiGraphics, x, currentY, SLOTS_BACKGROUNDS.computeIfAbsent(key, k ->
					new TextureBlitData(SLOTS_BACKGROUND, Dimension.SQUARE_256, new UV(0, 0), new Dimension(slotWidth * 18, finalRemainingSlotHeight * 18))
			));
		}
	}

	private static int getSlotsBackgroundKey(int slotWidth, int slotHeight) {
		return slotWidth * 31 + slotHeight;
	}

	public static void renderItemInGUI(GuiGraphics guiGraphics, Minecraft minecraft, ItemStack stack, int xPosition, int yPosition, boolean renderOverlay) {
		renderItemInGUI(guiGraphics, minecraft, stack, xPosition, yPosition, renderOverlay, null);
	}

	public static void renderItemInGUI(GuiGraphics guiGraphics, Minecraft minecraft, ItemStack stack, int xPosition, int yPosition, boolean renderOverlay,
									   @Nullable String countText) {
		guiGraphics.renderItem(stack, xPosition, yPosition);
		if (renderOverlay) {
			guiGraphics.renderItemDecorations(minecraft.font, stack, xPosition, yPosition, countText);
		}
	}

	public static void blit(GuiGraphics guiGraphics, int x, int y, TextureBlitData texData) {
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texData.getTextureName(), x + texData.getXOffset(), y + texData.getYOffset(), texData.getU(), texData.getV(), texData.getWidth(), texData.getHeight(), texData.getTextureWidth(), texData.getTextureHeight());
	}

	public static void blit(GuiGraphics guiGraphics, int x, int y, TextureBlitData texData, int width, int height) {
		int halfWidth = width / 2;
		int secondHalfWidth = width - halfWidth;
		int halfHeight = height / 2;
		int secondHalfHeight = height - halfHeight;

		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texData.getTextureName(), x + texData.getXOffset(), y + texData.getYOffset(), texData.getU(), texData.getV(), halfWidth, halfHeight, texData.getTextureWidth(), texData.getTextureHeight());
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texData.getTextureName(), x + texData.getXOffset() + halfWidth, y + texData.getYOffset(), (float) texData.getU() + texData.getWidth() - secondHalfWidth, texData.getV(), secondHalfWidth, halfHeight, texData.getTextureWidth(), texData.getTextureHeight());
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texData.getTextureName(), x + texData.getXOffset(), y + texData.getYOffset() + halfHeight, texData.getU(), (float) texData.getV() + texData.getHeight() - secondHalfHeight, halfWidth, secondHalfHeight, texData.getTextureWidth(), texData.getTextureHeight());
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texData.getTextureName(), x + texData.getXOffset() + halfWidth, y + texData.getYOffset() + halfHeight, (float) texData.getU() + texData.getWidth() - secondHalfWidth, (float) texData.getV() + texData.getHeight() - secondHalfHeight, secondHalfWidth, secondHalfHeight, texData.getTextureWidth(), texData.getTextureHeight());
	}

	public static void coloredBlit(GuiGraphics guiGraphics, int x, int y, TextureBlitData texData, int color) {
		int xMin = x + texData.getXOffset();
		int yMin = y + texData.getYOffset();

		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texData.getTextureName(), xMin, yMin, texData.getU(), texData.getV(), texData.getWidth(), texData.getHeight(), texData.getTextureWidth(), texData.getTextureHeight(), color);
	}

	public static void writeTooltipLines(GuiGraphics guiGraphics, List<FormattedCharSequence> textLines, Font font, int leftX, int topY, int color) {
		for (int i = 0; i < textLines.size(); ++i) {
			FormattedCharSequence line = textLines.get(i);
			if (line != null) {
				guiGraphics.drawString(font, line, leftX, topY, color, true);
			}

			if (i == 0) {
				topY += 2;
			}

			topY += 10;
		}
	}

	public static void fill(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, int color) {
		guiGraphics.fill(minX, minY, maxX, maxY, color);
	}

	public static ToggleButton.StateData getButtonStateData(UV uv, Dimension dimension, Position offset, Component... tooltip) {
		return getButtonStateData(uv, dimension, offset, Arrays.asList(tooltip));
	}

	public static ToggleButton.StateData getButtonStateData(UV uv, String tooltip, Dimension dimension) {
		return getButtonStateData(uv, tooltip, dimension, new Position(0, 0));
	}

	public static ToggleButton.StateData getButtonStateData(UV uv, String tooltip, Dimension dimension, Position offset) {
		return new ToggleButton.StateData(new TextureBlitData(ICONS, offset, Dimension.SQUARE_256, uv, dimension),
				Component.translatable(tooltip)
		);
	}

	public static ToggleButton.StateData getButtonStateData(UV uv, Dimension dimension, Position offset, List<Component> tooltip) {
		return new ToggleButton.StateData(new TextureBlitData(ICONS, offset, Dimension.SQUARE_256, uv, dimension), tooltip);
	}

	public static void renderSlotsBackground(GuiGraphics guiGraphics, int x, int y, int slotsInRow, int fullSlotRows, int extraRowSlots) {
		renderSlotsBackground(guiGraphics, x, y, slotsInRow, fullSlotRows);
		if (extraRowSlots > 0) {
			renderSlotsBackground(guiGraphics, x, y + fullSlotRows * 18, extraRowSlots, 1);
		}
	}

	public static void renderTiledSprite(GuiGraphics guiGraphics, TextureAtlasSprite sprite, int color, int x, int y, int height) {
		int spriteHeight = sprite.contents().height();
		int startY = y;
		int textureWidth = (int) (sprite.contents().width() / (sprite.getU1() - sprite.getU0()));
		int textureHeight = (int) (sprite.contents().height() / (sprite.getV1() - sprite.getV0()));
		do {
			int renderHeight = Math.min(spriteHeight, height);
			height -= renderHeight;

			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, sprite.atlasLocation(), x, startY, textureWidth * sprite.getU0(), textureHeight * sprite.getV0(), 16, renderHeight, textureWidth, textureHeight, color);

			startY += renderHeight;
		} while (height > 0);
	}

	public static void renderControlBackground(GuiGraphics guiGraphics, int x, int y, int renderWidth, int renderHeight) {
		int u = 29;
		int v = 146;
		int textureBgWidth = 66;
		int textureBgHeight = 56;
		renderControlBackground(guiGraphics, x, y, renderWidth, renderHeight, u, v, textureBgWidth, textureBgHeight);
	}

	public static void renderTabBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
		int halfHeight = height / 2;
		int secondHalfHeight = halfHeight + height % 2;
		if (width <= GUI_CONTROLS_TEXTURE_WIDTH / 2) {
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x, y, (float) GUI_CONTROLS_TEXTURE_WIDTH - width, 0, width, halfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x, y + halfHeight, (float) GUI_CONTROLS_TEXTURE_WIDTH - width, (float) GUI_CONTROLS_TEXTURE_HEIGHT - secondHalfHeight, width, secondHalfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x - 3, y, (float) (GUI_CONTROLS_TEXTURE_WIDTH / 2), (float) (GUI_CONTROLS_TEXTURE_HEIGHT - height), 3, height, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
			return;
		}

		renderTiledControlBackground(guiGraphics, x, y, width, height, GUI_CONTROLS_TEXTURE_WIDTH / 2, 0, GUI_CONTROLS_TEXTURE_WIDTH / 2, GUI_CONTROLS_TEXTURE_HEIGHT, 6, 6, halfHeight, secondHalfHeight);
	}

	public static void renderTiledControlBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
		renderTiledControlBackground(guiGraphics, x, y, width, height, u, v, textureWidth, textureHeight, 1);
	}

	public static void renderTiledControlBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight, int borderWidth) {
		renderTiledControlBackground(guiGraphics, x, y, width, height, u, v, textureWidth, textureHeight, borderWidth, borderWidth, borderWidth, borderWidth);
	}

	public static void renderTiledControlBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight, int leftWidth, int rightWidth, int topHeight, int bottomHeight) {
		rightWidth = Math.min(rightWidth, width - leftWidth);
		bottomHeight = Math.min(bottomHeight, height - topHeight);
		int sourceRightU = u + textureWidth - rightWidth;
		int sourceBottomV = v + textureHeight - bottomHeight;
		int centerWidth = width - leftWidth - rightWidth;
		int centerHeight = height - topHeight - bottomHeight;
		int sourceCenterWidth = textureWidth - leftWidth - rightWidth;
		int sourceCenterHeight = textureHeight - topHeight - bottomHeight;

		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x, y, (float) u, (float) v, leftWidth, topHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x + leftWidth + centerWidth, y, (float) sourceRightU, (float) v, rightWidth, topHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x, y + topHeight + centerHeight, (float) u, (float) sourceBottomV, leftWidth, bottomHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x + leftWidth + centerWidth, y + topHeight + centerHeight, (float) sourceRightU, (float) sourceBottomV, rightWidth, bottomHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);

		renderTiledTexture(guiGraphics, x + leftWidth, y, centerWidth, topHeight, u + leftWidth, v, sourceCenterWidth, topHeight);
		renderTiledTexture(guiGraphics, x + leftWidth, y + topHeight + centerHeight, centerWidth, bottomHeight, u + leftWidth, sourceBottomV, sourceCenterWidth, bottomHeight);
		renderTiledTexture(guiGraphics, x, y + topHeight, leftWidth, centerHeight, u, v + topHeight, leftWidth, sourceCenterHeight);
		renderTiledTexture(guiGraphics, x + leftWidth + centerWidth, y + topHeight, rightWidth, centerHeight, sourceRightU, v + topHeight, rightWidth, sourceCenterHeight);
		renderTiledTexture(guiGraphics, x + leftWidth, y + topHeight, centerWidth, centerHeight, u + leftWidth, v + topHeight, sourceCenterWidth, sourceCenterHeight);
	}

	private static void renderTiledTexture(GuiGraphics guiGraphics, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
		if (width <= 0 || height <= 0 || textureWidth <= 0 || textureHeight <= 0) {
			return;
		}
		int renderedY = 0;
		while (renderedY < height) {
			int chunkHeight = Math.min(textureHeight, height - renderedY);
			int renderedX = 0;
			while (renderedX < width) {
				int chunkWidth = Math.min(textureWidth, width - renderedX);
				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x + renderedX, y + renderedY, (float) u, (float) v, chunkWidth, chunkHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
				renderedX += chunkWidth;
			}
			renderedY += chunkHeight;
		}
	}

	public static void renderControlBackground(GuiGraphics guiGraphics, int x, int y, int renderWidth, int renderHeight, int u, int v, int textureBgWidth, int textureBgHeight) {
		int halfWidth = renderWidth / 2;
		int halfHeight = renderHeight / 2;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x, y, u, v, halfWidth, halfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x, y + halfHeight, u, (float) v + textureBgHeight - halfHeight, halfWidth, halfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x + halfWidth, y, (float) u + textureBgWidth - halfWidth, v, halfWidth, halfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x + halfWidth, y + halfHeight, (float) u + textureBgWidth - halfWidth, (float) v + textureBgHeight - halfHeight, halfWidth, halfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
	}

	private static MultiBufferSource.BufferSource getBufferSource() {
		return Minecraft.getInstance().renderBuffers().bufferSource();
	}

	public static void renderTooltip(Screen screen, GuiGraphics guiGraphics, List<Component> components, int x, int y) {
		if (components.isEmpty()) {
			return;
		}

		List<ClientTooltipComponent> list = gatherTooltipComponents(components, x, screen.width, screen.height, screen.getFont());
		guiGraphics.renderTooltip(screen.getFont(), list, x, y, DefaultTooltipPositioner.INSTANCE, null);
	}

	public static void renderTooltip(Screen screen, GuiGraphics guiGraphics, ItemStack tooltipStack, List<Component> components, Optional<TooltipComponent> tooltipComponent, int x, int y) {
		List<ClientTooltipComponent> list = ClientHooks.gatherTooltipComponents(tooltipStack, components, tooltipComponent, x, screen.width, screen.height, screen.getFont());
		guiGraphics.renderTooltip(screen.getFont(), list, x, y, DefaultTooltipPositioner.INSTANCE, null);
	}

	//copy of ForgeHooksClient.gatherTooltipComponents with splitting always called so that new lines in translation are properly wrapped
	public static List<ClientTooltipComponent> gatherTooltipComponents(List<? extends FormattedText> textElements, int mouseX, int screenWidth, int screenHeight, Font fallbackFont) {
		Font font = ClientHooks.getTooltipFont(ItemStack.EMPTY, fallbackFont);
		List<Either<FormattedText, TooltipComponent>> elements = textElements.stream()
				.map((Function<FormattedText, Either<FormattedText, TooltipComponent>>) Either::left)
				.collect(Collectors.toCollection(ArrayList::new));

		var event = new RenderTooltipEvent.GatherComponents(ItemStack.EMPTY, screenWidth, screenHeight, elements, -1);
		NeoForge.EVENT_BUS.post(event);
		if (event.isCanceled()) {
			return List.of();
		}

		// text wrapping
		int tooltipTextWidth = event.getTooltipElements().stream()
				.mapToInt(either -> either.map(font::width, component -> 0))
				.max()
				.orElse(0);

		int tooltipX = mouseX + 12;
		if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
			tooltipX = mouseX - 16 - tooltipTextWidth;
			if (tooltipX < 4) // if the tooltip doesn't fit on the screen
			{
				if (mouseX > screenWidth / 2) {
					tooltipTextWidth = mouseX - 12 - 8;
				} else {
					tooltipTextWidth = screenWidth - 16 - mouseX;
				}
			}
		}

		if (event.getMaxWidth() > 0 && tooltipTextWidth > event.getMaxWidth()) {
			tooltipTextWidth = event.getMaxWidth();
		}

		int tooltipTextWidthF = tooltipTextWidth;
		return event.getTooltipElements().stream()
				.flatMap(either -> either.map(
						text -> font.split(text, tooltipTextWidthF).stream().map(ClientTooltipComponent::create),
						component -> Stream.of(ClientTooltipComponent.create(component))
				))
				.toList();
	}

	public static Optional<Rect2i> getPositiveRectangle(int x, int y, int width, int height) {
		if (x + width <= 0 || y + height <= 0) {
			return Optional.empty();
		}

		int positiveX = Math.max(0, x);
		int positiveY = Math.max(0, y);
		int positiveWidth = width + Math.min(0, x);
		int positiveHeight = height + Math.min(0, y);

		return Optional.of(new Rect2i(positiveX, positiveY, positiveWidth, positiveHeight));
	}
}
