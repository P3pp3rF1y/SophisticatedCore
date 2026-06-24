package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;

import java.util.Map;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.*;

public abstract class JukeboxUpgradeTab extends UpgradeSettingsTab<JukeboxUpgradeContainer> {
	private static final TextureBlitData PLAY_FOREGROUND = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(16, 64),
			Dimension.SQUARE_16);
	private static final ButtonDefinition PLAY = new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND,
			PLAY_FOREGROUND, Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("play")));
	private static final TextureBlitData STOP_FOREGROUND = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(0, 64),
			Dimension.SQUARE_16);
	private static final ButtonDefinition STOP = new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND,
			STOP_FOREGROUND, Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("stop")));

	private static final TextureBlitData SHUFFLE_ON_FOREGROUND = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(96, 80),
			Dimension.SQUARE_16);
	private static final TextureBlitData SHUFFLE_OFF_FOREGROUND = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(112, 80),
			Dimension.SQUARE_16);
	private static final ButtonDefinition.Toggle<Boolean> SHUFFLE = new ButtonDefinition.Toggle<>(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, Map.of(true,
			new ToggleButton.StateData(SHUFFLE_ON_FOREGROUND, Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("shuffle_on"))), false,
			new ToggleButton.StateData(SHUFFLE_OFF_FOREGROUND, Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("shuffle_off")))),
			DEFAULT_BUTTON_HOVERED_BACKGROUND);

	private static final TextureBlitData REPEAT_ALL_FOREGROUND = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(128, 80),
			Dimension.SQUARE_16);
	private static final TextureBlitData REPEAT_ONE_FOREGROUND = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(144, 80),
			Dimension.SQUARE_16);
	private static final TextureBlitData NO_REPEAT_FOREGROUND = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(160, 80),
			Dimension.SQUARE_16);
	private static final ButtonDefinition.Toggle<RepeatMode> REPEAT = new ButtonDefinition.Toggle<>(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND,
			Map.of(RepeatMode.ALL,
					new ToggleButton.StateData(REPEAT_ALL_FOREGROUND, Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("repeat_all"))),
					RepeatMode.ONE,
					new ToggleButton.StateData(REPEAT_ONE_FOREGROUND, Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("repeat_one"))),
					RepeatMode.NO,
					new ToggleButton.StateData(NO_REPEAT_FOREGROUND, Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("no_repeat")))),
			DEFAULT_BUTTON_HOVERED_BACKGROUND);

	private static final TextureBlitData PREVIOUS_FOREGROUND = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(48, 96),
			Dimension.SQUARE_16);
	private static final ButtonDefinition PREVIOUS = new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND,
			PREVIOUS_FOREGROUND, Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("previous_disc")));

	private static final TextureBlitData NEXT_FOREGROUND = new TextureBlitData(ICONS, new Position(1, 1), Dimension.SQUARE_256, new UV(32, 96),
			Dimension.SQUARE_16);
	private static final ButtonDefinition NEXT = new ButtonDefinition(Dimension.SQUARE_18, DEFAULT_BUTTON_BACKGROUND, DEFAULT_BUTTON_HOVERED_BACKGROUND,
			NEXT_FOREGROUND, Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("next_disc")));
	private static final int BUTTON_PADDING = 3;
	public static final int TOP_Y = 24;

	private final int slotsInRow;

	public JukeboxUpgradeTab(JukeboxUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsInRow, Component tabLabel,
			Component closedTooltip) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);
		this.slotsInRow = slotsInRow;
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		super.extractBg(guiGraphics, minecraft, mouseX, mouseY);
		if (getContainer().isOpen()) {
			renderSlotsBackground(guiGraphics, x + 3, y + 24, slotsInRow, getContainer().getSlots().size() / slotsInRow,
					getContainer().getSlots().size() % slotsInRow);
		}
	}

	@Override
	protected void moveSlotsToTab() {
		int slotIndex = 0;
		for (Slot discSlot : getContainer().getSlots()) {
			discSlot.x = x - screen.getGuiLeft() + 4 + (slotIndex % slotsInRow) * 18;
			discSlot.y = y - screen.getGuiTop() + TOP_Y + 1 + (slotIndex / slotsInRow) * 18;
			slotIndex++;
		}
	}

	protected int getBottomSlotY() {
		return TOP_Y + (getContainer().getSlots().size() / slotsInRow) * 18 + (getContainer().getSlots().size() % slotsInRow > 0 ? 18 : 0);
	}

	public static class Basic extends JukeboxUpgradeTab {
		public Basic(JukeboxUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
			super(upgradeContainer, position, screen, 4, TranslationHelper.INSTANCE.translUpgrade("jukebox"),
					TranslationHelper.INSTANCE.translUpgradeTooltip("jukebox"));
			int bottomSlotY = getBottomSlotY();
			addHideableChild(new Button(new Position(x + 3, y + bottomSlotY + BUTTON_PADDING), STOP, button -> {
				if (button == 0) {
					getContainer().stop();
				}
			}));
			addHideableChild(new Button(new Position(x + 21, y + bottomSlotY + BUTTON_PADDING), PLAY, button -> {
				if (button == 0) {
					getContainer().play();
				}
			}));
		}
	}

	public static class Advanced extends JukeboxUpgradeTab {
		public Advanced(JukeboxUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsInRow) {
			super(upgradeContainer, position, screen, slotsInRow, TranslationHelper.INSTANCE.translUpgrade("advanced_jukebox"),
					TranslationHelper.INSTANCE.translUpgradeTooltip("advanced_jukebox"));
			int bottomSlotY = getBottomSlotY();
			addHideableChild(new Button(new Position(x + 3, y + bottomSlotY + BUTTON_PADDING), PREVIOUS, button -> {
				if (button == 0) {
					getContainer().previous();
				}
			}));
			addHideableChild(new Button(new Position(x + 21, y + bottomSlotY + BUTTON_PADDING), STOP, button -> {
				if (button == 0) {
					getContainer().stop();
				}
			}));
			addHideableChild(new Button(new Position(x + 39, y + bottomSlotY + BUTTON_PADDING), PLAY, button -> {
				if (button == 0) {
					getContainer().play();
				}
			}));
			addHideableChild(new Button(new Position(x + 57, y + bottomSlotY + BUTTON_PADDING), NEXT, button -> {
				if (button == 0) {
					getContainer().next();
				}
			}));
			addHideableChild(new ToggleButton<>(new Position(x + 12, y + bottomSlotY + BUTTON_PADDING + 20), SHUFFLE, button -> {
				if (button == 0) {
					getContainer().toggleShuffle();
				}
			}, () -> getContainer().isShuffleEnabled()));

			addHideableChild(new ToggleButton<>(new Position(x + 48, y + bottomSlotY + BUTTON_PADDING + 20), REPEAT, button -> {
				if (button == 0) {
					getContainer().toggleRepeat();
				}
			}, () -> getContainer().getRepeatMode()));
		}

		@Override
		protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			super.extractBg(guiGraphics, minecraft, mouseX, mouseY);
			getContainer().getDiscSlotActive()
					.ifPresent(slot -> renderPlaytimeOverLay(guiGraphics, 0x55_00CC00, screen.getLeftX() + slot.x, screen.getTopY() + slot.y, 16, 16));
		}

		private float getPlaybackRemainingProgress() {
			long finishTime = getContainer().getDiscFinishTime();
			int remaining = (int) (finishTime - minecraft.level.getGameTime());

			ItemStack disc = getContainer().getUpgradeWrapper().getDisc();
			return DiscHandlerRegistry.getMusicLengthInTicks(disc, minecraft.level).map(lengthInTicks -> remaining / (float) lengthInTicks).orElse(0f);
		}

		private void renderPlaytimeOverLay(GuiGraphicsExtractor guiGraphics, int slotColor, int xPos, int yPos, int width, int height) {
			float remainingProgress = getPlaybackRemainingProgress();
			if (remainingProgress <= 0) {
				return;
			}
			int progressOver = width - (int) (width * remainingProgress);

			guiGraphics.fillGradient(xPos + progressOver, yPos, xPos + width, yPos + height, slotColor, slotColor);
		}
	}
}
