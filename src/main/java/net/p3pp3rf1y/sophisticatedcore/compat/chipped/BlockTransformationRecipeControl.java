package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.LargeResultSelector;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;

import java.util.ArrayList;
import java.util.List;
public class BlockTransformationRecipeControl extends WidgetBase {
	private static final TextureBlitData SLIDER = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 131), Dimension.RECTANGLE_12_15);
	private static final TextureBlitData DISABLED_SLIDER = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(41, 131), Dimension.RECTANGLE_12_15);
	private static final TextureBlitData RECIPE_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(110, 148), Dimension.RECTANGLE_16_18);
	private static final TextureBlitData SELECTED_RECIPE_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(110, 166), Dimension.RECTANGLE_16_18);
	private static final TextureBlitData RECIPE_BACKGROUND_HOVERED = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(110, 184), Dimension.RECTANGLE_16_18);
	private static final TextureBlitData RECENT_RECIPE_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(63, 60), Dimension.RECTANGLE_16_18);
	private static final TextureBlitData LIST_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 146), new Dimension(81, 56));
	private static final TextureBlitData RESULT_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 146), new Dimension(66, 56));
	private static final TextureBlitData SCROLLBAR_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(96, 146), new Dimension(14, 56));
	private static final TextureBlitData BROWSE_BUTTON_FOREGROUND = new TextureBlitData(GuiHelper.ICONS, new Position(2, 2), Dimension.SQUARE_256, new UV(49, 157), Dimension.SQUARE_12);
	private static final ButtonDefinition BROWSE_RESULTS = new ButtonDefinition(new Dimension(14, 14), GuiHelper.DEFAULT_BUTTON_BACKGROUND, GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND, BROWSE_BUTTON_FOREGROUND,
			Component.translatable(TranslationHelper.INSTANCE.translButton("browse_results")));

	private static final int LIST_Y_OFFSET = 22;
	private static final int INPUT_SLOT_HEIGHT = 18;
	private static final int SPACING = 4;
	private static final int SCROLLBAR_X_OFFSET = 67;
	private static final int BROWSE_BUTTON_SIZE = 14;

	private boolean clickedOnScroll;
	private final StorageScreenBase<?> screen;
	private final BlockTransformationRecipeContainer container;
	private final Button browseButton;
	private boolean hasItemsInInputSlot;
	private int recipeIndexOffset;
	private float sliderProgress;

	protected BlockTransformationRecipeControl(StorageScreenBase<?> screen, BlockTransformationRecipeContainer container, Position position) {
		super(position, new Dimension(96, 108));
		this.screen = screen;
		this.container = container;
		browseButton = new Button(getBrowseButtonPosition(), BROWSE_RESULTS, button -> {
			if (button == 0 && shouldShowBrowseButton()) {
				openLargeResultSelector();
			}
		}) {
			@Override
			protected void renderHoveredBackground(GuiGraphics guiGraphics) {
				GuiHelper.blit(guiGraphics, x, y, GuiHelper.DEFAULT_BUTTON_HOVERED_BACKGROUND, getWidth(), getHeight());
			}

			@Override
			protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
				if (isMouseOver(mouseX, mouseY)) {
					renderHoveredBackground(guiGraphics);
				} else {
					GuiHelper.blit(guiGraphics, x, y, GuiHelper.DEFAULT_BUTTON_BACKGROUND, getWidth(), getHeight());
				}
			}
		};
		container.setInventoryUpdateListener(this::onInventoryUpdate);
		onInventoryUpdate();
	}

	public void moveSlotsToView() {
		Slot inputSlot = container.getInputSlot();
		inputSlot.x = x + getListCenteredX(16) - screen.getGuiLeft();
		inputSlot.y = y - screen.getGuiTop() + 1;
		Slot outputSlot = container.getOutputSlot();
		outputSlot.x = x + getListCenteredX(16) - screen.getGuiLeft();
		outputSlot.y = inputSlot.y + INPUT_SLOT_HEIGHT + SPACING + LIST_BACKGROUND.getHeight() + SPACING + 4;
	}

	private int getListCenteredX(int elementWidth) {
		return (LIST_BACKGROUND.getWidth() - elementWidth) / 2;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.renderSlotsBackground(guiGraphics, x + getListCenteredX(18), y, 1, 1);
		GuiHelper.renderTiledControlBackground(guiGraphics, x, y + LIST_Y_OFFSET, RESULT_BACKGROUND.getWidth(), RESULT_BACKGROUND.getHeight(), RESULT_BACKGROUND.getU(), RESULT_BACKGROUND.getV(), RESULT_BACKGROUND.getWidth(), RESULT_BACKGROUND.getHeight());
		GuiHelper.renderTiledControlBackground(guiGraphics, getScrollbarBackgroundX(), getScrollbarBackgroundY(), SCROLLBAR_BACKGROUND.getWidth(), getScrollbarBackgroundHeight(), SCROLLBAR_BACKGROUND.getU(), SCROLLBAR_BACKGROUND.getV(), SCROLLBAR_BACKGROUND.getWidth(), SCROLLBAR_BACKGROUND.getHeight());
		GuiHelper.blit(guiGraphics, x + getListCenteredX(26), y + INPUT_SLOT_HEIGHT + SPACING + LIST_BACKGROUND.getHeight() + SPACING, GuiHelper.CRAFTING_RESULT_SLOT);
		browseButton.render(guiGraphics, mouseX, mouseY, 0);
		int sliderYOffset = (int) (getSliderTravel() * sliderProgress);
		GuiHelper.blit(guiGraphics, getSliderX(), getScrollbarY() + sliderYOffset, canScroll() ? SLIDER : DISABLED_SLIDER);

		int listInnerLeftX = x + 1;
		int listTopY = getListTopY();
		int recipeIndexOffsetMax = recipeIndexOffset + 12;
		renderRecipeBackgrounds(guiGraphics, mouseX, mouseY, listInnerLeftX, listTopY, recipeIndexOffsetMax);
		drawRecipesItems(guiGraphics, listInnerLeftX, listTopY, recipeIndexOffsetMax);
	}

	private boolean shouldShowBrowseButton() {
		return hasItemsInInputSlot && container.getResults().size() >= LargeResultSelector.RESULT_COUNT_THRESHOLD;
	}

	private Position getBrowseButtonPosition() {
		return new Position(getBrowseButtonX(), getBrowseButtonY());
	}

	private int getBrowseButtonX() {
		return x + SCROLLBAR_X_OFFSET;
	}

	private int getBrowseButtonY() {
		return y + LIST_Y_OFFSET;
	}

	private int getScrollbarBackgroundX() {
		return x + SCROLLBAR_X_OFFSET;
	}

	private int getScrollbarBackgroundY() {
		return shouldShowBrowseButton() ? getBrowseButtonY() + BROWSE_BUTTON_SIZE + 1 : y + LIST_Y_OFFSET;
	}

	private int getScrollbarBackgroundHeight() {
		return y + LIST_Y_OFFSET + LIST_BACKGROUND.getHeight() - getScrollbarBackgroundY();
	}

	private int getScrollbarX() {
		return x + SCROLLBAR_X_OFFSET;
	}

	private int getSliderX() {
		return getScrollbarX() + 1;
	}

	private int getScrollbarY() {
		return getScrollbarBackgroundY() + 1;
	}

	private int getScrollbarHeight() {
		return getScrollbarBackgroundHeight() - 2;
	}

	private int getSliderTravel() {
		return getScrollbarHeight() - SLIDER.getHeight();
	}

	private void drawRecipesItems(GuiGraphics guiGraphics, int listInnerLeftX, int top, int recipeIndexOffsetMax) {
		List<ItemStack> results = container.getResults();
		List<Integer> sortedRecipeIndexes = getSortedRecipeIndexes();

		for (int displayIndex = recipeIndexOffset; displayIndex < recipeIndexOffsetMax && displayIndex < sortedRecipeIndexes.size(); ++displayIndex) {
			int j = displayIndex - recipeIndexOffset;
			int k = listInnerLeftX + j % 4 * 16;
			int l = j / 4;
			int i1 = top + l * 18 + 2;
			GuiHelper.renderItemInGUI(guiGraphics, minecraft, results.get(sortedRecipeIndexes.get(displayIndex)), k, i1);
		}

	}

	private int getListTopY() {
		return y + LIST_Y_OFFSET;
	}

	private void renderRecipeBackgrounds(GuiGraphics guiGraphics, int mouseX, int mouseY, int listInnerLeftX, int listTopY, int recipeIndexOffsetMax) {
		List<Integer> sortedRecipeIndexes = getSortedRecipeIndexes();
		for (int displayIndex = recipeIndexOffset; displayIndex < recipeIndexOffsetMax && displayIndex < sortedRecipeIndexes.size(); ++displayIndex) {
			int recipeIndex = sortedRecipeIndexes.get(displayIndex);
			int j = displayIndex - recipeIndexOffset;
			int recipeX = listInnerLeftX + j % 4 * 16;
			int row = j / 4;
			int recipeY = listTopY + row * 18 + 2;
			TextureBlitData background = RECIPE_BACKGROUND;

			if (recipeIndex == container.getSelectedRecipe()) {
				background = SELECTED_RECIPE_BACKGROUND;
			} else if (mouseX >= recipeX && mouseY >= recipeY && mouseX < recipeX + 16 && mouseY < recipeY + 18) {
				background = RECIPE_BACKGROUND_HOVERED;
			} else if (container.isRecentResult(recipeIndex)) {
				background = RECENT_RECIPE_BACKGROUND;
			}

			GuiHelper.blit(guiGraphics, recipeX, recipeY - 1, background);
		}
	}

	private boolean canScroll() {
		return hasItemsInInputSlot && container.getResults().size() > 12;
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		//noop - everything is rendered in background or after screen render is done
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		renderHoveredTooltip(guiGraphics, mouseX, mouseY);
	}

	private void renderHoveredTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if (shouldShowBrowseButton() && browseButton.isMouseOver(mouseX, mouseY)) {
			browseButton.renderTooltip(screen, guiGraphics, mouseX, mouseY);
			return;
		}
		if (hasItemsInInputSlot) {
			int listTopY = getListTopY();
			int k = recipeIndexOffset + 12;
			List<ItemStack> results = container.getResults();
			List<Integer> sortedRecipeIndexes = getSortedRecipeIndexes();

			for (int displayIndex = recipeIndexOffset; displayIndex < k && displayIndex < sortedRecipeIndexes.size(); ++displayIndex) {
				int inviewRecipeIndex = displayIndex - recipeIndexOffset;
				int recipeLeftX = x + inviewRecipeIndex % 4 * 16;
				int k1 = listTopY + inviewRecipeIndex / 4 * 18 + 2;
				if (mouseX >= recipeLeftX && mouseX < recipeLeftX + 16 && mouseY >= k1 && mouseY < k1 + 18) {
					renderTooltip(guiGraphics, results.get(sortedRecipeIndexes.get(displayIndex)), mouseX, mouseY);
				}
			}
		}
	}

	private void renderTooltip(GuiGraphics guiGraphics, ItemStack itemStack, int mouseX, int mouseY) {
		Font font = IClientItemExtensions.of(itemStack).getFont(itemStack, IClientItemExtensions.FontContext.TOOLTIP);
		guiGraphics.renderComponentTooltip((font == null ? this.font : font), screen.getTooltipFromItem(minecraft, itemStack), mouseX, mouseY);
	}

	private void onInventoryUpdate() {
		hasItemsInInputSlot = container.hasItemsInInputSlot();
		browseButton.setPosition(getBrowseButtonPosition());
		browseButton.setVisible(shouldShowBrowseButton());
		if (!hasItemsInInputSlot) {
			sliderProgress = 0.0F;
			recipeIndexOffset = 0;
		} else {
			scrollSelectedRecipeIntoView();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		clickedOnScroll = false;
		if (hasItemsInInputSlot) {
			if (shouldShowBrowseButton() && browseButton.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}

			int listInnerLeftX = x + 1;
			int listInnerTopY = y + LIST_Y_OFFSET + 1;
			int maxRecipeIndexOffset = recipeIndexOffset + 12;
			List<Integer> sortedRecipeIndexes = getSortedRecipeIndexes();

			for (int displayIndex = recipeIndexOffset; displayIndex < maxRecipeIndexOffset && displayIndex < sortedRecipeIndexes.size(); ++displayIndex) {
				int visibleRecipeIndex = displayIndex - recipeIndexOffset;
				double relativeX = mouseX - (listInnerLeftX + visibleRecipeIndex % 4 * 16);
				double relativeY = mouseY - (listInnerTopY + Math.floorDiv(visibleRecipeIndex, 4) * 18);
				int recipeIndex = sortedRecipeIndexes.get(displayIndex);
				if (relativeX >= 0.0D && relativeY >= 0.0D && relativeX < 16.0D && relativeY < 18.0D && container.selectRecipeIndex(recipeIndex)) {
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
					return true;
				}
			}

			int sliderLeftX = listInnerLeftX + 67;
			if (mouseX >= sliderLeftX && mouseX < sliderLeftX + 12 && mouseY >= getScrollbarY() && mouseY < getScrollbarY() + getScrollbarHeight()) {
				clickedOnScroll = true;
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void openLargeResultSelector() {
		List<LargeResultSelector.ResultEntry> results = new ArrayList<>();
		List<ItemStack> containerResults = container.getResults();
		for (int i : getSortedRecipeIndexes()) {
			results.add(new LargeResultSelector.ResultEntry(i, containerResults.get(i), container.isRecentResult(i)));
		}
		screen.setModalOverlay(new LargeResultSelector(screen, results, container.getSelectedRecipe(), this::selectRecipeFromLargeResultSelector, SoundEvents.UI_STONECUTTER_SELECT_RECIPE));
	}

	private void selectRecipeFromLargeResultSelector(int recipeIndex) {
		container.selectRecipeIndex(recipeIndex);
		scrollSelectedRecipeIntoView();
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (clickedOnScroll && canScroll()) {
			sliderProgress = ((float) mouseY - getScrollbarY() - SLIDER.getHeight() / 2f) / getSliderTravel();
			sliderProgress = Mth.clamp(sliderProgress, 0.0F, 1.0F);
			recipeIndexOffset = (int) ((sliderProgress * getHiddenRows()) + 0.5D) * 4;
			return true;
		} else {
			return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (canScroll()) {
			scrollRecipesByDelta(delta);
		}
		return true;
	}

	private void scrollRecipesByDelta(double delta) {
		int i = getHiddenRows();
		sliderProgress = (float) (sliderProgress - delta / i);
		sliderProgress = Mth.clamp(sliderProgress, 0.0F, 1.0F);
		recipeIndexOffset = (int) (sliderProgress * i + 0.5D) * 4;
	}

	private void scrollSelectedRecipeIntoView() {
		if (!canScroll()) {
			sliderProgress = 0.0F;
			recipeIndexOffset = 0;
			return;
		}

		int selectedDisplayIndex = getSortedRecipeIndexes().indexOf(container.getSelectedRecipe());
		if (selectedDisplayIndex < 0) {
			return;
		}

		int selectedRow = selectedDisplayIndex / 4;
		int firstVisibleRow = recipeIndexOffset / 4;
		if (selectedRow < firstVisibleRow) {
			setFirstVisibleRow(selectedRow);
		} else if (selectedRow >= firstVisibleRow + 3) {
			setFirstVisibleRow(selectedRow - 2);
		}
	}

	private void setFirstVisibleRow(int firstVisibleRow) {
		int hiddenRows = getHiddenRows();
		int clampedFirstVisibleRow = Mth.clamp(firstVisibleRow, 0, hiddenRows);
		recipeIndexOffset = clampedFirstVisibleRow * 4;
		sliderProgress = (float) clampedFirstVisibleRow / hiddenRows;
	}

	protected int getHiddenRows() {
		return (container.getResults().size() + 4 - 1) / 4 - 3;
	}

	private List<Integer> getSortedRecipeIndexes() {
		List<Integer> indexes = new ArrayList<>();
		for (int i = 0; i < container.getResults().size(); i++) {
			indexes.add(i);
		}
		indexes.sort((first, second) -> {
			int recentComparison = Integer.compare(container.getRecentResultOrder(first), container.getRecentResultOrder(second));
			return recentComparison != 0 ? recentComparison : Integer.compare(first, second);
		});
		return indexes;
	}

	@Override
	public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
		//TODO narration - probably just copy from stonecutter screen
	}
}
