package net.p3pp3rf1y.sophisticatedcore.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.InventoryScrollPanel;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageBackgroundProperties;
import net.p3pp3rf1y.sophisticatedcore.settings.StorageSettingsTabControlBase;

import javax.annotation.Nullable;

public abstract class SettingsScreen extends AbstractContainerScreen<SettingsContainerMenu<?>> implements InventoryScrollPanel.IInventoryScreen {
	public static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;
	private StorageSettingsTabControlBase settingsTabControl;
	private InventoryScrollPanel inventoryScrollPanel = null;
	private TemplatePersistanceControl templatePersistanceControl = null;
	private StorageBackgroundProperties storageBackgroundProperties;
	private boolean mouseDragHandledByOther = false;

	protected SettingsScreen(SettingsContainerMenu<?> screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn);
		updateDimensionsAndSlotPositions(Minecraft.getInstance().getWindow().getGuiScaledHeight());
		settingsTabControl = initializeTabControl();
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		updateDimensionsAndSlotPositions(height);
		super.resize(minecraft, width, height);
	}

	private void updateDimensionsAndSlotPositions(int height) {
		int displayableNumberOfRows = Math.min((height - HEIGHT_WITHOUT_STORAGE_SLOTS) / 18, getMenu().getNumberOfRows());
		int newImageHeight = HEIGHT_WITHOUT_STORAGE_SLOTS + getStorageInventoryHeight(displayableNumberOfRows);
		storageBackgroundProperties = (getMenu().getNumberOfStorageInventorySlots() + getMenu().getColumnsTaken() * getMenu().getNumberOfRows()) <= 81 ? StorageBackgroundProperties.REGULAR_9_SLOT : StorageBackgroundProperties.REGULAR_12_SLOT;

		imageWidth = storageBackgroundProperties.getSlotsOnLine() * 18 + 14;
		updateStorageSlotsPositions();
		if (displayableNumberOfRows < getMenu().getNumberOfRows()) {
			storageBackgroundProperties = storageBackgroundProperties == StorageBackgroundProperties.REGULAR_9_SLOT ? StorageBackgroundProperties.WIDER_9_SLOT : StorageBackgroundProperties.WIDER_12_SLOT;
			imageWidth += 6;
		}
		imageHeight = newImageHeight;
		inventoryLabelY = imageHeight - 94;
		inventoryLabelX = 8 + storageBackgroundProperties.getPlayerInventoryXOffset();
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
			inventoryScrollPanel = new InventoryScrollPanel(Minecraft.getInstance(), this, 0, getMenu().getNumberOfStorageInventorySlots(), getSlotsOnLine(), numberOfVisibleRows * 18, getGuiTop() + 17, getGuiLeft() + 7);
			addRenderableWidget(inventoryScrollPanel);
			inventoryScrollPanel.updateSlotsYPosition();
		} else {
			inventoryScrollPanel = null;
		}
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
		updateInventoryScrollPanel();
		settingsTabControl = initializeTabControl();
		templatePersistanceControl = initializeTemplatePersistanceControl();
		addWidget(settingsTabControl);
		addWidget(templatePersistanceControl);
	}

	private TemplatePersistanceControl initializeTemplatePersistanceControl() {
		return new TemplatePersistanceControl(new Position(leftPos + inventoryLabelX - 29, topPos + inventoryLabelY + 29), getMenu().getTemplatePersistanceContainer());
	}

	protected abstract StorageSettingsTabControlBase initializeTabControl();

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		StorageGuiHelper.renderStorageBackground(new Position(x, y), guiGraphics, storageBackgroundProperties.getTextureName(), imageWidth, getStorageInventoryHeight(getNumberOfVisibleRows()));
		if (inventoryScrollPanel == null) {
			drawSlotBg(guiGraphics, x, y);
		}
	}

	protected void drawSlotBg(GuiGraphics guiGraphics, int x, int y) {
		int inventorySlots = getMenu().getStorageInventorySlots().size();
		int slotsOnLine = getSlotsOnLine();
		int slotRows = inventorySlots / slotsOnLine;
		int remainingSlots = inventorySlots % slotsOnLine;
		GuiHelper.renderSlotsBackground(guiGraphics, x + StorageScreenBase.SLOTS_X_OFFSET, y + StorageScreenBase.SLOTS_Y_OFFSET, slotsOnLine, slotRows, remainingSlots);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		menu.detectSettingsChangeAndReload();
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, -20);
		renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
		poseStack.popPose();
		settingsTabControl.render(guiGraphics, mouseX, mouseY, partialTicks);
		templatePersistanceControl.render(guiGraphics, mouseX, mouseY, partialTicks);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		settingsTabControl.renderTooltip(this, guiGraphics, mouseX, mouseY);
		templatePersistanceControl.renderTooltip(this, guiGraphics, mouseX, mouseY);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	public void renderTransparentBackground(GuiGraphics guiGraphics) {
		PoseStack pose = guiGraphics.pose();
		pose.pushPose();
		pose.translate(0, 0, -12);
		super.renderTransparentBackground(guiGraphics);
		pose.popPose();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderLabels(guiGraphics, mouseX, mouseY);
		if (inventoryScrollPanel == null) {
			renderInventorySlots(guiGraphics, mouseX, mouseY, true);
		}
	}

	@Override
	public void renderInventorySlots(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean canShowHover) {
		for (int slotId = 0; slotId < menu.ghostSlots.size(); ++slotId) {
			Slot slot = menu.ghostSlots.get(slotId);
			renderSlot(guiGraphics, slot);

			settingsTabControl.renderSlotOverlays(guiGraphics, slot, this::renderSlotOverlay, isTemplateLoadHovered());

			if (canShowHover && isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				hoveredSlot = slot;
				renderSlotHighlight(guiGraphics, slot.x, slot.y, 0, getSlotColor(slotId));
			}

			settingsTabControl.renderSlotExtra(guiGraphics, slot);
		}
	}

	@Override
	protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
		ItemStack itemstack = slot.getItem() != ItemStack.EMPTY ? slot.getItem() : settingsTabControl.getSlotStackDisplayOverride(slot.getSlotIndex(), isTemplateLoadHovered());

		RenderSystem.enableDepthTest();
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 100);
		//noinspection ConstantConditions - by this point minecraft isn't null
		if (!settingsTabControl.renderGuiItem(guiGraphics, minecraft.getItemRenderer(), itemstack, slot, isTemplateLoadHovered())) {
			if (!getMenu().getSlotFilterItem(slot.index).isEmpty()) {
				guiGraphics.renderItem(getMenu().getSlotFilterItem(slot.index), slot.x, slot.y);
			} else {
				Pair<ResourceLocation, ResourceLocation> pair = slot.getNoItemIcon();
				if (pair != null) {
					TextureAtlasSprite textureatlassprite = minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
					guiGraphics.blit(slot.x, slot.y, 0, 16, 16, textureatlassprite);
				}
			}
		}
		poseStack.popPose();

		settingsTabControl.drawSlotStackOverlay(guiGraphics, slot, isTemplateLoadHovered());
	}

	private boolean isTemplateLoadHovered() {
		return templatePersistanceControl.isTemplateLoadHovered();
	}

	@SuppressWarnings("java:S2589") // slot can actually be null despite being marked non null
	@Override
	protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
		//noinspection ConstantConditions
		if (slot != null) {
			settingsTabControl.handleSlotClick(slot, mouseButton);
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (mouseDragHandledByOther) {
			return false;
		}
		Slot slot = findSlot(mouseX, mouseY);
		if (slot != null) {
			settingsTabControl.handleSlotClick(slot, button);
		}
		for (GuiEventListener child : children()) {
			if (child.isMouseOver(mouseX, mouseY) && child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
				return true;
			}
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Nullable
	@Override
	protected Slot findSlot(double mouseX, double mouseY) {
		for (int i = 0; i < menu.ghostSlots.size(); ++i) {
			Slot slot = menu.ghostSlots.get(i);
			if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				return slot;
			}
		}

		return null;
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
		return super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn, mouseButton) && hasClickedOutsideOfSettings(mouseX, mouseY);
	}

	private boolean hasClickedOutsideOfSettings(double mouseX, double mouseY) {
		return settingsTabControl.getTabRectangles().stream().noneMatch(r -> r.contains((int) mouseX, (int) mouseY));
	}

	private void renderSlotOverlay(GuiGraphics guiGraphics, int xPos, int yPos, int height, int slotColor) {
		RenderSystem.disableDepthTest();
		RenderSystem.colorMask(true, true, true, false);
		guiGraphics.fillGradient(xPos, yPos, xPos + 16, yPos + height, slotColor, slotColor);
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.enableDepthTest();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			sendStorageInventoryScreenOpenMessage();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	protected abstract void sendStorageInventoryScreenOpenMessage();

	public StorageSettingsTabControlBase getSettingsTabControl() {
		return settingsTabControl;
	}

	@Override
	public boolean isMouseOverSlot(Slot slot, double mouseX, double mouseY) {
		return isHovering(slot, mouseX, mouseY);
	}

	@Override
	public void drawSlotBg(GuiGraphics guiGraphics) {
		drawSlotBg(guiGraphics, (width - imageWidth) / 2, (height - imageHeight) / 2);
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
}
