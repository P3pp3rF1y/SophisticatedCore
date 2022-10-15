package net.p3pp3rf1y.sophisticatedcore.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.InventoryScrollPanel;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageBackgroundProperties;
import net.p3pp3rf1y.sophisticatedcore.settings.StorageSettingsTabControlBase;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class SettingsScreen extends AbstractContainerScreen<SettingsContainer<?>> implements InventoryScrollPanel.IInventoryScreen {
	public static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;
	private StorageSettingsTabControlBase settingsTabControl;
	private InventoryScrollPanel inventoryScrollPanel = null;
	private StorageBackgroundProperties storageBackgroundProperties;

	protected SettingsScreen(SettingsContainer<?> screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn);
		updateDimensionsAndSlotPositions(Minecraft.getInstance().getWindow().getGuiScaledHeight());
	}

	@Override
	public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
		updateDimensionsAndSlotPositions(pHeight);
		super.resize(pMinecraft, pWidth, pHeight);
	}

	private void updateDimensionsAndSlotPositions(int pHeight) {
		int displayableNumberOfRows = Math.min((pHeight - HEIGHT_WITHOUT_STORAGE_SLOTS) / 18, getMenu().getNumberOfRows());
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
		addWidget(settingsTabControl);
	}

	protected abstract StorageSettingsTabControlBase initializeTabControl();

	@Override
	protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		StorageGuiHelper.renderStorageBackground(new Position(x, y), matrixStack, storageBackgroundProperties.getTextureName(), imageWidth, getStorageInventoryHeight(getNumberOfVisibleRows()));
		if (inventoryScrollPanel == null) {
			drawSlotBg(matrixStack, x, y);
		}
	}

	protected void drawSlotBg(PoseStack matrixStack, int x, int y) {
		int inventorySlots = getMenu().getStorageInventorySlots().size();
		int slotsOnLine = getSlotsOnLine();
		int slotRows = inventorySlots / slotsOnLine;
		int remainingSlots = inventorySlots % slotsOnLine;
		GuiHelper.renderSlotsBackground(matrixStack, x + StorageScreenBase.SLOTS_X_OFFSET, y + StorageScreenBase.SLOTS_Y_OFFSET, slotsOnLine, slotRows, remainingSlots);
	}

	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		menu.detectSettingsChangeAndReload();
		renderBackground(matrixStack);
		settingsTabControl.render(matrixStack, mouseX, mouseY, partialTicks);
		matrixStack.translate(0, 0, 200);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		settingsTabControl.renderTooltip(this, matrixStack, mouseX, mouseY);
		renderTooltip(matrixStack, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
		super.renderLabels(matrixStack, mouseX, mouseY);
		if (inventoryScrollPanel == null) {
			renderInventorySlots(matrixStack, mouseX, mouseY, true);
		}
	}

	@Override
	public void renderInventorySlots(PoseStack matrixStack, int mouseX, int mouseY, boolean canShowHover) {
		for (int slotId = 0; slotId < menu.ghostSlots.size(); ++slotId) {
			Slot slot = menu.ghostSlots.get(slotId);
			renderSlot(matrixStack, slot);

			settingsTabControl.renderSlotOverlays(matrixStack, slot, this::renderSlotOverlay);

			if (canShowHover && isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				hoveredSlot = slot;
				renderSlotOverlay(matrixStack, slot, getSlotColor(slotId));
			}

			settingsTabControl.renderSlotExtra(matrixStack, slot);
		}
	}

	@Override
	protected void renderSlot(PoseStack poseStack, Slot slot) {
		Optional<ItemStack> memorizedStack = getMenu().getMemorizedStackInSlot(slot.getSlotIndex());
		ItemStack itemstack = slot.getItem();
		if (memorizedStack.isPresent()) {
			itemstack = memorizedStack.get();
		}

		setBlitOffset(100);
		itemRenderer.blitOffset = 100.0F;

		RenderSystem.enableDepthTest();
		poseStack.pushPose();
		settingsTabControl.renderGuiItem(itemRenderer, itemstack, slot);
		poseStack.popPose();
		itemRenderer.blitOffset = 0.0F;
		setBlitOffset(0);

		if (memorizedStack.isPresent()) {
			drawMemorizedStackOverlay(poseStack, slot.x, slot.y);
		}
	}

	private void drawMemorizedStackOverlay(PoseStack poseStack, int x, int y) {
		poseStack.pushPose();
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GuiHelper.GUI_CONTROLS);
		blit(poseStack, x, y, 77, 0, 16, 16);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		poseStack.popPose();
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

	private void renderSlotOverlay(PoseStack matrixStack, Slot slot, int slotColor) {
		renderSlotOverlay(matrixStack, slot.x, slot.y, 16, slotColor);
	}

	private void renderSlotOverlay(PoseStack matrixStack, int xPos, int yPos, int height, int slotColor) {
		RenderSystem.disableDepthTest();
		RenderSystem.colorMask(true, true, true, false);
		fillGradient(matrixStack, xPos, yPos, xPos + 16, yPos + height, slotColor, slotColor);
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
	public boolean isMouseOverSlot(Slot pSlot, double pMouseX, double pMouseY) {
		return isHovering(pSlot, pMouseX, pMouseY);
	}

	@Override
	public void drawSlotBg(PoseStack matrixStack) {
		drawSlotBg(matrixStack, (width - imageWidth) / 2, (height - imageHeight) / 2);
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
}
