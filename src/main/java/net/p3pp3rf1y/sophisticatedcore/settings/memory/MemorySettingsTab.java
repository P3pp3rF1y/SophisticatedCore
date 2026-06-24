package net.p3pp3rf1y.sophisticatedcore.settings.memory;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTab;

import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

public class MemorySettingsTab extends SettingsTab<MemorySettingsContainer> {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(128, 32), Dimension.SQUARE_16);
	private static final TextureBlitData SELECT_ALL_SLOTS_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256,
			new UV(16, 80), Dimension.SQUARE_16);
	public static final ButtonDefinition SELECT_ALL_SLOTS = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND,
			DEFAULT_BUTTON_HOVERED_BACKGROUND, SELECT_ALL_SLOTS_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("select_all_slots")));
	private static final TextureBlitData UNSELECT_ALL_SLOTS_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256,
			new UV(48, 80), Dimension.SQUARE_16);
	public static final ButtonDefinition UNSELECT_ALL_SLOTS = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND,
			DEFAULT_BUTTON_HOVERED_BACKGROUND, UNSELECT_ALL_SLOTS_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("unselect_all_slots")));

	public MemorySettingsTab(MemorySettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, Component.translatable(TranslationHelper.INSTANCE.translSettings(MemorySettingsCategory.NAME)),
				new ImmutableList.Builder<Component>()
						.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsTooltip(MemorySettingsCategory.NAME)))
						.addAll(TranslationHelper.INSTANCE.getTranslatedLines(
								TranslationHelper.INSTANCE.translSettingsTooltip(MemorySettingsCategory.NAME) + "_detail", null, ChatFormatting.GRAY))
						.build(),
				new ImmutableList.Builder<Component>()
						.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsTooltip(MemorySettingsCategory.NAME)))
						.addAll(TranslationHelper.INSTANCE.getTranslatedLines(
								TranslationHelper.INSTANCE.translSettingsTooltip(MemorySettingsCategory.NAME) + "_open_detail", null, ChatFormatting.GRAY))
						.build(),
				onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		addHideableChild(new Button(new Position(x + 3, y + 24), SELECT_ALL_SLOTS, button -> container.selectAllSlots()));
		addHideableChild(new Button(new Position(x + 21, y + 24), UNSELECT_ALL_SLOTS, button -> container.unselectAllSlots()));
		addHideableChild(new ToggleButton<>(new Position(x + 39, y + 24), ButtonDefinitions.MATCH_NBT,
				button -> container.setIgnoreNbt(!container.ignoresNbt()), () -> !container.ignoresNbt()));
	}

	@Override
	public Optional<Integer> getSlotOverlayColor(int slotNumber, boolean templateLoadHovered) {
		return Optional.empty();
	}

	@Override
	public void handleSlotClick(Slot slot, int mouseButton) {
		if (mouseButton == 0) {
			getSettingsContainer().selectSlot(slot.index);
		} else if (mouseButton == 1) {
			getSettingsContainer().unselectSlot(slot.index);
		}
	}

	@Override
	public ItemStack getItemDisplayOverride(int slotNumber, boolean templateLoadHovered) {
		if (templateLoadHovered) {
			ItemStack templatesMemorizedStack = getSettingsContainer().getSelectedTemplatesMemorizedStack(slotNumber);
			if (!templatesMemorizedStack.isEmpty()) {
				return templatesMemorizedStack;
			}
			return ItemStack.EMPTY;
		}

		return getSettingsContainer().getMemorizedStack(slotNumber);
	}

	@Override
	public void drawSlotStackOverlay(GuiGraphics guiGraphics, Slot slot, boolean templateLoadHovered) {
		if (templateLoadHovered) {
			if (!getSettingsContainer().getSelectedTemplatesMemorizedStack(slot.getSlotIndex()).isEmpty()) {
				drawMemorizedStackOverlay(guiGraphics, slot);
			}
		} else if (getSettingsContainer().isSlotSelected(slot.getSlotIndex())) {
			drawMemorizedStackOverlay(guiGraphics, slot);
		}
	}

	private void drawMemorizedStackOverlay(GuiGraphics guiGraphics, Slot slot) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 300);
		guiGraphics.blit(RenderType::guiTextured, GuiHelper.GUI_CONTROLS, slot.x, slot.y, 77, 0, 16, 16, 256, 256);
		poseStack.popPose();
	}
}
