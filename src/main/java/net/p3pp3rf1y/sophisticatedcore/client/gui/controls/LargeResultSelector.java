package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntConsumer;

public class LargeResultSelector extends CompositeWidgetBase<WidgetBase> {
	private static final Component SEARCH_ICON_TOOLTIP = Component.literal("Filter outputs by name");
	public static final int RESULT_COUNT_THRESHOLD = 13;
	public static final int COLUMNS = 9;
	public static final int ROWS = 5;
	private static final int CELL_WIDTH = 16;
	private static final int CELL_HEIGHT = 18;
	private static final int SEARCH_HEIGHT = 10;
	private static final int SEARCH_ICON_SIZE = 16;
	private static final int PADDING = 8;
	private static final int SEARCH_TO_RESULTS_SPACING = 6;
	private static final int RESULTS_TOP = PADDING + SEARCH_HEIGHT + SEARCH_TO_RESULTS_SPACING;
	private static final int RESULTS_WIDTH = COLUMNS * CELL_WIDTH;
	private static final int RESULTS_HEIGHT = ROWS * CELL_HEIGHT;
	private static final int SCROLLBAR_WIDTH = 14;
	private static final int SCROLLBAR_SPACING = 4;
	private static final int SEARCH_BOX_WIDTH = RESULTS_WIDTH + SCROLLBAR_SPACING + SCROLLBAR_WIDTH - SEARCH_ICON_SIZE;
	private static final int WIDTH = PADDING + RESULTS_WIDTH + SCROLLBAR_SPACING + SCROLLBAR_WIDTH + PADDING - 2;
	private static final int HEIGHT = RESULTS_TOP + RESULTS_HEIGHT + PADDING;
	private static final int RESULT_BACKGROUND_U = 29;
	private static final int RESULT_BACKGROUND_V = 146;
	private static final int RESULT_BACKGROUND_WIDTH = 66;
	private static final int RESULT_BACKGROUND_HEIGHT = 56;
	private static final int SCROLLBAR_BACKGROUND_U = 96;
	private static final int SCROLLBAR_BACKGROUND_V = 146;
	private static final int SCROLLBAR_BACKGROUND_HEIGHT = 56;
	private static final TextureBlitData SLIDER = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 131), Dimension.RECTANGLE_12_15);
	private static final TextureBlitData DISABLED_SLIDER = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(41, 131), Dimension.RECTANGLE_12_15);
	private static final TextureBlitData RECIPE_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(110, 148), Dimension.RECTANGLE_16_18);
	private static final TextureBlitData SELECTED_RECIPE_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(110, 166), Dimension.RECTANGLE_16_18);
	private static final TextureBlitData RECIPE_BACKGROUND_HOVERED = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(110, 184), Dimension.RECTANGLE_16_18);
	private static final TextureBlitData RECENT_RECIPE_BACKGROUND = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(63, 60), Dimension.RECTANGLE_16_18);
	private static final TextureBlitData SEARCH_ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(208, 32), Dimension.SQUARE_16);

	private final StorageScreenBase<?> screen;
	private final List<ResultEntry> allResults;
	private final List<ResultEntry> filteredResults = new ArrayList<>();
	private final int selectedResultIndex;
	private final IntConsumer onSelect;
	private final SoundEvent selectSound;
	private final ResultSearchBox searchBox;
	private int resultIndexOffset;
	private float sliderProgress;
	private boolean clickedOnScroll;

	public LargeResultSelector(StorageScreenBase<?> screen, List<ResultEntry> results, int selectedResultIndex, IntConsumer onSelect, SoundEvent selectSound) {
		super(new Position((Minecraft.getInstance().getWindow().getGuiScaledWidth() - WIDTH) / 2, (Minecraft.getInstance().getWindow().getGuiScaledHeight() - HEIGHT) / 2), new Dimension(WIDTH, HEIGHT));
		this.screen = screen;
		this.allResults = results;
		this.selectedResultIndex = selectedResultIndex;
		this.onSelect = onSelect;
		this.selectSound = selectSound;
		searchBox = addChild(new ResultSearchBox(new Position(x + PADDING - 1, y + PADDING), new Dimension(SEARCH_BOX_WIDTH, SEARCH_HEIGHT)));
		searchBox.setResponder(this::updateFilter);
		searchBox.setFocused(true);
		setFocused(searchBox);
		updateFilter("");
	}

	@Override
	public void setPosition(Position position) {
		super.setPosition(position);
		searchBox.setPosition(new Position(x + PADDING - 1, y + PADDING));
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.renderTabBackground(guiGraphics, x, y, getWidth(), getHeight());
		GuiHelper.renderTiledControlBackground(guiGraphics, getResultsX() - 1, getResultsY() - 1, RESULTS_WIDTH + 2, RESULTS_HEIGHT + 2, RESULT_BACKGROUND_U, RESULT_BACKGROUND_V, RESULT_BACKGROUND_WIDTH, RESULT_BACKGROUND_HEIGHT);
		GuiHelper.renderTiledControlBackground(guiGraphics, getScrollbarX() - 1, getResultsY() - 1, SCROLLBAR_WIDTH, RESULTS_HEIGHT + 2, SCROLLBAR_BACKGROUND_U, SCROLLBAR_BACKGROUND_V, SCROLLBAR_WIDTH, SCROLLBAR_BACKGROUND_HEIGHT);
		renderResultBackgrounds(guiGraphics, mouseX, mouseY);
		renderResultItems(guiGraphics);
		renderScrollBar(guiGraphics);
	}

	private int getResultsX() {
		return x + PADDING;
	}

	private int getResultsY() {
		return y + RESULTS_TOP;
	}

	private int getScrollbarX() {
		return getResultsX() + RESULTS_WIDTH + SCROLLBAR_SPACING;
	}

	private void renderResultBackgrounds(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		int maxResultIndex = Math.min(resultIndexOffset + getVisibleResultCount(), filteredResults.size());
		for (int resultIndex = resultIndexOffset; resultIndex < maxResultIndex; resultIndex++) {
			int visibleIndex = resultIndex - resultIndexOffset;
			int resultX = getResultsX() + visibleIndex % COLUMNS * CELL_WIDTH;
			int resultY = getResultsY() + visibleIndex / COLUMNS * CELL_HEIGHT;
			TextureBlitData background = RECIPE_BACKGROUND;
			ResultEntry result = filteredResults.get(resultIndex);
			if (result.resultIndex() == selectedResultIndex) {
				background = SELECTED_RECIPE_BACKGROUND;
			} else if (mouseX >= resultX && mouseY >= resultY + 1 && mouseX < resultX + 16 && mouseY < resultY + 19) {
				background = RECIPE_BACKGROUND_HOVERED;
			} else if (result.recent()) {
				background = RECENT_RECIPE_BACKGROUND;
			}
			GuiHelper.blit(guiGraphics, resultX, resultY, background);
		}
	}

	private void renderResultItems(GuiGraphics guiGraphics) {
		int maxResultIndex = Math.min(resultIndexOffset + getVisibleResultCount(), filteredResults.size());
		for (int resultIndex = resultIndexOffset; resultIndex < maxResultIndex; resultIndex++) {
			int visibleIndex = resultIndex - resultIndexOffset;
			int resultX = getResultsX() + visibleIndex % COLUMNS * CELL_WIDTH;
			int resultY = getResultsY() + visibleIndex / COLUMNS * CELL_HEIGHT + 1;
			ItemStack result = filteredResults.get(resultIndex).stack();
			GuiHelper.renderItemInGUI(guiGraphics, minecraft, result, resultX, resultY, result.getCount() > 1, String.valueOf(result.getCount()));
		}
	}

	private void renderScrollBar(GuiGraphics guiGraphics) {
		int sliderYOffset = (int) ((RESULTS_HEIGHT - SLIDER.getHeight()) * sliderProgress);
		GuiHelper.blit(guiGraphics, getScrollbarX(), getResultsY() + sliderYOffset, canScroll() ? SLIDER : DISABLED_SLIDER);
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
		GuiHelper.blit(guiGraphics, searchBox.getX() + searchBox.getWidth(), searchBox.getY() - (SEARCH_ICON_SIZE - searchBox.getHeight()) / 2, SEARCH_ICON);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		clickedOnScroll = false;
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		int maxResultIndex = Math.min(resultIndexOffset + getVisibleResultCount(), filteredResults.size());
		for (int resultIndex = resultIndexOffset; resultIndex < maxResultIndex; resultIndex++) {
			int visibleIndex = resultIndex - resultIndexOffset;
			double relativeX = mouseX - (getResultsX() + visibleIndex % COLUMNS * CELL_WIDTH);
			double relativeY = mouseY - (getResultsY() + visibleIndex / COLUMNS * CELL_HEIGHT + 1);
			if (relativeX >= 0.0D && relativeY >= 0.0D && relativeX < 16.0D && relativeY < 18.0D) {
				onSelect.accept(filteredResults.get(resultIndex).resultIndex());
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(selectSound, 1.0F));
				screen.setModalOverlay(null);
				return true;
			}
		}

		if (mouseX >= getScrollbarX() && mouseX < getScrollbarX() + SCROLLBAR_WIDTH && mouseY >= getResultsY() && mouseY < getResultsY() + RESULTS_HEIGHT) {
			clickedOnScroll = true;
			return true;
		}

		return isMouseOver(mouseX, mouseY);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (clickedOnScroll && canScroll()) {
			sliderProgress = ((float) mouseY - getResultsY() - SLIDER.getHeight() / 2f) / (RESULTS_HEIGHT - SLIDER.getHeight());
			sliderProgress = Mth.clamp(sliderProgress, 0.0F, 1.0F);
			resultIndexOffset = (int) (sliderProgress * getHiddenRows() + 0.5D) * COLUMNS;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		clickedOnScroll = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (canScroll()) {
			scrollResultsByDelta(scrollY);
		}
		return true;
	}

	private void scrollResultsByDelta(double delta) {
		int hiddenRows = getHiddenRows();
		sliderProgress = (float) (sliderProgress - delta / hiddenRows);
		sliderProgress = Mth.clamp(sliderProgress, 0.0F, 1.0F);
		resultIndexOffset = (int) (sliderProgress * hiddenRows + 0.5D) * COLUMNS;
	}

	private void updateFilter(String searchPhrase) {
		filteredResults.clear();
		String normalizedSearchPhrase = searchPhrase.trim().toLowerCase(Locale.ROOT);
		for (ResultEntry result : allResults) {
			if (normalizedSearchPhrase.isEmpty() || result.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(normalizedSearchPhrase)) {
				filteredResults.add(result);
			}
		}
		scrollSelectedResultIntoView();
	}

	private void scrollSelectedResultIntoView() {
		int selectedFilteredIndex = -1;
		for (int i = 0; i < filteredResults.size(); i++) {
			if (filteredResults.get(i).resultIndex() == selectedResultIndex) {
				selectedFilteredIndex = i;
				break;
			}
		}

		int hiddenRows = getHiddenRows();
		if (selectedFilteredIndex < 0 || hiddenRows <= 0) {
			resultIndexOffset = 0;
			sliderProgress = 0;
			return;
		}

		int selectedRow = selectedFilteredIndex / COLUMNS;
		int firstVisibleRow = Mth.clamp(selectedRow - ROWS / 2, 0, hiddenRows);
		resultIndexOffset = firstVisibleRow * COLUMNS;
		sliderProgress = (float) firstVisibleRow / hiddenRows;
	}

	private boolean canScroll() {
		return filteredResults.size() > getVisibleResultCount();
	}

	private int getHiddenRows() {
		return (filteredResults.size() + COLUMNS - 1) / COLUMNS - ROWS;
	}

	private int getVisibleResultCount() {
		return COLUMNS * ROWS;
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		if (isMouseOverSearchIcon(mouseX, mouseY)) {
			guiGraphics.renderTooltip(screen.font, SEARCH_ICON_TOOLTIP, mouseX, mouseY);
			return;
		}

		int maxResultIndex = Math.min(resultIndexOffset + getVisibleResultCount(), filteredResults.size());
		for (int resultIndex = resultIndexOffset; resultIndex < maxResultIndex; resultIndex++) {
			int visibleIndex = resultIndex - resultIndexOffset;
			int resultX = getResultsX() + visibleIndex % COLUMNS * CELL_WIDTH;
			int resultY = getResultsY() + visibleIndex / COLUMNS * CELL_HEIGHT + 1;
			if (mouseX >= resultX && mouseX < resultX + 16 && mouseY >= resultY && mouseY < resultY + 18) {
				renderTooltip(guiGraphics, filteredResults.get(resultIndex).stack(), mouseX, mouseY);
			}
		}
	}

	private void renderTooltip(GuiGraphics guiGraphics, ItemStack itemStack, int mouseX, int mouseY) {
		Font font = IClientItemExtensions.of(itemStack).getFont(itemStack, IClientItemExtensions.FontContext.TOOLTIP);
		guiGraphics.renderComponentTooltip((font == null ? this.font : font), Screen.getTooltipFromItem(minecraft, itemStack), mouseX, mouseY);
	}

	private boolean isMouseOverSearchIcon(int mouseX, int mouseY) {
		int iconX = searchBox.getX() + searchBox.getWidth();
		int iconY = searchBox.getY() - (SEARCH_ICON_SIZE - searchBox.getHeight()) / 2;
		return mouseX >= iconX && mouseX < iconX + SEARCH_ICON_SIZE && mouseY >= iconY && mouseY < iconY + SEARCH_ICON_SIZE;
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		//no narration
	}

	public record ResultEntry(int resultIndex, ItemStack stack, boolean recent) {}

	private class ResultSearchBox extends TextBox {
		private static final int UNFOCUSED_COLOR = 0xBBBBBB;

		private ResultSearchBox(Position position, Dimension dimension) {
			super(position, dimension);
			setTextColor(UNFOCUSED_COLOR);
			setTextColorUneditable(UNFOCUSED_COLOR);
			setBordered(false);
			setMaxLength(50);
			setUnfocusedEmptyHint("Search");
		}

		@Override
		protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			guiGraphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFF777777);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (!isMouseOver(mouseX, mouseY)) {
				return false;
			}
			if (button == 0) {
				setFocused(true);
				LargeResultSelector.this.setFocused(this);
			} else if (button == 1) {
				setValue("");
			}
			return true;
		}

		@Override
		public void setFocused(boolean focused) {
			super.setFocused(focused);
			setTextColor(focused ? -1 : UNFOCUSED_COLOR);
		}

		@Override
		public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
			super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
			if (!isFocused() && isMouseOver(mouseX, mouseY)) {
				guiGraphics.renderTooltip(screen.font, List.of(Component.translatable(TranslationHelper.INSTANCE.translGui("text_box.search_box"))), Optional.empty(), mouseX, mouseY);
			}
		}
	}
}
