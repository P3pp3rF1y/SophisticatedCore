package net.p3pp3rf1y.sophisticatedcore.upgrades.xppump;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions.createToggleButtonDefinition;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_BACKGROUND;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND;

public class XpPumpUpgradeTab extends UpgradeSettingsTab<XpPumpUpgradeContainer> {
	private static final ButtonDefinition.Toggle<AutomationDirection> DIRECTION = createToggleButtonDefinition(
			Map.of(
					AutomationDirection.INPUT, GuiHelper.getButtonStateData(new UV(144, 0), TranslationHelper.INSTANCE.translUpgradeButton("xp_pump_input"), Dimension.SQUARE_16, new Position(1, 1)),
					AutomationDirection.OUTPUT, GuiHelper.getButtonStateData(new UV(128, 16), TranslationHelper.INSTANCE.translUpgradeButton("xp_pump_output"), Dimension.SQUARE_16, new Position(1, 1)),
					AutomationDirection.OFF, GuiHelper.getButtonStateData(new UV(240, 0), TranslationHelper.INSTANCE.translUpgradeButton("xp_pump_off"), Dimension.SQUARE_16, new Position(1, 1))
			));
	private static final ButtonDefinition.Toggle<Boolean> MEND_ITEMS = createToggleButtonDefinition(
			Map.of(
					true, GuiHelper.getButtonStateData(new UV(144, 32), TranslationHelper.INSTANCE.translUpgradeButton("mend_items"), Dimension.SQUARE_16, new Position(1, 1)),
					false, GuiHelper.getButtonStateData(new UV(160, 32), TranslationHelper.INSTANCE.translUpgradeButton("do_not_mend_items"), Dimension.SQUARE_16, new Position(1, 1))
			));
	private static final TextureBlitData STORE_ALL_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(192, 16), Dimension.SQUARE_16);
	public static final ButtonDefinition STORE_ALL = new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, STORE_ALL_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("store_all_experience")));
	private static final TextureBlitData TAKE_ALL_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(144, 16), Dimension.SQUARE_16);
	public static final ButtonDefinition TAKE_ALL = new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, TAKE_ALL_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("take_all_experience")));
	private static final TextureBlitData TAKE_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(160, 16), Dimension.SQUARE_16);
	public static final ButtonDefinition TAKE = new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, TAKE_FOREGROUND);
	private static final TextureBlitData STORE_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(176, 16), Dimension.SQUARE_16);
	public static final ButtonDefinition STORE = new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND, STORE_FOREGROUND);
	private final Button takeButton;
	private final Button storeButton;

	public XpPumpUpgradeTab(XpPumpUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, boolean isMendingTurnedOn) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("xp_pump"), TranslationHelper.INSTANCE.translUpgradeTooltip("xp_pump"));
		int currentYOffset = 24;
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + currentYOffset), DIRECTION,
				button -> getContainer().setDirection(getContainer().getDirection().next()),
				() -> getContainer().getDirection()));
		addHideableChild(new LevelSelector(new Position(x + 21, y + currentYOffset), () -> String.valueOf(upgradeContainer.getLevel()), delta ->
				upgradeContainer.setLevel(upgradeContainer.getLevel() + (delta > 0 ? 1 : -1))));
		currentYOffset += 20;

		if (isMendingTurnedOn) {
			addHideableChild(new ToggleButton<>(new Position(x + 3, y + currentYOffset), MEND_ITEMS, button -> upgradeContainer.setMendItems(!upgradeContainer.shouldMendItems()), upgradeContainer::shouldMendItems));
			currentYOffset += 20;
		}

		addHideableChild(new Button(new Position(x + 3, y + currentYOffset), TAKE_ALL, button -> upgradeContainer.takeAllExperience()));
		takeButton = new Button(new Position(x + 21, y + currentYOffset), TAKE, button -> upgradeContainer.takeLevels()) {
			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
				upgradeContainer.setLevelsToTake(upgradeContainer.getLevelsToTake() + (delta > 0 ? 1 : -1));
				setTakeTooltip();
				return true;
			}
		};
		setTakeTooltip();
		addHideableChild(takeButton);
		storeButton = new Button(new Position(x + 39, y + currentYOffset), STORE, button -> upgradeContainer.storeLevels()) {
			@Override
			public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
				upgradeContainer.setLevelsToStore(upgradeContainer.getLevelsToStore() + (delta > 0 ? 1 : -1));
				setStoreTooltip();
				return true;
			}
		};
		setStoreTooltip();
		addHideableChild(storeButton);
		addHideableChild(new Button(new Position(x + 57, y + currentYOffset), STORE_ALL, button -> upgradeContainer.storeAllExperience()));

	}

	private void setStoreTooltip() {
		storeButton.setTooltip(List.of(
				Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("store_levels"), Component.literal(String.valueOf(getContainer().getLevelsToStore())).withStyle(ChatFormatting.RED)),
				Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("store_levels.controls")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY))
		);
	}

	private void setTakeTooltip() {
		takeButton.setTooltip(List.of(
				Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("take_levels"), Component.literal(String.valueOf(getContainer().getLevelsToTake())).withStyle(ChatFormatting.GREEN)),
				Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("take_levels.controls")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY))
		);
	}

	@Override
	protected void moveSlotsToTab() {
		//noop
	}

	private static class LevelSelector extends WidgetBase {
		private final Supplier<String> getText;
		private final DoubleConsumer onScroll;

		private static final List<Component> TOOLTIP = List.of(
				Component.translatable(TranslationHelper.INSTANCE.translUpgradeControl("xp_level_select.tooltip")),
				Component.translatable(TranslationHelper.INSTANCE.translUpgradeControl("xp_level_select.tooltip.controls")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));

		protected LevelSelector(Position position, Supplier<String> getText, DoubleConsumer onScroll) {
			super(position, new Dimension(54, 18));
			this.getText = getText;
			this.onScroll = onScroll;
		}

		@Override
		protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			GuiHelper.renderControlBackground(guiGraphics, x, y, 54, 18);
		}

		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
			String text = getText.get();
			Component fullText = Component.translatable(TranslationHelper.INSTANCE.translUpgradeControl("xp_level_select"), Component.literal(text).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY);
			int xOffset = (getWidth() - minecraft.font.width(fullText)) / 2;
			int yOffset = (int) Math.ceil((getHeight() - minecraft.font.lineHeight) / 2d);
			guiGraphics.drawString(minecraft.font, fullText, x + xOffset, y + yOffset, DyeColor.BLACK.getTextColor(), false);
		}

		@Override
		public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
			super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
			if (isMouseOver(mouseX, mouseY)) {
				guiGraphics.renderTooltip(screen.font, TOOLTIP, Optional.empty(), mouseX, mouseY);
			}
		}

		@Override
		public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
			onScroll.accept(pDelta);
			return true;
		}

		@Override
		public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
			//TODO narration
		}
	}
}
