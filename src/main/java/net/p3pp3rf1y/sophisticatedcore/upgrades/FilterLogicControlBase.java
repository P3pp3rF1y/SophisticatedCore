package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

import static net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControlBase.MatchButton.*;

public abstract class FilterLogicControlBase<F extends FilterLogic, S extends Slot, C extends FilterLogicContainerBase<F, S>>
		extends CompositeWidgetBase<WidgetBase> {
	public static final int TAG_FONT_COLOR = 16383998;
	public static final int MORE_TAGS_FONT_COLOR = 13882323;
	private static final int MAX_TAG_NAME_WIDTH = 68;

	protected final MatchButton[] showMatchButtons;
	protected final int slotsTopYOffset;
	protected final int slotsPerRow;
	protected final int slotsInExtraRow;
	protected final int fullSlotRows;
	private final int totalSlotRows;
	private final StorageScreenBase<?> screen;
	protected final C container;
	private final List<Component> addTagTooltip = new ArrayList<>();
	private final List<Component> removeTagTooltip = new ArrayList<>();
	private final List<Component> tagListTooltip = new ArrayList<>();
	@Nullable
	private ToggleButton<Boolean> nbtButton = null;
	@Nullable
	private ToggleButton<Boolean> durabilityButton = null;
	private int tagButtonsYOffset;

	protected FilterLogicControlBase(StorageScreenBase<?> screen, C container, Position position, boolean buttonsVisible, int slotsPerRow, MatchButton... showMatchButtons) {
		super(position, new Dimension(0, 0));
		this.screen = screen;
		this.container = container;
		slotsTopYOffset = buttonsVisible ? 21 : 0;
		this.slotsPerRow = slotsPerRow;
		this.showMatchButtons = showMatchButtons;
		fullSlotRows = container.getFilterSlots().size() / slotsPerRow;
		slotsInExtraRow = container.getFilterSlots().size() % slotsPerRow;
		totalSlotRows = fullSlotRows + (slotsInExtraRow > 0 ? 1 : 0);

		if (shouldShow(ALLOW_LIST)) {
			addChild(new ToggleButton<>(new Position(x, y), ButtonDefinitions.ALLOW_LIST, button -> container.setAllowList(!container.isAllowList()), container::isAllowList));
		}
		if (shouldShow(PRIMARY_MATCH)) {
			addChild(new ToggleButton<>(new Position(x + 18, y), ButtonDefinitions.PRIMARY_MATCH,
					button -> {
						PrimaryMatch next = container.getPrimaryMatch().next();
						if (next == PrimaryMatch.TAGS) {
							container.getFilterSlots().forEach(slot -> slot.x = StorageScreenBase.DISABLED_SLOT_X_POS);
							onTagsMatchSelected();
						}
						container.setPrimaryMatch(next);
						setDurabilityAndNbtButtonsVisibility();
						moveSlotsToView();
					}, container::getPrimaryMatch));
			addTagButtons();
		}
		if (shouldShow(DURABILITY)) {
			durabilityButton = new ToggleButton<>(new Position(x + 36, y), ButtonDefinitions.MATCH_DURABILITY,
					button -> container.setMatchDurability(!container.shouldMatchDurability()), container::shouldMatchDurability);
			addChild(durabilityButton);
		}
		if (shouldShow(NBT)) {
			nbtButton = new ToggleButton<>(new Position(x + 54, y), ButtonDefinitions.MATCH_NBT,
					button -> container.setMatchNbt(!container.shouldMatchNbt()), container::shouldMatchNbt);
			addChild(nbtButton);
		}
		updateDimensions(Math.max(slotsPerRow * 18, getMaxButtonWidth()), (fullSlotRows + (slotsInExtraRow > 0 ? 1 : 0)) * 18 + slotsTopYOffset);
		setDurabilityAndNbtButtonsVisibility();
	}

	private void setDurabilityAndNbtButtonsVisibility() {
		boolean visible = container.getPrimaryMatch() != PrimaryMatch.TAGS;
		if (nbtButton != null) {
			nbtButton.setVisible(visible);
		}
		if (durabilityButton != null) {
			durabilityButton.setVisible(visible);
		}
	}

	protected void onTagsMatchSelected() {
		//noop
	}

	private void addTagButtons() {
		tagButtonsYOffset = slotsTopYOffset + (getTagListHeight());
		addChild(new TagButton(new Position(x + 36, y + tagButtonsYOffset), ButtonDefinitions.REMOVE_TAG, button -> {
			container.removeSelectedTag();
			updateTagListAndRemoveTooltips();
			updateAddTooltip();
		}, delta -> {
			if (delta < 0) {
				container.selectNextTagToRemove();
			} else {
				container.selectPreviousTagToRemove();
			}
			updateTagListAndRemoveTooltips();
		}) {
			@Override
			protected List<Component> getTooltip() {
				return removeTagTooltip;
			}
		});
		updateTagListAndRemoveTooltips();

		addChild(new TagButton(new Position(x + 18, y + tagButtonsYOffset), ButtonDefinitions.ADD_TAG, button -> {
			container.addSelectedTag();
			updateAddTooltip();
			updateTagListAndRemoveTooltips();
		}, delta -> {
			if (delta < 0) {
				container.selectNextTagToAdd();
			} else {
				container.selectPreviousTagToAdd();
			}
			updateAddTooltip();
		}) {
			@Override
			protected List<Component> getTooltip() {
				return addTagTooltip;
			}
		});
		updateAddTooltip();
		container.getTagSelectionSlot().setOnUpdate(this::updateAddTooltip);

		addChild(new ToggleButton<Boolean>(new Position(x + 54, y + tagButtonsYOffset), ButtonDefinitions.MATCH_ANY_TAG, button -> container.setMatchAnyTag(!container.shouldMatchAnyTag()), container::shouldMatchAnyTag) {
			@Override
			protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
				if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
					super.renderBg(guiGraphics, minecraft, mouseX, mouseY);
				}
			}

			@Override
			protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
					super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
				}
			}

			@Override
			public boolean isMouseOver(double mouseX, double mouseY) {
				return container.getPrimaryMatch() == PrimaryMatch.TAGS && super.isMouseOver(mouseX, mouseY);
			}
		});
	}

	private void updateTagListAndRemoveTooltips() {
		updateTagListTooltip();
		updateRemoveTooltip();
	}

	private void updateTagListTooltip() {
		tagListTooltip.clear();
		tagListTooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("tag_list.title")).withStyle());
		Set<TagKey<Item>> tagNames = container.getTagNames();
		if (tagNames.isEmpty()) {
			tagListTooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("tag_list.empty")).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		for (TagKey<Item> tagName : tagNames) {
			tagListTooltip.add(Component.literal("> " + tagName.location()).withStyle(ChatFormatting.GRAY));
		}
	}

	private void updateRemoveTooltip() {
		removeTagTooltip.clear();
		removeTagTooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("remove_tag")));
		Set<TagKey<Item>> tagNames = container.getTagNames();
		if (tagNames.isEmpty()) {
			removeTagTooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("remove_tag.empty")).withStyle(ChatFormatting.RED));
			return;
		}

		int curIndex = 0;
		for (TagKey<Item> tagName : tagNames) {
			if (curIndex == container.getSelectedTagToRemove()) {
				removeTagTooltip.add(Component.literal("-> " + tagName.location()).withStyle(ChatFormatting.RED));
			} else {
				removeTagTooltip.add(Component.literal("> " + tagName.location()).withStyle(ChatFormatting.GRAY));
			}
			curIndex++;
		}
		removeTagTooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("remove_tag.controls")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
	}

	private void updateAddTooltip() {
		addTagTooltip.clear();
		addTagTooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("add_tag")));
		if (container.getTagSelectionSlot().getItem().isEmpty()) {
			addTagTooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("add_tag.no_item")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
			return;
		}
		Set<TagKey<Item>> tagsToAdd = container.getTagsToAdd();
		int curIndex = 0;
		for (TagKey<Item> tagName : tagsToAdd) {
			if (curIndex == container.getSelectedTagToAdd()) {
				addTagTooltip.add(Component.literal("-> " + tagName.location()).withStyle(ChatFormatting.GREEN));
			} else {
				addTagTooltip.add(Component.literal("> " + tagName.location()).withStyle(ChatFormatting.GRAY));
			}
			curIndex++;
		}
		if (tagsToAdd.isEmpty()) {
			addTagTooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("add_tag.no_additional_tags")).withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW));
		} else {
			addTagTooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeButton("add_tag.controls")).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
		}
	}

	protected int getMaxButtonWidth() {
		int maxWidth = 0;
		for (WidgetBase w : children) {
			int buttonWidth = w.getX() + w.getWidth() - x;
			if (buttonWidth > maxWidth) {
				maxWidth = buttonWidth;
			}
		}
		return maxWidth;
	}

	protected boolean shouldShow(MatchButton matchButton) {
		for (MatchButton showMatchButton : showMatchButtons) {
			if (showMatchButton == matchButton) {
				return true;
			}
		}
		return false;
	}

	public void moveSlotsToView() {
		if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
			Slot slot = container.getTagSelectionSlot();
			slot.x = x - screen.getGuiLeft() + 1;
			slot.y = y - screen.getGuiTop() + tagButtonsYOffset + 1;
			container.getFilterSlots().forEach(s -> s.x = StorageScreenBase.DISABLED_SLOT_X_POS);
		} else {
			int upgradeSlotNumber = 0;
			for (S slot : container.getFilterSlots()) {
				slot.x = x - screen.getGuiLeft() + 1 + (upgradeSlotNumber % slotsPerRow) * 18;
				slot.y = y - screen.getGuiTop() + slotsTopYOffset + 1 + (upgradeSlotNumber / slotsPerRow) * 18;
				upgradeSlotNumber++;
			}
			container.getTagSelectionSlot().x = StorageScreenBase.DISABLED_SLOT_X_POS;
		}
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
		if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
			renderTagNames(guiGraphics);
		}
	}

	private void renderTagNames(GuiGraphics guiGraphics) {
		int count = 0;
		int prefixWidth = font.width("...");
		Set<TagKey<Item>> tagNames = container.getTagNames();
		int maxTagNameLines = getTagListHeight() / 10;
		for (TagKey<Item> tagName : tagNames) {
			if (tagNames.size() > maxTagNameLines && count == maxTagNameLines - 1) {
				guiGraphics.drawString(minecraft.font, Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("tag_list.tag_overflow"), String.valueOf(tagNames.size() - (maxTagNameLines - 1))), x + 2, y + 23 + count * 10, MORE_TAGS_FONT_COLOR, false);
				break;
			}
			String name = tagName.location().toString();
			String shortened = name;
			if (font.width(name) > MAX_TAG_NAME_WIDTH) {
				shortened = font.plainSubstrByWidth(name, MAX_TAG_NAME_WIDTH - prefixWidth, true);
				if (!shortened.equals(name)) {
					shortened = "..." + shortened;
				}
			}
			guiGraphics.drawString(minecraft.font, shortened, x + 2, y + 23 + count * 10, TAG_FONT_COLOR, false);
			count++;
		}
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		if (container.getPrimaryMatch() == PrimaryMatch.TAGS && isMouseOverTagList(mouseX, mouseY)) {
			guiGraphics.renderTooltip(screen.font, tagListTooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	private int getTagListHeight() {
		return (totalSlotRows - 1) * 18;
	}

	private boolean isMouseOverTagList(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + getTagListWidth() && mouseY >= y + slotsTopYOffset && mouseY < y + slotsTopYOffset + getTagListHeight();
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		if (container.getPrimaryMatch() != PrimaryMatch.TAGS) {
			GuiHelper.renderSlotsBackground(guiGraphics, x, y + slotsTopYOffset, slotsPerRow, fullSlotRows, slotsInExtraRow);
		} else {
			GuiHelper.renderSlotsBackground(guiGraphics, x, y + tagButtonsYOffset, 1, 1, 0);
			GuiHelper.renderControlBackground(guiGraphics, x, y + slotsTopYOffset, getTagListWidth(), getTagListHeight());
		}
	}

	private int getTagListWidth() {
		return slotsPerRow * 18;
	}

	private class TagButton extends Button {
		private final DoubleConsumer onScroll;

		public TagButton(Position position, ButtonDefinition buttonDefinition, IntConsumer onClick, DoubleConsumer onScroll) {
			super(position, buttonDefinition, onClick);
			this.onScroll = onScroll;
		}

		@Override
		protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
				super.renderBg(guiGraphics, minecraft, mouseX, mouseY);
			}
		}

		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
			if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
				super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
			}
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return container.getPrimaryMatch() == PrimaryMatch.TAGS && super.isMouseOver(mouseX, mouseY);
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (container.getPrimaryMatch() != PrimaryMatch.TAGS) {
				return false;
			}
			onScroll.accept(scrollY);
			return true;
		}
	}

	public enum MatchButton {
		ALLOW_LIST,
		PRIMARY_MATCH,
		DURABILITY,
		NBT
	}
}
