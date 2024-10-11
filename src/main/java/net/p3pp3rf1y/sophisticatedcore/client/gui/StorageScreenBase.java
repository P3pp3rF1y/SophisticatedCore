package net.p3pp3rf1y.sophisticatedcore.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.p3pp3rf1y.sophisticatedcore.Config;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.*;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.TransferFullSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.ICraftingUIPart;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.GUI_CONTROLS;

public abstract class StorageScreenBase<S extends StorageContainerMenuBase<?>> extends AbstractContainerScreen<S>
		implements InventoryScrollPanel.IInventoryScreen {
	public static final int ERROR_BACKGROUND_COLOR = 0xF0100010;
	public static final int ERROR_BORDER_COLOR = ColorHelper.getColor(DyeColor.RED.getTextureDiffuseColors()) | 0xFF000000;
	private static final int DISABLED_SLOT_COLOR = -1072689136;
	private static final int UPGRADE_TOP_HEIGHT = 7;
	private static final int UPGRADE_SLOT_HEIGHT = 16;
	private static final int UPGRADE_BOTTOM_HEIGHT = 6;
	public static final int UPGRADE_INVENTORY_OFFSET = 21;
	public static final int DISABLED_SLOT_X_POS = -1000;
	static final int SLOTS_Y_OFFSET = 17;
	static final int SLOTS_X_OFFSET = 7;
	public static final int ERROR_SLOT_COLOR = ColorHelper.getColor(DyeColor.RED.getTextureDiffuseColors()) | 0xAA000000;
	private static final int ERROR_TEXT_COLOR = ColorHelper.getColor(DyeColor.RED.getTextureDiffuseColors());
	public static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;

	private UpgradeSettingsTabControl settingsTabControl = new UpgradeSettingsTabControl(new Position(0, 0), this, "");
	private final int numberOfUpgradeSlots;
	@Nullable
	private Button sortButton = null;
	@Nullable
	private ToggleButton<SortBy> sortByButton = null;

	private InventoryScrollPanel inventoryScrollPanel = null;
	private final Set<ToggleButton<Boolean>> upgradeSwitches = new HashSet<>();

	private final Map<Integer, UpgradeInventoryPartBase<?>> inventoryParts = new LinkedHashMap<>();

	private static ICraftingUIPart craftingUIPart = ICraftingUIPart.NOOP;
	private static ISlotDecorationRenderer slotDecorationRenderer = (guiGraphics, slot) -> {
	};

	protected StorageBackgroundProperties storageBackgroundProperties;

	public static void setCraftingUIPart(ICraftingUIPart part) {
		craftingUIPart = part;
	}

	public static void setSlotDecorationRenderer(ISlotDecorationRenderer renderer) {
		slotDecorationRenderer = renderer;
	}

	private static final Set<IButtonFactory> buttonFactories = new HashSet<>();

	public static void addButtonFactory(IButtonFactory buttonFactory) {
		buttonFactories.add(buttonFactory);
	}

	protected StorageScreenBase(S pMenu, Inventory pPlayerInventory, Component pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
		numberOfUpgradeSlots = getMenu().getNumberOfUpgradeSlots();
		updateDimensionsAndSlotPositions(Minecraft.getInstance().getWindow().getGuiScaledHeight());
	}

	public ICraftingUIPart getCraftingUIAddition() {
		return craftingUIPart;
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
		updatePlayerSlotsPositions();
		updateUpgradeSlotsPositions();
	}

	protected int getStorageInventoryHeight(int displayableNumberOfRows) {
		return displayableNumberOfRows * 18;
	}

	@Override
	public Slot getSlot(int slotIndex) {
		return getMenu().getSlot(slotIndex);
	}

	protected void updateUpgradeSlotsPositions() {
		int yPosition = 6;
		for (int slotIndex = 0; slotIndex < numberOfUpgradeSlots; slotIndex++) {
			Slot slot = getMenu().getSlot(getMenu().getFirstUpgradeSlot() + slotIndex);
			slot.y = yPosition;
			yPosition += UPGRADE_SLOT_HEIGHT;
		}
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

	protected void updatePlayerSlotsPositions() {
		int playerInventoryXOffset = storageBackgroundProperties.getPlayerInventoryXOffset();

		int yPosition = inventoryLabelY + 12;

		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				int slotIndex = j + i * 9;
				int xPosition = playerInventoryXOffset + 8 + j * 18;
				Slot slot = getMenu().getSlot(getMenu().getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + slotIndex);
				slot.x = xPosition;
				slot.y = yPosition;
			}
			yPosition += 18;
		}

		yPosition += 4;

		for (int slotIndex = 0; slotIndex < 9; ++slotIndex) {
			int xPosition = playerInventoryXOffset + 8 + slotIndex * 18;
			Slot slot = getMenu().getSlot(getMenu().getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + 3 * 9 + slotIndex);
			slot.x = xPosition;
			slot.y = yPosition;
		}
	}

	@Override
	protected void init() {
		super.init();
		updateInventoryScrollPanel();
		craftingUIPart.setStorageScreen(this);
		initUpgradeSettingsControl();
		initUpgradeInventoryParts();
		addUpgradeSwitches();
		getMenu().setUpgradeChangeListener(c -> {
			updateStorageSlotsPositions();
			updatePlayerSlotsPositions();
			updateUpgradeSlotsPositions();
			updateInventoryScrollPanel();
			children().remove(settingsTabControl);
			craftingUIPart.onCraftingSlotsHidden();
			initUpgradeSettingsControl();
			initUpgradeInventoryParts();
			addUpgradeSwitches();
		});
		if (shouldShowSortButtons()) {
			addSortButtons();
		}
		addAdditionalButtons();
	}

	protected boolean shouldShowSortButtons() {
		return true;
	}

	private void addAdditionalButtons() {
		buttonFactories.forEach(factory -> addRenderableWidget(factory.instantiateButton(this)));
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

	public int getSlotsOnLine() {
		return storageBackgroundProperties.getSlotsOnLine() - getMenu().getColumnsTaken();
	}

	private void initUpgradeInventoryParts() {
		inventoryParts.clear();
		if (getMenu().getColumnsTaken() == 0) {
			return;
		}

		int numberOfVisibleRows = getNumberOfVisibleRows();
		int scrollBarOffset = numberOfVisibleRows < getMenu().getNumberOfRows() ? 6 : 0;
		AtomicReference<Position> pos = new AtomicReference<>(new Position(SLOTS_X_OFFSET + getSlotsOnLine() * 18 + scrollBarOffset, SLOTS_Y_OFFSET));
		int height = numberOfVisibleRows * 18;
		for (Map.Entry<Integer, UpgradeContainerBase<?, ?>> entry : getMenu().getUpgradeContainers().entrySet()) {
			UpgradeContainerBase<?, ?> container = entry.getValue();
			UpgradeGuiManager.getInventoryPart(entry.getKey(), container, pos.get(), height, this).ifPresent(part -> {
				inventoryParts.put(entry.getKey(), part);
				pos.set(new Position(pos.get().x() + 36, pos.get().y()));
			});
		}
	}

	private void addUpgradeSwitches() {
		upgradeSwitches.clear();
		int switchTop = topPos + 8;
		for (int slot = 0; slot < numberOfUpgradeSlots; slot++) {
			if (menu.canDisableUpgrade(slot)) {
				int finalSlot = slot;
				ToggleButton<Boolean> upgradeSwitch = new ToggleButton<>(new Position(leftPos - 22, switchTop), ButtonDefinitions.UPGRADE_SWITCH,
						button -> getMenu().setUpgradeEnabled(finalSlot, !getMenu().getUpgradeEnabled(finalSlot)), () -> getMenu().getUpgradeEnabled(finalSlot));
				addWidget(upgradeSwitch);
				upgradeSwitches.add(upgradeSwitch);
			}
			switchTop += UPGRADE_SLOT_HEIGHT;
		}
	}

	private void addSortButtons() {
		SortButtonsPosition sortButtonsPosition = Config.CLIENT.sortButtonsPosition.get();
		if (sortButtonsPosition == SortButtonsPosition.HIDDEN) {
			return;
		}

		Position pos = getSortButtonsPosition(sortButtonsPosition);

		sortButton = new Button(new Position(pos.x(), pos.y()), ButtonDefinitions.SORT, button -> {
			if (button == 0) {
				getMenu().sort();
				//noinspection ConstantConditions - by this point player can't be null
				Minecraft.getInstance().player.displayClientMessage(Component.literal("Sorted"), true);
			}
		});
		addWidget(sortButton);
		sortByButton = new ToggleButton<>(new Position(pos.x() + 14, pos.y()), ButtonDefinitions.SORT_BY, button -> {
			if (button == 0) {
				getMenu().setSortBy(getMenu().getSortBy().next());
			}
		}, () -> getMenu().getSortBy());
		addWidget(sortByButton);

	}

	private Position getSortButtonsPosition(SortButtonsPosition sortButtonsPosition) {
		return switch (sortButtonsPosition) {
			case BELOW_UPGRADES ->
					new Position(leftPos - UPGRADE_INVENTORY_OFFSET - 2, topPos + getUpgradeHeightWithoutBottom() + UPGRADE_BOTTOM_HEIGHT + 2);
			case BELOW_UPGRADE_TABS ->
					new Position(settingsTabControl.getX() + 2, settingsTabControl.getY() + Math.max(0, settingsTabControl.getHeight() + 2));
			default -> new Position(leftPos + imageWidth - 34, topPos + 4);
		};
	}

	private void initUpgradeSettingsControl() {
		settingsTabControl = new UpgradeSettingsTabControl(new Position(leftPos + imageWidth, topPos + 4), this, getStorageSettingsTabTooltip());
		addWidget(settingsTabControl);
	}

	protected abstract String getStorageSettingsTabTooltip();

	public int getUpgradeHeight() {
		return getUpgradeHeightWithoutBottom() + UPGRADE_TOP_HEIGHT;
	}

	protected int getUpgradeHeightWithoutBottom() {
		return UPGRADE_BOTTOM_HEIGHT + numberOfUpgradeSlots * UPGRADE_SLOT_HEIGHT;
	}

	public Optional<Rect2i> getSortButtonsRectangle() {
		if (sortButton == null || sortByButton == null) {
			return Optional.empty();
		}
		return GuiHelper.getPositiveRectangle(sortButton.getX(), sortButton.getY(), sortByButton.getX() + sortByButton.getWidth() - sortButton.getX(), sortByButton.getY() + sortByButton.getHeight() - sortButton.getY());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		if (menu.detectSettingsChangeAndReload()) {
			updateStorageSlotsPositions();
			updatePlayerSlotsPositions();
			updateInventoryScrollPanel();
		}
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, -20);
		renderBackground(guiGraphics);
		poseStack.popPose();
		settingsTabControl.render(guiGraphics, mouseX, mouseY, partialTicks);

		renderSuper(guiGraphics, mouseX, mouseY, partialTicks);

		settingsTabControl.renderTooltip(this, guiGraphics, mouseX, mouseY);
		if (sortButton != null && sortByButton != null) {
			sortButton.render(guiGraphics, mouseX, mouseY, partialTicks);
			sortByButton.render(guiGraphics, mouseX, mouseY, partialTicks);
		}
		upgradeSwitches.forEach(us -> us.render(guiGraphics, mouseX, mouseY, partialTicks));
		renderErrorOverlay(guiGraphics);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@SuppressWarnings("java:S4449")
	//renderFloatingItem should really have altText as nullable as it is then only passed to nullable parameter
	private void renderSuper(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) { //copy of super.render with storage inventory slots rendering and snap rendering removed
		int i = leftPos;
		int j = topPos;
		renderBg(guiGraphics, pPartialTick, pMouseX, pMouseY);
		//noinspection UnstableApiUsage
		MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ContainerScreenEvent.Render.Background(this, guiGraphics, pMouseX, pMouseY));
		RenderSystem.disableDepthTest();

		hoveredSlot = null;

		for (Renderable widget : renderables) {
			widget.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
		}

		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(i, j, 0.0D);

		for (int k = 0; k < StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS; ++k) {
			Slot slot = getMenu().getSlot(getMenu().getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + k);
			if (slot.isActive()) {
				renderSlot(guiGraphics, slot);
			}

			if (isHovering(slot, pMouseX, pMouseY) && slot.isActive()) {
				hoveredSlot = slot;
				int l = slot.x;
				int i1 = slot.y;
				renderSlotHighlight(guiGraphics, l, i1, 0, getSlotColor(k));
			}
		}

		renderLabels(guiGraphics, pMouseX, pMouseY);
		//noinspection UnstableApiUsage
		MinecraftForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Foreground(this, guiGraphics, pMouseX, pMouseY));
		ItemStack itemstack = draggingItem.isEmpty() ? menu.getCarried() : draggingItem;
		if (!itemstack.isEmpty()) {
			int i2 = draggingItem.isEmpty() ? 8 : 16;
			String s = null;
			if (!draggingItem.isEmpty() && isSplittingStack) {
				itemstack = itemstack.copy();
				itemstack.setCount(Mth.ceil(itemstack.getCount() / 2.0F));
			} else if (isQuickCrafting && quickCraftSlots.size() > 1) {
				itemstack = itemstack.copy();
				itemstack.setCount(quickCraftingRemainder);
				if (itemstack.isEmpty()) {
					s = ChatFormatting.YELLOW + "0";
				}
			}

			//noinspection ConstantConditions - renderFloatingItem should really have altText as nullable as it is then only passed to nullable parameter
			renderFloatingItem(guiGraphics, itemstack, pMouseX - i - 8, pMouseY - j - i2, s);
		}

		poseStack.popPose();
		RenderSystem.enableDepthTest();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderLabels(guiGraphics, mouseX, mouseY);
		renderUpgradeInventoryParts(guiGraphics, mouseX, mouseY);
		renderUpgradeSlots(guiGraphics, mouseX, mouseY);
		if (inventoryScrollPanel == null) {
			renderStorageInventorySlots(guiGraphics, mouseX, mouseY);
		}
	}

	private void renderUpgradeInventoryParts(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		inventoryParts.values().forEach(ip -> ip.render(guiGraphics, mouseX, mouseY));
	}

	private void renderStorageInventorySlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		renderStorageInventorySlots(guiGraphics, mouseX, mouseY, true);
	}

	private void renderStorageInventorySlots(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean canShowHover) {
		for (int slotId = 0; slotId < menu.realInventorySlots.size(); ++slotId) {
			Slot slot = menu.realInventorySlots.get(slotId);
			renderSlot(guiGraphics, slot);

			if (canShowHover && isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				hoveredSlot = slot;
				renderSlotHighlight(guiGraphics, slot.x, slot.y, 0, getSlotColor(slotId));
			}
		}
	}

	private void renderUpgradeSlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		for (int slotId = 0; slotId < menu.upgradeSlots.size(); ++slotId) {
			Slot slot = menu.upgradeSlots.get(slotId);
			if (slot.x != DISABLED_SLOT_X_POS) {
				renderSlot(guiGraphics, slot);
				if (!slot.isActive()) {
					renderSlotOverlay(guiGraphics, slot, DISABLED_SLOT_COLOR);
				}
			}

			if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				hoveredSlot = slot;
				renderSlotHighlight(guiGraphics, slot.x, slot.y, 0, getSlotColor(slotId));
			}
		}
	}

	@Override
	protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
		int i = slot.x;
		int j = slot.y;
		ItemStack stackToRender = slot.getItem();
		boolean flag = false;
		boolean rightClickDragging = slot == clickedSlot && !draggingItem.isEmpty() && !isSplittingStack;
		ItemStack carriedStack = getMenu().getCarried();
		String stackCountText = null;
		if (slot == clickedSlot && !draggingItem.isEmpty() && isSplittingStack && !stackToRender.isEmpty()) {
			stackToRender = stackToRender.copy();
			stackToRender.setCount(stackToRender.getCount() / 2);
		} else if (isQuickCrafting && quickCraftSlots.contains(slot) && !carriedStack.isEmpty()) {
			if (quickCraftSlots.size() == 1) {
				return;
			}

			if (StorageContainerMenuBase.canItemQuickReplace(slot, carriedStack) && menu.canDragTo(slot)) {
				flag = true;
				int slotStackCount = stackToRender.isEmpty() ? 0 : stackToRender.getCount();
				int renderCount = AbstractContainerMenu.getQuickCraftPlaceCount(quickCraftSlots, quickCraftingType, carriedStack) + slotStackCount;
				int slotLimit = slot.getMaxStackSize(stackToRender);
				if (renderCount > slotLimit) {
					stackCountText = ChatFormatting.YELLOW + CountAbbreviator.abbreviate(slotLimit);
				}
				stackToRender = carriedStack.copyWithCount(renderCount);
			} else {
				quickCraftSlots.remove(slot);
				recalculateQuickCraftRemaining();
			}
		}
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 100);
		if (stackToRender.isEmpty() && slot.isActive()) {
			renderSlotBackground(guiGraphics, slot, i, j);
		} else if (!rightClickDragging) {
			renderStack(guiGraphics, i, j, stackToRender, flag, stackCountText);
			slotDecorationRenderer.renderDecoration(guiGraphics, slot);
		}
		poseStack.popPose();
	}

	private void renderStack(GuiGraphics guiGraphics, int x, int y, ItemStack itemstack, boolean flag, @Nullable String stackCountText) {
		if (flag) {
			guiGraphics.fill(x, y, x + 16, y + 16, -2130706433);
		}

		RenderSystem.enableDepthTest();
		guiGraphics.renderItem(itemstack, x, y);
		if (shouldUseSpecialCountRender(itemstack)) {
			guiGraphics.renderItemDecorations(font, itemstack, x, y, "");
			if (stackCountText == null) {
				stackCountText = CountAbbreviator.abbreviate(itemstack.getCount());
			}
			renderStackCount(guiGraphics, stackCountText, x, y);
		} else {
			guiGraphics.renderItemDecorations(font, itemstack, x, y, stackCountText);
		}
	}

	private void renderSlotBackground(GuiGraphics guiGraphics, Slot slot, int i, int j) {
		Optional<ItemStack> memorizedStack = getMenu().getMemorizedStackInSlot(slot.index);
		if (memorizedStack.isPresent()) {
			guiGraphics.renderItem(memorizedStack.get(), i, j);
			drawStackOverlay(guiGraphics, i, j);
		} else if (!getMenu().getSlotFilterItem(slot.index).isEmpty()) {
			guiGraphics.renderItem(getMenu().getSlotFilterItem(slot.index), i, j);
			drawStackOverlay(guiGraphics, i, j);
		} else {
			Pair<ResourceLocation, ResourceLocation> pair = slot.getNoItemIcon();
			if (pair != null) {
				//noinspection ConstantConditions - by this point minecraft isn't null
				TextureAtlasSprite textureatlassprite = minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
				guiGraphics.blit(i, j, 0, 16, 16, textureatlassprite);
			}
		}
	}

	private void drawStackOverlay(GuiGraphics guiGraphics, int x, int y) {
		guiGraphics.pose().pushPose();
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		guiGraphics.blit(GuiHelper.GUI_CONTROLS, x, y, 77, 0, 16, 16);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		guiGraphics.pose().popPose();
	}

	private boolean shouldUseSpecialCountRender(ItemStack itemstack) {
		return itemstack.getCount() > 99;
	}

	private void renderSlotOverlay(GuiGraphics guiGraphics, Slot slot, int slotColor) {
		renderSlotOverlay(guiGraphics, slot, slotColor, 0, 16);
	}

	private void renderSlotOverlay(GuiGraphics guiGraphics, Slot slot, int slotColor, int yOffset, int height) {
		renderOverlay(guiGraphics, slotColor, slot.x, slot.y + yOffset, 16, height);
	}

	public void renderOverlay(GuiGraphics guiGraphics, int slotColor, int xPos, int yPos, int width, int height) {
		RenderSystem.disableDepthTest();
		RenderSystem.colorMask(true, true, true, false);
		guiGraphics.fillGradient(xPos, yPos, xPos + width, yPos + height, 0, slotColor, slotColor);
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.enableDepthTest();
	}

	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		drawInventoryBg(guiGraphics, x, y, storageBackgroundProperties.getTextureName());
		if (inventoryScrollPanel == null) {
			drawSlotBg(guiGraphics, x, y, getMenu().getNumberOfStorageInventorySlots());
			drawSlotOverlays(guiGraphics);
		}
		drawUpgradeBackground(guiGraphics);
	}

	protected void drawSlotBg(GuiGraphics guiGraphics, int x, int y, int visibleSlotsCount) {
		int slotsOnLine = getSlotsOnLine();
		int slotRows = visibleSlotsCount / slotsOnLine;
		int remainingSlots = visibleSlotsCount % slotsOnLine;
		GuiHelper.renderSlotsBackground(guiGraphics, x + StorageScreenBase.SLOTS_X_OFFSET, y + StorageScreenBase.SLOTS_Y_OFFSET, slotsOnLine, slotRows, remainingSlots);
	}

	private void drawSlotOverlays(GuiGraphics guiGraphics) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(getGuiLeft(), getGuiTop(), 0.0F);
		for (int slotNumber = 0; slotNumber < menu.getNumberOfStorageInventorySlots(); slotNumber++) {
			List<Integer> colors = menu.getSlotOverlayColors(slotNumber);
			if (!colors.isEmpty()) {
				int stripeHeight = 16 / colors.size();
				int i = 0;
				for (int slotColor : colors) {
					int yOffset = i * stripeHeight;
					renderSlotOverlay(guiGraphics, menu.getSlot(slotNumber), slotColor | (80 << 24), yOffset, i == colors.size() - 1 ? 16 - yOffset : stripeHeight);
					i++;
				}
			}
		}
		poseStack.popPose();
	}

	@Override
	protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
		inventoryParts.values().forEach(part -> part.renderTooltip(this, guiGraphics, x, y));
		if (getMenu().getCarried().isEmpty() && hoveredSlot != null) {
			if (hoveredSlot.hasItem()) {
				super.renderTooltip(guiGraphics, x, y);
			} else if (hoveredSlot instanceof INameableEmptySlot emptySlot && emptySlot.hasEmptyTooltip()) {
				guiGraphics.renderComponentTooltip(font, Collections.singletonList(emptySlot.getEmptyTooltip()), x, y);
			}
		}
		if (sortButton != null) {
			sortButton.renderTooltip(this, guiGraphics, x, y);
		}
		if (sortByButton != null) {
			sortByButton.renderTooltip(this, guiGraphics, x, y);
		}
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
		List<Component> ret = getTooltipFromItem(minecraft, itemStack);
		if (hoveredSlot != null && hoveredSlot.getMaxStackSize() > 64) {
			ret.add(Component.translatable("gui.sophisticatedcore.tooltip.stack_count",
							Component.literal(NumberFormat.getNumberInstance().format(itemStack.getCount())).withStyle(ChatFormatting.DARK_AQUA)
									.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
									.append(Component.literal(NumberFormat.getNumberInstance().format(hoveredSlot.getMaxStackSize(itemStack))).withStyle(ChatFormatting.DARK_AQUA)))
					.withStyle(ChatFormatting.GRAY)
			);
		}
		return ret;
	}

	public void drawInventoryBg(GuiGraphics guiGraphics, int x, int y, ResourceLocation textureName) {
		StorageGuiHelper.renderStorageBackground(new Position(x, y), guiGraphics, textureName, imageWidth, imageHeight - HEIGHT_WITHOUT_STORAGE_SLOTS);
	}

	private void drawUpgradeBackground(GuiGraphics guiGraphics) {
		if (numberOfUpgradeSlots == 0) {
			return;
		}

		int heightWithoutBottom = getUpgradeHeightWithoutBottom();

		guiGraphics.blit(GUI_CONTROLS, leftPos - UPGRADE_INVENTORY_OFFSET, topPos, 0, 0, 26, 4, 256, 256);
		guiGraphics.blit(GUI_CONTROLS, leftPos - UPGRADE_INVENTORY_OFFSET, topPos + 4, 0, 4, 25, heightWithoutBottom - 4, 256, 256);
		guiGraphics.blit(GUI_CONTROLS, leftPos - UPGRADE_INVENTORY_OFFSET, topPos + heightWithoutBottom, 0, 198, 25, UPGRADE_BOTTOM_HEIGHT, 256, 256);

		boolean previousHasSwitch = false;
		for (int slot = 0; slot < numberOfUpgradeSlots; slot++) {
			if (menu.canDisableUpgrade(slot)) {
				int y = topPos + 5 + slot * UPGRADE_SLOT_HEIGHT + (previousHasSwitch ? 1 : 0);

				guiGraphics.blit(GUI_CONTROLS, leftPos - UPGRADE_INVENTORY_OFFSET - 4, y, 0, 204 + (previousHasSwitch ? 1 : 0), 7, 18 - (previousHasSwitch ? 1 : 0), 256, 256);
				previousHasSwitch = true;
			} else {
				previousHasSwitch = false;
			}
		}
	}

	public UpgradeSettingsTabControl getUpgradeSettingsControl() {
		return settingsTabControl;
	}

	@Nullable
	@Override
	public Slot findSlot(double mouseX, double mouseY) {
		for (int i = 0; i < menu.upgradeSlots.size(); ++i) {
			Slot slot = menu.upgradeSlots.get(i);
			if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				return slot;
			}
		}

		if (inventoryScrollPanel != null) {
			Optional<Slot> result = inventoryScrollPanel.findSlot(mouseX, mouseY);
			if (result.isPresent()) {
				return result.get();
			}
			Slot slot = super.findSlot(mouseX, mouseY);

			return slot == null || menu.isStorageInventorySlot(slot.index) ? null : slot; //if super finds inventory slot that's hidden inside the scroll panel just return null
		} else {
			for (int i = 0; i < menu.realInventorySlots.size(); ++i) {
				Slot slot = menu.realInventorySlots.get(i);
				if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
					return slot;
				}
			}
			return super.findSlot(mouseX, mouseY);
		}
	}



	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		for (UpgradeInventoryPartBase<?> inventoryPart : inventoryParts.values()) {
			if (inventoryPart.handleMouseReleased(mouseX, mouseY, button)) {
				return true;
			}
		}

		handleQuickMoveAll(mouseX, mouseY, button);

		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void handleQuickMoveAll(double mouseX, double mouseY, int button) {
		Slot slot = findSlot(mouseX, mouseY);
		if (doubleclick && !getMenu().getCarried().isEmpty() && slot != null && button == 0 && menu.canTakeItemForPickAll(ItemStack.EMPTY, slot) && hasShiftDown() && !lastQuickMoved.isEmpty()) {
			for (Slot slot2 : menu.realInventorySlots) {
				tryQuickMoveSlot(button, slot, slot2);
			}
		}
	}

	private void tryQuickMoveSlot(int button, Slot slot, Slot slot2) {
		//noinspection ConstantConditions - by this point minecraft isn't null
		if (slot2.mayPickup(minecraft.player) && slot2.hasItem() && slot2.isSameInventory(slot)) {
			ItemStack slotItem = slot2.getItem();
			if (ItemStack.isSameItemSameTags(lastQuickMoved, slotItem)) {
				if (slotItem.getCount() > slotItem.getMaxStackSize()) {
					PacketHandler.INSTANCE.sendToServer(new TransferFullSlotMessage(slot2.index));
				} else {
					slotClicked(slot2, slot2.index, button, ClickType.QUICK_MOVE);
				}
			}
		}
	}

	@Override
	protected void slotClicked(Slot slot, int slotNumber, int mouseButton, ClickType type) {
		if (type == ClickType.PICKUP_ALL && !menu.getSlotUpgradeContainer(slot).map(c -> c.allowsPickupAll(slot)).orElse(true)) {
			type = ClickType.PICKUP;
		}

		handleInventoryMouseClick(slotNumber, mouseButton, type);
	}

	private void handleInventoryMouseClick(int slotNumber, int mouseButton, ClickType type) {
		StorageContainerMenuBase<?> menu = getMenu();
		List<ItemStack> realInventoryItems = new ArrayList<>(menu.realInventorySlots.size());
		menu.realInventorySlots.forEach(slot -> realInventoryItems.add(slot.getItem().copy()));
		List<ItemStack> upgradeItems = new ArrayList<>(menu.upgradeSlots.size());
		menu.upgradeSlots.forEach(slot -> upgradeItems.add(slot.getItem().copy()));

		//noinspection ConstantConditions - by this point minecraft isn't null
		menu.clicked(slotNumber, mouseButton, type, minecraft.player);
		Int2ObjectMap<ItemStack> changedSlotIndexes = new Int2ObjectOpenHashMap<>();

		int inventorySlotsToCheck = Math.min(realInventoryItems.size() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS, menu.getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS);

		for (int i = 0; i < inventorySlotsToCheck; i++) {
			ItemStack itemstack = realInventoryItems.get(i);
			ItemStack slotStack = menu.getSlot(i).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				changedSlotIndexes.put(i, slotStack.copy());
			}
		}

		for (int i = 0; i < StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS; i++) {
			ItemStack itemstack = realInventoryItems.get(realInventoryItems.size() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + i);
			int slotIndex = menu.getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + i;
			ItemStack slotStack = menu.getSlot(slotIndex).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				changedSlotIndexes.put(slotIndex, slotStack.copy());
			}
		}

		int lastChecked = 0;
		int upgradeSlotsToCheck = Math.min(menu.getUpgradeSlotsSize(), upgradeItems.size());

		for (; lastChecked < upgradeSlotsToCheck; lastChecked++) {
			ItemStack itemstack = upgradeItems.get(lastChecked);
			ItemStack slotStack = menu.getSlot(menu.getInventorySlotsSize() + lastChecked).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				break;
			}
		}

		for (int i = upgradeSlotsToCheck - 1; i >= lastChecked; i--) {
			ItemStack itemstack = upgradeItems.get(i);
			int slotIndex = menu.getInventorySlotsSize() + i;
			ItemStack slotStack = menu.getSlot(slotIndex).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				changedSlotIndexes.put(slotIndex, slotStack.copy());
			}
		}

		minecraft.player.connection.send(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), slotNumber, mouseButton, type, menu.getCarried().copy(), changedSlotIndexes));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		Slot slot = findSlot(mouseX, mouseY);
		if (hasShiftDown() && hasControlDown() && slot instanceof StorageInventorySlot && button == 0) {
			PacketHandler.INSTANCE.sendToServer(new TransferFullSlotMessage(slot.index));
			return true;
		}
		GuiEventListener focused = getFocused();
		if (focused != null && !focused.isMouseOver(mouseX, mouseY) && (focused instanceof WidgetBase widgetBase)) {
			widgetBase.setFocused(false);
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		for (GuiEventListener child : children()) {
			if (child.isMouseOver(mouseX, mouseY) && child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
				return true;
			}
		}
		Slot slot = findSlot(mouseX, mouseY);
		ItemStack itemstack = getMenu().getCarried();
		if (isQuickCrafting) {
			if (slot != null && !itemstack.isEmpty()
					&& (itemstack.getCount() > quickCraftSlots.size() || quickCraftingType == 2)
					&& StorageContainerMenuBase.canItemQuickReplace(slot, itemstack) && slot.mayPlace(itemstack)
					&& menu.canDragTo(slot)
					&& isAllowedSlotCombination(slot, itemstack)) {
				quickCraftSlots.add(slot);
				recalculateQuickCraftRemaining();
			}
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	private boolean isAllowedSlotCombination(Slot slot, ItemStack carried) {
		if (quickCraftSlots.isEmpty() || !(carried.getItem() instanceof UpgradeItemBase<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
			return true;
		}
		return quickCraftSlots.contains(slot) || (!(quickCraftSlots.iterator().next() instanceof StorageContainerMenuBase.StorageUpgradeSlot) && !(slot instanceof StorageContainerMenuBase.StorageUpgradeSlot));
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
		return super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn, mouseButton) && hasClickedOutsideOfUpgradeSlots(mouseX, mouseY)
				&& hasClickedOutsideOfUpgradeSettings(mouseX, mouseY);
	}

	private boolean hasClickedOutsideOfUpgradeSettings(double mouseX, double mouseY) {
		return settingsTabControl.getTabRectangles().stream().noneMatch(r -> r.contains((int) mouseX, (int) mouseY));
	}

	private boolean hasClickedOutsideOfUpgradeSlots(double mouseX, double mouseY) {
		return !getUpgradeSlotsRectangle().map(r -> r.contains((int) mouseX, (int) mouseY)).orElse(false);
	}

	public Optional<Rect2i> getUpgradeSlotsRectangle() {
		return numberOfUpgradeSlots == 0 ? Optional.empty() : GuiHelper.getPositiveRectangle(leftPos - UPGRADE_INVENTORY_OFFSET + 4, topPos, UPGRADE_INVENTORY_OFFSET + 4, getUpgradeHeight());
	}

	private void renderStackCount(GuiGraphics guiGraphics, String count, int x, int y) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, 200.0F);
		float scale = Math.min(1f, (float) 16 / font.width(count));
		if (scale < 1f) {
			poseStack.scale(scale, scale, 1.0F);
		}
		MultiBufferSource.BufferSource renderBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		font.drawInBatch(count, (x + 19 - 2 - (font.width(count) * scale)) / scale,
				(y + 6 + 3 + (1 / (scale * scale) - 1)) / scale, 16777215, true, poseStack.last().pose(), renderBuffer, Font.DisplayMode.NORMAL, 0, 15728880);
		renderBuffer.endBatch();
		poseStack.popPose();
	}

	@Override
	protected void recalculateQuickCraftRemaining() {
		ItemStack carriedStack = getMenu().getCarried();
		if (!carriedStack.isEmpty() && isQuickCrafting) {
			if (quickCraftingType == 2) {
				quickCraftingRemainder = carriedStack.getMaxStackSize();
			} else {
				quickCraftingRemainder = carriedStack.getCount();

				for (Slot slot : quickCraftSlots) {
					ItemStack slotStack = slot.getItem();
					int slotStackCount = slotStack.isEmpty() ? 0 : slotStack.getCount();
					int maxStackSize = slot.getMaxStackSize(carriedStack);
					int quickCraftPlaceCount = Math.min(AbstractContainerMenu.getQuickCraftPlaceCount(quickCraftSlots, quickCraftingType, carriedStack) + slotStackCount, maxStackSize);
					quickCraftingRemainder -= quickCraftPlaceCount - slotStackCount;
				}
			}
		}
	}

	private void renderErrorOverlay(GuiGraphics guiGraphics) {
		menu.getErrorUpgradeSlotChangeResult().ifPresent(upgradeSlotChangeResult -> upgradeSlotChangeResult.getErrorMessage().ifPresent(overlayErrorMessage -> {
			RenderSystem.disableDepthTest();
			PoseStack poseStack = guiGraphics.pose();
			poseStack.pushPose();
			poseStack.translate(getGuiLeft(), getGuiTop(), 0.0F);
			upgradeSlotChangeResult.getErrorUpgradeSlots().forEach(slotIndex -> {
				Slot upgradeSlot = menu.getSlot(menu.getFirstUpgradeSlot() + slotIndex);
				renderSlotHighlight(guiGraphics, upgradeSlot.x, upgradeSlot.y, 0, ERROR_SLOT_COLOR);
			});
			upgradeSlotChangeResult.getErrorInventorySlots().forEach(slotIndex -> {
				Slot slot = menu.getSlot(slotIndex);
				//noinspection ConstantConditions
				if (slot != null) {
					renderSlotHighlight(guiGraphics, slot.x, slot.y, 0, ERROR_SLOT_COLOR);
				}
			});
			upgradeSlotChangeResult.getErrorInventoryParts().forEach(partIndex -> {
				if (inventoryParts.size() > partIndex) {
					UpgradeInventoryPartBase<?> inventoryPart = inventoryParts.get(partIndex);
					if (inventoryPart != null) {
						inventoryPart.renderErrorOverlay(guiGraphics);
					}
				}
			});
			poseStack.popPose();

			renderErrorMessage(poseStack, overlayErrorMessage);
		}));
	}

	private void renderErrorMessage(PoseStack matrixStack, Component overlayErrorMessage) {
		matrixStack.pushPose();
		matrixStack.translate((float) width / 2, (double) topPos + inventoryLabelY + 4, 300F);
		Font fontrenderer = Minecraft.getInstance().font;

		int tooltipWidth = font.width(overlayErrorMessage);

		List<FormattedText> wrappedTextLines = new ArrayList<>();
		int maxLineWidth = 260;
		if (tooltipWidth > maxLineWidth) {
			int wrappedTooltipWidth = 0;
			List<FormattedText> wrappedLine = font.getSplitter().splitLines(overlayErrorMessage, maxLineWidth, Style.EMPTY);

			for (FormattedText line : wrappedLine) {
				int lineWidth = font.width(line);
				if (lineWidth > wrappedTooltipWidth) {
					wrappedTooltipWidth = lineWidth;
				}
				wrappedTextLines.add(line);
			}
			tooltipWidth = wrappedTooltipWidth;
		} else {
			wrappedTextLines.add(overlayErrorMessage);
		}

		int tooltipHeight = 8;
		if (wrappedTextLines.size() > 1) {
			tooltipHeight += 2 + (wrappedTextLines.size() - 1) * 10;
		}

		Matrix4f matrix4f = matrixStack.last().pose();
		float leftX = (float) -tooltipWidth / 2;

		GuiHelper.renderTooltipBackground(matrix4f, tooltipWidth, (int) leftX, 0, tooltipHeight, StorageScreenBase.ERROR_BACKGROUND_COLOR, StorageScreenBase.ERROR_BORDER_COLOR, StorageScreenBase.ERROR_BORDER_COLOR);
		MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		matrixStack.translate(0.0D, 0.0D, 400.0D);
		GuiHelper.writeTooltipLines(wrappedTextLines, fontrenderer, leftX, 0, matrix4f, renderTypeBuffer, ERROR_TEXT_COLOR);
		renderTypeBuffer.endBatch();
		matrixStack.popPose();
	}

	@Override
	public void renderInventorySlots(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean canShowHover) {
		renderStorageInventorySlots(guiGraphics, mouseX, mouseY, canShowHover);
	}

	@Override
	public boolean isMouseOverSlot(Slot pSlot, double pMouseX, double pMouseY) {
		return isHovering(pSlot, pMouseX, pMouseY);
	}

	@Override
	protected boolean isHovering(Slot slot, double mouseX, double mouseY) {
		return super.isHovering(slot, mouseX, mouseY) && getUpgradeSettingsControl().slotIsNotCoveredAt(slot, mouseX, mouseY);
	}

	@Override
	public int getTopY() {
		return getGuiTop();
	}

	@Override
	public void drawSlotBg(GuiGraphics guiGraphics, int visibleSlotsCount) {
		drawSlotBg(guiGraphics, (width - imageWidth) / 2, (height - imageHeight) / 2, visibleSlotsCount);
		drawSlotOverlays(guiGraphics);
	}

	@Override
	public int getLeftX() {
		return getGuiLeft();
	}

	public Position getRightTopAbovePlayersInventory() {
		return new Position(storageBackgroundProperties.getPlayerInventoryXOffset() + 8 + 9 * 18, inventoryLabelY);
	}
}
