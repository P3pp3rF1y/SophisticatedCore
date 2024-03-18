package net.p3pp3rf1y.sophisticatedcore.common.gui;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntComparators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.items.SlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.network.*;
import net.p3pp3rf1y.sophisticatedcore.settings.ISlotColorCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class StorageContainerMenuBase<S extends IStorageWrapper> extends AbstractContainerMenu implements IAdditionalSlotInfoMenu {
	public static final int NUMBER_OF_PLAYER_SLOTS = 36;
	public static final ResourceLocation EMPTY_UPGRADE_SLOT_BACKGROUND = new ResourceLocation(SophisticatedCore.MOD_ID, "item/empty_upgrade_slot");
	public static final Pair<ResourceLocation, ResourceLocation> INACCESSIBLE_SLOT_BACKGROUND = new Pair<>(InventoryMenu.BLOCK_ATLAS, SophisticatedCore.getRL("item/inaccessible_slot"));
	protected static final String UPGRADE_ENABLED_TAG = "upgradeEnabled";
	protected static final String UPGRADE_SLOT_TAG = "upgradeSlot";
	protected static final String ACTION_TAG = "action";
	protected static final String OPEN_TAB_ID_TAG = "openTabId";
	protected static final String SORT_BY_TAG = "sortBy";
	private static final Method ON_SWAP_CRAFT = ObfuscationReflectionHelper.findMethod(Slot.class, "m_6405_", int.class);
	public final NonNullList<ItemStack> lastUpgradeSlots = NonNullList.create();
	public final List<Slot> upgradeSlots = Lists.newArrayList();
	public final NonNullList<ItemStack> remoteUpgradeSlots = NonNullList.create();
	public final NonNullList<ItemStack> lastRealSlots = NonNullList.create();
	public final List<Slot> realInventorySlots = Lists.newArrayList();
	private final Map<Integer, UpgradeContainerBase<?, ?>> upgradeContainers = new LinkedHashMap<>();
	private final NonNullList<ItemStack> remoteRealSlots = NonNullList.create();
	protected final Player player;
	protected final S storageWrapper;
	protected final IStorageWrapper parentStorageWrapper;
	private final Map<Integer, ItemStack> slotStacksToUpdate = new HashMap<>();
	private final int storageItemSlotIndex;
	private final boolean shouldLockStorageItemSlot;
	private int storageItemSlotNumber = -1;
	private Consumer<StorageContainerMenuBase<?>> upgradeChangeListener = null;
	private boolean isUpdatingFromPacket = false;
	private long errorResultExpirationTime = 0;
	@Nullable
	private UpgradeSlotChangeResult errorUpgradeSlotChangeResult;
	private CompoundTag lastSettingsNbt = null;
	private boolean inventorySlotStackChanged = false;
	private final Set<Integer> inaccessibleSlots = new HashSet<>();
	private final Map<Integer, Integer> slotLimitOverrides = new HashMap<>();
	private final Map<Integer, ItemStack> slotFilterItems = new HashMap<>();
	private final Map<Integer, Pair<ResourceLocation, ResourceLocation>> emptySlotIcons = new HashMap<>();

	private boolean slotsChangedSinceStartOfClick = false;

	protected StorageContainerMenuBase(MenuType<?> pMenuType, int pContainerId, Player player, S storageWrapper, IStorageWrapper parentStorageWrapper, int storageItemSlotIndex, boolean shouldLockStorageItemSlot) {
		super(pMenuType, pContainerId);
		this.player = player;
		this.storageWrapper = storageWrapper;
		this.parentStorageWrapper = parentStorageWrapper;
		this.storageItemSlotIndex = storageItemSlotIndex;
		this.shouldLockStorageItemSlot = shouldLockStorageItemSlot;

		removeOpenTabIfKeepOff();
		storageWrapper.fillWithLoot(player);
		initSlotsAndContainers(player, storageItemSlotIndex, shouldLockStorageItemSlot);
	}

	public abstract Optional<BlockPos> getBlockPosition();

	protected void initSlotsAndContainers(Player player, int storageItemSlotIndex, boolean shouldLockStorageItemSlot) {
		addStorageInventorySlots();
		addPlayerInventorySlots(player.getInventory(), storageItemSlotIndex, shouldLockStorageItemSlot);
		addUpgradeSlots();
		addUpgradeSettingsContainers(player);
	}

	public S getStorageWrapper() {
		return storageWrapper;
	}

	protected void addUpgradeSettingsContainers(Player player) {
		UpgradeHandler upgradeHandler = storageWrapper.getUpgradeHandler();
		upgradeHandler.getSlotWrappers().forEach((slot, wrapper) -> UpgradeContainerRegistry.instantiateContainer(player, slot, wrapper)
				.ifPresent(container -> upgradeContainers.put(slot, container)));

		for (UpgradeContainerBase<?, ?> container : upgradeContainers.values()) {
			container.getSlots().forEach(this::addUpgradeSlot);
			container.onInit();
		}

		storageWrapper.getOpenTabId().ifPresent(id -> {
			if (upgradeContainers.containsKey(id)) {
				upgradeContainers.get(id).setIsOpen(true);
			}
		});
	}

	private void addUpgradeSlots() {
		UpgradeHandler upgradeHandler = storageWrapper.getUpgradeHandler();

		int numberOfSlots = upgradeHandler.getSlots();

		if (numberOfSlots == 0) {
			return;
		}

		int slotIndex = 0;

		while (slotIndex < upgradeHandler.getSlots()) {
			addUpgradeSlot(instantiateUpgradeSlot(upgradeHandler, slotIndex));

			slotIndex++;
		}
	}

	public int getColumnsTaken() {
		return storageWrapper.getColumnsTaken();
	}

	public Optional<UpgradeSlotChangeResult> getErrorUpgradeSlotChangeResult() {
		if (errorUpgradeSlotChangeResult != null && player.level().getGameTime() >= errorResultExpirationTime) {
			errorResultExpirationTime = 0;
			errorUpgradeSlotChangeResult = null;
		}
		return Optional.ofNullable(errorUpgradeSlotChangeResult);
	}

	protected void sendStorageSettingsToClient() {
		//noop by default
	}

	protected abstract StorageUpgradeSlot instantiateUpgradeSlot(UpgradeHandler upgradeHandler, int slotIndex);

	protected void addUpgradeSlot(Slot slot) {
		slot.index = getTotalSlotsNumber();
		upgradeSlots.add(slot);
		lastUpgradeSlots.add(ItemStack.EMPTY);
		remoteUpgradeSlots.add(ItemStack.EMPTY);
	}

	protected void addNoSortSlot(Slot slot) {
		slot.index = getInventorySlotsSize();
		realInventorySlots.add(slot);
		lastRealSlots.add(ItemStack.EMPTY);
		remoteRealSlots.add(ItemStack.EMPTY);
	}

	@Override
	protected Slot addSlot(Slot slot) {
		slot.index = getInventorySlotsSize();
		slots.add(slot);
		lastSlots.add(ItemStack.EMPTY);
		remoteSlots.add(ItemStack.EMPTY);
		realInventorySlots.add(slot);
		lastRealSlots.add(ItemStack.EMPTY);
		remoteRealSlots.add(ItemStack.EMPTY);
		return slot;
	}

	public int getInventorySlotsSize() {
		return realInventorySlots.size();
	}

	public int getNumberOfStorageInventorySlots() {
		return storageWrapper.getInventoryHandler().getSlots();
	}

	public int getNumberOfUpgradeSlots() {
		return storageWrapper.getUpgradeHandler().getSlots();
	}

	public Map<Integer, UpgradeContainerBase<?, ?>> getUpgradeContainers() {
		return upgradeContainers;
	}

	protected void addStorageInventorySlots() {
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		int slotIndex = 0;

		Set<Integer> noSortSlotIndexes = getNoSortSlotIndexes();
		while (slotIndex < inventoryHandler.getSlots()) {
			int finalSlotIndex = slotIndex;
			StorageInventorySlot slot = new StorageInventorySlot(player.level().isClientSide, storageWrapper, inventoryHandler, finalSlotIndex) {
				@Override
				public void set(@Nonnull ItemStack stack) {
					super.set(stack);
					onStorageInventorySlotSet(finalSlotIndex);
				}

				@Nullable
				@Override
				public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
					return inaccessibleSlots.contains(finalSlotIndex) ? INACCESSIBLE_SLOT_BACKGROUND : emptySlotIcons.getOrDefault(finalSlotIndex, null);
				}

				@Override
				public boolean mayPlace(@Nonnull ItemStack stack) {
					return !inaccessibleSlots.contains(finalSlotIndex) && super.mayPlace(stack);
				}

				@Override
				public boolean mayPickup(Player playerIn) {
					return !inaccessibleSlots.contains(finalSlotIndex) && super.mayPickup(playerIn);
				}

				@Override
				public int getMaxStackSize(ItemStack stack) {
					return slotLimitOverrides.containsKey(finalSlotIndex) ? slotLimitOverrides.get(finalSlotIndex) : super.getMaxStackSize(stack);
				}

				@Override
				public int getMaxStackSize() {
					return slotLimitOverrides.containsKey(finalSlotIndex) ? slotLimitOverrides.get(finalSlotIndex) : super.getMaxStackSize();
				}
			};
			if (noSortSlotIndexes.contains(slotIndex)) {
				addNoSortSlot(slot);
			} else {
				addSlot(slot);
			}

			slotIndex++;
		}
	}

	protected void onStorageInventorySlotSet(int slotIndex) {
		//noop by default
	}

	protected void addPlayerInventorySlots(Inventory playerInventory, int storageItemSlotIndex, boolean shouldLockStorageItemSlot) {
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				int slotIndex = j + i * 9 + 9;
				Slot slot = addStorageItemSafeSlot(playerInventory, slotIndex, storageItemSlotIndex, shouldLockStorageItemSlot);
				addSlotAndUpdateStorageItemSlotNumber(storageItemSlotIndex, shouldLockStorageItemSlot, slotIndex, slot);
			}
		}

		for (int slotIndex = 0; slotIndex < 9; ++slotIndex) {
			Slot slot = addStorageItemSafeSlot(playerInventory, slotIndex, storageItemSlotIndex, shouldLockStorageItemSlot);
			addSlotAndUpdateStorageItemSlotNumber(storageItemSlotIndex, shouldLockStorageItemSlot, slotIndex, slot);
		}
	}

	private Slot addStorageItemSafeSlot(Inventory playerInventory, int slotIndex, int storageItemSlotIndex, boolean shouldLockStorageItemSlot) {
		Slot slot;
		if (shouldLockStorageItemSlot && slotIndex == storageItemSlotIndex) {
			slot = new Slot(playerInventory, slotIndex, 0, 0) {
				@Override
				public boolean mayPickup(Player playerIn) {
					return false;
				}
			};
		} else {
			slot = new Slot(playerInventory, slotIndex, 0, 0);
		}

		return addSlot(slot);
	}

	public void closeScreenIfSomethingMessedWithStorageItemStack() {
		if (!isClientSide() && storageItemHasChanged()) {
			player.closeContainer();
		}
	}

	protected boolean isClientSide() {
		return player.level().isClientSide;
	}

	private void addSlotAndUpdateStorageItemSlotNumber(int storageItemSlotIndex, boolean lockStorageItemSlot, int slotIndex, Slot slot) {
		if (lockStorageItemSlot && slotIndex == storageItemSlotIndex) {
			storageItemSlotNumber = slot.index;
		}
	}

	public int getNumberOfRows() {
		return storageWrapper.getNumberOfSlotRows();
	}

	public int getFirstUpgradeSlot() {
		return getInventorySlotsSize();
	}

	public boolean isFirstLevelStorage() {
		return parentStorageWrapper == NoopStorageWrapper.INSTANCE;
	}

	@Override
	public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
		storageWrapper.setPersistent(player.level().isClientSide);
		isUpdatingFromPacket = true;
		super.initializeContents(stateId, items, carried);
		isUpdatingFromPacket = false;
		storageWrapper.setPersistent(true);
		storageWrapper.getInventoryHandler().saveInventory();
		storageWrapper.getUpgradeHandler().saveInventory();
	}

	protected boolean isUpgradeSettingsSlot(int index) {
		return index >= getNumberOfStorageInventorySlots() + getNumberOfUpgradeSlots() + StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS;
	}

	public boolean isStorageInventorySlot(int index) {
		return index >= 0 && index < getNumberOfStorageInventorySlots();
	}

	protected boolean isUpgradeSlot(int index) {
		return index >= getFirstUpgradeSlot() && (index - getFirstUpgradeSlot() < getNumberOfUpgradeSlots());
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
		if (isUpgradeSettingsSlot(slotId) && getSlot(slotId) instanceof IFilterSlot && getSlot(slotId).mayPlace(getCarried())) {
			Slot slot = getSlot(slotId);
			ItemStack cursorStack = getCarried().copy();
			if (cursorStack.getCount() > 1) {
				cursorStack.setCount(1);
			}

			slot.set(cursorStack);
			return;
		} else if (isUpgradeSlot(slotId) && getSlot(slotId) instanceof StorageContainerMenuBase<?>.StorageUpgradeSlot slot) {
			ItemStack slotStack = slot.getItem();
			if (slot.mayPlace(getCarried())) {
				ItemStack carriedStack = getCarried();
				IUpgradeItem<?> upgradeItem = (IUpgradeItem<?>) carriedStack.getItem();
				int newColumnsTaken = upgradeItem.getInventoryColumnsTaken();
				int currentColumnsTaken = 0;
				if (!slotStack.isEmpty()) {
					currentColumnsTaken = ((IUpgradeItem<?>) slotStack.getItem()).getInventoryColumnsTaken();
				}
				if (needsSlotsThatAreOccupied(carriedStack, currentColumnsTaken, newColumnsTaken)) {
					return;
				}

				int columnsToRemove = newColumnsTaken - currentColumnsTaken;
				if (slotStack.isEmpty()) {
					slot.set(carriedStack.split(1));
					if (carriedStack.isEmpty()) {
						setCarried(ItemStack.EMPTY);
					}
				} else if (carriedStack.getCount() == 1) {
					slot.set(carriedStack);
					setCarried(upgradeItem.getCleanedUpgradeStack(slotStack.copy()));
				}

				updateColumnsTaken(columnsToRemove);
				slot.setChanged();
			} else if (getCarried().isEmpty() && !slotStack.isEmpty() && slot.mayPickup(player)) {
				int k2 = dragType == 0 ? Math.min(slotStack.getCount(), slotStack.getMaxStackSize()) : Math.min(slotStack.getMaxStackSize() + 1, slotStack.getCount() + 1) / 2;
				IUpgradeItem<?> upgradeItem = (IUpgradeItem<?>) slotStack.getItem();
				int columnsTaken = upgradeItem.getInventoryColumnsTaken();
				if (clickType == ClickType.QUICK_MOVE) {
					quickMoveStack(player, slotId);
					slot.wasEmpty = false; // slot was not empty when this was reached and need to force onTake below to trigger slot position recalculation if slots are refreshed when columns taken changes
				} else {
					setCarried(upgradeItem.getCleanedUpgradeStack(slot.remove(k2)));
				}
				updateColumnsTaken(-columnsTaken);
				slot.onTake(player, getCarried());
			}
			return;
		} else if (isOverflowLogicSlotAndAction(slotId, clickType) && handleOverflow(slotId, clickType, dragType, player)) {
			return;
		}

		super.clicked(slotId, dragType, clickType, player);
	}

	@Override
	public boolean isValidSlotIndex(int slotIndex) {
		return slotIndex == -1 || slotIndex == -999 || slotIndex < getTotalSlotsNumber();
	}

	private boolean handleOverflow(int slotId, ClickType clickType, int dragType, Player player) {
		ItemStack cursorStack = clickType == ClickType.SWAP ? player.getInventory().getItem(dragType) : getCarried();
		Consumer<ItemStack> updateCursorStack = clickType == ClickType.SWAP ? s -> player.getInventory().setItem(dragType, s) : this::setCarried;
		Slot slot = getSlot(slotId);
		if ((clickType != ClickType.SWAP && cursorStack.isEmpty()) || !slot.mayPlace(cursorStack)) {
			return false;
		}
		ItemStack slotStack = slot.getItem();
		if (slotStack.isEmpty() || (slot.mayPickup(player) && slotStack.getItem() != cursorStack.getItem() && cursorStack.getCount() <= slot.getMaxStackSize(cursorStack) && slotStack.getCount() <= slotStack.getMaxStackSize())) {
			return processOverflowIfSlotWithSameItemFound(slotId, cursorStack, updateCursorStack);
		} else if (slotStack.getItem() == cursorStack.getItem()) {
			return processOverflowForAnythingOverSlotMaxSize(cursorStack, updateCursorStack, slot, slotStack);
		}
		return false;
	}

	private boolean processOverflowForAnythingOverSlotMaxSize(ItemStack cursorStack, Consumer<ItemStack> updateCursorStack, Slot slot, ItemStack slotStack) {
		int remainingSpaceInSlot = slot.getMaxStackSize(cursorStack) - slotStack.getCount();
		if (remainingSpaceInSlot < cursorStack.getCount()) {
			ItemStack overflow = cursorStack.copy();
			int overflowCount = cursorStack.getCount() - remainingSpaceInSlot;
			overflow.setCount(overflowCount);
			ItemStack result = processOverflowLogic(overflow);
			if (result.getCount() < overflowCount) {
				cursorStack.shrink(overflowCount - result.getCount());
				if (cursorStack.isEmpty()) {
					updateCursorStack.accept(ItemStack.EMPTY);
					return true;
				} else {
					updateCursorStack.accept(cursorStack);
				}
			}
		}
		return false;
	}

	private boolean processOverflowIfSlotWithSameItemFound(int slotId, ItemStack cursorStack, Consumer<ItemStack> updateCursorStack) {
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)) {
			if (overflowUpgrade.stackMatchesFilter(cursorStack) && overflowUpgrade.worksInGui()
					&& findSlotWithMatchingStack(slotId, cursorStack, updateCursorStack, overflowUpgrade)) {
				return true;
			}
		}
		return false;
	}

	private boolean findSlotWithMatchingStack(int slotId, ItemStack cursorStack, Consumer<ItemStack> updateCursorStack, IOverflowResponseUpgrade overflowUpgrade) {
		for (int slotIndex = 0; slotIndex < getNumberOfStorageInventorySlots(); slotIndex++) {
			if (slotIndex != slotId && overflowUpgrade.stackMatchesFilterStack(getSlot(slotIndex).getItem(), cursorStack)) {
				ItemStack result = cursorStack;
				result = overflowUpgrade.onOverflow(result);
				updateCursorStack.accept(result);
				if (result.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isOverflowLogicSlotAndAction(int slotId, ClickType clickType) {
		return isStorageInventorySlot(slotId) && (clickType == ClickType.SWAP || clickType == ClickType.PICKUP);
	}

	protected void updateColumnsTaken(int columnsChange) {
		if (columnsChange != 0) {
			storageWrapper.setColumnsTaken(Math.max(0, storageWrapper.getColumnsTaken() + columnsChange), true);
			storageWrapper.onContentsNbtUpdated();
			refreshAllSlots();
		}
	}

	protected boolean needsSlotsThatAreOccupied(ItemStack cursorStack, int currentColumnsTaken, int newColumnsTaken) {
		if (currentColumnsTaken >= newColumnsTaken) {
			return false;
		}

		int slotsToCheck = (newColumnsTaken - currentColumnsTaken) * getNumberOfRows();

		InventoryHandler invHandler = storageWrapper.getInventoryHandler();
		Set<Integer> errorSlots = new HashSet<>();
		int slots = getNumberOfStorageInventorySlots();
		for (int slotIndex = slots - 1; slotIndex >= slots - slotsToCheck; slotIndex--) {
			if (!invHandler.getStackInSlot(slotIndex).isEmpty()) {
				errorSlots.add(slotIndex);
			}
		}

		if (!errorSlots.isEmpty()) {
			updateSlotChangeError(new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("add.needs_occupied_inventory_slots", slotsToCheck, cursorStack.getHoverName()), Collections.emptySet(), errorSlots, Collections.emptySet()));
			return true;
		}
		return false;
	}

	public int getUpgradeSlotsSize() {
		return upgradeSlots.size();
	}

	public List<Integer> getSlotOverlayColors(int slot) {
		List<Integer> ret = new ArrayList<>();
		storageWrapper.getSettingsHandler().getCategoriesThatImplement(ISlotColorCategory.class).forEach(c -> c.getSlotColor(slot).ifPresent(ret::add));
		return ret;
	}

	public Optional<UpgradeContainerBase<?, ?>> getOpenContainer() {
		return storageWrapper.getOpenTabId().flatMap(id -> upgradeContainers.containsKey(id) ? Optional.of(upgradeContainers.get(id)) : Optional.empty());
	}

	protected void sendToServer(Consumer<CompoundTag> addData) {
		CompoundTag data = new CompoundTag();
		addData.accept(data);
		PacketHandler.INSTANCE.sendToServer(new SyncContainerClientDataMessage(data));
	}

	public void setUpgradeEnabled(int upgradeSlot, boolean enabled) {
		Map<Integer, IUpgradeWrapper> slotWrappers = storageWrapper.getUpgradeHandler().getSlotWrappers();
		if (!slotWrappers.containsKey(upgradeSlot)) {
			return;
		}
		if (isClientSide()) {
			sendToServer(data -> {
				data.putBoolean(UPGRADE_ENABLED_TAG, enabled);
				data.putInt(UPGRADE_SLOT_TAG, upgradeSlot);
			});
		}
		slotWrappers.get(upgradeSlot).setEnabled(enabled);
	}

	public boolean getUpgradeEnabled(int upgradeSlot) {
		Map<Integer, IUpgradeWrapper> slotWrappers = storageWrapper.getUpgradeHandler().getSlotWrappers();
		if (!slotWrappers.containsKey(upgradeSlot)) {
			return false;
		}
		return slotWrappers.get(upgradeSlot).isEnabled();
	}

	public boolean canDisableUpgrade(int upgradeSlot) {
		Map<Integer, IUpgradeWrapper> slotWrappers = storageWrapper.getUpgradeHandler().getSlotWrappers();
		if (!slotWrappers.containsKey(upgradeSlot)) {
			return false;
		}
		return slotWrappers.get(upgradeSlot).canBeDisabled();
	}

	public void sort() {
		if (isClientSide()) {
			sendToServer(data -> data.putString(ACTION_TAG, "sort"));
			return;
		}

		storageWrapper.sort();
	}

	public void setOpenTabId(int tabId) {
		if (isClientSide()) {
			sendToServer(data -> data.putInt(OPEN_TAB_ID_TAG, tabId));
		}

		if (tabId == -1) {
			storageWrapper.removeOpenTabId();
		} else {
			storageWrapper.setOpenTabId(tabId);
		}
	}

	public void removeOpenTabId() {
		setOpenTabId(-1);
	}

	public SortBy getSortBy() {
		return storageWrapper.getSortBy();
	}

	public void setSortBy(SortBy sortBy) {
		if (isClientSide()) {
			sendToServer(data -> data.putString(SORT_BY_TAG, sortBy.getSerializedName()));
		}
		storageWrapper.setSortBy(sortBy);
	}

	public void handleMessage(CompoundTag data) {
		if (data.contains("containerId")) {
			int containerId = data.getInt("containerId");
			if (upgradeContainers.containsKey(containerId)) {
				upgradeContainers.get(containerId).handleMessage(data);
			}
		} else if (data.contains(OPEN_TAB_ID_TAG)) {
			setOpenTabId(data.getInt(OPEN_TAB_ID_TAG));
		} else if (data.contains(SORT_BY_TAG)) {
			setSortBy(SortBy.fromName(data.getString(SORT_BY_TAG)));
		} else if (data.contains(ACTION_TAG)) {
			String actionName = data.getString(ACTION_TAG);
			switch (actionName) {
				case "sort" -> sort();
				case "openSettings" -> openSettings();
				default -> {
					//noop
				}
			}
		} else if (data.contains(UPGRADE_ENABLED_TAG)) {
			setUpgradeEnabled(data.getInt(UPGRADE_SLOT_TAG), data.getBoolean(UPGRADE_ENABLED_TAG));
		}
	}

	public Optional<UpgradeContainerBase<?, ?>> getSlotUpgradeContainer(Slot slot) {
		if (isUpgradeSettingsSlot(slot.index)) {
			for (UpgradeContainerBase<?, ?> upgradeContainer : upgradeContainers.values()) {
				if (upgradeContainer.containsSlot(slot)) {
					return Optional.of(upgradeContainer);
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = getSlot(index);
		if (slot.hasItem()) {
			Optional<UpgradeContainerBase<?, ?>> upgradeContainer = getSlotUpgradeContainer(slot);
			ItemStack slotStack = upgradeContainer.map(c -> c.getSlotStackToTransfer(slot)).orElse(slot.getItem());
			itemstack = slotStack.copy();

			ItemStack stackToMerge = isUpgradeSlot(index) && slotStack.getItem() instanceof IUpgradeItem<?> upgradeItem ? upgradeItem.getCleanedUpgradeStack(slotStack.copy()) : slotStack;
			if (!mergeSlotStack(slot, index, stackToMerge)) {
				return ItemStack.EMPTY;
			}

			if (stackToMerge.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			slot.onQuickCraft(slotStack, itemstack);

			if (upgradeContainer.isPresent()) {
				upgradeContainer.ifPresent(c -> c.onTakeFromSlot(slot, player, slotStack));
			} else {
				slot.onTake(player, slotStack);
			}
		}

		return itemstack;
	}

	private boolean mergeSlotStack(Slot slot, int index, ItemStack slotStack) {
		if (isUpgradeSlot(index)) {
			return mergeStackToPlayersInventory(slot, slotStack) || mergeStackToStorage(slot, slotStack);
		} else if (isStorageInventorySlot(index)) {
			if (shouldShiftClickIntoOpenTabFirst()) {
				return mergeStackToOpenUpgradeTab(slot, slotStack) || mergeStackToPlayersInventory(slot, slotStack);
			}
			return mergeStackToPlayersInventory(slot, slotStack) || mergeStackToOpenUpgradeTab(slot, slotStack);
		} else if (isUpgradeSettingsSlot(index)) {
			if (getSlotUpgradeContainer(slot).map(c -> c.mergeIntoStorageFirst(slot)).orElse(true)) {
				return mergeStackToStorage(slot, slotStack) || mergeStackToPlayersInventory(slot, slotStack);
			}
			return mergeStackToPlayersInventory(slot, slotStack) || mergeStackToStorage(slot, slotStack);
		} else {
			if (shouldShiftClickIntoOpenTabFirst()) {
				return mergeStackToOpenUpgradeTab(slot, slotStack) || mergeStackToUpgradeSlots(slot, slotStack) || mergeStackToStorage(slot, slotStack);
			}
			return mergeStackToUpgradeSlots(slot, slotStack) || mergeStackToStorage(slot, slotStack) || mergeStackToOpenUpgradeTab(slot, slotStack);
		}
	}

	private boolean shouldShiftClickIntoOpenTabFirst() {
		MainSettingsCategory category = storageWrapper.getSettingsHandler().getGlobalSettingsCategory();
		return SettingsManager.getSettingValue(player, category.getPlayerSettingsTagName(), category, SettingsManager.SHIFT_CLICK_INTO_OPEN_TAB_FIRST);
	}

	private boolean mergeStackToUpgradeSlots(Slot sourceSlot, ItemStack slotStack) {
		return !upgradeSlots.isEmpty() && moveItemStackTo(sourceSlot, slotStack, getInventorySlotsSize(), getInventorySlotsSize() + getNumberOfUpgradeSlots(), false);
	}

	private boolean mergeStackToOpenUpgradeTab(Slot sourceSlot, ItemStack slotStack) {
		return getOpenContainer().map(c -> {
			List<Slot> slots = c.getSlots();
			if (slots.isEmpty()) {
				return false;
			}
			int firstSlotIndex = slots.get(0).index;
			int lastSlotIndex = slots.get(slots.size() - 1).index;
			return mergeItemStack(sourceSlot, slotStack, firstSlotIndex, lastSlotIndex + 1, false, true);
		}).orElse(false);
	}

	private boolean mergeStackToStorage(Slot slot, ItemStack slotStack) {
		ItemStack remaining = mergeItemStack(slotStack, 0, getNumberOfStorageInventorySlots(), false, false, true);
		if (remaining.getCount() != slotStack.getCount()) {
			slot.set(remaining);
			return true;
		}
		return false;
	}

	private boolean mergeStackToPlayersInventory(Slot sourceSlot, ItemStack slotStack) {
		return mergeItemStack(sourceSlot, slotStack, getNumberOfStorageInventorySlots(), getInventorySlotsSize(), true, true);
	}

	public boolean isNotPlayersInventorySlot(int slotNumber) {
		return slotNumber < getNumberOfStorageInventorySlots() || slotNumber >= getInventorySlotsSize();
	}

	public Optional<ItemStack> getMemorizedStackInSlot(int slotId) {
		return storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotFilterStack(slotId, false);
	}

	public void setUpgradeChangeListener(Consumer<StorageContainerMenuBase<?>> upgradeChangeListener) {
		this.upgradeChangeListener = upgradeChangeListener;
	}

	public abstract void openSettings();

	protected abstract boolean storageItemHasChanged();

	@SuppressWarnings("unchecked") // both conditions of T are checked before casting it in the result
	public <T extends UpgradeContainerBase<?, ?> & ICraftingContainer> Optional<T> getOpenOrFirstCraftingContainer() {
		T firstContainer = null;
		for (UpgradeContainerBase<?, ?> container : upgradeContainers.values()) {
			if (container instanceof ICraftingContainer) {
				if (container.isOpen()) {
					return Optional.of((T) container);
				} else if (firstContainer == null) {
					firstContainer = (T) container;
				}
			}
		}
		return Optional.ofNullable(firstContainer);
	}

	public int getTotalSlotsNumber() {
		return getInventorySlotsSize() + upgradeSlots.size();
	}

	protected void removeOpenTabIfKeepOff() {
		MainSettingsCategory category = storageWrapper.getSettingsHandler().getGlobalSettingsCategory();
		if (Boolean.FALSE.equals(SettingsManager.getSettingValue(player, category.getPlayerSettingsTagName(), category, SettingsManager.KEEP_TAB_OPEN))) {
			storageWrapper.removeOpenTabId();
		}
	}

	protected Set<Integer> getNoSortSlotIndexes() {
		SettingsHandler settingsHandler = storageWrapper.getSettingsHandler();
		Set<Integer> slotIndexesExcludedFromSort = new HashSet<>();
		slotIndexesExcludedFromSort.addAll(settingsHandler.getTypeCategory(NoSortSettingsCategory.class).getNoSortSlots());
		slotIndexesExcludedFromSort.addAll(settingsHandler.getTypeCategory(MemorySettingsCategory.class).getSlotIndexes());
		return slotIndexesExcludedFromSort;
	}

	@Override
	public void broadcastFullState() {
		broadcastFullStateOf(lastUpgradeSlots, upgradeSlots, getFirstUpgradeSlot());
		broadcastFullStateOf(lastRealSlots, realInventorySlots, 0);

		sendAllDataToRemote();
	}

	private void broadcastFullStateOf(NonNullList<ItemStack> lastSlotsCollection, List<Slot> slotsCollection, int slotIndexOffset) {
		for (int i = 0; i < slotsCollection.size(); ++i) {
			ItemStack itemstack = slotsCollection.get(i).getItem();
			triggerSlotListeners(i, itemstack, itemstack::copy, lastSlotsCollection, slotIndexOffset);
		}
	}

	protected void triggerSlotListeners(int stackIndex, ItemStack slotStack, Supplier<ItemStack> slotStackCopy, NonNullList<ItemStack> lastSlotsCollection, int slotIndexOffset) {
		ItemStack itemstack = lastSlotsCollection.get(stackIndex);
		if (!ItemStack.matches(itemstack, slotStack)) {
			boolean clientStackChanged = !slotStack.equals(itemstack, true);
			ItemStack stackCopy = slotStackCopy.get();
			lastSlotsCollection.set(stackIndex, stackCopy);

			if (clientStackChanged) {
				for (ContainerListener containerlistener : containerListeners) {
					containerlistener.slotChanged(this, stackIndex + slotIndexOffset, stackCopy);
				}
			}
		}
	}

	@Override
	public void sendAllDataToRemote() {
		for (int i = 0; i < getInventorySlotsSize(); ++i) {
			remoteRealSlots.set(i, realInventorySlots.get(i).getItem().copy());
		}

		for (int i = 0; i < upgradeSlots.size(); ++i) {
			remoteUpgradeSlots.set(i, upgradeSlots.get(i).getItem().copy());
		}

		NonNullList<ItemStack> allRemoteSlots = NonNullList.create();
		allRemoteSlots.addAll(remoteRealSlots);
		allRemoteSlots.addAll(remoteUpgradeSlots);

		remoteCarried = getCarried().copy();

		if (synchronizer != null) {
			synchronizer.sendInitialData(this, allRemoteSlots, remoteCarried, new int[]{});
		}

		sendEmptySlotIcons();
		sendAdditionalSlotInfo();
	}

	private void sendEmptySlotIcons() {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		Map<ResourceLocation, Set<Integer>> noItemSlotTextures = new HashMap<>();
		for (int slot = 0; slot < storageWrapper.getInventoryHandler().getSlots(); slot++) {
			Pair<ResourceLocation, ResourceLocation> noItemIcon = storageWrapper.getInventoryHandler().getNoItemIcon(slot);
			if (noItemIcon != null) {
				noItemSlotTextures.computeIfAbsent(noItemIcon.getSecond(), rl -> new HashSet<>()).add(slot);
			}
		}
		PacketHandler.INSTANCE.sendToClient(serverPlayer, new SyncEmptySlotIconsMessage(noItemSlotTextures));
	}

	private void sendAdditionalSlotInfo() {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		Set<Integer> inaccessibleSlots = new HashSet<>();
		Map<Integer, Integer> slotLimitOverrides = new HashMap<>();
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		Map<Integer, Item> slotFilterItems = new HashMap<>();
		for (int slot = 0; slot < inventoryHandler.getSlots(); slot++) {
			if (!inventoryHandler.isSlotAccessible(slot)) {
				inaccessibleSlots.add(slot);
			}
			ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
			int stackLimit = inventoryHandler.getStackLimit(slot, stackInSlot);
			if (stackLimit != inventoryHandler.getBaseStackLimit(stackInSlot)) {
				slotLimitOverrides.put(slot, stackLimit);
			}
			if (inventoryHandler.getFilterItem(slot) != Items.AIR) {
				slotFilterItems.put(slot, inventoryHandler.getFilterItem(slot));
			}
		}
		PacketHandler.INSTANCE.sendToClient(serverPlayer, new SyncAdditionalSlotInfoMessage(inaccessibleSlots, slotLimitOverrides, slotFilterItems));
	}

	@Override
	public void setRemoteSlot(int slotIndex, ItemStack stack) {
		if (slotIndex < getInventorySlotsSize()) {
			remoteRealSlots.set(slotIndex, stack.copy());
		} else {
			remoteUpgradeSlots.set(slotIndex, stack.copy());
		}
	}

	@Override
	public void setRemoteSlotNoCopy(int slotIndex, ItemStack stack) {
		if (slotIndex < getInventorySlotsSize()) {
			ItemStack previous = remoteRealSlots.get(slotIndex);
			remoteRealSlots.set(slotIndex, stack);

			if (previous.isEmpty() || stack.isEmpty()) {
				inventorySlotStackChanged = true;
			}
		} else {
			remoteUpgradeSlots.set(slotIndex - getInventorySlotsSize(), stack);
		}
	}

	@Override
	public OptionalInt findSlot(Container container, int slotIdx) {
		for (int i = 0; i < getTotalSlotsNumber(); ++i) {
			Slot slot = getSlot(i);
			if (slot.container == container && slotIdx == slot.getContainerSlot()) {
				return OptionalInt.of(i);
			}
		}
		return OptionalInt.empty();
	}

	private void refreshAllSlots() {
		slots.clear();
		lastSlots.clear();
		realInventorySlots.clear();
		lastRealSlots.clear();
		remoteRealSlots.clear();
		upgradeSlots.clear();
		lastUpgradeSlots.clear();
		remoteUpgradeSlots.clear();
		upgradeContainers.clear();

		initSlotsAndContainers(player, storageItemSlotIndex, shouldLockStorageItemSlot);
		slotsChangedSinceStartOfClick = true;
	}

	protected ItemStack processOverflowLogic(ItemStack stack) {
		ItemStack result = stack;
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)) {
			if (overflowUpgrade.worksInGui()) {
				result = overflowUpgrade.onOverflow(result);
				if (result.isEmpty()) {
					break;
				}
			}
		}
		return result;
	}

	private void onSwapCraft(Slot slot, int numItemsCrafted) {
		try {
			ON_SWAP_CRAFT.invoke(slot, numItemsCrafted);
		} catch (IllegalAccessException | InvocationTargetException e) {
			SophisticatedCore.LOGGER.error("Error invoking onSwapCraft method in Slot class", e);
		}
	}

	//copy of Container's doClick with the replacement of inventorySlots.get to getSlot, call to onswapcraft as that's protected in vanilla and an addition of upgradeSlots to pickup all
	@SuppressWarnings("java:S3776")
	//complexity here is brutal, but it's something that's in vanilla and need to keep this as close to it as possible for easier ports
	@Override
	protected void doClick(int slotId, int dragType, ClickType clickType, Player player) {
		slotsChangedSinceStartOfClick = false;
		Inventory inventory = player.getInventory();
		if (clickType == ClickType.QUICK_CRAFT) {
			int i = quickcraftStatus;
			quickcraftStatus = getQuickcraftHeader(dragType);
			if ((i != 1 || quickcraftStatus != 2) && i != quickcraftStatus) {
				resetQuickCraft();
			} else if (getCarried().isEmpty()) {
				resetQuickCraft();
			} else if (quickcraftStatus == 0) {
				quickcraftType = getQuickcraftType(dragType);
				if (isValidQuickcraftType(quickcraftType, player)) {
					quickcraftStatus = 1;
					quickcraftSlots.clear();
				} else {
					resetQuickCraft();
				}
			} else if (quickcraftStatus == 1) {
				Slot slot = getSlot(slotId);
				ItemStack itemstack = getCarried();
				if (StorageContainerMenuBase.canItemQuickReplace(slot, itemstack) && slot.mayPlace(itemstack) && (quickcraftType == 2 || itemstack.getCount() > quickcraftSlots.size()) && canDragTo(slot)) {
					quickcraftSlots.add(slot);
				}
			} else if (quickcraftStatus == 2) {
				if (!quickcraftSlots.isEmpty()) {
					if (quickcraftSlots.size() == 1) {
						int l = (quickcraftSlots.iterator().next()).index;
						resetQuickCraft();
						clicked(l, quickcraftType, ClickType.PICKUP, player);
						return;
					}

					ItemStack carried = getCarried().copy();
					int j1 = getCarried().getCount();

					for (Slot slot1 : quickcraftSlots) {
						ItemStack itemstack1 = getCarried();
						if (slot1 != null && StorageContainerMenuBase.canItemQuickReplace(slot1, itemstack1) && slot1.mayPlace(itemstack1) && (quickcraftType == 2 || itemstack1.getCount() >= quickcraftSlots.size()) && canDragTo(slot1)) {
							ItemStack carriedCopy = carried.copy();

							int j = slot1.hasItem() ? slot1.getItem().getCount() : 0;
							int slotStackLimit = slot1.getMaxStackSize(carriedCopy);
							if (!(slot1 instanceof StorageInventorySlot) && slotStackLimit > carriedCopy.getMaxStackSize()) {
								slotStackLimit = carriedCopy.getMaxStackSize();
							}

							int l = Math.min(getQuickCraftPlaceCount(this.quickcraftSlots, this.quickcraftType, carriedCopy) + j, slotStackLimit);
							j1 -= l - j;
							slot1.setByPlayer(carriedCopy.copyWithCount(l));
						}
					}

					carried.setCount(j1);
					setCarried(carried);
				}

				resetQuickCraft();
			} else {
				resetQuickCraft();
			}
		} else if (quickcraftStatus != 0) {
			resetQuickCraft();
		} else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
			ClickAction clickaction = dragType == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
			if (slotId == -999) {
				if (!getCarried().isEmpty()) {
					if (clickaction == ClickAction.PRIMARY) {
						player.drop(getCarried(), true);
						setCarried(ItemStack.EMPTY);
					} else {
						player.drop(getCarried().split(1), true);
					}
				}
			} else if (clickType == ClickType.QUICK_MOVE) {
				if (slotId < 0) {
					return;
				}

				Slot slot6 = getSlot(slotId);
				if (!slot6.mayPickup(player)) {
					return;
				}

				if (isStorageInventorySlot(slotId)) {
					quickMoveStack(this.player, slotId).copy();
				} else {
					ItemStack itemstack8 = quickMoveStack(this.player, slotId);
					while (!slotsChangedSinceStartOfClick && !itemstack8.isEmpty() && ItemStack.isSameItem(slot6.getItem(), itemstack8)) {
						itemstack8 = quickMoveStack(this.player, slotId);
					}
				}
			} else {
				if (slotId < 0) {
					return;
				}

				Slot slot7 = getSlot(slotId);
				ItemStack slotStack = slot7.getItem();
				ItemStack carriedStack = getCarried();
				player.updateTutorialInventoryAction(carriedStack, slot7.getItem(), clickaction);
				if (!carriedStack.overrideStackedOnOther(slot7, clickaction, player) && !slotStack.overrideOtherStackedOnMe(carriedStack, slot7, clickaction, player, createCarriedSlotAccess())) {
					if (slotStack.isEmpty()) {
						if (!carriedStack.isEmpty()) {
							int l2 = clickaction == ClickAction.PRIMARY ? carriedStack.getCount() : 1;
							setCarried(slot7.safeInsert(carriedStack, l2));
						}
					} else if (slot7.mayPickup(player)) {
						if (carriedStack.isEmpty()) {
							int i3 = clickaction == ClickAction.PRIMARY ? Math.min(slotStack.getCount(), slotStack.getMaxStackSize()) : Math.min(slotStack.getMaxStackSize() + 1, slotStack.getCount() + 1) / 2;
							Optional<ItemStack> optional1 = slot7.tryRemove(i3, Integer.MAX_VALUE, player);
							optional1.ifPresent((p_150421_) -> {
								setCarried(p_150421_);
								slot7.onTake(player, p_150421_);
							});
						} else if (slot7.mayPlace(carriedStack)) {
							if (ItemStack.isSameItemSameTags(slotStack, carriedStack)) {
								int j3 = clickaction == ClickAction.PRIMARY ? carriedStack.getCount() : 1;
								setCarried(slot7.safeInsert(carriedStack, j3));
							} else if (carriedStack.getCount() <= slot7.getMaxStackSize(carriedStack) && slotStack.getCount() <= slotStack.getMaxStackSize()) {
								slot7.set(carriedStack);
								setCarried(slotStack);
							}
						} else if (ItemStack.isSameItemSameTags(slotStack, carriedStack)) {
							Optional<ItemStack> optional = slot7.tryRemove(slotStack.getCount(), carriedStack.getMaxStackSize() - carriedStack.getCount(), player);
							optional.ifPresent((p_150428_) -> {
								carriedStack.grow(p_150428_.getCount());
								slot7.onTake(player, p_150428_);
							});
						}
					}
				}

				slot7.setChanged();
			}
		} else if (clickType == ClickType.SWAP) {
			Slot slot2 = getSlot(slotId);
			ItemStack itemstack4 = inventory.getItem(dragType);
			ItemStack slotStack = slot2.getItem();
			if (!itemstack4.isEmpty() || !slotStack.isEmpty()) {
				if (itemstack4.isEmpty()) {
					if (slot2.mayPickup(player)) {
						if (slotStack.getCount() <= slotStack.getMaxStackSize()) {
							inventory.setItem(dragType, slotStack);
							onSwapCraft(slot2, slotStack.getCount());
							slot2.set(ItemStack.EMPTY);
							slot2.onTake(player, slotStack);
						} else {
							inventory.setItem(dragType, slotStack.split(slotStack.getMaxStackSize()));
							slot2.setChanged();
						}
					}
				} else if (slotStack.isEmpty()) {
					if (slot2.mayPlace(itemstack4)) {
						int l1 = slot2.getMaxStackSize(itemstack4);
						if (itemstack4.getCount() > l1) {
							slot2.set(itemstack4.split(l1));
						} else {
							slot2.set(itemstack4);
							inventory.setItem(dragType, ItemStack.EMPTY);
						}
					}
				} else if (slotStack.getCount() <= slotStack.getMaxStackSize() && slot2.mayPickup(player) && slot2.mayPlace(itemstack4)) {
					int i2 = slot2.getMaxStackSize(itemstack4);
					if (itemstack4.getCount() > i2) {
						slot2.set(itemstack4.split(i2));
						slot2.onTake(player, slotStack);
						if (!inventory.add(slotStack)) {
							player.drop(slotStack, true);
						}
					} else {
						slot2.set(itemstack4);
						inventory.setItem(dragType, slotStack);
						slot2.onTake(player, slotStack);
					}
				}
			}
		} else if (clickType == ClickType.CLONE && player.getAbilities().instabuild && getCarried().isEmpty() && slotId >= 0) {
			Slot slot5 = getSlot(slotId);
			if (slot5.hasItem()) {
				ItemStack itemstack6 = slot5.getItem().copy();
				itemstack6.setCount(itemstack6.getMaxStackSize());
				setCarried(itemstack6);
			}
		} else if (clickType == ClickType.THROW && getCarried().isEmpty() && slotId >= 0) {
			Slot slot4 = getSlot(slotId);
			int i1 = dragType == 0 ? 1 : slot4.getItem().getCount();
			ItemStack itemstack8 = slot4.safeTake(i1, slot4.getItem().getMaxStackSize(), player);
			player.drop(itemstack8, true);
		} else if (clickType == ClickType.PICKUP_ALL && slotId >= 0) {
			Slot slot3 = getSlot(slotId);
			ItemStack carriedStack = getCarried();
			if (!carriedStack.isEmpty() && (!slot3.hasItem() || !slot3.mayPickup(player))) {
				int k1 = dragType == 0 ? 0 : getInventorySlotsSize() - 1;
				int j2 = dragType == 0 ? 1 : -1;

				for (int k2 = 0; k2 < 2; ++k2) {
					for (int k3 = k1; k3 >= 0 && k3 < getInventorySlotsSize() && carriedStack.getCount() < carriedStack.getMaxStackSize(); k3 += j2) {
						Slot slot8 = getSlot(k3);
						if (slot8.hasItem() && StorageContainerMenuBase.canItemQuickReplace(slot8, carriedStack) && slot8.mayPickup(player) && canTakeItemForPickAll(carriedStack, slot8)) {
							ItemStack itemstack12 = slot8.getItem();
							if (k2 != 0 || itemstack12.getCount() != itemstack12.getMaxStackSize()) {
								ItemStack itemstack13 = slot8.safeTake(itemstack12.getCount(), carriedStack.getMaxStackSize() - carriedStack.getCount(), player);
								carriedStack.grow(itemstack13.getCount());
							}
						}
					}
				}

				k1 = dragType == 0 ? 0 : upgradeSlots.size() - 1;

				for (int j = 0; j < 2; ++j) {
					for (int upgradeSlotId = k1; upgradeSlotId >= 0 && upgradeSlotId < upgradeSlots.size() && carriedStack.getCount() < carriedStack.getMaxStackSize(); upgradeSlotId += j2) {
						Slot upgradeSlot = upgradeSlots.get(upgradeSlotId);
						if (upgradeSlot.hasItem() && StorageContainerMenuBase.canItemQuickReplace(upgradeSlot, carriedStack) && upgradeSlot.mayPickup(this.player) && canTakeItemForPickAll(carriedStack, upgradeSlot)) {
							ItemStack itemstack3 = upgradeSlot.getItem();
							if (j != 0 || itemstack3.getCount() != itemstack3.getMaxStackSize()) {
								int l = Math.min(carriedStack.getMaxStackSize() - carriedStack.getCount(), itemstack3.getCount());
								ItemStack upgradeStack = upgradeSlot.remove(l);
								carriedStack.grow(l);
								if (upgradeStack.isEmpty()) {
									upgradeSlot.set(ItemStack.EMPTY);
								}

								upgradeSlot.onTake(this.player, upgradeStack);
							}
						}
					}
				}
			}
		}

		sendSlotUpdates();
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack pStack, Slot slot) {
		if (isUpgradeSlot(slot.index) && slot.getItem().getItem() instanceof IUpgradeItem<?> upgradeItem && upgradeItem.getInventoryColumnsTaken() > 0) {
			return false;
		}

		return super.canTakeItemForPickAll(pStack, slot);
	}

	public void sendSlotUpdates() {
		if (!player.level().isClientSide) {
			ServerPlayer serverPlayer = (ServerPlayer) player;
			slotStacksToUpdate.forEach((slot, stack) -> serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(serverPlayer.containerMenu.containerId, incrementStateId(), slot, stack)));
			slotStacksToUpdate.clear();
		}
	}

	@Override
	public void removed(Player player) {
		for (Slot slot : upgradeSlots) {
			if (!(slot instanceof StorageContainerMenuBase<?>.StorageUpgradeSlot) && isInventorySlotInUpgradeTab(player, slot) && shouldSlotItemBeDroppedFromStorage(slot)) {
				ItemStack slotStack = slot.getItem();
				slot.set(ItemStack.EMPTY);
				if (!player.addItem(slotStack)) {
					player.drop(slotStack, false);
				}
			}
		}
		super.removed(player);
		if (!player.level().isClientSide) {
			removeOpenTabIfKeepOff();
		}
	}

	/**
	 * @param sourceStack                    stack to merge
	 * @param startIndex                     index to start at inclusive
	 * @param endIndex                       index to end at exclusive
	 * @param reverseDirection               whether to insert into slots in reverse direction
	 * @param transferMaxStackSizeFromSource Whether to transfer max stack size even when stack size is expanded by stack upgrades
	 * @param runOverflowLogic               whether to run overflow logic
	 * @return remaining sourceStack after merge
	 */
	protected ItemStack mergeItemStack(ItemStack sourceStack, int startIndex, int endIndex, boolean reverseDirection, boolean transferMaxStackSizeFromSource, boolean runOverflowLogic) {
		boolean mergedSomething = false;
		int i = startIndex;
		if (reverseDirection) {
			i = endIndex - 1;
		}

		ItemStack result = sourceStack.copy();

		int toTransfer = transferMaxStackSizeFromSource ? Math.min(result.getMaxStackSize(), result.getCount()) : result.getCount();
		if (runOverflowLogic || result.isStackable() || getSlot(startIndex).getMaxStackSize() > 64) {
			while (toTransfer > 0) {
				if (reverseDirection) {
					if (i < startIndex) {
						break;
					}
				} else if (i >= endIndex) {
					break;
				}

				Slot slot = getSlot(i);
				if (slot.mayPlace(result)) { //Added to vanilla logic as some slots may not want anything to be added to them
					ItemStack destStack = slot.getItem();
					if (!destStack.isEmpty() && ItemStack.isSameItemSameTags(result, destStack)) {
						int j = destStack.getCount() + toTransfer;
						int maxSize = slot.getMaxStackSize(result);
						if (j <= maxSize) {
							result.shrink(toTransfer);
							ItemStack copy = destStack.copy();
							copy.setCount(j);
							slot.set(copy);
							toTransfer = 0;
							slot.setChanged();
							mergedSomething = true;
						} else if (destStack.getCount() < maxSize) {
							result.shrink(maxSize - destStack.getCount());
							toTransfer -= maxSize - destStack.getCount();
							ItemStack copy = destStack.copy();
							copy.setCount(maxSize);
							slot.set(copy);
							slot.setChanged();
							mergedSomething = true;
						}

						if (runOverflowLogic && !result.isEmpty()) {
							ItemStack overflowResult = processOverflowLogic(result);
							if (overflowResult != result) {
								result.setCount(overflowResult.getCount());
								mergedSomething = true;
							}
						}
					}
				}

				if (reverseDirection) {
					--i;
				} else {
					++i;
				}
			}
		}

		if (toTransfer > 0) {
			int firstIndex = reverseDirection ? endIndex - 1 : startIndex;
			int increment = reverseDirection ? -1 : 1;

			MemorySettingsCategory memory = storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			for (int slotIndex = firstIndex; (reverseDirection ? slotIndex >= startIndex : slotIndex < endIndex) && toTransfer > 0; slotIndex += increment) {
				if (memory.isSlotSelected(slotIndex) && memory.matchesFilter(slotIndex, result)) {
					Slot slot = getSlot(slotIndex);
					if (!slot.mayPlace(result)) {
						continue;
					}
					ItemStack destStack = slot.getItem();
					if (destStack.isEmpty()) {
						slot.set(result.split(slot.getMaxStackSize()));
						slot.setChanged();
						toTransfer = result.getCount();
						mergedSomething = true;
					}
				}
			}
		}

		if (toTransfer > 0) {
			if (reverseDirection) {
				i = endIndex - 1;
			} else {
				i = startIndex;
			}

			while (true) {
				if (reverseDirection) {
					if (i < startIndex) {
						break;
					}
				} else if (i >= endIndex) {
					break;
				}

				Slot destSlot = getSlot(i);
				ItemStack itemstack1 = destSlot.getItem();
				if (itemstack1.isEmpty() && destSlot.mayPlace(result) && !(destSlot instanceof IFilterSlot)) {
					boolean errorMerging = false;
					if (toTransfer > destSlot.getMaxStackSize()) {
						if (runOverflowLogic && processOverflowIfSlotWithSameItemFound(i, result, s -> {})) {
							result.shrink(result.getCount());
							mergedSomething = true;
						} else {
							if (isUpgradeSlot(i)) {
								IUpgradeItem<?> upgradeItem = (IUpgradeItem<?>) result.getItem();
								int newColumnsTaken = upgradeItem.getInventoryColumnsTaken();
								if (!needsSlotsThatAreOccupied(result, 0, newColumnsTaken)) {
									destSlot.set(result.split(destSlot.getMaxStackSize()));
									updateColumnsTaken(newColumnsTaken);
								} else {
									errorMerging = true;
								}
							} else {
								destSlot.set(result.split(destSlot.getMaxStackSize()));
							}
						}
					} else {
						if (isUpgradeSlot(i)) {
							IUpgradeItem<?> upgradeItem = (IUpgradeItem<?>) result.getItem();
							int newColumnsTaken = upgradeItem.getInventoryColumnsTaken();
							if (!needsSlotsThatAreOccupied(result, 0, newColumnsTaken)) {
								destSlot.set(result.split(toTransfer));
								updateColumnsTaken(newColumnsTaken);
							} else {
								errorMerging = true;
							}
						} else {
							if (runOverflowLogic && processOverflowIfSlotWithSameItemFound(i, result, s -> {})) {
								result.shrink(result.getCount());
								mergedSomething = true;
							} else {
								destSlot.set(result.split(toTransfer));
							}
						}
					}
					if (!errorMerging) {
						destSlot.setChanged();
						mergedSomething = true;
						break;
					}
				}

				if (reverseDirection) {
					--i;
				} else {
					++i;
				}
			}
		}

		return result;
	}

	protected boolean moveItemStackTo(Slot sourceSlot, ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
		return mergeItemStack(sourceSlot, stack, startIndex, endIndex, reverseDirection, false);
	}

	protected boolean mergeItemStack(Slot sourceSlot, ItemStack sourceStack, int startIndex, int endIndex, boolean reverseDirection, boolean transferMaxStackSizeFromSource) {
		ItemStack remaining = mergeItemStack(sourceStack, startIndex, endIndex, reverseDirection, transferMaxStackSizeFromSource, false);
		if (remaining.getCount() != sourceStack.getCount()) {
			sourceSlot.set(remaining);
			return true;
		}
		return false;
	}

	@Override
	public void setSynchronizer(ContainerSynchronizer synchronizer) {
		if (player instanceof ServerPlayer serverPlayer) {
			super.setSynchronizer(new HighStackCountSynchronizer(serverPlayer));
			return;
		}
		super.setSynchronizer(synchronizer);
	}

	public static boolean canItemQuickReplace(@Nullable Slot slot, ItemStack stack) {
		boolean flag = slot == null || !slot.hasItem();
		if (!flag && ItemStack.isSameItemSameTags(stack, slot.getItem())) {
			return slot.getItem().getCount() <= slot.getMaxStackSize(stack);
		} else {
			return flag;
		}
	}

	@Override
	public Slot getSlot(int slotId) {
		if (slotId >= getInventorySlotsSize()) {
			return upgradeSlots.get(slotId - getInventorySlotsSize());
		} else {
			return realInventorySlots.get(slotId);
		}
	}

	@Override
	public void setItem(int slotId, int pStateId, ItemStack pStack) {
		if (getTotalSlotsNumber() > slotId) {
			super.setItem(slotId, pStateId, pStack);
		}
	}

	@Override
	public void broadcastChanges() {
		closeScreenIfSomethingMessedWithStorageItemStack();

		synchronizeCarriedToRemote();
		broadcastChangesIn(lastUpgradeSlots, remoteUpgradeSlots, upgradeSlots, getFirstUpgradeSlot());
		broadcastChangesIn(lastRealSlots, remoteRealSlots, realInventorySlots, 0);

		if (inventorySlotStackChanged) {
			inventorySlotStackChanged = false;
			sendAdditionalSlotInfo();
		}

		if (lastSettingsNbt == null || !lastSettingsNbt.equals(storageWrapper.getSettingsHandler().getNbt())) {
			lastSettingsNbt = storageWrapper.getSettingsHandler().getNbt().copy();
			sendStorageSettingsToClient();
			refreshInventorySlotsIfNeeded();
		}
	}


	public Optional<ItemStack> getVisibleStorageItem() {
		return storageItemSlotNumber != -1 ? Optional.of(getSlot(storageItemSlotNumber).getItem()) : Optional.empty();
	}

	private void broadcastChangesIn(NonNullList<ItemStack> lastSlotsCollection, NonNullList<ItemStack> remoteSlotsCollection, List<Slot> slotsCollection, int slotIndexOffset) {
		for (int i = 0; i < slotsCollection.size(); ++i) {
			ItemStack itemstack = slotsCollection.get(i).getItem();
			Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
			triggerSlotListeners(i, itemstack, supplier, lastSlotsCollection, slotIndexOffset);
			synchronizeSlotToRemote(i, itemstack, supplier, remoteSlotsCollection, slotIndexOffset);
		}
	}

	private void synchronizeSlotToRemote(int slotIndex, ItemStack slotStack, Supplier<ItemStack> slotStackCopy, NonNullList<ItemStack> remoteSlotsCollection, int slotIndexOffset) {
		if (!suppressRemoteUpdates) {
			ItemStack remoteStack = remoteSlotsCollection.get(slotIndex);
			if (!ItemStack.matches(remoteStack, slotStack)) {
				ItemStack stackCopy = slotStackCopy.get();
				remoteSlotsCollection.set(slotIndex, stackCopy);
				if (isStorageInventorySlot(slotIndex) && (remoteStack.isEmpty() || slotStack.isEmpty())) {
					inventorySlotStackChanged = true;
				}
				if (synchronizer != null) {
					synchronizer.sendSlotChange(this, slotIndex + slotIndexOffset, stackCopy);
				}
			}
		}
	}

	protected void refreshInventorySlotsIfNeeded() {
		Set<Integer> noSortSlotIndexes = getNoSortSlotIndexes();
		boolean needRefresh = false;
		if (getInventorySlotsSize() - slots.size() != noSortSlotIndexes.size()) {
			needRefresh = true;
		} else {
			for (Slot slot : realInventorySlots) {
				if (!slots.contains(slot) && !noSortSlotIndexes.contains(slot.index)) {
					needRefresh = true;
					break;
				}
			}
		}

		if (!needRefresh) {
			return;
		}

		slots.clear();
		lastSlots.clear();
		realInventorySlots.clear();
		lastRealSlots.clear();
		remoteRealSlots.clear();
		addStorageInventorySlots();
		addPlayerInventorySlots(player.getInventory(), storageItemSlotIndex, shouldLockStorageItemSlot);
	}

	@Override
	public NonNullList<ItemStack> getItems() {
		NonNullList<ItemStack> list = NonNullList.create();

		realInventorySlots.forEach(slot -> list.add(slot.getItem()));
		upgradeSlots.forEach(upgradeSlot -> list.add(upgradeSlot.getItem()));
		return list;
	}

	public abstract boolean detectSettingsChangeAndReload();

	@SuppressWarnings("java:S1172") // slot parameter is used in overrides
	protected boolean shouldSlotItemBeDroppedFromStorage(Slot slot) {
		return false;
	}

	private boolean isInventorySlotInUpgradeTab(Player player, Slot slot) {
		return slot.mayPickup(player) && !(slot instanceof ResultSlot);
	}

	public void setSlotStackToUpdate(int slot, ItemStack stack) {
		slotStacksToUpdate.put(slot, stack);
	}

	private void reloadUpgradeControl() {
		if (!isUpdatingFromPacket) {
			storageWrapper.removeOpenTabId();
		}
		removeUpgradeSettingsSlots();
		upgradeContainers.clear();
		addUpgradeSettingsContainers(player);
		onUpgradesChanged();
	}

	private void removeUpgradeSettingsSlots() {
		List<Integer> slotNumbersToRemove = new ArrayList<>();
		for (UpgradeContainerBase<?, ?> container : upgradeContainers.values()) {
			container.getSlots().forEach(slot -> {
				int upgradeSlotIndex = slot.index - getInventorySlotsSize();
				slotNumbersToRemove.add(upgradeSlotIndex);
				upgradeSlots.remove(slot);
			});
		}
		slotNumbersToRemove.sort(IntComparators.OPPOSITE_COMPARATOR);
		for (int slotNumber : slotNumbersToRemove) {
			lastUpgradeSlots.remove(slotNumber);
			remoteUpgradeSlots.remove(slotNumber);
		}
	}

	private void onUpgradesChanged() {
		if (upgradeChangeListener != null) {
			upgradeChangeListener.accept(StorageContainerMenuBase.this);
		}

		sendEmptySlotIcons();
		sendAdditionalSlotInfo();
	}

	@Override
	public void updateAdditionalSlotInfo(Set<Integer> inaccessibleSlots, Map<Integer, Integer> slotLimitOverrides, Map<Integer, Item> slotFilterItems) {
		this.inaccessibleSlots.clear();
		this.inaccessibleSlots.addAll(inaccessibleSlots);

		this.slotLimitOverrides.clear();
		this.slotLimitOverrides.putAll(slotLimitOverrides);

		this.slotFilterItems.clear();
		slotFilterItems.forEach((slot, item) -> this.slotFilterItems.put(slot, new ItemStack(item)));
	}

	@Override
	public void updateEmptySlotIcons(Map<ResourceLocation, Set<Integer>> emptySlotIcons) {
		this.emptySlotIcons.clear();
		emptySlotIcons.forEach((textureName, slots) -> slots.forEach(slot -> this.emptySlotIcons.put(slot, new Pair<>(InventoryMenu.BLOCK_ATLAS, textureName))));
	}

	public ItemStack getSlotFilterItem(int slot) {
		return slotFilterItems.getOrDefault(slot, ItemStack.EMPTY);
	}

	public void updateSlotChangeError(UpgradeSlotChangeResult result) {
		if (player.level().isClientSide && !result.isSuccessful()) {
			errorUpgradeSlotChangeResult = result;
			errorResultExpirationTime = player.level().getGameTime() + 60;
		} else if (!player.level().isClientSide() && !result.isSuccessful()) {
			PacketHandler.INSTANCE.sendToClient((ServerPlayer) player, new SyncSlotChangeErrorMessage(result));
		}
	}

	public class StorageUpgradeSlot extends SlotItemHandler {
		private boolean wasEmpty = false;
		private final int slotIndex;

		public StorageUpgradeSlot(UpgradeHandler upgradeHandler, int slotIndex) {
			super(upgradeHandler, slotIndex, -15, 0);
			this.slotIndex = slotIndex;
		}

		@Override
		public void setChanged() {
			super.setChanged();
			if ((!isUpdatingFromPacket && wasEmpty != getItem().isEmpty()) || updateWrappersAndCheckForReloadNeeded()) {
				reloadUpgradeControl();
				if (!isFirstLevelStorage()) {
					parentStorageWrapper.getUpgradeHandler().refreshUpgradeWrappers();
				}
				onUpgradeChanged();
			}
			wasEmpty = getItem().isEmpty();
		}

		protected void onUpgradeChanged() {
			//noop by default
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			if (stack.isEmpty() || !getItemHandler().isItemValid(slotIndex, stack)) {
				return false;
			}
			UpgradeSlotChangeResult result;
			if (getItem().isEmpty()) {
				result = ((IUpgradeItem<?>) stack.getItem()).canAddUpgradeTo(storageWrapper, stack, isFirstLevelStorage(), player.level().isClientSide());
			} else if (stack.getCount() > 1) {
				return false;
			} else {
				result = ((IUpgradeItem<?>) getItem().getItem()).canSwapUpgradeFor(stack, storageWrapper, player.level().isClientSide());
			}

			updateSlotChangeError(result);
			return result.isSuccessful();
		}

		@Override
		public boolean mayPickup(Player player) {
			boolean ret = super.mayPickup(player);
			if (!ret) {
				return false;
			}

			UpgradeSlotChangeResult result = ((IUpgradeItem<?>) getItem().getItem()).canRemoveUpgradeFrom(storageWrapper, player.level().isClientSide());
			updateSlotChangeError(result);
			return result.isSuccessful();
		}

		public boolean canSwapStack(Player player, ItemStack stackToPut) {
			boolean ret = super.mayPickup(player);
			if (!ret || stackToPut.getCount() > 1) {
				return false;
			}
			UpgradeSlotChangeResult result = ((IUpgradeItem<?>) getItem().getItem()).canSwapUpgradeFor(stackToPut, storageWrapper, player.level().isClientSide());
			updateSlotChangeError(result);
			return result.isSuccessful();
		}

		private boolean updateWrappersAndCheckForReloadNeeded() {
			int checkedContainersCount = 0;
			for (Map.Entry<Integer, IUpgradeWrapper> slotWrapper : storageWrapper.getUpgradeHandler().getSlotWrappers().entrySet()) {
				UpgradeContainerBase<?, ?> container = upgradeContainers.get(slotWrapper.getKey());
				if (slotWrapper.getValue().hideSettingsTab()) {
					if (container != null) {
						return true;
					}
				} else if (container == null || container.getUpgradeWrapper().isEnabled() != slotWrapper.getValue().isEnabled()) {
					return true;
				} else if (container.getUpgradeWrapper() != slotWrapper.getValue()) {
					if (container.getUpgradeWrapper().getUpgradeStack().getItem() != slotWrapper.getValue().getUpgradeStack().getItem()) {
						return true;
					} else {
						container.setUpgradeWrapper(slotWrapper.getValue());
						checkedContainersCount++;
					}
				}
			}
			return checkedContainersCount != upgradeContainers.size();
		}

		@Nullable
		@Override
		public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
			return new Pair<>(InventoryMenu.BLOCK_ATLAS, StorageContainerMenuBase.EMPTY_UPGRADE_SLOT_BACKGROUND);
		}
	}
}
