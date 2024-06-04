package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.GUI_CONTROLS;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.ICONS;

public class AdvancedCraftingUpgradeTab extends UpgradeSettingsTab<AdvancedCraftingUpgradeContainer> {
	public static final int RESULT_SELECTION_BORDER_WIDTH = 3;
	private static final TextureBlitData ARROW = new TextureBlitData(GUI_CONTROLS, new UV(97, 216), new Dimension(15, 8));
	private static final Dimension DIMENSION_8_12 = new Dimension(8, 12);
	private static final Dimension DIMENSION_16_12 = new Dimension(16, 12);
	private static final TextureBlitData SMALL_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(53, 18), DIMENSION_8_12);
	private static final TextureBlitData SMALL_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(61, 18), DIMENSION_8_12);
	private static final TextureBlitData PREVIOS_RESULT_FOREGROUND = new TextureBlitData(ICONS, new Position(0, 0), Dimension.SQUARE_256, new UV(48, 144), DIMENSION_8_12);
	private static final ButtonDefinition PREVIOUS_RESULT = new ButtonDefinition(DIMENSION_8_12, SMALL_BUTTON_BACKGROUND, SMALL_BUTTON_HOVERED_BACKGROUND, PREVIOS_RESULT_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("previous_result")));
	private static final TextureBlitData NEXT_RESULT_FOREGROUND = new TextureBlitData(ICONS, new Position(0, 0), Dimension.SQUARE_256, new UV(56, 144), DIMENSION_8_12);
	private static final ButtonDefinition NEXT_RESULT = new ButtonDefinition(DIMENSION_8_12, SMALL_BUTTON_BACKGROUND, SMALL_BUTTON_HOVERED_BACKGROUND, NEXT_RESULT_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("next_result")));
	private static final TextureBlitData BIG_BUTTON_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(69, 18), DIMENSION_16_12);
	private static final TextureBlitData BIG_BUTTON_HOVERED_BACKGROUND = new TextureBlitData(GUI_CONTROLS, new UV(63, 30), DIMENSION_16_12);
	private static final ButtonDefinition SELECT_RESULT = new ButtonDefinition(DIMENSION_16_12, BIG_BUTTON_BACKGROUND, BIG_BUTTON_HOVERED_BACKGROUND, null,
			Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("select_result")));

	private static final ClientTooltipPositioner LEFT_SIDE_TOOLTIP_POSITIONER = new ClientTooltipPositioner() {
		@Override
		public Vector2ic positionTooltip(int guiWidth, int guiHeight, int mouseX, int mouseY, int tooltipWidth, int tooltipHeight) {
			Vector2i tooltipTopLeft = (new Vector2i(mouseX, mouseY)).add(12, -12);
			this.positionTooltip(guiHeight, tooltipTopLeft, tooltipWidth, tooltipHeight);
			return tooltipTopLeft;
		}

		private void positionTooltip(int guiHeight, Vector2i tooltipTopLeft, int tooltipWidth, int tooltipHeight) {
			tooltipTopLeft.x = Math.max(tooltipTopLeft.x - 24 - tooltipWidth, 4);

			int i = tooltipHeight + 3;
			if (tooltipTopLeft.y + i > guiHeight) {
				tooltipTopLeft.y = guiHeight - i;
			}
		}
	};

	private final ICraftingUIPart craftingUIAddition;
	private final Button previousResultButton;
	private final Button nextResultButton;
	private final Button selectResultButton;
	private boolean resultSelectionShown = false;
	private Tuple<Position, Dimension> resultListPositionDimensions;
	private final List<Position> resultChoicePositions = new ArrayList<>();

	public AdvancedCraftingUpgradeTab(AdvancedCraftingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ButtonDefinition.Toggle<Boolean> shiftClickTargetButton, ButtonDefinition.Toggle<CraftingRefillType> refillCraftingGridButton) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("crafting"), TranslationHelper.INSTANCE.translUpgradeTooltip("crafting"));
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), shiftClickTargetButton, button -> getContainer().setShiftClickIntoStorage(!getContainer().shouldShiftClickIntoStorage()),
				getContainer()::shouldShiftClickIntoStorage));
		addHideableChild(new ToggleButton<>(new Position(x + 21, y + 24), refillCraftingGridButton, button -> getContainer().setRefillCraftingGrid(getContainer().shouldRefillCraftingGrid().next()),
				getContainer()::shouldRefillCraftingGrid));
		craftingUIAddition = screen.getCraftingUIAddition();
		openTabDimension = new Dimension(63 + craftingUIAddition.getWidth(), 142);
		previousResultButton = new Button(new Position(x + 3 + 6 + craftingUIAddition.getWidth(), y + 118), PREVIOUS_RESULT, button -> {
			if (button == 0) {
				getContainer().selectPreviousCraftingResult();
			}
		}) {
			@Override
			public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
				if (visible && isMouseOver(mouseX, mouseY)) {
					guiGraphics.renderTooltip(screen.font, getTooltip().stream().map(Component::getVisualOrderText).toList(), LEFT_SIDE_TOOLTIP_POSITIONER, mouseX, mouseY);
				}
			}
		};
		addHideableChild(previousResultButton);
		nextResultButton = new Button(new Position(x + 3 + 6 + craftingUIAddition.getWidth() + 8 + 26, y + 118), NEXT_RESULT, button -> {
			if (button == 0) {
				getContainer().selectNextCraftingResult();
			}
		});
		addHideableChild(nextResultButton);
		selectResultButton = new Button(new Position(x + 3 + 6 + craftingUIAddition.getWidth() + 13, y + 99), SELECT_RESULT, button -> {
			if (button == 0) {
				resultSelectionShown = !resultSelectionShown;
			}
		}) {

			@Override
			protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
				guiGraphics.drawString(font, String.valueOf(getContainer().getMatchedCraftingResults().size()), x + 5, y + 2, 0xFFFFFF, true);
			}
		};
		addHideableChild(selectResultButton);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		super.renderBg(guiGraphics, minecraft, mouseX, mouseY);
		if (getContainer().isOpen()) {
			GuiHelper.renderSlotsBackground(guiGraphics, x + 3 + craftingUIAddition.getWidth(), y + 44, 3, 3);
			GuiHelper.blit(guiGraphics, x + 3 + craftingUIAddition.getWidth() + 19, y + 101, ARROW);
			GuiHelper.blit(guiGraphics, x + 3 + craftingUIAddition.getWidth() + 14, y + 111, GuiHelper.CRAFTING_RESULT_SLOT);
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		List<ItemStack> matchedCraftingResults = getContainer().getMatchedCraftingResults();
		previousResultButton.setVisible(shouldShowResultSelection());
		nextResultButton.setVisible(shouldShowResultSelection());
		selectResultButton.setVisible(shouldShowResultSelection());

		if (!shouldShowResultSelection()) {
			resultSelectionShown = false;
			resultListPositionDimensions = null;
			resultChoicePositions.clear();
		} else if (resultSelectionShown) {
			if (resultListPositionDimensions == null || resultChoicePositions.size() != matchedCraftingResults.size()) {
				initResultSelectionPositionDimension(matchedCraftingResults);
				resultChoicePositions.clear();
				for (int i = 0; i < matchedCraftingResults.size(); i++) {
					int xOffset = (i % 3) * 18;
					int yOffset = (i / 3) * 18;
					resultChoicePositions.add(new Position(resultListPositionDimensions.getA().x() + RESULT_SELECTION_BORDER_WIDTH + xOffset, resultListPositionDimensions.getA().y() + RESULT_SELECTION_BORDER_WIDTH + yOffset));
				}
			}

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0, 0, 410);

			renderResultSelectionBackground(guiGraphics, matchedCraftingResults, resultListPositionDimensions.getB().width(), resultListPositionDimensions.getB().height(), resultListPositionDimensions.getA().x(), resultListPositionDimensions.getA().y());
			renderResultChoices(guiGraphics, matchedCraftingResults, resultListPositionDimensions.getA().x(), resultListPositionDimensions.getA().y());
			renderSelectionSlotHover(guiGraphics, mouseX, mouseY);

			guiGraphics.pose().popPose();
		}
	}

	private void renderSelectionSlotHover(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		getResultChoiceHovered(mouseX, mouseY).ifPresent(i -> {
			Position position = resultChoicePositions.get(i);
			AbstractContainerScreen.renderSlotHighlight(guiGraphics, position.x() + 1, position.y() + 1, 0, -2130706433);
		});
	}

	private Optional<Integer> getResultChoiceHovered(int mouseX, int mouseY) {
		if (!resultSelectionShown || resultListPositionDimensions == null) {
			return Optional.empty();
		}
		Position pos = resultListPositionDimensions.getA();
		Dimension dim = resultListPositionDimensions.getB();
		int slotsLeftX = pos.x() + RESULT_SELECTION_BORDER_WIDTH;
		int slotsTopY = pos.y() + RESULT_SELECTION_BORDER_WIDTH;

		if (mouseX >= slotsLeftX && mouseX < pos.x() + dim.width() - RESULT_SELECTION_BORDER_WIDTH && mouseY >= slotsTopY && mouseY < pos.y() + dim.height() - RESULT_SELECTION_BORDER_WIDTH) {
			for (int i = 0; i < resultChoicePositions.size(); i++) {
				Position position = resultChoicePositions.get(i);
				if (mouseX >= position.x() && mouseX < position.x() + 18 && mouseY >= position.y() && mouseY < position.y() + 18) {
					return Optional.of(i);
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean ret = super.mouseClicked(mouseX, mouseY, button);
		if (ret) {
			return true;
		}

		return getResultChoiceHovered((int) mouseX, (int) mouseY).map(i -> {
			getContainer().selectCraftingResult(i);
			resultSelectionShown = false;
			return true;
		}).orElse(false);
	}

	private void initResultSelectionPositionDimension(List<ItemStack> matchedCraftingResults) {
		int height = RESULT_SELECTION_BORDER_WIDTH + ((matchedCraftingResults.size() - 1) / 3 + 1) * 18 + RESULT_SELECTION_BORDER_WIDTH;
		int width = RESULT_SELECTION_BORDER_WIDTH + Math.min(matchedCraftingResults.size(), 3) * 18 + RESULT_SELECTION_BORDER_WIDTH;

		int resultListLeftX = selectResultButton.getX() + 8 - RESULT_SELECTION_BORDER_WIDTH -(int) (Math.min(matchedCraftingResults.size(), 3) / 2f * 18);
		int resultListTopY = selectResultButton.getY() - height;
		resultListPositionDimensions = new Tuple<>(new Position(resultListLeftX, resultListTopY), new Dimension(width, height));

	}

	private static void renderResultSelectionBackground(GuiGraphics guiGraphics, List<ItemStack> matchedCraftingResults, int width, int height, int resultListLeftX, int resultListTopY) {
		int halfWidth = width / 2;
		int halfHeight = height / 2;

		guiGraphics.blit(GUI_CONTROLS, resultListLeftX, resultListTopY, 85, 24, halfWidth, halfHeight, 256, 256);
		guiGraphics.blit(GUI_CONTROLS, resultListLeftX + halfWidth, resultListTopY, (float) 117 - halfWidth, 24, halfWidth, halfHeight, 256, 256);
		guiGraphics.blit(GUI_CONTROLS, resultListLeftX, resultListTopY + halfHeight, 85, (float) 56 - halfHeight, halfWidth, halfHeight, 256, 256);
		guiGraphics.blit(GUI_CONTROLS, resultListLeftX + halfWidth, resultListTopY + halfHeight, (float) 117 - halfWidth, (float) 56 - halfHeight, halfWidth, halfHeight, 256, 256);

		GuiHelper.renderSlotsBackground(guiGraphics, resultListLeftX + RESULT_SELECTION_BORDER_WIDTH, resultListTopY + RESULT_SELECTION_BORDER_WIDTH, 3, matchedCraftingResults.size() / 3, matchedCraftingResults.size() % 3);
	}

	private void renderResultChoices(GuiGraphics guiGraphics, List<ItemStack> matchedCraftingResults, int resultListLeftX, int resultListTopY) {
		for (int i = 0; i < matchedCraftingResults.size(); i++) {
			ItemStack resultStack = matchedCraftingResults.get(i);

			int xOffset = (i % 3) * 18;
			int yOffset = (i / 3) * 18;
			int x = resultListLeftX + RESULT_SELECTION_BORDER_WIDTH + 1 + xOffset;
			int y = resultListTopY + RESULT_SELECTION_BORDER_WIDTH + 1 + yOffset;
			guiGraphics.renderItem(resultStack, x, y);
			guiGraphics.renderItemDecorations(font, resultStack, x, y, null);
		}
	}

	@Override
	protected void onTabClose() {
		super.onTabClose();
		craftingUIAddition.onCraftingSlotsHidden();
	}

	@Override
	protected void moveSlotsToTab() {
		int slotNumber = 0;
		for (Slot slot : getContainer().getSlots()) {
			slot.x = x + 3 + craftingUIAddition.getWidth() - screen.getGuiLeft() + 1 + (slotNumber % 3) * 18;
			slot.y = y + 44 - screen.getGuiTop() + 1 + (slotNumber / 3) * 18;
			slotNumber++;
			if (slotNumber >= 9) {
				break;
			}
		}

		Slot craftingSlot = getContainer().getSlots().get(9);
		craftingSlot.x = x + 3 + craftingUIAddition.getWidth() - screen.getGuiLeft() + 19;
		craftingSlot.y = y + 44 - screen.getGuiTop() + 72;

		craftingUIAddition.onCraftingSlotsDisplayed(getContainer().getSlots());
	}

	private boolean shouldShowResultSelection() {
		return getContainer().getMatchedCraftingResults().size() > 1;
	}

	@Override
	public boolean slotIsNotCoveredAt(Slot slot, double mouseX, double mouseY) {
		if (!isOpen || !shouldShowResultSelection() || !resultSelectionShown) {
			return true;
		}

		Position pos = resultListPositionDimensions.getA();
		Dimension dim = resultListPositionDimensions.getB();

		return mouseX < pos.x() || mouseX > pos.x() + dim.width() || mouseY < pos.y() || mouseY > pos.y() + dim.height();
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, 0, 410);
		getResultChoiceHovered(mouseX, mouseY).ifPresent(i -> {
			List<ItemStack> matchedCraftingResults = getContainer().getMatchedCraftingResults();
			if (i < matchedCraftingResults.size()) {
				ItemStack stack = matchedCraftingResults.get(i);
				guiGraphics.renderTooltip(screen.font, stack, mouseX, mouseY);
			}
		});
		guiGraphics.pose().popPose();
	}
}
