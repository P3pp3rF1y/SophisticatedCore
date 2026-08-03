package net.p3pp3rf1y.sophisticatedcore.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.InventoryScrollPanel;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Label;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageBackgroundProperties;
import net.p3pp3rf1y.sophisticatedcore.settings.StorageSettingsTabControlBase;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.IItemDisplaySettingsPreviewProvider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class SettingsScreen extends AbstractContainerScreen<SettingsContainerMenu<?>> implements InventoryScrollPanel.IInventoryScreen {
	public static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;
	public static final Predicate<ItemStack> MATCH_ALL_FILTER = stack -> true;
	private StorageSettingsTabControlBase settingsTabControl;
	private InventoryScrollPanel inventoryScrollPanel = null;
	private TemplatePersistanceControl templatePersistanceControl = null;
	private StorageBackgroundProperties storageBackgroundProperties;
	private boolean mouseDragHandledByOther = false;
	private int visibleSlotsCount;

	protected SettingsScreen(SettingsContainerMenu<?> screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn, 176, 166);
		updateDimensionsAndSlotPositions(Minecraft.getInstance().getWindow().getGuiScaledHeight());
		settingsTabControl = initializeTabControl();
	}

	@Override
	public void resize(int width, int height) {
		updateDimensionsAndSlotPositions(height);
		super.resize(width, height);
	}

	private void updateDimensionsAndSlotPositions(int height) {
		int displayableNumberOfRows = Math.min((height - HEIGHT_WITHOUT_STORAGE_SLOTS) / 18, getMenu().getNumberOfRows());
		int newImageHeight = HEIGHT_WITHOUT_STORAGE_SLOTS + getStorageInventoryHeight(displayableNumberOfRows);
		storageBackgroundProperties = (getMenu().getNumberOfStorageInventorySlots() + getMenu().getColumnsTaken() * getMenu().getNumberOfRows()) <= 81
				? StorageBackgroundProperties.REGULAR_9_SLOT
				: StorageBackgroundProperties.REGULAR_12_SLOT;

		imageWidth = storageBackgroundProperties.getSlotsOnLine() * 18 + 14;
		updateStorageSlotsPositions();
		if (displayableNumberOfRows < getMenu().getNumberOfRows()) {
			storageBackgroundProperties = storageBackgroundProperties == StorageBackgroundProperties.REGULAR_9_SLOT
					? StorageBackgroundProperties.WIDER_9_SLOT
					: StorageBackgroundProperties.WIDER_12_SLOT;
			imageWidth += 6;
		}
		imageHeight = newImageHeight;
		inventoryLabelY = imageHeight - 94;
		inventoryLabelX = 8 + storageBackgroundProperties.getPlayerInventoryXOffset();
		if (width > 0 && height > 0) {
			leftPos = (width - imageWidth) / 2;
			topPos = (height - imageHeight) / 2;
		}
	}

	protected int getStorageInventoryHeight(int displayableNumberOfRows) {
		return displayableNumberOfRows * 18;
	}

	private void updateInventoryScrollPanel() {
		if (inventoryScrollPanel != null) {
			removeWidget(inventoryScrollPanel);
		}

		int numberOfVisibleRows = getNumberOfVisibleRows();
		if (numberOfVisibleRows < getMenu().getNumberOfRows()) {
			inventoryScrollPanel = new InventoryScrollPanel(Minecraft.getInstance(), this, 0, getMenu().getNumberOfStorageInventorySlots(), getSlotsOnLine(),
					numberOfVisibleRows * 18, getGuiTop() + 17, getGuiLeft() + 7);
			addRenderableWidget(inventoryScrollPanel);
			inventoryScrollPanel.updateSlotsPosition();
		} else {
			inventoryScrollPanel = null;
		}
	}

	@Override
	public int getVisibleSlotsCount() {
		return visibleSlotsCount;
	}

	@Override
	public void setVisibleSlotsCount(int visibleSlotsCount) {
		this.visibleSlotsCount = visibleSlotsCount;
	}

	private int getNumberOfVisibleRows() {
		return Math.min((imageHeight - HEIGHT_WITHOUT_STORAGE_SLOTS) / 18, getMenu().getNumberOfRows());
	}

	protected void updateStorageSlotsPositions() {
		int yPosition = 18;

		int slotIndex = 0;
		while (slotIndex < getMenu().getNumberOfStorageInventorySlots()) {
			Slot slot = getMenu().getSlot(slotIndex);
			int lineIndex = slotIndex % getSlotsOnLine();
			slot.x = 8 + lineIndex * 18;
			slot.y = yPosition;

			slotIndex++;
			if (slotIndex % getSlotsOnLine() == 0) {
				yPosition += 18;
			}
		}
	}

	public int getSlotsOnLine() {
		return storageBackgroundProperties.getSlotsOnLine() - getMenu().getColumnsTaken();
	}

	@Override
	protected void init() {
		super.init();
		updateDimensionsAndSlotPositions(height);
		updateInventoryScrollPanel();
		settingsTabControl = initializeTabControl();
		templatePersistanceControl = initializeTemplatePersistanceControl();
		addWidget(settingsTabControl);
		addWidget(templatePersistanceControl);
	}

	private TemplatePersistanceControl initializeTemplatePersistanceControl() {
		return new TemplatePersistanceControl(new Position(leftPos + inventoryLabelX - 29, topPos + inventoryLabelY + 29),
				getMenu().getTemplatePersistanceContainer());
	}

	protected abstract StorageSettingsTabControlBase initializeTabControl();

	public abstract IItemDisplaySettingsPreviewProvider getItemDisplaySettingsPreviewProvider();

	protected void extractBg(GuiGraphicsExtractor guiGraphics, float partialTicks, int mouseX, int mouseY) {
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		StorageGuiHelper.renderStorageBackground(new Position(x, y), guiGraphics, storageBackgroundProperties.getTextureName(), imageWidth,
				getStorageInventoryHeight(getNumberOfVisibleRows()));
		if (inventoryScrollPanel == null) {
			drawSlotBg(guiGraphics, x, y, getMenu().getStorageInventorySlots().size());
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		menu.detectSettingsChangeAndReload();
		super.extractBackground(guiGraphics, mouseX, mouseY, partialTicks);
		settingsTabControl.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		extractBg(guiGraphics, partialTicks, mouseX, mouseY);
	}

	protected void drawSlotBg(GuiGraphicsExtractor guiGraphics, int x, int y, int visibleSlotsCount) {
		int slotsOnLine = getSlotsOnLine();
		int slotRows = visibleSlotsCount / slotsOnLine;
		int remainingSlots = visibleSlotsCount % slotsOnLine;
		GuiHelper.renderSlotsBackground(guiGraphics, x + StorageScreenBase.SLOTS_X_OFFSET, y + StorageScreenBase.SLOTS_Y_OFFSET, slotsOnLine, slotRows,
				remainingSlots);
	}

	@Override
	public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		for (var widget : renderables) {
			widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		}
		templatePersistanceControl.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(leftPos, topPos);
		extractLabels(guiGraphics, mouseX, mouseY);
		guiGraphics.pose().popMatrix();
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		extractSettingsTitle(guiGraphics);
		guiGraphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ARGB.opaque(4210752), false);
		if (inventoryScrollPanel == null) {
			extractStorageInventorySlots(guiGraphics, mouseX, mouseY, true);
		}
	}

	private void extractSettingsTitle(GuiGraphicsExtractor guiGraphics) {
		int titleMaxWidth = getTitleMaxWidth();
		if (Label.isTextTruncated(font, title, titleMaxWidth)) {
			guiGraphics.text(font, Label.getTruncatedText(font, title, titleMaxWidth), titleLabelX, titleLabelY, ARGB.opaque(4210752), false);
		} else {
			guiGraphics.text(font, title, titleLabelX, titleLabelY, ARGB.opaque(4210752), false);
		}
	}

	private int getTitleMaxWidth() {
		return Math.max(0, imageWidth - titleLabelX - 7);
	}

	private void extractSettingsTitleTooltip(GuiGraphicsExtractor guiGraphics, int x, int y) {
		int titleMaxWidth = getTitleMaxWidth();
		int titleX = leftPos + titleLabelX;
		int titleY = topPos + titleLabelY;
		if (Label.isTextTruncated(font, title, titleMaxWidth) && x >= titleX && x < titleX + titleMaxWidth && y >= titleY && y < titleY + font.lineHeight) {
			guiGraphics.setTooltipForNextFrame(font, List.of(title), Optional.empty(), x, y);
		}
	}

	@Override
	public void extractStorageInventorySlots(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean canShowHover) {
		for (int slotId = 0; slotId < menu.ghostSlots.size(); ++slotId) {
			Slot slot = menu.ghostSlots.get(slotId);

			extractSlot(guiGraphics, slot, mouseX, mouseY);

			settingsTabControl.extractSlotOverlays(guiGraphics, slot, this::extractSlotOverlay, isTemplateLoadHovered());
			settingsTabControl.extractSlotExtra(guiGraphics, slot);
		}
	}

	@Override
	protected void extractSlot(GuiGraphicsExtractor guiGraphics, Slot slot, int mouseX, int mouseY) {
		ItemStack itemstack = slot.getItem() != ItemStack.EMPTY
				? slot.getItem()
				: settingsTabControl.getSlotStackDisplayOverride(slot.getSlotIndex(), isTemplateLoadHovered());

		if (!settingsTabControl.extractGuiItem(guiGraphics, itemstack, slot, isTemplateLoadHovered())) {
			if (!getMenu().getSlotFilterItem(slot.index).isEmpty()) {
				guiGraphics.item(getMenu().getSlotFilterItem(slot.index), slot.x, slot.y);
			} else {
				Identifier icon = slot.getNoItemIcon();
				if (icon != null) {
					guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, slot.x, slot.y, 16, 16);
				}
			}
		}

		settingsTabControl.extractSlotStackOverlay(guiGraphics, slot, isTemplateLoadHovered());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		extractSettingsTitleTooltip(guiGraphics, mouseX, mouseY);
		settingsTabControl.extractTooltip(this, guiGraphics, mouseX, mouseY);
		templatePersistanceControl.extractTooltip(this, guiGraphics, mouseX, mouseY);
		super.extractTooltip(guiGraphics, mouseX, mouseY);
	}

	private boolean isTemplateLoadHovered() {
		return templatePersistanceControl.isTemplateLoadHovered();
	}

	@SuppressWarnings("java:S2589") // slot can actually be null despite being marked non null
	@Override
	protected void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput type) {
		// noinspection ConstantConditions
		if (slot != null) {
			settingsTabControl.handleSlotClick(slot, mouseButton);
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (mouseDragHandledByOther) {
			return false;
		}
		Slot slot = getHoveredSlot(mouseX, mouseY);
		if (slot != null) {
			settingsTabControl.handleSlotClick(slot, button);
		}
		for (GuiEventListener child : children()) {
			if (child.isMouseOver(mouseX, mouseY) && child.mouseDragged(event, dragX, dragY)) {
				return true;
			}
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		for (GuiEventListener child : children()) {
			if (child.isMouseOver(mouseX, mouseY) && child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Nullable
	@Override
	protected Slot getHoveredSlot(double mouseX, double mouseY) {
		for (int i = 0; i < menu.ghostSlots.size(); ++i) {
			Slot slot = menu.ghostSlots.get(i);
			if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				return slot;
			}
		}

		return null;
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn) {
		return (mouseX < guiLeftIn || mouseY < guiTopIn || mouseX >= guiLeftIn + imageWidth || mouseY >= guiTopIn + imageHeight)
				&& hasClickedOutsideOfSettings(mouseX, mouseY);
	}

	private boolean hasClickedOutsideOfSettings(double mouseX, double mouseY) {
		return settingsTabControl.getTabRectangles().stream().noneMatch(r -> r.contains((int) mouseX, (int) mouseY));
	}

	private void extractSlotOverlay(GuiGraphicsExtractor guiGraphics, int xPos, int yPos, int height, int slotColor) {
		guiGraphics.fillGradient(xPos, yPos, xPos + 16, yPos + height, slotColor, slotColor);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == 256) {
			sendStorageInventoryScreenOpenMessage();
			return true;
		}
		return super.keyPressed(event);
	}

	protected abstract void sendStorageInventoryScreenOpenMessage();

	public StorageSettingsTabControlBase getSettingsTabControl() {
		return settingsTabControl;
	}

	private Rect2i getTemplatePersistanceControlRectangle() {
		return new Rect2i(templatePersistanceControl.getX(), templatePersistanceControl.getY(), templatePersistanceControl.getWidth(),
				templatePersistanceControl.getHeight());
	}

	public List<Rect2i> getExtendedControlsRectangles() {
		if (settingsTabControl == null || templatePersistanceControl == null) {
			return Collections.emptyList();
		}

		List<Rect2i> rectangles = new ArrayList<>(settingsTabControl.getTabRectangles());
		rectangles.add(getTemplatePersistanceControlRectangle());
		return rectangles;
	}

	@Override
	public boolean isMouseOverSlot(Slot slot, double mouseX, double mouseY) {
		return isHovering(slot, mouseX, mouseY);
	}

	@Override
	public void drawSlotBg(GuiGraphicsExtractor guiGraphics, int visibleSlotsCount) {
		drawSlotBg(guiGraphics, (width - imageWidth) / 2, (height - imageHeight) / 2, visibleSlotsCount);
	}

	@Override
	public int getTopY() {
		return getGuiTop();
	}

	@Override
	public int getLeftX() {
		return getGuiLeft();
	}

	@Override
	public Slot getSlot(int slotIndex) {
		return getMenu().getSlot(slotIndex);
	}

	public void startMouseDragHandledByOther() {
		mouseDragHandledByOther = true;
	}

	public void stopMouseDragHandledByOther() {
		mouseDragHandledByOther = false;
	}

	@Override
	public Predicate<ItemStack> getStackFilter() {
		return MATCH_ALL_FILTER;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
		return superMouseClicked(event, doubleClicked);
	}

	// The only modification here is calling of the containerEventHandlerMouseClicked method
	private boolean superMouseClicked(MouseButtonEvent event, boolean doubleClicked) {
		if (containerEventHandlerMouseClicked(event, doubleClicked)) {
			return true;
		} else {
			InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(event.button());
			boolean flag = this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey);
			Slot slot = this.getHoveredSlot(event.x(), event.y());
			this.doubleclick = this.lastClickSlot == slot && doubleClicked;
			this.skipNextRelease = false;
			if (event.button() != 0 && event.button() != 1 && !flag) {
				this.checkHotbarMouseClicked(event);
			} else {
				int i = this.leftPos;
				int j = this.topPos;
				boolean flag1 = this.hasClickedOutside(event.x(), event.y(), i, j);
				if (slot != null) {
					flag1 = false;
				}

				int k = -1;
				if (slot != null) {
					k = slot.index;
				}

				if (flag1) {
					k = -999;
				}

				if (k != -1 && !this.isQuickCrafting) {
					if (this.menu.getCarried().isEmpty()) {
						if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
							this.slotClicked(slot, k, event.button(), ContainerInput.CLONE);
						} else {
							boolean flag2 = k != -999 && event.hasShiftDown();
							ContainerInput clicktype = ContainerInput.PICKUP;
							if (flag2) {
								this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
								clicktype = ContainerInput.QUICK_MOVE;
							} else if (k == -999) {
								clicktype = ContainerInput.THROW;
							}

							this.slotClicked(slot, k, event.button(), clicktype);
						}

						this.skipNextRelease = true;
					} else {
						this.isQuickCrafting = true;
						this.quickCraftingButton = event.button();
						this.quickCraftSlots.clear();
						if (event.button() == 0) {
							this.quickCraftingType = 0;
						} else if (event.button() == 1) {
							this.quickCraftingType = 1;
						} else if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
							this.quickCraftingType = 2;
						}
					}
				}
			}

			this.lastClickSlot = slot;
			return true;
		}
	}

	// Modified to actually return false if child didn't handle the click
	private boolean containerEventHandlerMouseClicked(MouseButtonEvent event, boolean doubleClicked) {
		return getChildAt(event.x(), event.y()).map(child -> {
			if (child.mouseClicked(event, doubleClicked)) {
				setFocused(child);
				if (event.button() == 0) {
					setDragging(true);
				}
				return true;
			}
			return false;
		}).orElse(false);
	}
}
