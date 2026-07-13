package net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.settings.ColorToggleButton;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTab;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

public class ItemDisplaySettingsTab extends SettingsTab<ItemDisplaySettingsContainer> {
	private static final Vector3f ITEM_DISPLAY_PREVIEW_LIGHT_0 = new Vector3f(-0.65F, 0.25F, 0.8F).normalize();
	private static final Vector3f ITEM_DISPLAY_PREVIEW_LIGHT_1 = new Vector3f(-0.15F, 0.05F, 1F).normalize();
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(112, 64), Dimension.SQUARE_16);
	private static final TextureBlitData SLOT_SELECTION = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(93, 0), Dimension.SQUARE_24);
	private static final int BUTTON_SPACING = 2;
	private static final int BUTTON_ROW_WIDTH = 4 * Dimension.SQUARE_16.width() + 3 * BUTTON_SPACING + 2;
	private static final Dimension PREVIEW_DIMENSION = new Dimension(BUTTON_ROW_WIDTH, BUTTON_ROW_WIDTH);
	private static final List<Component> ROTATE_TOOLTIP = new ImmutableList.Builder<Component>()
			.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("rotate")))
			.addAll(TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("rotate_detail"), null, ChatFormatting.GRAY))
			.build();
	private static final TextureBlitData ROTATE_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(128, 64),
			Dimension.SQUARE_16);
	public static final ButtonDefinition ROTATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND,
			ROTATE_FOREGROUND);
	private static final TextureBlitData Z_OFFSET_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(96, 112),
			Dimension.SQUARE_16);
	public static final ButtonDefinition Z_OFFSET = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND,
			Z_OFFSET_FOREGROUND);

	private static final ButtonDefinition.Toggle<DisplaySide> DISPLAY_SIDE = ButtonDefinitions.createToggleButtonDefinition(Map.of(DisplaySide.FRONT,
			GuiHelper.getButtonStateData(new UV(144, 64), Dimension.SQUARE_16, new Position(1, 1),
					TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("display_side_front"), null)),
			DisplaySide.LEFT,
			GuiHelper.getButtonStateData(new UV(160, 64), Dimension.SQUARE_16, new Position(1, 1),
					TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("display_side_left"), null)),
			DisplaySide.RIGHT, GuiHelper.getButtonStateData(new UV(176, 64), Dimension.SQUARE_16, new Position(1, 1),
					TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("display_side_right"), null))));
	private int currentSelectedSlot = -1;
	private final ItemDisplayPreview preview;

	public ItemDisplaySettingsTab(ItemDisplaySettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, Component.translatable(TranslationHelper.INSTANCE.translSettings(ItemDisplaySettingsCategory.NAME)),
				new ImmutableList.Builder<Component>()
						.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsTooltip(ItemDisplaySettingsCategory.NAME)))
						.addAll(TranslationHelper.INSTANCE.getTranslatedLines(
								TranslationHelper.INSTANCE.translSettingsTooltip(ItemDisplaySettingsCategory.NAME) + "_detail", null, ChatFormatting.GRAY))
						.build(),
				new ImmutableList.Builder<Component>()
						.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsTooltip(ItemDisplaySettingsCategory.NAME)))
						.addAll(TranslationHelper.INSTANCE.getTranslatedLines(
								TranslationHelper.INSTANCE.translSettingsTooltip(ItemDisplaySettingsCategory.NAME) + "_open_detail", null, ChatFormatting.GRAY))
						.build(),
				onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		int buttonX = x + 3;
		int buttonY = y + 24 + PREVIEW_DIMENSION.height() + 1;
		preview = addHideableChild(new ItemDisplayPreview(new Position(x + 3, y + 24)));
		if (showSlotColorSelection()) {
			addHideableChild(new ColorToggleButton(new Position(buttonX, buttonY), container::getColor, container::setColor));
			buttonX += Dimension.SQUARE_16.width() + BUTTON_SPACING;
		}
		addHideableChild(new Button(new Position(buttonX, buttonY), ROTATE, button -> {
			if (button == 0) {
				container.rotateClockwise(currentSelectedSlot);
			} else if (button == 1) {
				container.rotateCounterClockwise(currentSelectedSlot);
			}
		}) {
			@Override
			protected List<Component> getTooltip() {
				return ROTATE_TOOLTIP;
			}
		});
		buttonX += Dimension.SQUARE_16.width() + BUTTON_SPACING;
		addHideableChild(new Button(new Position(buttonX, buttonY), Z_OFFSET, button -> container.changeZOffset(currentSelectedSlot, button == 0 ? 1 : -1)) {
			@Override
			protected List<Component> getTooltip() {
				return getZOffsetTooltip();
			}
		});
		buttonX += Dimension.SQUARE_16.width() + BUTTON_SPACING;
		if (showSideSelection()) {
			addHideableChild(new ToggleButton<>(new Position(buttonX, buttonY), DISPLAY_SIDE, button -> {
				if (button == 0) {
					container.setDisplaySide(container.getDisplaySide().next());
				} else if (button == 1) {
					container.setDisplaySide(container.getDisplaySide().previous());
				}
				preview.updateTargetRotation();
			}, container::getDisplaySide));
		}
		currentSelectedSlot = getSettingsContainer().getFirstSelectedSlot();
		preview.updateTargetRotation();
	}

	@Override
	public Optional<Integer> getSlotOverlayColor(int slotNumber, boolean templateLoadHovered) {
		if (!getSettingsContainer().canDeselectSlots()) {
			return Optional.empty();
		}

		if (templateLoadHovered) {
			return getSettingsContainer().getSettingsContainer().getSelectedTemplatesCategory(ItemDisplaySettingsCategory.class)
					.filter(c -> c.getSlots().contains(slotNumber)).map(category -> category.getColor().getTextureDiffuseColor() & 0x00_FFFFFF | (80 << 24));
		}

		return getSettingsContainer().isSlotSelected(slotNumber)
				? Optional.of(getSettingsContainer().getColor().getTextureDiffuseColor() | (80 << 24))
				: Optional.empty();
	}

	@Override
	public void handleSlotClick(Slot slot, int mouseButton) {
		if (mouseButton == 0) {
			getSettingsContainer().selectSlot(slot.index);
			if (getSettingsContainer().isSlotSelected(slot.index)) {
				currentSelectedSlot = slot.index;
			}
		} else if (mouseButton == 1 && getSettingsContainer().canDeselectSlots()) {
			getSettingsContainer().unselectSlot(slot.index);
			if (!getSettingsContainer().isSlotSelected(slot.index) && currentSelectedSlot == slot.index) {
				currentSelectedSlot = getSettingsContainer().getFirstSelectedSlot();
			}
		}
	}

	@Override
	public void renderExtra(GuiGraphics guiGraphics, Slot slot) {
		super.renderExtra(guiGraphics, slot);
		if (isOpen && slot.index == currentSelectedSlot) {
			GuiHelper.blit(guiGraphics, slot.x - 4, slot.y - 4, SLOT_SELECTION);
		}
	}

	@Override
	public int getItemRotation(int slotIndex, boolean templateLoadHovered) {
		if (templateLoadHovered) {
			return getSettingsContainer().getSettingsContainer().getSelectedTemplatesCategory(ItemDisplaySettingsCategory.class)
					.filter(c -> c.getSlots().contains(slotIndex)).map(category -> category.getRotation(slotIndex)).orElse(0);
		}

		return getSettingsContainer().getRotation(slotIndex);
	}

	private boolean showSideSelection() {
		return getSettingsContainer().supportsSideSelection();
	}

	private boolean showSlotColorSelection() {
		return getSettingsContainer().canDeselectSlots();
	}

	private List<Component> getZOffsetTooltip() {
		return new ImmutableList.Builder<Component>().add(Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("z_offset"))).addAll(
				TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("z_offset_detail"), null, ChatFormatting.GRAY))
				.build();
	}

	public void renderDefaultItemDisplaySettingsPreview(GuiGraphics guiGraphics, int x, int y, int width, int height, ItemDisplaySettingsContainer container,
			int selectedSlot, float xAxisRotation, float yAxisRotation) {
		IItemDisplaySettingsPreviewProvider provider = getPreviewProvider();
		provider.getItemDisplaySettingsPreviewStack(screen, container, selectedSlot).ifPresent(stack -> {
			float previewYAxisRotation = provider.getItemDisplayPreviewYAxisRotation(yAxisRotation);
			renderWithItemDisplayPreviewLighting(guiGraphics, () -> renderItemDisplayPreviewItem(guiGraphics, stack, x, y, width, height, xAxisRotation,
					previewYAxisRotation, provider.getItemDisplayPreviewScaleMultiplier()));
		});
	}

	public void renderItemDisplayPreviewItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int width, int height, float xAxisRotation,
			float yAxisRotation, float scaleMultiplier) {
		if (stack.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		ItemStackRenderState renderState = new ItemStackRenderState();
		minecraft.getItemModelResolver().updateForTopItem(renderState, stack, ItemDisplayContext.NONE, minecraft.level, minecraft.player, 0);
		if (renderState.isEmpty()) {
			return;
		}

		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(x + width / 2F, y + height / 2F, 150);
		poseStack.mulPose(Axis.XN.rotationDegrees(xAxisRotation));
		poseStack.mulPose(Axis.YP.rotationDegrees(yAxisRotation));
		float scale = (Math.min(width, height) - 16) * scaleMultiplier;
		poseStack.scale(scale, -scale, scale);
		poseStack.translate(-0.5, -0.5, -0.5);
		int combinedLight = 15728880;
		guiGraphics.drawSpecial(buffer -> renderState.render(poseStack, buffer, combinedLight, OverlayTexture.NO_OVERLAY));
		poseStack.popPose();
	}

	public void renderWithItemDisplayPreviewLighting(GuiGraphics guiGraphics, Runnable renderPreview) {
		RenderSystem.setShaderLights(ITEM_DISPLAY_PREVIEW_LIGHT_0, ITEM_DISPLAY_PREVIEW_LIGHT_1);
		renderPreview.run();
		guiGraphics.flush();
		Lighting.setupFor3DItems();
	}

	private IItemDisplaySettingsPreviewProvider getPreviewProvider() {
		return screen.getItemDisplaySettingsPreviewProvider();
	}

	private class ItemDisplayPreview extends RotatablePreviewWidget {
		protected ItemDisplayPreview(Position position) {
			super(position, PREVIEW_DIMENSION);
		}

		private void updateTargetRotation() {
			switch (getSettingsContainer().getDisplaySide()) {
				case FRONT -> setTargetRotations(30, 45);
				case LEFT -> setTargetRotations(30, 135);
				case RIGHT -> setTargetRotations(30, -45);
			}
		}

		@Override
		protected void renderPreview(GuiGraphics guiGraphics, int x, int y, int width, int height, float xAxisRotation, float yAxisRotation,
				float partialTicks) {
			IItemDisplaySettingsPreviewProvider provider = getPreviewProvider();
			if (!provider.renderItemDisplaySettingsPreview(ItemDisplaySettingsTab.this, screen, guiGraphics, x, y, width, height, getSettingsContainer(),
					currentSelectedSlot, xAxisRotation, yAxisRotation, partialTicks)) {
				renderDefaultItemDisplaySettingsPreview(guiGraphics, x, y, width, height, getSettingsContainer(), currentSelectedSlot, xAxisRotation,
						yAxisRotation);
			}
		}
	}
}
