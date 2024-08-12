package net.p3pp3rf1y.sophisticatedcore.client.gui.utils;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiHelper {
	public static final ResourceLocation GUI_CONTROLS = SophisticatedCore.getRL("textures/gui/gui_controls.png");
	private static final int GUI_CONTROLS_TEXTURE_WIDTH = 256;
	private static final int GUI_CONTROLS_TEXTURE_HEIGHT = 256;
	public static final TextureBlitData BAR_BACKGROUND_BOTTOM = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 66), Dimension.SQUARE_18);
	public static final TextureBlitData BAR_BACKGROUND_MIDDLE = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 48), Dimension.SQUARE_18);
	public static final TextureBlitData BAR_BACKGROUND_TOP = new TextureBlitData(GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 30), Dimension.SQUARE_18);
	public static final ResourceLocation ICONS = SophisticatedCore.getRL("textures/gui/icons.png");
	public static final TextureBlitData CRAFTING_RESULT_SLOT = new TextureBlitData(GUI_CONTROLS, new UV(71, 216), new Dimension(26, 26));
	public static final TextureBlitData DEFAULT_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(47, 0), Dimension.SQUARE_18);
	public static final TextureBlitData DEFAULT_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(29, 0), Dimension.SQUARE_18);
	public static final TextureBlitData SMALL_BUTTON_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 18), Dimension.SQUARE_12);
	public static final TextureBlitData SMALL_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(41, 18), Dimension.SQUARE_12);
	public static final ResourceLocation SLOTS_BACKGROUND = SophisticatedCore.getRL("textures/gui/slots_background.png");

	private static final Map<Integer, TextureBlitData> SLOTS_BACKGROUNDS = new HashMap<>();

	private GuiHelper() {}

	public static void renderItemInGUI(GuiGraphics guiGraphics, Minecraft minecraft, ItemStack stack, int xPosition, int yPosition) {
		renderItemInGUI(guiGraphics, minecraft, stack, xPosition, yPosition, false);
	}

	public static void renderSlotsBackground(GuiGraphics guiGraphics, int x, int y, int slotWidth, int slotHeight) {
		for(int currentY = y, remainingSlotHeight = slotHeight; remainingSlotHeight > 0; currentY += 12 *18, remainingSlotHeight -= Math.min(slotHeight, 12)) {
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
		RenderSystem.enableDepthTest();
		guiGraphics.renderItem(stack, xPosition, yPosition);
		if (renderOverlay) {
			guiGraphics.renderItemDecorations(minecraft.font, stack, xPosition, yPosition, countText);
		}
	}

	public static void blit(GuiGraphics guiGraphics, int x, int y, TextureBlitData texData) {
		guiGraphics.blit(texData.getTextureName(), x + texData.getXOffset(), y + texData.getYOffset(), texData.getU(), texData.getV(), texData.getWidth(), texData.getHeight(), texData.getTextureWidth(), texData.getTextureHeight());
	}

	public static void blit(GuiGraphics guiGraphics, int x, int y, TextureBlitData texData, int width, int height) {
		int halfWidth = width / 2;
		int secondHalfWidth = width - halfWidth;
		int halfHeight = height / 2;
		int secondHalfHeight = height - halfHeight;

		guiGraphics.blit(texData.getTextureName(), x + texData.getXOffset(), y + texData.getYOffset(), texData.getU(), texData.getV(), halfWidth, halfHeight, texData.getTextureWidth(), texData.getTextureHeight());
		guiGraphics.blit(texData.getTextureName(), x + texData.getXOffset() + halfWidth, y + texData.getYOffset(), (float) texData.getU() + texData.getWidth() - secondHalfWidth, texData.getV(), secondHalfWidth, halfHeight, texData.getTextureWidth(), texData.getTextureHeight());
		guiGraphics.blit(texData.getTextureName(), x + texData.getXOffset(), y + texData.getYOffset() + halfHeight, texData.getU(), (float) texData.getV() + texData.getHeight() - secondHalfHeight, halfWidth, secondHalfHeight, texData.getTextureWidth(), texData.getTextureHeight());
		guiGraphics.blit(texData.getTextureName(), x + texData.getXOffset() + halfWidth, y + texData.getYOffset() + halfHeight, (float) texData.getU() + texData.getWidth() - secondHalfWidth, (float) texData.getV() + texData.getHeight() - secondHalfHeight, secondHalfWidth, secondHalfHeight, texData.getTextureWidth(), texData.getTextureHeight());
	}

	public static void coloredBlit(Matrix4f matrix, int x, int y, TextureBlitData texData, int color) {
		float red = (color >> 16 & 255) / 255F;
		float green = (color >> 8 & 255) / 255F;
		float blue = (color & 255) / 255F;
		float alpha = (color >> 24 & 255) / 255F;

		int xMin = x + texData.getXOffset();
		int yMin = y + texData.getYOffset();
		int xMax = xMin + texData.getWidth();
		int yMax = yMin + texData.getHeight();

		float minU = (float) texData.getU() / texData.getTextureWidth();
		float maxU = minU + ((float) texData.getWidth() / texData.getTextureWidth());
		float minV = (float) texData.getV() / texData.getTextureHeight();
		float maxV = minV + ((float) texData.getHeight() / texData.getTextureWidth());

		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		bufferbuilder.addVertex(matrix, xMin, yMax, 0).setUv(minU, maxV).setColor(red, green, blue, alpha);
		bufferbuilder.addVertex(matrix, xMax, yMax, 0).setUv(maxU, maxV).setColor(red, green, blue, alpha);
		bufferbuilder.addVertex(matrix, xMax, yMin, 0).setUv(maxU, minV).setColor(red, green, blue, alpha);
		bufferbuilder.addVertex(matrix, xMin, yMin, 0).setUv(minU, minV).setColor(red, green, blue, alpha);
		BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
	}

	public static void renderTooltipBackground(Matrix4f matrix4f, int tooltipWidth, int leftX, int topY, int tooltipHeight, int backgroundColor, int borderColorStart, int borderColorEnd) {
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		fillGradient(matrix4f, bufferbuilder, leftX - 3, topY - 4, leftX + tooltipWidth + 3, topY - 3, backgroundColor, backgroundColor);
		fillGradient(matrix4f, bufferbuilder, leftX - 3, topY + tooltipHeight + 3, leftX + tooltipWidth + 3, topY + tooltipHeight + 4, backgroundColor, backgroundColor);
		fillGradient(matrix4f, bufferbuilder, leftX - 3, topY - 3, leftX + tooltipWidth + 3, topY + tooltipHeight + 3, backgroundColor, backgroundColor);
		fillGradient(matrix4f, bufferbuilder, leftX - 4, topY - 3, leftX - 3, topY + tooltipHeight + 3, backgroundColor, backgroundColor);
		fillGradient(matrix4f, bufferbuilder, leftX + tooltipWidth + 3, topY - 3, leftX + tooltipWidth + 4, topY + tooltipHeight + 3, backgroundColor, backgroundColor);
		fillGradient(matrix4f, bufferbuilder, leftX - 3, topY - 3 + 1, leftX - 3 + 1, topY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
		fillGradient(matrix4f, bufferbuilder, leftX + tooltipWidth + 2, topY - 3 + 1, leftX + tooltipWidth + 3, topY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
		fillGradient(matrix4f, bufferbuilder, leftX - 3, topY - 3, leftX + tooltipWidth + 3, topY - 3 + 1, borderColorStart, borderColorStart);
		fillGradient(matrix4f, bufferbuilder, leftX - 3, topY + tooltipHeight + 2, leftX + tooltipWidth + 3, topY + tooltipHeight + 3, borderColorEnd, borderColorEnd);
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
		RenderSystem.disableBlend();
	}

	public static void writeTooltipLines(GuiGraphics guiGraphics, List<FormattedCharSequence> textLines, Font font, float leftX, int topY, Matrix4f matrix4f, MultiBufferSource.BufferSource renderTypeBuffer, int color) {
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

	private static void fillGradient(Matrix4f matrix, BufferBuilder builder, int x1, int y1, int x2, int y2, int colorA, int colorB) {
		float f = (colorA >> 24 & 255) / 255.0F;
		float f1 = (colorA >> 16 & 255) / 255.0F;
		float f2 = (colorA >> 8 & 255) / 255.0F;
		float f3 = (colorA & 255) / 255.0F;
		float f4 = (colorB >> 24 & 255) / 255.0F;
		float f5 = (colorB >> 16 & 255) / 255.0F;
		float f6 = (colorB >> 8 & 255) / 255.0F;
		float f7 = (colorB & 255) / 255.0F;
		builder.addVertex(matrix, x2, y1, 400).setColor(f1, f2, f3, f);
		builder.addVertex(matrix, x1, y1, 400).setColor(f1, f2, f3, f);
		builder.addVertex(matrix, x1, y2, 400).setColor(f5, f6, f7, f4);
		builder.addVertex(matrix, x2, y2, 400).setColor(f5, f6, f7, f4);
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

	public static void renderTiledFluidTextureAtlas(GuiGraphics guiGraphics, TextureAtlasSprite sprite, int color, int x, int y, int height) {
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, sprite.atlasLocation());
		BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		float u0 = sprite.getU0();
		float v0 = sprite.getV0();
		int spriteHeight = sprite.contents().height();
		int startY = y;
		float red = (color >> 16 & 255) / 255.0F;
		float green = (color >> 8 & 255) / 255.0F;
		float blue = (color & 255) / 255.0F;
		do {
			int renderHeight = Math.min(spriteHeight, height);
			height -= renderHeight;
			float v1 = sprite.getV((float)renderHeight / spriteHeight);

			// we need to draw the quads per width too
			Matrix4f matrix = guiGraphics.pose().last().pose();
			float u1 = sprite.getU1();
			builder.addVertex(matrix, x, (float) startY + renderHeight, 100).setUv(u0, v1).setColor(red, green, blue, 1);
			builder.addVertex(matrix, (float) x + 16, (float) startY + renderHeight, 100).setUv(u1, v1).setColor(red, green, blue, 1);
			builder.addVertex(matrix, (float) x + 16, startY, 100).setUv(u1, v0).setColor(red, green, blue, 1);
			builder.addVertex(matrix, x, startY, 100).setUv(u0, v0).setColor(red, green, blue, 1);

			startY += renderHeight;
		} while (height > 0);

		// finish drawing sprites
		BufferUploader.drawWithShader(builder.buildOrThrow());
	}

	public static void renderControlBackground(GuiGraphics guiGraphics, int x, int y, int renderWidth, int renderHeight) {
		int u = 29;
		int v = 146;
		int textureBgWidth = 66;
		int textureBgHeight = 56;
		int halfWidth = renderWidth / 2;
		int halfHeight = renderHeight / 2;
		guiGraphics.blit(GuiHelper.GUI_CONTROLS, x, y, u, v, halfWidth, halfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
		guiGraphics.blit(GuiHelper.GUI_CONTROLS, x, y + halfHeight, u, (float) v + textureBgHeight - halfHeight, halfWidth, halfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
		guiGraphics.blit(GuiHelper.GUI_CONTROLS, x + halfWidth, y, (float) u + textureBgWidth - halfWidth, v, halfWidth, halfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
		guiGraphics.blit(GuiHelper.GUI_CONTROLS, x + halfWidth, y + halfHeight, (float) u + textureBgWidth - halfWidth, (float) v + textureBgHeight - halfHeight, halfWidth, halfHeight, GUI_CONTROLS_TEXTURE_WIDTH, GUI_CONTROLS_TEXTURE_HEIGHT);
	}

	public static void tryRenderGuiItem(GuiGraphics guiGraphics, ItemRenderer itemRenderer, @Nullable LivingEntity livingEntity, ItemStack stack, int x, int y, int rotation) {
		if (!stack.isEmpty()) {
			BakedModel bakedmodel = itemRenderer.getModel(stack, null, livingEntity, 0);
			try {
				renderGuiItem(guiGraphics, itemRenderer, stack, x, y, bakedmodel, rotation);
			}
			catch (Throwable throwable) {
				CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering item");
				CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
				crashreportcategory.setDetail("Item Type", () -> String.valueOf(stack.getItem()));
				crashreportcategory.setDetail("Item Components", () -> String.valueOf(stack.getComponents()));
				crashreportcategory.setDetail("Item Foil", () -> String.valueOf(stack.hasFoil()));
				throw new ReportedException(crashreport);
			}
		}
	}

	private static void renderGuiItem(GuiGraphics guiGraphics, ItemRenderer itemRenderer, ItemStack stack, int x, int y, BakedModel bakedModel, int rotation) {
		PoseStack posestack = guiGraphics.pose();
		posestack.pushPose();
		posestack.translate(x + 8F, y + 8F, 150.0F);
		if (rotation != 0) {
			posestack.mulPose(Axis.ZP.rotationDegrees(rotation));
		}
		posestack.scale(1.0F, -1.0F, 1.0F);
		posestack.scale(16.0F, 16.0F, 16.0F);
		RenderSystem.applyModelViewMatrix();
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		boolean flag = !bakedModel.usesBlockLight();
		if (flag) {
			Lighting.setupForFlatItems();
		}

		itemRenderer.render(stack, ItemDisplayContext.GUI, false, posestack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY, bakedModel);

		RenderSystem.disableDepthTest();
		bufferSource.endBatch();
		RenderSystem.enableDepthTest();

		if (flag) {
			Lighting.setupFor3DItems();
		}

		posestack.popPose();
		RenderSystem.applyModelViewMatrix();
	}

	public static void renderTooltip(Screen screen, GuiGraphics guiGraphics, List<Component> components, int x, int y) {
		List<ClientTooltipComponent> list = gatherTooltipComponents(components, x, screen.width, screen.height, screen.font);
		guiGraphics.renderTooltipInternal(screen.font, list, x, y, DefaultTooltipPositioner.INSTANCE);
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
