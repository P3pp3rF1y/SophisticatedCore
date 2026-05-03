package net.p3pp3rf1y.sophisticatedcore.client.gui;

import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.Config;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.*;
import net.p3pp3rf1y.sophisticatedcore.network.TransferFullSlotPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.ICraftingUIPart;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.GUI_CONTROLS;

public abstract class StorageScreenBase<S extends StorageContainerMenuBase<?>> extends AbstractContainerScreen<S>
		implements InventoryScrollPanel.IInventoryScreen {
	private static final int UPGRADE_TOP_HEIGHT = 7;
	private static final int UPGRADE_SLOT_HEIGHT = 16;
	private static final int UPGRADE_BOTTOM_HEIGHT = 6;
	public static final int UPGRADE_INVENTORY_OFFSET = 21;
	public static final int DISABLED_SLOT_X_POS = -2000;
	static final int SLOTS_Y_OFFSET = 17;
	static final int SLOTS_X_OFFSET = 7;
	public static final int ERROR_SLOT_COLOR = (DyeColor.RED.getTextureDiffuseColor() & 0x00_FFFFFF) | 0xAA000000;
	private static final int ERROR_TEXT_COLOR = DyeColor.RED.getTextureDiffuseColor();
	public static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;

	private UpgradeSettingsTabControl settingsTabControl;
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
	@Nullable
	private Button transferToStorageButton;
	@Nullable
	private Button transferToInventoryButton;
	private int transferButtonsShiftX = 0;
	private TextBox searchBox;
	private Label noResultsLabel;
	@Nullable
	private WidgetBase modalOverlay;
	private Predicate<ItemStack> stackFilter = stack -> searchBox == null || searchBox.getValue().isEmpty()
			|| (!stack.isEmpty() && stack.getHoverName().getString().toLowerCase().contains(searchBox.getValue().toLowerCase()));
	private int visibleSlotsCount;
	private boolean initializing = true;

	public static void setCraftingUIPart(ICraftingUIPart part) {
		craftingUIPart = part;
	}

	public static void setSlotDecorationRenderer(ISlotDecorationRenderer renderer) {
		slotDecorationRenderer = renderer;
	}

	protected StorageScreenBase(S menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		numberOfUpgradeSlots = getMenu().getNumberOfUpgradeSlots();
		visibleSlotsCount = getMenu().getNumberOfStorageInventorySlots();
		updateDimensionsAndSlotPositions(Minecraft.getInstance().getWindow().getGuiScaledHeight());
	}

	public ICraftingUIPart getCraftingUIAddition() {
		return craftingUIPart;
	}

	@Override
	public void resize(int width, int height) {
		updateDimensionsAndSlotPositions(height);
		super.resize(width, height);
		centerModalOverlay();
	}

	private void updateDimensionsAndSlotPositions(int height) {
		int displayableNumberOfRows = Math.min((height - HEIGHT_WITHOUT_STORAGE_SLOTS) / 18, getMenu().getNumberOfRows());
		int newImageHeight = HEIGHT_WITHOUT_STORAGE_SLOTS + getStorageInventoryHeight(displayableNumberOfRows);
		storageBackgroundProperties = (getMenu().getNumberOfStorageInventorySlots() + getMenu().getColumnsTaken() * getMenu().getNumberOfRows()) <= 81 ? StorageBackgroundProperties.REGULAR_9_SLOT : StorageBackgroundProperties.REGULAR_12_SLOT;

		imageWidth = storageBackgroundProperties.getSlotsOnLine() * 18 + 14;
		updateStorageSlotsPositions();
		updateNoResultsLabel();
		if (displayableNumberOfRows < getMenu().getNumberOfRows()) {
			storageBackgroundProperties = storageBackgroundProperties == StorageBackgroundProperties.REGULAR_9_SLOT ? StorageBackgroundProperties.WIDER_9_SLOT : StorageBackgroundProperties.WIDER_12_SLOT;
			imageWidth += 6;
		}
		imageHeight = newImageHeight;
		inventoryLabelY = imageHeight - 94;
		inventoryLabelX = 8 + storageBackgroundProperties.getPlayerInventoryXOffset();
		updatePlayerSlotsPositions();
		updateExtraSlotsPositions();
		updateUpgradeSlotsPositions();
		updateTransferButtonsPositions();
	}

	public int getInventoryLabelX() {
		return inventoryLabelX;
	}

	protected void updateExtraSlotsPositions() {
		//noop by default
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

	private void updateNoResultsLabel() {
		if (noResultsLabel != null) {
			if (visibleSlotsCount == 0) {
				if (!renderables.contains(noResultsLabel)) {
					addRenderableWidget(noResultsLabel);
				}
			} else {
				removeWidget(noResultsLabel);
			}
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

	protected void updateStorageSlotsPositions() {
		int yPosition = 18;

		visibleSlotsCount = 0;
		int slotIndex = 0;
		while (slotIndex < getMenu().getNumberOfStorageInventorySlots()) {
			Slot slot = getMenu().getSlot(slotIndex);
			int lineIndex = visibleSlotsCount % getSlotsOnLine();
			slotIndex++;

			if (stackFilter.test(slot.getItem())) {
				slot.x = 8 + lineIndex * 18;
				slot.y = yPosition;
				visibleSlotsCount++;
				if (visibleSlotsCount % getSlotsOnLine() == 0) {
					yPosition += 18;
				}
			} else {
				slot.x = DISABLED_SLOT_X_POS;
			}
		}
	}

	@Override
	public Predicate<ItemStack> getStackFilter() {
		return stackFilter;
	}

	protected void updatePlayerSlotsPositions() {
		int playerInventoryXOffset = storageBackgroundProperties.getPlayerInventoryXOffset();

		int yPosition = inventoryLabelY + 12;

		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				int slotIndex = j + i * 9;
				int xPosition = playerInventoryXOffset + 8 + j * 18;
				Slot slot = getMenu().getSlot(getMenu().getInventorySlotsSize() - getMenu().getExtraSlots().size() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + slotIndex);
				slot.x = xPosition;
				slot.y = yPosition;
			}
			yPosition += 18;
		}

		yPosition += 4;

		for (int slotIndex = 0; slotIndex < 9; ++slotIndex) {
			int xPosition = playerInventoryXOffset + 8 + slotIndex * 18;
			Slot slot = getMenu().getSlot(getMenu().getInventorySlotsSize() - getMenu().getExtraSlots().size() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + 3 * 9 + slotIndex);
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
			updateExtraSlotsPositions();
			updateUpgradeSlotsPositions();
			updateInventoryScrollPanel();
			updateNoResultsLabel();
			children().remove(settingsTabControl);
			craftingUIPart.onCraftingSlotsHidden();
			initUpgradeSettingsControl();
			initUpgradeInventoryParts();
			addUpgradeSwitches();
		});
		if (shouldShowSortButtons()) {
			addSortButtons();
		}

		addTransferButtons();
		addSearchBox();

		initializing = false;
	}

	protected void addSearchBox() {
		SortButtonsPosition sortButtonsPosition = Config.CLIENT.sortButtonsPosition.get();
		int x = 7;
		int xEnd = sortButtonsPosition == SortButtonsPosition.TITLE_LINE_RIGHT ? getSortButtonsPosition(sortButtonsPosition).x() - 1 - leftPos : imageWidth - 7;
		int width = xEnd - x;

		searchBox = new SearchBox(new Position(leftPos + x, topPos + 5), new Dimension(width, 10), this);
		searchBox.setResponder(this::onSearchPhraseChange);
		if (getMenu().shouldKeepSearchPhrase()) {
			searchBox.setValue(getMenu().getSearchPhrase());
		}
		addWidget(searchBox);

		if (noResultsLabel != null) {
			removeWidget(noResultsLabel);
		}
		noResultsLabel = new Label(new Position(leftPos + 7, topPos + 18), Component.translatable(TranslationHelper.INSTANCE.translGui("label.no_search_results")));
		if (visibleSlotsCount == 0) {
			addRenderableWidget(noResultsLabel);
		}
	}

	private void onSearchPhraseChange(String searchPhrase) {
		if (!initializing) {
			getMenu().setSearchPhrase(searchPhrase);
		}
		updateSearchFilter(searchPhrase);
		if (inventoryScrollPanel != null) {
			inventoryScrollPanel.resetScrollDistance();
			inventoryScrollPanel.updateSlotsPosition();
		} else {
			updateStorageSlotsPositions();
		}
		updateNoResultsLabel();
	}

	private void updateSearchFilter(String searchPhrase) {
		if (searchPhrase.trim().isEmpty()) {
			stackFilter = stack -> true;
			return;
		}

		String[] searchTerms = searchPhrase.trim().split(" ");

		List<Predicate<ItemStack>> filters = new ArrayList<>();

		for (String searchTerm : searchTerms) {
			if (searchTerm.startsWith("@")) {
				String modName = searchTerm.substring(1).toLowerCase();
				filters.add(stack -> modName.isEmpty() || BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().contains(modName));
			} else if (searchTerm.startsWith("#")) {
				String tooltipKeyword = searchTerm.substring(1).toLowerCase();
				filters.add(stack -> getTooltipFromItem(minecraft, stack).stream().anyMatch(line -> line.getString().toLowerCase().contains(tooltipKeyword)));
			} else {
				filters.add(stack -> stack.getHoverName().getString().toLowerCase().contains(searchTerm.toLowerCase()));
			}
		}

		stackFilter = stack -> !stack.isEmpty() && filters.stream().allMatch(f -> f.test(stack));
	}

	private void addTransferButtons() {
		transferToStorageButton = new TransferButton(filterByContents -> getMenu().transferItemsToStorage(filterByContents), ButtonDefinitions.TRANSFER_TO_STORAGE, ButtonDefinitions.TRANSFER_TO_STORAGE_FILTERED);
		addRenderableWidget(transferToStorageButton);

		transferToInventoryButton = new TransferButton(filterByContents -> getMenu().transferItemsToPlayerInventory(filterByContents), ButtonDefinitions.TRANSFER_TO_INVENTORY, ButtonDefinitions.TRANSFER_TO_INVENTORY_FILTERED);
		addRenderableWidget(transferToInventoryButton);
		updateTransferButtonsPositions();
	}

	protected boolean shouldShowSortButtons() {
		return true;
	}

	private void updateInventoryScrollPanel() {
		if (inventoryScrollPanel != null) {
			removeWidget(inventoryScrollPanel);
		}

		int numberOfVisibleRows = getNumberOfVisibleRows();
		if (numberOfVisibleRows < getMenu().getNumberOfRows()) {
			inventoryScrollPanel = new InventoryScrollPanel(Minecraft.getInstance(), this, 0, getMenu().getNumberOfStorageInventorySlots(), getSlotsOnLine(), numberOfVisibleRows * 18, getGuiTop() + 17, getGuiLeft() + 7);
			addRenderableWidget(inventoryScrollPanel);
			inventoryScrollPanel.updateSlotsPosition();
		} else {
			inventoryScrollPanel = null;
		}
	}

	private void updateTransferButtonsPositions() {
		if (transferToStorageButton == null || transferToInventoryButton == null) {
			return;
		}
		transferToStorageButton.setPosition(new Position(leftPos + inventoryLabelX + 137 + transferButtonsShiftX, topPos + inventoryLabelY - 2));
		transferToInventoryButton.setPosition(new Position(leftPos + inventoryLabelX + 149 + transferButtonsShiftX, topPos + inventoryLabelY - 2));
	}

	public Optional<Position> getTransferToInventoryButtonPosition() {
		if (transferToInventoryButton == null) {
			return Optional.empty();
		}
		return Optional.of(new Position(transferToInventoryButton.getX(), transferToInventoryButton.getY()));
	}

	public void setTransferButtonsShift(int shiftX) {
		if (transferButtonsShiftX == shiftX) {
			return;
		}

		transferButtonsShiftX = shiftX;
		updateTransferButtonsPositions();
	}

	public void setExternalSearchPhrase(String searchPhrase) {
		String phrase = searchPhrase == null ? "" : searchPhrase;
		if (searchBox != null) {
			searchBox.setValue(phrase);
		} else {
			getMenu().setSearchPhrase(phrase);
			updateSearchFilter(phrase);
		}
	}

	public void setModalOverlay(@Nullable WidgetBase modalOverlay) {
		this.modalOverlay = modalOverlay;
		centerModalOverlay();
		setFocused(modalOverlay);
	}

	private void centerModalOverlay() {
		if (modalOverlay != null) {
			modalOverlay.setPosition(new Position((width - modalOverlay.getWidth()) / 2, (height - modalOverlay.getHeight()) / 2));
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
		upgradeSwitches.forEach(this::removeWidget);
		upgradeSwitches.clear();
		int switchTop = topPos + 8;
		for (int slot = 0; slot < numberOfUpgradeSlots; slot++) {
			if (menu.canDisableUpgrade(slot)) {
				int finalSlot = slot;
				ToggleButton<Boolean> upgradeSwitch = new ToggleButton<>(new Position(leftPos - 22, switchTop), ButtonDefinitions.UPGRADE_SWITCH,
						button -> getMenu().setUpgradeEnabled(finalSlot, !getMenu().getUpgradeEnabled(finalSlot)), () -> getMenu().getUpgradeEnabled(finalSlot)) {
					@Override
					protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
						if (menu.isUpgradeRunnable(finalSlot)) {
							super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
						} else {
							GuiHelper.blit(guiGraphics, x, y, ButtonDefinitions.UPGRADE_SWITCH_INACTIVE.getForegroundTexture());
						}
					}

					@Override
					public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
						if (menu.isUpgradeRunnable(finalSlot)) {
							super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
						} else {
							if (isMouseOver(mouseX, mouseY)) {
								GuiHelper.renderTooltip(screen, guiGraphics, ButtonDefinitions.UPGRADE_SWITCH_INACTIVE.getTooltip(), mouseX, mouseY);
							}
						}
					}

					@Override
					protected void renderHoveredBackground(GuiGraphics guiGraphics) {
						if (menu.isUpgradeRunnable(finalSlot)) {
							super.renderHoveredBackground(guiGraphics);
						} else {
							GuiHelper.blit(guiGraphics, x, y, ButtonDefinitions.UPGRADE_SWITCH_INACTIVE.getBackgroundTexture());
						}
					}
				};
				addRenderableWidget(upgradeSwitch);
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
			}
		});
		addRenderableWidget(sortButton);
		sortByButton = new ToggleButton<>(new Position(pos.x() + 12, pos.y()), ButtonDefinitions.SORT_BY, button -> {
			if (button == 0) {
				getMenu().setSortBy(getMenu().getSortBy().next());
			}
		}, () -> getMenu().getSortBy());
		addRenderableWidget(sortByButton);
	}

	private Position getSortButtonsPosition(SortButtonsPosition sortButtonsPosition) {
		return switch (sortButtonsPosition) {
			case BELOW_UPGRADES ->
					new Position(leftPos - UPGRADE_INVENTORY_OFFSET - 2, topPos + getUpgradeHeightWithoutBottom() + UPGRADE_BOTTOM_HEIGHT + 2);
			case BELOW_UPGRADE_TABS ->
					new Position(settingsTabControl.getX() + 2, settingsTabControl.getY() + Math.max(0, settingsTabControl.getHeight() + 2));
			default -> new Position(leftPos + imageWidth - 31, topPos + 4);
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

	private void refreshForSettingsChange() {
		if (menu.detectSettingsChangeAndReload()) {
			updateStorageSlotsPositions();
			updatePlayerSlotsPositions();
			updateExtraSlotsPositions();
			updateInventoryScrollPanel();
			updateNoResultsLabel();
			updateTransferButtonsPositions();
		}
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		refreshForSettingsChange();
		renderTransparentBackground(guiGraphics);
		settingsTabControl.render(guiGraphics, mouseX, mouseY, partialTicks);
		renderBg(guiGraphics, partialTicks, mouseX, mouseY);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		refreshForSettingsChange();
		renderSuper(guiGraphics, mouseX, mouseY, partialTicks);

		settingsTabControl.renderForeground(guiGraphics, mouseX, mouseY, partialTicks);

		if (getMenu().getCarried().isEmpty()) {
			settingsTabControl.renderTooltip(this, guiGraphics, mouseX, mouseY);
		}
		renderErrorOverlay(guiGraphics);
		if (modalOverlay == null) {
			settingsTabControl.renderTooltip(this, guiGraphics, mouseX, mouseY);
			renderTooltip(guiGraphics, mouseX, mouseY);
		} else {
			renderModalOverlay(guiGraphics, mouseX, mouseY, partialTicks);
		}
	}

	private void renderModalOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		Matrix3x2fStack poseStack = guiGraphics.pose();
		poseStack.pushMatrix();
		guiGraphics.fill(0, 0, width, height, 0x99000000);
		modalOverlay.render(guiGraphics, mouseX, mouseY, partialTicks);
		modalOverlay.renderTooltip(this, guiGraphics, mouseX, mouseY);
		poseStack.popMatrix();
	}

	private void renderSuper(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) { //copy of super.render with storage inventory slots rendering and snap rendering removed
		int i = leftPos;
		int j = topPos;

		for (Renderable widget : renderables) {
			widget.render(guiGraphics, mouseX, mouseY, partialTick);
		}

		Matrix3x2fStack pose = guiGraphics.pose();
		pose.pushMatrix();
		pose.translate(i, j);

		renderLabels(guiGraphics, mouseX, mouseY);
		//noinspection UnstableApiUsage
		NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Foreground(this, guiGraphics, mouseX, mouseY));

		pose.popMatrix();

		if (searchBox != null) {
			searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
		}

		pose.pushMatrix();
		pose.translate(i, j);

		ItemStack itemstack = draggingItem.isEmpty() ? menu.getCarried() : draggingItem;
		if (!itemstack.isEmpty()) {
			int l = draggingItem.isEmpty() ? 8 : 16;
			String s = null;
			if (!draggingItem.isEmpty() && isSplittingStack) {
				itemstack = itemstack.copyWithCount(Mth.ceil((float) itemstack.getCount() / 2.0F));
			} else if (isQuickCrafting && quickCraftSlots.size() > 1) {
				itemstack = itemstack.copyWithCount(quickCraftingRemainder);
				if (itemstack.isEmpty()) {
					s = ChatFormatting.YELLOW + "0";
				}
			}

			renderFloatingItem(guiGraphics, itemstack, mouseX - i - 8, mouseY - j - l, s);
		}

		pose.popMatrix();
	}

	@Nullable
	@Override
	public Slot getHoveredSlot(double mouseX, double mouseY) {
		for (int i = 0; i < menu.upgradeSlots.size(); ++i) {
			Slot slot = menu.upgradeSlots.get(i);
			if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				return slot;
			}
		}

		if (inventoryScrollPanel != null) {
			Optional<Slot> result = inventoryScrollPanel.getHoveredSlot(mouseX, mouseY);
			if (result.isPresent()) {
				return result.get();
			}
			Slot slot = super.getHoveredSlot(mouseX, mouseY);

			return slot == null || menu.isStorageInventorySlot(slot) ? null : slot; //if super finds inventory slot that's hidden inside the scroll panel just return null
		} else {
			for (int i = 0; i < menu.realInventorySlots.size(); ++i) {
				Slot slot = menu.realInventorySlots.get(i);
				if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
					return slot;
				}
			}
			return super.getHoveredSlot(mouseX, mouseY);
		}
	}

	@Nullable
	public Slot findSlot(double mouseX, double mouseY) {
		return getHoveredSlot(mouseX, mouseY);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderLabels(guiGraphics, mouseX, mouseY);
		renderUpgradeInventoryParts(guiGraphics, mouseX, mouseY);
		renderUpgradeSlots(guiGraphics, mouseX, mouseY);
		if (inventoryScrollPanel == null) {
			renderStorageInventorySlots(guiGraphics, mouseX, mouseY);
		}
		renderPlayerInventorySlots(guiGraphics, mouseX, mouseY);
	}

	private void renderUpgradeInventoryParts(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		inventoryParts.values().forEach(ip -> ip.render(guiGraphics, mouseX, mouseY));
	}

	private void renderStorageInventorySlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		renderStorageInventorySlots(guiGraphics, mouseX, mouseY, true);
	}

	@Override
	public void renderStorageInventorySlots(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean canShowHover) {
		renderSlotsList(guiGraphics, mouseX, mouseY, menu.realInventorySlots, slot -> true, canShowHover, 0, menu.getNumberOfStorageInventorySlots());
	}

	private void renderPlayerInventorySlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		renderSlotsList(guiGraphics, mouseX, mouseY, menu.realInventorySlots, slot -> true, true, menu.getNumberOfStorageInventorySlots(), menu.realInventorySlots.size());
	}

	private void renderSlotsList(GuiGraphics guiGraphics, int mouseX, int mouseY, List<Slot> slots, Predicate<Slot> canShow, boolean canShowHover) {
		renderSlotsList(guiGraphics, mouseX, mouseY, slots, canShow, canShowHover, 0, slots.size());
	}

	private void renderSlotsList(GuiGraphics guiGraphics, int mouseX, int mouseY, List<Slot> slots, Predicate<Slot> canShow, boolean canShowHover, int startIndex, int endIndex) {
		Slot hoveredSlotBefore = hoveredSlot;
		hoveredSlot = getHoveredSlot(mouseX, mouseY);

		for (int i = startIndex; i < endIndex; i++) {
			Slot slot = slots.get(i);
			if (!canShow.test(slot)) {
				continue;
			}
			if (canShowHover && slot == hoveredSlot) {
				renderSlotHighlightBack(guiGraphics);
			}
			renderSlot(guiGraphics, slot, mouseX, mouseY);
			if (canShowHover && slot == hoveredSlot) {
				renderSlotHighlightFront(guiGraphics);
			}
		}

		if (hoveredSlotBefore != null && hoveredSlotBefore != hoveredSlot) {
			onStopHovering(hoveredSlotBefore);
		}
	}

	private void renderUpgradeSlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		renderSlotsList(guiGraphics, mouseX, mouseY, menu.upgradeSlots, slot -> slot.x != DISABLED_SLOT_X_POS, true);
	}

	@Override
	protected void renderSlot(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY) {
		int i = slot.x;
		int j = slot.y;
		ItemStack stackToRender = slot.getItem();
		boolean flag = false;
		boolean rightClickDragging = slot == clickedSlot && !draggingItem.isEmpty() && !isSplittingStack;
		ItemStack carriedStack = getMenu().getCarried();
		String stackCountText = null;
		if (getMenu().isInfiniteSlot(slot.index)) {
			stackCountText = "∞";
		}

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
				int renderCount = StorageContainerMenuBase.getQuickCraftPlaceCount(slot, quickCraftSlots.size(), quickCraftingType, carriedStack) + slotStackCount;
				int slotLimit = stackToRender.isEmpty() ? 64 : slot.getMaxStackSize(stackToRender);
				if (renderCount > slotLimit) {
					stackCountText = ChatFormatting.YELLOW + CountAbbreviator.abbreviate(slotLimit);
				}
				stackToRender = carriedStack.copyWithCount(renderCount);
			} else {
				quickCraftSlots.remove(slot);
				recalculateQuickCraftRemaining();
			}
		}
		if (stackToRender.isEmpty() && slot.isActive()) {
			renderSlotBackground(guiGraphics, slot, i, j);
		} else if (!rightClickDragging) {
			renderStack(guiGraphics, i, j, stackToRender, flag, stackCountText);
			slotDecorationRenderer.renderDecoration(guiGraphics, slot);
		}
	}

	private void renderStack(GuiGraphics guiGraphics, int x, int y, ItemStack itemstack, boolean flag, @Nullable String stackCountText) {
		if (flag) {
			guiGraphics.fill(x, y, x + 16, y + 16, -2130706433);
		}

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
		if (getMenu().isStorageInventorySlot(slot)) {
			if (memorizedStack.isPresent()) {
				guiGraphics.renderItem(memorizedStack.get(), i, j);
				drawStackOverlay(guiGraphics, i, j);
				return;
			} else if (!getMenu().getSlotFilterItem(slot.index).isEmpty()) {
				guiGraphics.renderItem(getMenu().getSlotFilterItem(slot.index), i, j);
				drawStackOverlay(guiGraphics, i, j);
				return;
			}
		}
		Identifier icon = slot.getNoItemIcon();
		if (icon != null) {
			guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, i, j, 16, 16);
		}
	}

	private void drawStackOverlay(GuiGraphics guiGraphics, int x, int y) {
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, x, y, 77, 0, 16, 16, 256, 256);
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
		guiGraphics.fillGradient(xPos, yPos, xPos + width, yPos + height, slotColor, slotColor);
	}

	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		drawInventoryBg(guiGraphics, x, y, storageBackgroundProperties.getTextureName());
		if (inventoryScrollPanel == null) {
			drawSlotBg(guiGraphics, visibleSlotsCount);
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
		Matrix3x2fStack pose = guiGraphics.pose();
		pose.pushMatrix();
		pose.translate(getGuiLeft(), getGuiTop());
		for (int slotNumber = 0; slotNumber < menu.getNumberOfStorageInventorySlots(); slotNumber++) {
			List<Integer> colors = menu.getSlotOverlayColors(slotNumber);
			if (!colors.isEmpty()) {
				int stripeHeight = 16 / colors.size();
				int i = 0;
				for (int slotColor : colors) {
					int yOffset = i * stripeHeight;
					renderSlotOverlay(guiGraphics, menu.getSlot(slotNumber), slotColor & 0x00_FFFFFF | 0x50_000000, yOffset, i == colors.size() - 1 ? 16 - yOffset : stripeHeight);
					i++;
				}
			}
		}
		pose.popMatrix();
	}

	@Override
	protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
		if (!getMenu().getCarried().isEmpty()) {
			return;
		}
		inventoryParts.values().forEach(part -> part.renderTooltip(this, guiGraphics, x, y));
		if (hoveredSlot != null) {
			if (hoveredSlot.hasItem()) {
				super.renderTooltip(guiGraphics, x, y);
			} else if (hoveredSlot instanceof INameableEmptySlot emptySlot && emptySlot.hasEmptyTooltip()) {
				guiGraphics.setComponentTooltipForNextFrame(font, Collections.singletonList(emptySlot.getEmptyTooltip()), x, y);
			}
		}
		if (sortButton != null) {
			sortButton.renderTooltip(this, guiGraphics, x, y);
		}
		if (sortByButton != null) {
			sortByButton.renderTooltip(this, guiGraphics, x, y);
		}
		if (transferToStorageButton != null) {
			transferToStorageButton.renderTooltip(this, guiGraphics, x, y);
		}
		if (transferToInventoryButton != null) {
			transferToInventoryButton.renderTooltip(this, guiGraphics, x, y);
		}
		if (searchBox != null) {
			searchBox.renderTooltip(this, guiGraphics, x, y);
		}
		upgradeSwitches.forEach(us -> us.renderTooltip(this, guiGraphics, x, y));
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
		List<Component> ret = getTooltipFromItem(minecraft, itemStack);
		if (hoveredSlot != null && hoveredSlot instanceof StorageInventorySlot && hoveredSlot.getMaxStackSize() != itemStack.getMaxStackSize()) {
			ret.add(Component.translatable(TranslationHelper.INSTANCE.translGuiTooltip("stack_count"),
							Component.literal(NumberFormat.getNumberInstance().format(itemStack.getCount())).withStyle(ChatFormatting.DARK_AQUA)
									.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
									.append(Component.literal(NumberFormat.getNumberInstance().format(hoveredSlot.getMaxStackSize(itemStack))).withStyle(ChatFormatting.DARK_AQUA)))
					.withStyle(ChatFormatting.GRAY)
			);
		}
		return ret;
	}

	public void drawInventoryBg(GuiGraphics guiGraphics, int x, int y, Identifier textureName) {
		StorageGuiHelper.renderStorageBackground(new Position(x, y), guiGraphics, textureName, imageWidth, imageHeight - HEIGHT_WITHOUT_STORAGE_SLOTS);
	}

	private void drawUpgradeBackground(GuiGraphics guiGraphics) {
		if (numberOfUpgradeSlots == 0) {
			return;
		}

		int heightWithoutBottom = getUpgradeHeightWithoutBottom();

		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_CONTROLS, leftPos - UPGRADE_INVENTORY_OFFSET, topPos, 0, 0, 26, 4, 256, 256);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_CONTROLS, leftPos - UPGRADE_INVENTORY_OFFSET, topPos + 4, 0, 4, 25, heightWithoutBottom - 4, 256, 256);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_CONTROLS, leftPos - UPGRADE_INVENTORY_OFFSET, topPos + heightWithoutBottom, 0, 198, 25, UPGRADE_BOTTOM_HEIGHT, 256, 256);

		boolean previousHasSwitch = false;
		for (int slot = 0; slot < numberOfUpgradeSlots; slot++) {
			if (menu.canDisableUpgrade(slot)) {
				int y = topPos + 5 + slot * UPGRADE_SLOT_HEIGHT + (previousHasSwitch ? 1 : 0);

				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_CONTROLS, leftPos - UPGRADE_INVENTORY_OFFSET - 4, y, 0, 204 + (previousHasSwitch ? 1 : 0), 7, 18 - (previousHasSwitch ? 1 : 0), 256, 256);
				previousHasSwitch = true;
			} else {
				previousHasSwitch = false;
			}
		}
	}

	public UpgradeSettingsTabControl getUpgradeSettingsControl() {
		return settingsTabControl;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (modalOverlay != null) {
			modalOverlay.mouseReleased(event);
			return true;
		}

		for (UpgradeInventoryPartBase<?> inventoryPart : inventoryParts.values()) {
			if (inventoryPart.handleMouseReleased(event)) {
				return true;
			}
		}

		handleQuickMoveAll(event.x(), event.y(), event.button());

		return super.mouseReleased(event);
	}

	private void handleQuickMoveAll(double mouseX, double mouseY, int button) {
		Slot slot = getHoveredSlot(mouseX, mouseY);
		if (doubleclick && !getMenu().getCarried().isEmpty() && slot != null && button == 0 && menu.canTakeItemForPickAll(ItemStack.EMPTY, slot) && Minecraft.getInstance().hasShiftDown() && !lastQuickMoved.isEmpty()) {
			for (Slot slot2 : menu.realInventorySlots) {
				tryQuickMoveSlot(button, slot, slot2);
			}
		}
	}

	private void tryQuickMoveSlot(int button, Slot slot, Slot slot2) {
		//noinspection ConstantConditions - by this point minecraft isn't null
		if (slot2.mayPickup(minecraft.player) && slot2.hasItem() && slot2.isSameInventory(slot)) {
			ItemStack slotItem = slot2.getItem();
			if (ItemStack.isSameItemSameComponents(lastQuickMoved, slotItem)) {
				if (slotItem.getCount() > slotItem.getMaxStackSize()) {
					ClientPacketDistributor.sendToServer(new TransferFullSlotPayload(slot2.index));
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

		int inventorySlotsToCheck = Math.min(realInventoryItems.size() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS, menu.getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS);

		Int2ObjectMap<HashedStack> changedSlotStacks = new Int2ObjectOpenHashMap<>();

		for (int slotIndex = 0; slotIndex < inventorySlotsToCheck; slotIndex++) {
			ItemStack itemstack = realInventoryItems.get(slotIndex);
			ItemStack slotStack = menu.getSlot(slotIndex).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				changedSlotStacks.put(slotIndex, HashedStack.create(slotStack, minecraft.getConnection().decoratedHashOpsGenenerator()));
			}
		}

		for (int i = 0; i < StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS; i++) {
			ItemStack itemstack = realInventoryItems.get(realInventoryItems.size() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + i);
			int slotIndex = menu.getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + i;
			ItemStack slotStack = menu.getSlot(slotIndex).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				changedSlotStacks.put(slotIndex, HashedStack.create(slotStack, minecraft.getConnection().decoratedHashOpsGenenerator()));
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
				changedSlotStacks.put(slotIndex, HashedStack.create(slotStack, minecraft.getConnection().decoratedHashOpsGenenerator()));
			}
		}

		HashedStack hashedCarried = HashedStack.create(menu.getCarried(), minecraft.getConnection().decoratedHashOpsGenenerator());
		minecraft.player.connection.send(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), Shorts.checkedCast(slotNumber), SignedBytes.checkedCast(mouseButton), type, changedSlotStacks, hashedCarried));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (modalOverlay != null) {
			if (modalOverlay.mouseClicked(event, doubleClicked)) {
				return true;
			}
			if (!modalOverlay.isMouseOver(mouseX, mouseY)) {
				setModalOverlay(null);
			}
			return true;
		}

		Slot slot = getHoveredSlot(mouseX, mouseY);
		if (event.hasShiftDown() && event.hasControlDown() && slot instanceof StorageInventorySlot && event.button() == 0) {
			ClientPacketDistributor.sendToServer(new TransferFullSlotPayload(slot.index));
			return true;
		}
		GuiEventListener focused = getFocused();
		if (focused != null && !focused.isMouseOver(mouseX, mouseY) && (focused instanceof WidgetBase widgetBase)) {
			widgetBase.setFocused(false);
		}

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

				if ((Boolean) this.minecraft.options.touchscreen().get() && flag1 && this.menu.getCarried().isEmpty()) {
					this.onClose();
					return true;
				}

				if (k != -1) {
					if ((Boolean) this.minecraft.options.touchscreen().get()) {
						if (slot != null && slot.hasItem()) {
							this.clickedSlot = slot;
							this.draggingItem = ItemStack.EMPTY;
							this.isSplittingStack = event.button() == 1;
						} else {
							this.clickedSlot = null;
						}
					} else if (!this.isQuickCrafting) {
						if (this.menu.getCarried().isEmpty()) {
							if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
								this.slotClicked(slot, k, event.button(), ClickType.CLONE);
							} else {
								boolean flag2 = k != -999 && event.hasShiftDown();
								ClickType clicktype = ClickType.PICKUP;
								if (flag2) {
									this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
									clicktype = ClickType.QUICK_MOVE;
								} else if (k == -999) {
									clicktype = ClickType.THROW;
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
			}

			this.lastClickSlot = slot;
			return true;
		}
	}

	//Modified to actually return false if child didn't handle the click
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

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (modalOverlay != null) {
			modalOverlay.mouseDragged(event, dragX, dragY);
			return true;
		}

		for (GuiEventListener child : children()) {
			if (child.isMouseOver(mouseX, mouseY) && child.mouseDragged(event, dragX, dragY)) {
				return true;
			}
		}
		Slot slot = getHoveredSlot(mouseX, mouseY);
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

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (modalOverlay != null) {
			modalOverlay.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
			return true;
		}
		if (getChildAt(mouseX, mouseY).filter(child -> child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)).isPresent()) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (modalOverlay != null) {
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				setModalOverlay(null);
				return true;
			}
			modalOverlay.keyPressed(event);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (modalOverlay != null) {
			modalOverlay.charTyped(event);
			return true;
		}
		return super.charTyped(event);
	}

	private boolean isAllowedSlotCombination(Slot slot, ItemStack carried) {
		if (quickCraftSlots.isEmpty() || !(carried.getItem() instanceof UpgradeItemBase<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
			return true;
		}
		return quickCraftSlots.contains(slot) || (!(quickCraftSlots.iterator().next() instanceof StorageContainerMenuBase.StorageUpgradeSlot) && !(slot instanceof StorageContainerMenuBase.StorageUpgradeSlot));
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn) {
		return super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn) && hasClickedOutsideOfUpgradeSlots(mouseX, mouseY)
				&& hasClickedOutsideOfUpgradeSettings(mouseX, mouseY);
	}

	private boolean hasClickedOutsideOfUpgradeSettings(double mouseX, double mouseY) {
		return settingsTabControl.getTabRectangles().stream().noneMatch(r -> r.contains((int) mouseX, (int) mouseY));
	}

	private boolean hasClickedOutsideOfUpgradeSlots(double mouseX, double mouseY) {
		return !getUpgradeSlotsRectangle().map(r -> r.contains((int) mouseX, (int) mouseY)).orElse(false);
	}

	public Optional<Rect2i> getUpgradeSlotsRectangle() {
		return numberOfUpgradeSlots == 0 ? Optional.empty() : GuiHelper.getPositiveRectangle(leftPos - UPGRADE_INVENTORY_OFFSET - (!upgradeSwitches.isEmpty() ? 4 : 0), topPos, UPGRADE_INVENTORY_OFFSET + 4, getUpgradeHeight());
	}

	private void renderStackCount(GuiGraphics guiGraphics, String count, int x, int y) {
		Matrix3x2fStack pose = guiGraphics.pose();
		pose.pushMatrix();
		float scale = Math.min(1f, (float) 16 / font.width(count));
		if (scale < 1f) {
			pose.scale(scale, scale);
		}
		guiGraphics.drawString(font, count, (int) ((x + 19 - 2 - (font.width(count) * scale)) / scale), (int) ((y + 6 + 3 + (1 / (scale * scale) - 1)) / scale), ARGB.opaque(16777215), true);
		pose.popMatrix();
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
					int quickCraftPlaceCount = Math.min(StorageContainerMenuBase.getQuickCraftPlaceCount(slot, quickCraftSlots.size(), quickCraftingType, carriedStack) + slotStackCount, maxStackSize);
					quickCraftingRemainder -= quickCraftPlaceCount - slotStackCount;
				}
			}
		}
	}

	private void renderErrorOverlay(GuiGraphics guiGraphics) {
		menu.getErrorUpgradeSlotChangeResult().ifPresent(upgradeSlotChangeResult -> upgradeSlotChangeResult.getErrorMessage().ifPresent(overlayErrorMessage -> {
			Matrix3x2fStack pose = guiGraphics.pose();
			pose.pushMatrix();
			pose.translate(getGuiLeft(), getGuiTop());
			upgradeSlotChangeResult.errorUpgradeSlots().forEach(slotIndex -> {
				Slot upgradeSlot = menu.getSlot(menu.getFirstUpgradeSlot() + slotIndex);
				renderSlotOverlay(guiGraphics, upgradeSlot, ERROR_SLOT_COLOR);
			});
			upgradeSlotChangeResult.errorInventorySlots().forEach(slotIndex -> {
				Slot slot = menu.getSlot(slotIndex);
				//noinspection ConstantConditions
				if (slot != null) {
					renderSlotOverlay(guiGraphics, slot, ERROR_SLOT_COLOR);
				}
			});
			upgradeSlotChangeResult.errorInventoryParts().forEach(partIndex -> {
				UpgradeInventoryPartBase<?> inventoryPart = inventoryParts.get(partIndex);
				if (inventoryPart != null) {
					inventoryPart.renderErrorOverlay(guiGraphics);
				}
			});
			pose.popMatrix();

			renderErrorMessage(guiGraphics, pose, overlayErrorMessage);
		}));
	}

	private void renderErrorMessage(GuiGraphics guiGraphics, Matrix3x2fStack pose, Component overlayErrorMessage) {
		pose.pushMatrix();
		pose.translate((float) width / 2, (float) topPos + inventoryLabelY + 4);
		Font fontrenderer = Minecraft.getInstance().font;

		int tooltipWidth = font.width(overlayErrorMessage);

		List<FormattedCharSequence> wrappedTextLines = new ArrayList<>();
		int maxLineWidth = 260;
		if (tooltipWidth > maxLineWidth) {
			int wrappedTooltipWidth = 0;
			List<FormattedCharSequence> wrappedLine = font.split(overlayErrorMessage, maxLineWidth);

			for (FormattedCharSequence line : wrappedLine) {
				int lineWidth = font.width(line);
				if (lineWidth > wrappedTooltipWidth) {
					wrappedTooltipWidth = lineWidth;
				}
				wrappedTextLines.add(line);
			}
			tooltipWidth = wrappedTooltipWidth;
		} else {
			wrappedTextLines.add(overlayErrorMessage.getVisualOrderText());
		}

		int tooltipHeight = 8;
		if (wrappedTextLines.size() > 1) {
			tooltipHeight += 2 + (wrappedTextLines.size() - 1) * 10;
		}

		int leftX = -tooltipWidth / 2;

		TooltipRenderUtil.renderTooltipBackground(guiGraphics, leftX, 0, tooltipWidth, tooltipHeight, SophisticatedCore.getIdentifier("error"));
		MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
		GuiHelper.writeTooltipLines(guiGraphics, wrappedTextLines, fontrenderer, leftX, 0, ERROR_TEXT_COLOR);
		renderTypeBuffer.endBatch();
		pose.popMatrix();
	}

	@Override
	public boolean isMouseOverSlot(Slot slot, double mouseX, double mouseY) {
		return isHovering(slot, mouseX, mouseY);
	}

	@Override
	protected boolean isHovering(Slot slot, double mouseX, double mouseY) {
		if (modalOverlay != null) {
			return false;
		}
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
		getMenu().getExtraSlots().forEach(slot -> GuiHelper.renderSlotsBackground(guiGraphics, slot.x + leftPos - 1, slot.y + topPos - 1, 1, 1));
	}

	@Override
	public int getLeftX() {
		return getGuiLeft();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		settingsTabControl.tick();
	}

	private class TransferButton extends Button {
		private final ButtonDefinition shiftDefinition;
		private final ButtonDefinition definition;

		public TransferButton(Consumer<Boolean> transferItems, ButtonDefinition shiftDefinition, ButtonDefinition definition) {
			super(new Position(leftPos, topPos), definition, button -> {
				if (button == 0) {
					transferItems.accept(!Minecraft.getInstance().hasShiftDown());
				}
			});
			this.shiftDefinition = shiftDefinition;
			this.definition = definition;
		}

		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
			if (Minecraft.getInstance().hasShiftDown()) {
				if (shiftDefinition.getForegroundTexture() != null) {
					GuiHelper.blit(guiGraphics, x, y, shiftDefinition.getForegroundTexture());
				}
			} else {
				if (definition.getForegroundTexture() != null) {
					GuiHelper.blit(guiGraphics, x, y, definition.getForegroundTexture());
				}
			}
		}

		@Override
		protected List<Component> getTooltip() {
			if (Minecraft.getInstance().hasShiftDown()) {
				return shiftDefinition.getTooltip();
			} else {
				return definition.getTooltip();
			}
		}
	}
}
