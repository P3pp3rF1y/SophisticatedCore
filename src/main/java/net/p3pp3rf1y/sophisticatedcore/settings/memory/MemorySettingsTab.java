package net.p3pp3rf1y.sophisticatedcore.settings.memory;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTab;

import java.util.List;
import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

public class MemorySettingsTab extends SettingsTab<MemorySettingsContainer> {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(128, 32), Dimension.SQUARE_16);
	private static final TextureBlitData SELECT_ALL_SLOTS_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(16, 80), Dimension.SQUARE_16);
	public static final ButtonDefinition SELECT_ALL_SLOTS = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, SELECT_ALL_SLOTS_FOREGROUND,
			new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("select_all_slots")));
	private static final TextureBlitData UNSELECT_ALL_SLOTS_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(48, 80), Dimension.SQUARE_16);
	public static final ButtonDefinition UNSELECT_ALL_SLOTS = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, UNSELECT_ALL_SLOTS_FOREGROUND,
			new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("unselect_all_slots")));

	private static final TextureBlitData SAVE_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(240, 16), Dimension.SQUARE_16);
	public static final ButtonDefinition SAVE_TEMPLATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, SAVE_FOREGROUND,
			new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("save_template")));

	private static final TextureBlitData LOAD_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(240, 32), Dimension.SQUARE_16);
	public static final ButtonDefinition LOAD_TEMPLATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, LOAD_FOREGROUND,
			new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("load_template")));
	private final Button loadTemplateButton;
	private final Button saveTemplateButton;

	public MemorySettingsTab(MemorySettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, new TranslatableComponent(TranslationHelper.INSTANCE.translSettings(MemorySettingsCategory.NAME)),
				new ImmutableList.Builder<Component>()
						.add(new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsTooltip(MemorySettingsCategory.NAME)))
						.addAll(TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsTooltip(MemorySettingsCategory.NAME) + "_detail", null, ChatFormatting.GRAY))
						.build(),
				new ImmutableList.Builder<Component>()
						.add(new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsTooltip(MemorySettingsCategory.NAME)))
						.addAll(TranslationHelper.INSTANCE.getTranslatedLines(TranslationHelper.INSTANCE.translSettingsTooltip(MemorySettingsCategory.NAME) + "_open_detail", null, ChatFormatting.GRAY))
						.build(),
				onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		addHideableChild(new Button(new Position(x + 3, y + 24), SELECT_ALL_SLOTS, button -> container.selectAllSlots()));
		addHideableChild(new Button(new Position(x + 21, y + 24), UNSELECT_ALL_SLOTS, button -> container.unselectAllSlots()));
		addHideableChild( new ToggleButton<>(new Position(x + 39, y + 24), ButtonDefinitions.MATCH_NBT,
				button -> container.setIgnoreNbt(!container.ignoresNbt()), () -> !container.ignoresNbt()));

		saveTemplateButton = new Button(new Position(x + 12, y + 44), SAVE_TEMPLATE, button -> container.saveTemplate()) {
			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
				container.scrollSaveSlot(delta > 0);
				setSaveTooltip();
				return true;
			}
		};
		setSaveTooltip();
		addHideableChild(saveTemplateButton);
		loadTemplateButton = new Button(new Position(x + 30, y + 44), LOAD_TEMPLATE, button -> container.loadTemplate()) {
			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
				container.scrollLoadSlot(delta > 0);
				setLoadTooltip();
				return true;
			}
		};
		setLoadTooltip();
		addHideableChild(loadTemplateButton);
	}

	private void setLoadTooltip() {
		if (getSettingsContainer().getLoadSlot() == -1) {
			loadTemplateButton.setTooltip(List.of(new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("load_template.no_save")).withStyle(ChatFormatting.RED)));
		} else {
			loadTemplateButton.setTooltip(List.of(
					new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("load_template"), new TextComponent(String.valueOf(getSettingsContainer().getLoadSlot())).withStyle(ChatFormatting.GREEN)),
					new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("load_template.controls")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY))
			);
		}
	}

	private void setSaveTooltip() {
		saveTemplateButton.setTooltip(List.of(
				new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("save_template"), new TextComponent(String.valueOf(getSettingsContainer().getSaveSlot())).withStyle(ChatFormatting.GREEN)),
				new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsButton("save_template.controls")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY))
		);
	}

	@Override
	public Optional<Integer> getSlotOverlayColor(int slotNumber) {
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
	public ItemStack getItemDisplayOverride(int slotNumber) {
		if (loadTemplateButton.isHovered()) {
			ItemStack templatesMemorizedStack = getSettingsContainer().getSelectedTemplatesMemorizedStack(slotNumber);
			if (!templatesMemorizedStack.isEmpty()) {
				return templatesMemorizedStack;
			}
		}

		return getSettingsContainer().getMemorizedStack(slotNumber);
	}

	@Override
	public void drawSlotStackOverlay(PoseStack poseStack, Slot slot) {
		if (getSettingsContainer().isSlotSelected(slot.getSlotIndex()) || isShowingTemplateItemInSlot(slot)) {
			drawMemorizedStackOverlay(poseStack, slot);
		}
	}

	private boolean isShowingTemplateItemInSlot(Slot slot) {
		return loadTemplateButton.isHovered() && !getSettingsContainer().getSelectedTemplatesMemorizedStack(slot.getSlotIndex()).isEmpty();
	}

	private void drawMemorizedStackOverlay(PoseStack poseStack, Slot slot) {
		poseStack.pushPose();
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GuiHelper.GUI_CONTROLS);
		blit(poseStack, slot.x, slot.y, 77, 0, 16, 16);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		poseStack.popPose();
	}
}
