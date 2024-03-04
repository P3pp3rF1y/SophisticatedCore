package net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.api.IDisplaySideStorage;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ImageButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.settings.ColorToggleButton;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTab;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

public class ItemDisplaySettingsTab extends SettingsTab<ItemDisplaySettingsContainer> {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(112, 64), Dimension.SQUARE_16);
	private static final TextureBlitData SLOT_SELECTION = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(93, 0), Dimension.SQUARE_24);
	private static final List<Component> ROTATE_TOOLTIP = new ImmutableList.Builder<Component>()
			.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("rotate")))
			.addAll(TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("rotate_detail"), null, ChatFormatting.GRAY))
			.build();
	private static final TextureBlitData ROTATE_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(128, 64), Dimension.SQUARE_16);
	public static final ButtonDefinition ROTATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, ROTATE_FOREGROUND);

	private static final ButtonDefinition.Toggle<DisplaySide> DISPLAY_SIDE = ButtonDefinitions.createToggleButtonDefinition(
			Map.of(
					DisplaySide.FRONT, GuiHelper.getButtonStateData(new UV(144, 64), Dimension.SQUARE_16, new Position(1, 1),
							TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("display_side_front"), null)),
					DisplaySide.LEFT, GuiHelper.getButtonStateData(new UV(160, 64), Dimension.SQUARE_16, new Position(1, 1),
							TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("display_side_left"), null)),
					DisplaySide.RIGHT, GuiHelper.getButtonStateData(new UV(176, 64), Dimension.SQUARE_16, new Position(1, 1),
							TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsButton("display_side_right"), null))
			));
	private int currentSelectedSlot = -1;

	public ItemDisplaySettingsTab(ItemDisplaySettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, Component.translatable(TranslationHelper.INSTANCE.translSettings(ItemDisplaySettingsCategory.NAME)),
				new ImmutableList.Builder<Component>()
						.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsTooltip(ItemDisplaySettingsCategory.NAME)))
						.addAll(TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsTooltip(ItemDisplaySettingsCategory.NAME) + "_detail", null, ChatFormatting.GRAY))
						.build(),
				new ImmutableList.Builder<Component>()
						.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsTooltip(ItemDisplaySettingsCategory.NAME)))
						.addAll(TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsTooltip(ItemDisplaySettingsCategory.NAME) + "_open_detail", null, ChatFormatting.GRAY))
						.build(),
				onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		addHideableChild(new Button(new Position(x + 3, y + 24), ROTATE, button -> {
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
		addHideableChild(new ColorToggleButton(new Position(x + 21, y + 24), container::getColor, container::setColor));
		if (showSideSelection()) {
			addHideableChild(new ToggleButton<>(new Position(x + 39, y + 24), DISPLAY_SIDE, button -> {
				if (button == 0) {
					container.setDisplaySide(container.getDisplaySide().next());
				} else if (button == 1) {
					container.setDisplaySide(container.getDisplaySide().previous());
				}
			}, container::getDisplaySide));
		}
		currentSelectedSlot = getSettingsContainer().getFirstSelectedSlot();
	}

	@Override
	public Optional<Integer> getSlotOverlayColor(int slotNumber, boolean templateLoadHovered) {
		if (templateLoadHovered) {
			return getSettingsContainer().getSettingsContainer().getSelectedTemplatesCategory(ItemDisplaySettingsCategory.class)
					.filter(c -> c.getSlots().contains(slotNumber))
					.map(category -> ColorHelper.getColor(category.getColor().getTextureDiffuseColors()) | (80 << 24));
		}

		return getSettingsContainer().isSlotSelected(slotNumber) ? Optional.of(ColorHelper.getColor(getSettingsContainer().getColor().getTextureDiffuseColors()) | (80 << 24)) : Optional.empty();
	}

	@Override
	public void handleSlotClick(Slot slot, int mouseButton) {
		if (mouseButton == 0) {
			getSettingsContainer().selectSlot(slot.index);
			if (getSettingsContainer().isSlotSelected(slot.index)) {
				currentSelectedSlot = slot.index;
			}
		} else if (mouseButton == 1) {
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
			RenderSystem.disableDepthTest();
			RenderSystem.colorMask(true, true, true, false);
			GuiHelper.blit(guiGraphics, slot.x - 4, slot.y - 4, SLOT_SELECTION);
			RenderSystem.colorMask(true, true, true, true);
			RenderSystem.enableDepthTest();
		}
	}

	@Override
	public int getItemRotation(int slotIndex, boolean templateLoadHovered) {
		if (templateLoadHovered) {
			return getSettingsContainer().getSettingsContainer().getSelectedTemplatesCategory(ItemDisplaySettingsCategory.class)
					.filter(c -> c.getSlots().contains(slotIndex))
					.map(category -> category.getRotation(slotIndex))
					.orElse(0);
		}

		return getSettingsContainer().getRotation(slotIndex);
	}

	private boolean showSideSelection() {
		if (minecraft.level == null) {
			return false;
		}
		BlockState state = minecraft.level.getBlockState(getSettingsContainer().getSettingsContainer().getBlockPosition());
		return state.getBlock() instanceof IDisplaySideStorage displaySideStorage && displaySideStorage.canChangeDisplaySide(state);
	}
}
