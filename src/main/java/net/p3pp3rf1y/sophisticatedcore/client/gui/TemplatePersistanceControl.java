package net.p3pp3rf1y.sophisticatedcore.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.CompositeWidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.TextBox;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.common.gui.TemplatePersistanceContainer;

import java.util.List;
import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

public class TemplatePersistanceControl extends CompositeWidgetBase<WidgetBase> {
	private static final int BUTTON_GAP = 0;
	private static final TextureBlitData SAVE_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(240, 16), Dimension.SQUARE_16);
	public static final ButtonDefinition SAVE_TEMPLATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, SAVE_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("save_template")));

	private static final TextureBlitData LOAD_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(240, 32), Dimension.SQUARE_16);
	public static final ButtonDefinition LOAD_TEMPLATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, LOAD_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("load_template")));
	private static final TextureBlitData EXPORT_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(240, 48), Dimension.SQUARE_16);
	public static final ButtonDefinition EXPORT_TEMPLATE = new ButtonDefinition(Dimension.SQUARE_16, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, EXPORT_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("export_template")));
	private final Button loadTemplateButton;
	private final Button saveTemplateButton;
	private final Button exportTemplateButton;
	private final TextBox saveInput;
	private final TemplatePersistanceContainer container;
	private final TextBox exportInput;

	protected TemplatePersistanceControl(Position position, TemplatePersistanceContainer container) {
		super(position, new Dimension(18, 2 * 18 + BUTTON_GAP));
		this.container = container;
		container.setOnSlotsRefreshed(() -> {
			setSaveTooltip();
			setLoadTooltip();
		});
		saveInput = new TextBox(new Position(x + 20, y), new Dimension(192, 18)) {
			@Override
			protected void onEnterPressed() {
				container.saveTemplate(getValue());
				setValue("");
				setSaveTooltip();
			}
		};
		saveInput.setVisible(false);
		addChild(saveInput);
		saveTemplateButton = new Button(new Position(x, y), SAVE_TEMPLATE, button -> {
			container.saveTemplate(saveInput.getValue());
			saveInput.setValue("");
			setSaveTooltip();
		}) {
			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
				container.scrollSaveSlot(delta > 0);
				setSaveTooltip();
				return true;
			}

			@Override
			public void renderTooltip(Screen screen, PoseStack poseStack, int mouseX, int mouseY) {
				boolean mouseOver = isMouseOver(mouseX, mouseY);
				boolean showTextBox = container.showsTextbox() && mouseOver;
				if (mouseOver) {
					screen.renderTooltip(poseStack, getTooltip(), Optional.empty(), saveTemplateButton.getX() + 10, saveTemplateButton.getY() + (showTextBox ? -13 : 6));
				}
				saveInput.setVisible(showTextBox);
				saveInput.setFocus(showTextBox);
				if (mouseOver && screen.getFocused() != saveInput) {
					screen.setFocused(saveInput);
				} else if (!mouseOver && screen.getFocused() == saveInput) {
					screen.setFocused(null);
				}
			}
		};
		setSaveTooltip();
		addChild(saveTemplateButton);

		loadTemplateButton = new Button(new Position(x, y + 18 + BUTTON_GAP), LOAD_TEMPLATE, button -> container.loadTemplate()) {
			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
				container.scrollLoadSlot(delta > 0);
				setLoadTooltip();
				return true;
			}

			@Override
			public void renderTooltip(Screen screen, PoseStack poseStack, int mouseX, int mouseY) {
				boolean mouseOver = isMouseOver(mouseX, mouseY);
				if (mouseOver) {
					screen.renderTooltip(poseStack, getTooltip(), Optional.empty(), loadTemplateButton.getX() + 10, loadTemplateButton.getY() + 6);
				}
			}
		};
		setLoadTooltip();
		addChild(loadTemplateButton);

		exportInput = new TextBox(new Position(x + 20, y + 2 * (18 + BUTTON_GAP)), new Dimension(171, 18)) {
			@Override
			protected void onEnterPressed() {
				container.exportTemplate(getValue());
				setValue("");
			}
		};
		exportInput.setVisible(false);
		addChild(exportInput);
		exportTemplateButton = new Button(new Position(x, y + 2 * (18 + BUTTON_GAP)), EXPORT_TEMPLATE, button -> container.exportTemplate(exportInput.getValue())) {
			@Override
			public void renderTooltip(Screen screen, PoseStack poseStack, int mouseX, int mouseY) {
				boolean mouseOver = isMouseOver(mouseX, mouseY);
				if (mouseOver) {
					screen.renderTooltip(poseStack, getTooltip(), Optional.empty(), exportTemplateButton.getX() + 10, exportTemplateButton.getY() - 13);
				}
				exportInput.setVisible(mouseOver);
				exportInput.setFocus(mouseOver);
				if (mouseOver && screen.getFocused() != exportInput) {
					screen.setFocused(exportInput);
				} else if (!mouseOver && screen.getFocused() == exportInput) {
					screen.setFocused(null);
				}
			}
		};

		exportTemplateButton.setTooltip(List.of(
				Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("export_template"),
						Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("export_template.enter_file_name")).withStyle(ChatFormatting.GREEN)),
				Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("export_template.additional_info")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY))
		);
		addChild(exportTemplateButton);
	}

	private void setLoadTooltip() {
		if (container.getLoadSlot() == -1) {
			loadTemplateButton.setTooltip(List.of(Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("load_template.no_save")).withStyle(ChatFormatting.RED)));
		} else {
			List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
			tooltip.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("load_template"), container.getLoadSlotTooltipName().withStyle(ChatFormatting.GREEN)));
			container.getLoadSlotSource().ifPresent(source -> tooltip.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("load_template.source"), Component.literal(source).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY)));
			tooltip.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("load_template.controls")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
			if (container.templateHasTooManySlots()) {
				tooltip.add(Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("load_template.too_many_setting_slots")).withStyle(ChatFormatting.RED));
			}
			loadTemplateButton.setTooltip(tooltip
			);
		}
	}

	private void setSaveTooltip() {
		saveTemplateButton.setTooltip(List.of(
				Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("save_template"), container.getSaveSlotTooltipName().withStyle(ChatFormatting.GREEN)),
				Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("save_template.controls")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY))
		);
	}

	@Override
	protected void renderBg(PoseStack matrixStack, Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
		//noop
	}

	public boolean isTemplateLoadHovered() {
		return loadTemplateButton.isHovered();
	}
}
