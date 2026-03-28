package net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.EntityMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.ICONS;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.getButtonStateData;

public class AlchemyUpgradeTab extends UpgradeSettingsTab<AlchemyUpgradeContainer> {
	public static final ButtonDefinition.Toggle<AlchemyCondition> ALCHEMY_CONDITION = ButtonDefinitions.createToggleButtonDefinition(
			Map.of(
					AlchemyCondition.NEVER, getButtonStateData(new UV(240, 0), TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition_never"), Dimension.SQUARE_16, new Position(1, 1)),
					AlchemyCondition.ALWAYS, getButtonStateData(new UV(128, 64), TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition_always"), Dimension.SQUARE_16, new Position(1, 1)),
					AlchemyCondition.ON_FIRE, getButtonStateData(new UV(176, 80), TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition_on_fire"), Dimension.SQUARE_16, new Position(1, 1)),
					AlchemyCondition.UNDER_WATER, getButtonStateData(new UV(192, 80), TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition_under_water"), Dimension.SQUARE_16, new Position(1, 1)),
					AlchemyCondition.FALLING, getButtonStateData(new UV(208, 80), TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition_falling"), Dimension.SQUARE_16, new Position(1, 1)),
					AlchemyCondition.SPRINTING, getButtonStateData(new UV(224, 80), TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition_sprinting"), Dimension.SQUARE_16, new Position(1, 1)),
					AlchemyCondition.NEGATIVE_EFFECT, getButtonStateData(new UV(240, 80), TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition_negative_effect"), Dimension.SQUARE_16, new Position(1, 1)),
					AlchemyCondition.MINING, getButtonStateData(new UV(64, 64), TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition_mining"), Dimension.SQUARE_16, new Position(1, 1)),
					AlchemyCondition.HURT, getButtonStateData(new UV(96, 16), TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition_hurt"), Dimension.SQUARE_16, new Position(1, 1))
			));
	private static final TextureBlitData DISABLED_CONDITION = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(240, 64), Dimension.SQUARE_16);

	public static final ButtonDefinition.Toggle<Boolean> ANY_EFFECT_MISSING = ButtonDefinitions.createToggleButtonDefinition(
			Map.of(
					true, getButtonStateData(new UV(176, 96), TranslationHelper.INSTANCE.translUpgradeButton("any_effect_missing"), Dimension.SQUARE_16, new Position(1, 1)),
					false, getButtonStateData(new UV(48, 80), TranslationHelper.INSTANCE.translUpgradeButton("all_effects_missing"), Dimension.SQUARE_16, new Position(1, 1))
			)
	);
	public static final ButtonDefinition.Toggle<Boolean> MATCH_DURATION = ButtonDefinitions.createToggleButtonDefinition(
			Map.of(
					true, getButtonStateData(new UV(192, 96), TranslationHelper.INSTANCE.translUpgradeButton("match_duration"), Dimension.SQUARE_16, new Position(1, 1)),
					false, getButtonStateData(new UV(208, 96), TranslationHelper.INSTANCE.translUpgradeButton("ignore_duration"), Dimension.SQUARE_16, new Position(1, 1))
			)
	);
	public static final ButtonDefinition.Toggle<Boolean> MATCH_AMPLIFIER = ButtonDefinitions.createToggleButtonDefinition(
			Map.of(
					true, getButtonStateData(new UV(224, 96), TranslationHelper.INSTANCE.translUpgradeButton("match_amplifier"), Dimension.SQUARE_16, new Position(1, 1)),
					false, getButtonStateData(new UV(240, 96), TranslationHelper.INSTANCE.translUpgradeButton("ignore_amplifier"), Dimension.SQUARE_16, new Position(1, 1))
			)
	);

	public static final ButtonDefinition.Toggle<EntityMatch> ENTITY_MATCH = ButtonDefinitions.createToggleButtonDefinition(
			Map.of(
					EntityMatch.ENTITIES, getButtonStateData(new UV(128, 96), TranslationHelper.INSTANCE.translUpgradeButton("entity_match_entities"), Dimension.SQUARE_16, new Position(1, 1)),
					EntityMatch.PLAYERS_AND_ENTITIES, getButtonStateData(new UV(144, 96), TranslationHelper.INSTANCE.translUpgradeButton("entity_match_players_and_entities"), Dimension.SQUARE_16, new Position(1, 1)),
					EntityMatch.PLAYERS, getButtonStateData(new UV(160, 96), TranslationHelper.INSTANCE.translUpgradeButton("entity_match_players"), Dimension.SQUARE_16, new Position(1, 1))
			)
	);

	private static final int TOP_POS = 24;
	private final int buttonYOffset;

	protected AlchemyUpgradeTab(AlchemyUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, MutableComponent tabLabel, Component closedTooltip, boolean topButtonsOffset) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);

		buttonYOffset = topButtonsOffset ? 20 : 0;

		for (int i = 0; i < upgradeContainer.getSlots().size(); i++) {
			int slot = i;
			addHideableChild(new ToggleButton<>(new Position(x + 21 + (i % 2) * (18 + 18), y + TOP_POS + buttonYOffset + (i / 2) * 20), ALCHEMY_CONDITION, button -> getContainer().toggleCondition(slot), () -> getContainer().getCondition(slot)) {
				@Override
				protected List<Component> getTooltip(StateData data) {
					if (getContainer().hasNoFilter(slot)) {
						return Collections.emptyList();
					}

					float value = upgradeContainer.getValue(slot);
					if (value < 0) {
						return super.getTooltip(data);
					}

					List<Component> tooltip = new ArrayList<>(super.getTooltip(data));
					Component component = tooltip.getFirst();
					if (getState.get() == AlchemyCondition.HURT) {
						tooltip.set(0, Component.translatable(component.getString(), Component.literal(Math.round(value * 100) + "%").withStyle(ChatFormatting.DARK_GREEN)));
						tooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("alchemy_condition.value_controls")).withStyle(ChatFormatting.DARK_GRAY));
					}
					return tooltip;
				}

				@Override
				public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
					if (isOpen && getState.get() == AlchemyCondition.HURT && isMouseOver(mouseX, mouseY)) {
						if (scrollY > 0) {
							getContainer().setValue(slot, Math.min(getContainer().getValue(slot) + 0.05f, 1));
						} else if (scrollY < 0) {
							getContainer().setValue(slot, Math.max(getContainer().getValue(slot) - 0.05f, 0));
						}
						return true;
					}
					return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
				}

				@Override
				protected void extractWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
					if (getContainer().hasNoFilter(slot)) {
						GuiHelper.blit(guiGraphics, x, y, DISABLED_CONDITION);
					} else {
						super.extractWidget(guiGraphics, mouseX, mouseY, partialTicks);
					}
				}
			});
		}
	}

	@Override
	protected void moveSlotsToTab() {
		int slotIndex = 0;
		for (Slot slot : getContainer().getSlots()) {
			slot.x = x - screen.getGuiLeft() + 4 + (slotIndex % 2) * (18 + 18);
			slot.y = y - screen.getGuiTop() + TOP_POS + buttonYOffset + 1 + (slotIndex / 2) * 20;
			slotIndex++;
		}
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		super.extractBg(guiGraphics, minecraft, mouseX, mouseY);
		if (getContainer().isOpen()) {
			for (Slot slot : getContainer().getSlots()) {
				GuiHelper.renderSlotsBackground(guiGraphics, screen.getGuiLeft() + slot.x - 1, screen.getGuiTop() + slot.y - 1, 1, 1);
			}
		}
	}

	public static class Basic extends AlchemyUpgradeTab {
		public Basic(AlchemyUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("alchemy"), TranslationHelper.INSTANCE.translUpgradeTooltip("alchemy"), false);
		}
	}

	public static class Advanced extends AlchemyUpgradeTab {
		public Advanced(AlchemyUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("advanced_alchemy"), TranslationHelper.INSTANCE.translUpgradeTooltip("advanced_alchemy"), true);
			addHideableChild(new ToggleButton<>(new Position(x + 3, y + TOP_POS), ANY_EFFECT_MISSING, button -> getContainer().toggleMatchAll(), () -> getContainer().shouldMatchAll()));
			addHideableChild(new ToggleButton<>(new Position(x + 3 + 18, y + TOP_POS), MATCH_DURATION, button -> getContainer().toggleMatchDuration(), () -> getContainer().shouldMatchDuration()));
			addHideableChild(new ToggleButton<>(new Position(x + 3 + 18 * 2, y + TOP_POS), MATCH_AMPLIFIER, button -> getContainer().toggleMatchAmplifier(), () -> getContainer().shouldMatchAmplifier()));

			if (getContainer().hasEntityMatchOption()) {
				addHideableChild(new ToggleButton<>(new Position(x + 3 + 18 * 3, y + TOP_POS), ENTITY_MATCH, button -> getContainer().toggleEntityMatch(), () -> getContainer().getEntityMatch()));
			}
		}
	}
}
