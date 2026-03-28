package net.p3pp3rf1y.sophisticatedcore.common.gui;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntComparators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.HashedStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.world.inventory.StackCopySlot;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.network.*;
import net.p3pp3rf1y.sophisticatedcore.settings.ISlotColorCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.util.DummySlot;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.MathHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class StorageContainerMenuBase<S extends IStorageWrapper> extends AbstractContainerMenu implements IAdditionalSlotInfoMenu {
	public static final int NUMBER_OF_PLAYER_SLOTS = 36;
	public static final Identifier EMPTY_UPGRADE_SLOT_BACKGROUND = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "container/slot/upgrade");
	public static final Identifier INACCESSIBLE_SLOT_BACKGROUND = SophisticatedCore.getIdentifier("container/slot/inaccessible");
	protected static final String UPGRADE_ENABLED_TAG = "upgradeEnabled";
	protected static final String UPGRADE_SLOT_TAG = "upgradeSlot";
	protected static final String ACTION_TAG = "action";
	protected static final String OPEN_TAB_ID_TAG = "openTabId";
	protected static final String SORT_BY_TAG = "sortBy";
	private static final String SEARCH_PHRASE_TAG = "searchPhrase";
	private static final Method ON_SWAP_CRAFT = ObfuscationReflectionHelper.findMethod(Slot.class, "onSwapCraft", int.class);
	public final NonNullList<ItemStack> lastUpgradeSlots = NonNullList.create();
	public final List<Slot> upgradeSlots = Lists.newArrayList();
	public final NonNullList<RemoteSlot> remoteUpgradeSlots = NonNullList.create();
	public final NonNullList<ItemStack> lastRealSlots = NonNullList.create();
	public final List<Slot> realInventorySlots = Lists.newArrayList();
	private final Map<Integer, UpgradeContainerBase<?, ?>> upgradeContainers = new LinkedHashMap<>();
	private final NonNullList<RemoteSlot> remoteRealSlots = NonNullList.create();
	protected final Player player;
	protected final S storageWrapper;
	protected final IStorageWrapper parentStorageWrapper;
	private final int storageItemSlotIndex;
	private final boolean shouldLockStorageItemSlot;
	private final List<Slot> extraSlots;
	private int storageItemSlotNumber = -1;
	private Consumer<StorageContainerMenuBase<?>> upgradeChangeListener = null;
	private boolean isUpdatingFromPacket = false;
	private long errorResultExpirationTime = 0;
	@Nullable
	private UpgradeSlotChangeResult errorUpgradeSlotChangeResult;
	private ContainerContents.SettingsData lastSettingsData = null;
	private boolean inventorySlotStackChanged = false;
	private final Set<Integer> inaccessibleSlots = new HashSet<>();
	private final Map<Integer, Integer> slotLimitOverrides = new HashMap<>();
	private final Set<Integer> infiniteSlots = new HashSet<>();
	private final Map<Integer, ItemStack> slotFilterItems = new HashMap<>();
	private final Map<Integer, Identifier> emptySlotIcons = new HashMap<>();

	private boolean slotsChangedSinceStartOfClick = false;
	private boolean tryingToMergeUpgrade = false;
	private boolean initialBroadcast = true;

	private int extraSlotsSize = 0;

	private int columnsChange = 0;
	private int inventorySlotsBeforeClickHandled;

	protected StorageContainerMenuBase(MenuType<?> menuType, int containerId, Player player, S storageWrapper, IStorageWrapper parentStorageWrapper, int storageItemSlotIndex, boolean shouldLockStorageItemSlot) {
		this(menuType, containerId, player, storageWrapper, parentStorageWrapper, storageItemSlotIndex, shouldLockStorageItemSlot, Collections.emptyList());
	}

	protected StorageContainerMenuBase(MenuType<?> menuType, int containerId, Player player, S storageWrapper, IStorageWrapper parentStorageWrapper, int storageItemSlotIndex, boolean shouldLockStorageItemSlot, List<Slot> extraSlots) {
		super(menuType, containerId);
		this.player = player;
		this.storageWrapper = storageWrapper;
		this.parentStorageWrapper = parentStorageWrapper;
		this.storageItemSlotIndex = storageItemSlotIndex;
		this.shouldLockStorageItemSlot = shouldLockStorageItemSlot;
		this.extraSlots = extraSlots;

		removeOpenTabIfKeepOff();
		storageWrapper.fillWithLoot(player);
		initSlotsAndContainers(player, storageItemSlotIndex, shouldLockStorageItemSlot, extraSlots);

		inventorySlotsBeforeClickHandled = getInventorySlotsSize();
	}

	public abstract Optional<BlockPos> getBlockPosition();

	public abstract Optional<Entity> getEntity();

	protected void initSlotsAndContainers(Player player, int storageItemSlotIndex, boolean shouldLockStorageItemSlot, List<Slot> extraSlots) {
		addStorageInventorySlots();
		addPlayerInventorySlots(player.getInventory(), storageItemSlotIndex, shouldLockStorageItemSlot);
		addExtraSlots(extraSlots);
		addUpgradeSlots();
		addUpgradeSettingsContainers(player);
	}

	private void addExtraSlots(List<Slot> extraSlots) {
		extraSlots.forEach(this::addExtraSlot);
	}

	protected void addExtraSlot(Slot slot) {
		extraSlotsSize++;
		addSlot(slot);
	}

	public List<Slot> getExtraSlots() {
		return extraSlots;
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

		int numberOfSlots = upgradeHandler.size();

		if (numberOfSlots == 0) {
			return;
		}

		int slotIndex = 0;

		while (slotIndex < upgradeHandler.size()) {
			addUpgradeSlot(new StorageUpgradeSlot(upgradeHandler, slotIndex));

			slotIndex++;
		}
	}

	public int getColumnsTaken() {
		return storageWrapper.getColumnsTaken();
	}

	public Optional<UpgradeSlotChangeResult> getErrorUpgradeSlotChangeResult() {
		if (errorUpgradeSlotChangeResult != null && player.level().getGameTime() >= errorResultExpirationTime) {
			clearErrorUpgradeSlotChangeResult();
		}
		return Optional.ofNullable(errorUpgradeSlotChangeResult);
	}

	private void clearErrorUpgradeSlotChangeResult() {
		errorResultExpirationTime = 0;
		errorUpgradeSlotChangeResult = null;
	}

	protected void sendStorageSettingsToClient() {
		//noop by default
	}

	protected void addUpgradeSlot(Slot slot) {
		slot.index = getTotalSlotsNumber();
		upgradeSlots.add(slot);
		lastUpgradeSlots.add(ItemStack.EMPTY);
		remoteUpgradeSlots.add(synchronizer != null ? synchronizer.createSlot() : RemoteSlot.PLACEHOLDER);
	}

	protected void addNoSortSlot(Slot slot) {
		slot.index = getInventorySlotsSize();
		realInventorySlots.add(slot);
		lastRealSlots.add(ItemStack.EMPTY);
		remoteRealSlots.add(synchronizer != null ? synchronizer.createSlot() : RemoteSlot.PLACEHOLDER);
	}

	@Override
	protected Slot addSlot(Slot slot) {
		slot.index = getInventorySlotsSize();
		slots.add(slot);
		lastSlots.add(ItemStack.EMPTY);
		remoteSlots.add(synchronizer != null ? synchronizer.createSlot() : RemoteSlot.PLACEHOLDER);
		realInventorySlots.add(slot);
		lastRealSlots.add(ItemStack.EMPTY);
		remoteRealSlots.add(synchronizer != null ? synchronizer.createSlot() : RemoteSlot.PLACEHOLDER);
		return slot;
	}

	public int getInventorySlotsSize() {
		return realInventorySlots.size();
	}

	public int getNumberOfStorageInventorySlots() {
		return storageWrapper.getInventoryHandler().size();
	}

	public int getNumberOfUpgradeSlots() {
		return storageWrapper.getUpgradeHandler().size();
	}

	public Map<Integer, UpgradeContainerBase<?, ?>> getUpgradeContainers() {
		return upgradeContainers;
	}

	protected void addStorageInventorySlots() {
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		int slotIndex = 0;

		Set<Integer> noSortSlotIndexes = getNoSortSlotIndexes();
		while (slotIndex < inventoryHandler.size()) {
			int finalSlotIndex = slotIndex;
			StorageInventorySlot slot = new StorageInventorySlot(storageWrapper, finalSlotIndex, player) {
				@Nullable
				@Override
				public Identifier getNoItemIcon() {
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

				@Override
				public void set(ItemStack stack) {
					super.set(stack);
					onStorageInventorySlotSet(finalSlotIndex);
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

	public boolean hasSomethingMessedWithStorage() {
		return !isClientSide() && (storageItemHasChanged() || realInventorySlots.size() != storageWrapper.getInventoryHandler().size() + NUMBER_OF_PLAYER_SLOTS + extraSlotsSize);
	}

	protected boolean isClientSide() {
		return player.level().isClientSide();
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
		storageWrapper.setPersistent(player.level().isClientSide());
		isUpdatingFromPacket = true;
		super.initializeContents(stateId, items, carried);
		isUpdatingFromPacket = false;
		storageWrapper.setPersistent(true);
		storageWrapper.getInventoryHandler().saveInventory();
		storageWrapper.getUpgradeHandler().saveInventory();
	}

	protected boolean isUpgradeSettingsSlot(int index) {
		return index >= getNumberOfStorageInventorySlots() + getNumberOfUpgradeSlots() + StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS && index < getTotalSlotsNumber();
	}

	public boolean isStorageInventorySlot(int slotIndex) {
		return slotIndex >= 0 && slotIndex < getNumberOfStorageInventorySlots();
	}

	public boolean isStorageInventorySlot(Slot slot) {
		return slot instanceof StorageInventorySlot && isStorageInventorySlot(slot.index);
	}

	protected boolean isUpgradeSlot(int index) {
		return index >= getFirstUpgradeSlot() && (index - getFirstUpgradeSlot() < getNumberOfUpgradeSlots());
	}

	@Override
	public void clicked(int slotId, int dragType, ContainerInput clickType, Player player) {
		inventorySlotsBeforeClickHandled = getInventorySlotsSize();
		boolean handled = false;
		if (isUpgradeSettingsSlot(slotId) && getSlot(slotId) instanceof IFilterSlot && getSlot(slotId).mayPlace(getCarried())) {
			if (!player.level().isClientSide()) { // don't do slot updates on client to not prevent upgrade stacks from being synced from server when they are updated with these
				Slot slot = getSlot(slotId);
				ItemStack cursorStack = getCarried().copy();
				if (cursorStack.getCount() > 1) {
					cursorStack.setCount(1);
				}

				slot.set(cursorStack);
			}
			handled = true;
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
				if (!needsSlotsThatAreOccupied(carriedStack, currentColumnsTaken, newColumnsTaken)) {
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
					if (columnsToRemove != 0 && player.level().isClientSide()) {
						onUpgradesChanged(); // need to trigger onUpgradesChanged again so that screen can react to this with updating slot positions after slots were refreshed as part of columns update
					}
				}
			} else if (getCarried().isEmpty() && !slotStack.isEmpty() && slot.mayPickup(player)) {
				int k2 = dragType == 0 ? Math.min(slotStack.getCount(), slotStack.getMaxStackSize()) : Math.min(slotStack.getMaxStackSize() + 1, slotStack.getCount() + 1) / 2;
				IUpgradeItem<?> upgradeItem = (IUpgradeItem<?>) slotStack.getItem();
				int columnsTaken = upgradeItem.getInventoryColumnsTaken();
				if (clickType == ContainerInput.QUICK_MOVE) {
					quickMoveStack(player, slotId);
				} else {
					setCarried(upgradeItem.getCleanedUpgradeStack(slot.remove(k2)));
				}
				updateColumnsTaken(-columnsTaken);
				slot.onTake(player, getCarried());
			}
			handled = true;
		} else if (isOverflowLogicSlotAndAction(slotId, clickType) && handleOverflow(slotId, clickType, dragType, player)) {
			handled = true;
		}

		if (!handled) {
			super.clicked(slotId, dragType, clickType, player);
		}

		flushPendingColumnsChange();
	}

	private void flushPendingColumnsChange() {
		if (columnsChange == 0) {
			return;
		}

		if (!player.level().isClientSide()) {
			actuallyUpdateColumnsTaken(columnsChange);
			columnsChange = 0;
		}
	}

	@Override
	public boolean isValidSlotIndex(int slotIndex) {
		return slotIndex == -1 || slotIndex == -999 || slotIndex < getTotalSlotsNumber();
	}

	private boolean handleOverflow(int slotId, ContainerInput clickType, int dragType, Player player) {
		ItemStack cursorStack = clickType == ContainerInput.SWAP ? player.getInventory().getItem(dragType) : getCarried();
		Consumer<ItemStack> updateCursorStack = clickType == ContainerInput.SWAP ? s -> player.getInventory().setItem(dragType, s) : this::setCarried;
		Slot slot = getSlot(slotId);
		if ((clickType != ContainerInput.SWAP && cursorStack.isEmpty()) || !slot.mayPlace(cursorStack)) {
			return false;
		}
		ItemStack slotStack = slot.getItem();
		if (slotStack.isEmpty() || (slot.mayPickup(player) && slotStack.getItem() != cursorStack.getItem() && cursorStack.getCount() <= slot.getMaxStackSize(cursorStack) && slotStack.getCount() <= slotStack.getMaxStackSize())) {
			return processOverflowIfSlotWithSameItemFound(cursorStack, updateCursorStack);
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

	private boolean processOverflowIfSlotWithSameItemFound(ItemStack cursorStack, Consumer<ItemStack> updateCursorStack) {
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)) {
			if (overflowUpgrade.stackMatchesFilter(cursorStack) && overflowUpgrade.worksInGui()
					&& findSlotWithMatchingStack(cursorStack, updateCursorStack, overflowUpgrade)) {
				return true;
			}
		}
		return false;
	}

	private boolean findSlotWithMatchingStack(ItemStack cursorStack, Consumer<ItemStack> updateCursorStack, IOverflowResponseUpgrade overflowUpgrade) {
		if (storageWrapper.getInventoryHandler().getSlotTracker().getFullStacks().contains(ItemStackKey.of(cursorStack))) {
			ItemStack result = cursorStack;
			result = overflowUpgrade.onSlotOverflow(result);
			updateCursorStack.accept(result);
			if (result.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private boolean isOverflowLogicSlotAndAction(int slotId, ContainerInput clickType) {
		return isStorageInventorySlot(slotId) && (clickType == ContainerInput.SWAP || clickType == ContainerInput.PICKUP);
	}

	protected void updateColumnsTaken(int columnsChange) {
		this.columnsChange += columnsChange;
	}

	private void actuallyUpdateColumnsTaken(int columnsChange) {
		if (columnsChange != 0) {
			//when these get changed recalculate columns taken to fix columnsTaken out of sync issues
			AtomicInteger columnsTaken = new AtomicInteger(0);
			InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, resource, amount) -> {
				if (resource.getItem() instanceof UpgradeItemBase<?> upgradeItem) {
					columnsTaken.addAndGet(upgradeItem.getInventoryColumnsTaken());
				}
			});
			storageWrapper.setColumnsTaken(columnsTaken.get(), true);
			storageWrapper.onContentsUpdated();
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
			if (!invHandler.getResource(slotIndex).isEmpty()) {
				errorSlots.add(slotIndex);
			}
		}

		if (!errorSlots.isEmpty()) {
			updateSlotChangeError(UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.needs_occupied_inventory_slots", slotsToCheck, cursorStack.getHoverName()), Collections.emptySet(), errorSlots, Collections.emptySet()));
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
		ClientPacketDistributor.sendToServer(new SyncContainerClientDataPayload(data));
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

	public boolean isUpgradeRunnable(int upgradeSlot) {
		Map<Integer, IUpgradeWrapper> slotWrappers = storageWrapper.getUpgradeHandler().getSlotWrappers();
		if (!slotWrappers.containsKey(upgradeSlot)) {
			return false;
		}
		IUpgradeWrapper upgradeWrapper = slotWrappers.get(upgradeSlot);
		return !(upgradeWrapper instanceof ITickableUpgrade) || storageWrapper.isUpgradeRunnable(upgradeWrapper.getUpgradeStack());
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

	public void handlePacket(CompoundTag data) {
		data.getInt("containerId").ifPresent(containerId ->
				{
					if (upgradeContainers.containsKey(containerId)) {
						upgradeContainers.get(containerId).handlePacket(data);
					}
				}
		);
		data.getInt(OPEN_TAB_ID_TAG).ifPresent(this::setOpenTabId);
		data.getString(SORT_BY_TAG).ifPresent(sortByName -> setSortBy(SortBy.fromName(sortByName)));
		data.getString(SEARCH_PHRASE_TAG).ifPresent(this::setSearchPhrase);
		data.getString(ACTION_TAG).ifPresent(actionName -> {
			switch (actionName) {
				case "sort" -> sort();
				case "openSettings" -> openSettings();
				default -> {
					//noop
				}
			}
		});
		data.getBoolean(UPGRADE_ENABLED_TAG).ifPresent(enabled -> data.getInt(UPGRADE_SLOT_TAG).ifPresent(upgradeSlot -> setUpgradeEnabled(upgradeSlot, enabled)));
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
			return mergeStackToPlayersInventory(slot, slotStack) || mergeStackToExtraSlots(slot, slotStack) || mergeStackToStorage(slot, slotStack);
		} else if (isStorageInventorySlot(index)) {
			if (shouldShiftClickIntoOpenTabFirst()) {
				return mergeStackToOpenUpgradeTab(slot, slotStack) || mergeStackToPlayersInventory(slot, slotStack) || mergeStackToExtraSlots(slot, slotStack);
			}
			return mergeStackToPlayersInventory(slot, slotStack) || mergeStackToExtraSlots(slot, slotStack) || mergeStackToOpenUpgradeTab(slot, slotStack);
		} else if (isUpgradeSettingsSlot(index)) {
			if (getSlotUpgradeContainer(slot).map(c -> c.mergeIntoStorageFirst(slot)).orElse(true)) {
				return mergeStackToStorage(slot, slotStack) || mergeStackToPlayersInventory(slot, slotStack) || mergeStackToExtraSlots(slot, slotStack);
			}
			return mergeStackToPlayersInventory(slot, slotStack) || mergeStackToExtraSlots(slot, slotStack) || mergeStackToStorage(slot, slotStack);
		} else if (isExtraSlot(index)) {
			return mergeStackToPlayersInventory(slot, slotStack) || mergeStackToStorage(slot, slotStack) || mergeStackToOpenUpgradeTab(slot, slotStack);
		} else {
			if (shouldShiftClickIntoOpenTabFirst()) {
				return mergeStackToExtraSlots(slot, slotStack) || mergeStackToOpenUpgradeTab(slot, slotStack) || mergeStackToUpgradeSlots(slot, slotStack) || mergeStackToStorage(slot, slotStack);
			}
			return mergeStackToExtraSlots(slot, slotStack) || mergeStackToUpgradeSlots(slot, slotStack) || mergeStackToStorage(slot, slotStack) || mergeStackToOpenUpgradeTab(slot, slotStack);
		}
	}

	private boolean isExtraSlot(int slotIndex) {
		return slotIndex >= getInventorySlotsSize() - extraSlotsSize && slotIndex < getInventorySlotsSize();
	}

	private boolean shouldShiftClickIntoOpenTabFirst() {
		return storageWrapper.getSettingsHandler().getMainSettingValue(player, MainSettingsCategoryData::shiftClickIntoOpenTab);
	}

	public boolean shouldKeepSearchPhrase() {
		return storageWrapper.getSettingsHandler().getMainSettingValue(player, MainSettingsCategoryData::keepSearchPhrase);
	}

	public String getSearchPhrase() {
		SettingsHandler settingsHandler = storageWrapper.getSettingsHandler();
		boolean keepSearchPhrase = settingsHandler.getMainSettingValue(player, MainSettingsCategoryData::keepSearchPhrase);
		return keepSearchPhrase ? settingsHandler.getSettingsData().searchPhrase() : "";
	}

	public void setSearchPhrase(String searchPhrase) {
		SettingsHandler settingsHandler = storageWrapper.getSettingsHandler();
		if (!settingsHandler.getMainSettingValue(player, MainSettingsCategoryData::keepSearchPhrase)) {
			return;
		}
		settingsHandler.setSearchPhrase(searchPhrase);
		if (isClientSide()) {
			sendToServer(data -> data.putString(SEARCH_PHRASE_TAG, searchPhrase));
		}
	}

	private boolean mergeStackToUpgradeSlots(Slot sourceSlot, ItemStack slotStack) {
		if (!(slotStack.getItem() instanceof IUpgradeItem<?>)) {
			return false;
		}

		clearErrorUpgradeSlotChangeResult();
		tryingToMergeUpgrade = true;
		boolean result = !upgradeSlots.isEmpty() && moveItemStackTo(sourceSlot, slotStack, getInventorySlotsSize(), getInventorySlotsSize() + getNumberOfUpgradeSlots(), false);
		tryingToMergeUpgrade = false;
		if (columnsChange != 0) {
			actuallyUpdateColumnsTaken(columnsChange);
			if (player.level().isClientSide()) {
				onUpgradesChanged();
			}
			columnsChange = 0;
		}
		showUpgradeSlotChangeError();
		return result;
	}

	private boolean mergeStackToOpenUpgradeTab(Slot sourceSlot, ItemStack slotStack) {
		return getOpenContainer().map(c -> {
			List<Slot> slots = c.getSlots();
			if (slots.isEmpty()) {
				return false;
			}
			int firstSlotIndex = slots.getFirst().index;
			int lastSlotIndex = slots.getLast().index;
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

	private boolean mergeStackToExtraSlots(Slot sourceSlot, ItemStack slotStack) {
		return mergeItemStack(sourceSlot, slotStack, getInventorySlotsSize() - extraSlotsSize, getInventorySlotsSize(), true, true);
	}

	private boolean mergeStackToPlayersInventory(Slot sourceSlot, ItemStack slotStack) {
		return mergeItemStack(sourceSlot, slotStack, getNumberOfStorageInventorySlots(), getInventorySlotsSize() - extraSlotsSize, true, true);
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
	public <T extends UpgradeContainerBase<?, ?> & ICraftingContainer> Optional<T> getOpenOrFirstCraftingContainer(RecipeType<?> recipeType) {
		T firstContainer = null;
		for (UpgradeContainerBase<?, ?> container : upgradeContainers.values()) {
			if (container instanceof ICraftingContainer craftingContainer && craftingContainer.getRecipeType() == recipeType) {
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
		if (!storageWrapper.getSettingsHandler().getMainSettingValue(player, MainSettingsCategoryData::keepTabOpen)) {
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
			Slot slot = slotsCollection.get(i);
			ItemStack itemstack = slot.getItem();
			triggerSlotListeners(i, itemstack, itemstack::copy, lastSlotsCollection, slotIndexOffset, slot);
		}
	}

	protected void triggerSlotListeners(int stackIndex, ItemStack slotStack, Supplier<ItemStack> slotStackCopy, NonNullList<ItemStack> lastSlotsCollection, int slotIndexOffset, Slot slot) {
		ItemStack itemstack = lastSlotsCollection.get(stackIndex);
		if (!ItemStack.matches(itemstack, slotStack)) {
			ItemStack stackCopy = slotStackCopy.get();
			lastSlotsCollection.set(stackIndex, stackCopy);

			for (ContainerListener containerlistener : containerListeners) {
				containerlistener.slotChanged(this, stackIndex + slotIndexOffset, stackCopy);
			}

			if (!initialBroadcast && isUpgradeSettingsSlot(slot.index)) {
				slot.setChanged(); //updating slots in upgrade tabs to trigger related logic like updating recipe result on another player's screen
			}
		}
	}

	@Override
	public void sendAllDataToRemote() {
		List<ItemStack> allRemoteStacks = NonNullList.create();
		for (int i = 0; i < getInventorySlotsSize(); ++i) {
			ItemStack stack = realInventorySlots.get(i).getItem();
			allRemoteStacks.add(stack.copy());
			remoteRealSlots.get(i).force(stack);
		}

		for (int i = 0; i < upgradeSlots.size(); ++i) {
			ItemStack stack = upgradeSlots.get(i).getItem();
			allRemoteStacks.add(stack.copy());
			remoteUpgradeSlots.get(i).force(stack);
		}

		ItemStack carried = getCarried();
		remoteCarried.force(carried);

		if (synchronizer != null) {
			synchronizer.sendInitialData(this, allRemoteStacks, carried, new int[]{});
		}

		sendEmptySlotIcons();
		sendAdditionalSlotInfo();
	}

	public boolean isInfiniteSlot(int slot) {
		return infiniteSlots.contains(slot);
	}

	private void sendEmptySlotIcons() {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		Map<Identifier, Set<Integer>> noItemSlotTextures = new HashMap<>();
		for (int slot = 0; slot < storageWrapper.getInventoryHandler().size(); slot++) {
			Identifier noItemIcon = storageWrapper.getInventoryHandler().getNoItemIcon(slot);
			if (noItemIcon != null) {
				noItemSlotTextures.computeIfAbsent(noItemIcon, rl -> new HashSet<>()).add(slot);
			}
		}
		PacketDistributor.sendToPlayer(serverPlayer, new SyncEmptySlotIconsPayload(noItemSlotTextures));
	}

	private void sendAdditionalSlotInfo() {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		Set<Integer> inaccessibleSlots = new HashSet<>();
		Map<Integer, Integer> slotLimitOverrides = new HashMap<>();
		Set<Integer> infiniteSlots = new HashSet<>();
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		Map<Integer, Holder<Item>> slotFilterItems = new HashMap<>();
		for (int slot = 0; slot < inventoryHandler.size(); slot++) {
			if (!inventoryHandler.isSlotAccessible(slot)) {
				inaccessibleSlots.add(slot);
			}
			if (inventoryHandler.isInfinite(slot)) {
				infiniteSlots.add(slot);
			}
			ItemResource itemResource = inventoryHandler.getResource(slot);
			int stackLimit = inventoryHandler.getCapacityAsInt(slot, itemResource);
			if (stackLimit != inventoryHandler.getBaseCapacity(itemResource)) {
				slotLimitOverrides.put(slot, stackLimit);
			}
			if (inventoryHandler.getFilterItem(slot) != Items.AIR) {
				slotFilterItems.put(slot, inventoryHandler.getFilterItem(slot).builtInRegistryHolder());
			}
		}
		PacketDistributor.sendToPlayer(serverPlayer, new SyncAdditionalSlotInfoPayload(inaccessibleSlots, slotLimitOverrides, infiniteSlots, slotFilterItems));
	}

	@Override
	public void setRemoteSlot(int slotIndex, ItemStack stack) {
		if (slotIndex < getInventorySlotsSize()) {
			remoteRealSlots.get(slotIndex).force(stack);
		} else {
			remoteUpgradeSlots.get(slotIndex).force(stack.copy());
		}
	}

	@Override
	public void setRemoteSlotUnsafe(int slotIndex, HashedStack hashedStack) {
		if (slotIndex < getInventorySlotsSize()) {
			remoteRealSlots.get(slotIndex).receive(hashedStack);

			if (isStorageInventorySlot(slotIndex)) {
				inventorySlotStackChanged = true;
			}
		} else {
			remoteUpgradeSlots.get(slotIndex - inventorySlotsBeforeClickHandled).receive(hashedStack);
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

		initSlotsAndContainers(player, storageItemSlotIndex, shouldLockStorageItemSlot, extraSlots);
		slotsChangedSinceStartOfClick = true;
	}

	protected ItemStack processOverflowLogic(ItemStack stack) {
		ItemStack result = stack;
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)) {
			if (overflowUpgrade.worksInGui()) {
				result = overflowUpgrade.onSlotOverflow(result);
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

	public static int getQuickCraftPlaceCount(Slot slot, int quickCraftSlotsSize, int quickCraftingType, ItemStack carriedStack) {
		int placeCount;
		switch (quickCraftingType) {
			case 0 -> placeCount = Mth.floor((float) carriedStack.getCount() / (float) quickCraftSlotsSize);
			case 1 -> placeCount = 1;
			case 2 -> placeCount = carriedStack.getMaxStackSize();
			default -> placeCount = carriedStack.getCount();
		}
		return Math.min(slot.getMaxStackSize(carriedStack), placeCount);
	}

	//copy of Container's doClick with the replacement of inventorySlots.get to getSlot, call to onswapcraft as that's protected in vanilla and an addition of upgradeSlots to pickup all
	@SuppressWarnings("java:S3776")
	//complexity here is brutal, but it's something that's in vanilla and need to keep this as close to it as possible for easier ports
	@Override
	protected void doClick(int slotId, int dragType, ContainerInput clickType, Player player) {
		if (slotId >= getTotalSlotsNumber()) {
			return;
		}
		slotsChangedSinceStartOfClick = false;
		Inventory inventory = player.getInventory();
		if (clickType == ContainerInput.QUICK_CRAFT) {
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
						clicked(l, quickcraftType, ContainerInput.PICKUP, player);
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

							int l = Math.min(MathHelper.intMaxCappedAddition(getQuickCraftPlaceCount(slot1, quickcraftSlots.size(), quickcraftType, carriedCopy), j), slotStackLimit);
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
		} else if ((clickType == ContainerInput.PICKUP || clickType == ContainerInput.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
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
			} else if (clickType == ContainerInput.QUICK_MOVE) {
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
					if (getOpenOrFirstCraftingContainer(RecipeType.CRAFTING).map(ICraftingContainer::shouldRefillCraftingGrid).orElse(false)) {
						int i = 1;
						int maxStackSize = itemstack8.getMaxStackSize();
						while (!itemstack8.isEmpty() && ItemStack.isSameItemSameComponents(slot6.getItem(), itemstack8) && i < maxStackSize) {
							itemstack8 = quickMoveStack(this.player, slotId);
							i++;
						}
					} else {
						while (!slotsChangedSinceStartOfClick && !itemstack8.isEmpty() && ItemStack.isSameItem(slot6.getItem(), itemstack8)) {
							itemstack8 = quickMoveStack(this.player, slotId);
						}
					}
				}
			} else {
				if (slotId < 0) {
					return;
				}

				Slot slot7 = getSlot(slotId);
				ItemStack slotStack = slot7.getItem();
				ItemStack carriedStack = getCarried();
				player.updateTutorialInventoryAction(carriedStack, slotStack, clickaction);
				if (!carriedStack.overrideStackedOnOther(slot7, clickaction, player) && !slotStack.overrideOtherStackedOnMe(carriedStack, slot7, clickaction, player, SlotAccess.of(this::getCarried, this::setCarried))) {
					if (slotStack.isEmpty()) {
						if (!carriedStack.isEmpty()) {
							int l2 = clickaction == ClickAction.PRIMARY ? carriedStack.getCount() : 1;
							setCarried(slot7.safeInsert(carriedStack, l2));
						}
					} else if (slot7.mayPickup(player)) {
						if (carriedStack.isEmpty()) {
							int countToRemove = Math.min(slotStack.getCount(), slotStack.getMaxStackSize());
							if (clickaction == ClickAction.SECONDARY) {
								countToRemove = countToRemove / 2 + countToRemove % 2;
							}
							Optional<ItemStack> optional1 = slot7.tryRemove(countToRemove, Integer.MAX_VALUE, player);
							optional1.ifPresent((p_150421_) -> {
								setCarried(p_150421_);
								slot7.onTake(player, p_150421_);
							});
						} else if (slot7.mayPlace(carriedStack)) {
							if (ItemStack.isSameItemSameComponents(slotStack, carriedStack)) {
								int j3 = clickaction == ClickAction.PRIMARY ? carriedStack.getCount() : 1;
								setCarried(slot7.safeInsert(carriedStack, j3));
							} else if (carriedStack.getCount() <= slot7.getMaxStackSize(carriedStack) && slotStack.getCount() <= slotStack.getMaxStackSize()) {
								slot7.set(carriedStack);
								setCarried(slotStack);
							}
						} else if (ItemStack.isSameItemSameComponents(slotStack, carriedStack)) {
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
		} else if (clickType == ContainerInput.SWAP) {
			Slot slot2 = getSlot(slotId);
			ItemStack itemstack4 = inventory.getItem(dragType);
			ItemStack slotStack = slot2.getItem();
			if (!itemstack4.isEmpty() || !slotStack.isEmpty()) {
				if (itemstack4.isEmpty()) {
					if (slot2.mayPickup(player)) {
						if (slotStack.getCount() <= slotStack.getMaxStackSize()) {
							inventory.setItem(dragType, slotStack.copy());
							onSwapCraft(slot2, slotStack.getCount());
							slot2.set(ItemStack.EMPTY);
							slot2.onTake(player, slotStack);
						} else {
							inventory.setItem(dragType, slotStack.copyWithCount(slotStack.getMaxStackSize()));
							slot2.set(slotStack.copyWithCount(slotStack.getCount() - slotStack.getMaxStackSize()));
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
						ItemStack slotStackCopy = slotStack.copy();
						slot2.set(itemstack4);
						inventory.setItem(dragType, slotStackCopy);
						slot2.onTake(player, slotStackCopy);
					}
				}
			}
		} else if (clickType == ContainerInput.CLONE && player.getAbilities().instabuild && getCarried().isEmpty() && slotId >= 0) {
			Slot slot5 = getSlot(slotId);
			if (slot5.hasItem()) {
				ItemStack itemstack6 = slot5.getItem().copy();
				itemstack6.setCount(itemstack6.getMaxStackSize());
				setCarried(itemstack6);
			}
		} else if (clickType == ContainerInput.THROW && getCarried().isEmpty() && slotId >= 0) {
			Slot slot4 = getSlot(slotId);
			int i1 = dragType == 0 ? 1 : slot4.getItem().getCount();
			ItemStack itemstack8 = slot4.safeTake(i1, slot4.getItem().getMaxStackSize(), player);
			player.drop(itemstack8, true);
		} else if (clickType == ContainerInput.PICKUP_ALL && slotId >= 0) {
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
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		if (isUpgradeSlot(slot.index) && slot.getItem().getItem() instanceof IUpgradeItem<?> upgradeItem && upgradeItem.getInventoryColumnsTaken() > 0) {
			return false;
		}

		return super.canTakeItemForPickAll(stack, slot);
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
		if (!player.level().isClientSide()) {
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
					if (!destStack.isEmpty() && ItemStack.isSameItemSameComponents(result, destStack)) {
						int maxSize = slot.getMaxStackSize(result);
						if (destStack.getCount() <= maxSize - toTransfer) {
							result.shrink(toTransfer);
							ItemStack copy = destStack.copy();
							copy.setCount(destStack.getCount() + toTransfer);
							slot.set(copy);
							toTransfer = 0;
							slot.setChanged();
						} else if (destStack.getCount() < maxSize) {
							result.shrink(maxSize - destStack.getCount());
							toTransfer -= maxSize - destStack.getCount();
							ItemStack copy = destStack.copy();
							copy.setCount(maxSize);
							slot.set(copy);
							slot.setChanged();
						}

						if (runOverflowLogic && !result.isEmpty()) {
							ItemStack overflowResult = processOverflowLogic(result);
							if (overflowResult != result) {
								result.setCount(overflowResult.getCount());
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
						if (runOverflowLogic && processOverflowIfSlotWithSameItemFound(result, s -> {
						})) {
							result.shrink(result.getCount());
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
								if (isClientSide()) {
									onUpgradesChanged();
								}
							} else {
								errorMerging = true;
							}
						} else {
							if (runOverflowLogic && processOverflowIfSlotWithSameItemFound(result, s -> {
							})) {
								result.shrink(result.getCount());
							} else {
								destSlot.set(result.split(toTransfer));
							}
						}
					}
					if (!errorMerging) {
						destSlot.setChanged();
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
			remoteRealSlots.replaceAll(rs -> synchronizer.createSlot());
			remoteUpgradeSlots.replaceAll(rs -> synchronizer.createSlot());
			super.setSynchronizer(new HighStackCountSynchronizer(serverPlayer));
			return;
		}
		super.setSynchronizer(synchronizer);
	}

	public static boolean canItemQuickReplace(@Nullable Slot slot, ItemStack stack) {
		boolean flag = slot == null || !slot.hasItem();
		if (!flag && ItemStack.isSameItemSameComponents(stack, slot.getItem())) {
			return slot.getItem().getCount() <= slot.getMaxStackSize(stack);
		} else {
			return flag;
		}
	}

	@Override
	public Slot getSlot(int slotId) {
		if (slotId >= getInventorySlotsSize()) {
			int upgradeSlotId = slotId - getInventorySlotsSize();
			return upgradeSlots.size() > upgradeSlotId ? upgradeSlots.get(upgradeSlotId) : DummySlot.INSTANCE;
		} else {
			return realInventorySlots.get(slotId);
		}
	}

	@Override
	public void setItem(int slotId, int stateId, ItemStack stack) {
		if (getTotalSlotsNumber() > slotId) {
			super.setItem(slotId, stateId, stack);
		}
	}

	@Override
	public void broadcastChanges() {
		if (hasSomethingMessedWithStorage()) {
			player.closeContainer();
			return;
		}

		synchronizeCarriedToRemote();
		broadcastChangesIn(lastUpgradeSlots, remoteUpgradeSlots, upgradeSlots, getFirstUpgradeSlot());
		broadcastChangesIn(lastRealSlots, remoteRealSlots, realInventorySlots, 0);

		if (inventorySlotStackChanged) {
			inventorySlotStackChanged = false;
			sendAdditionalSlotInfo();
		}

		if (lastSettingsData == null || !lastSettingsData.equals(storageWrapper.getSettingsHandler().getSettingsData())) {
			lastSettingsData = storageWrapper.getSettingsHandler().getSettingsData().copy();
			sendStorageSettingsToClient();
			refreshInventorySlotsIfNeeded();
		}

		initialBroadcast = false;
	}


	public Optional<ItemStack> getVisibleStorageItem() {
		return storageItemSlotNumber != -1 ? Optional.of(getSlot(storageItemSlotNumber).getItem()) : Optional.empty();
	}

	private void broadcastChangesIn(NonNullList<ItemStack> lastSlotsCollection, NonNullList<RemoteSlot> remoteSlotsCollection, List<Slot> slotsCollection, int slotIndexOffset) {
		for (int i = 0; i < slotsCollection.size(); ++i) {
			Slot slot = slotsCollection.get(i);
			ItemStack itemstack = slot.getItem();
			Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
			triggerSlotListeners(i, itemstack, supplier, lastSlotsCollection, slotIndexOffset, slot);
			synchronizeSlotToRemote(i, itemstack, supplier, remoteSlotsCollection, slotIndexOffset);
		}
	}

	private void synchronizeSlotToRemote(int slotIndex, ItemStack slotStack, Supplier<ItemStack> slotStackCopy, NonNullList<RemoteSlot> remoteSlotsCollection, int slotIndexOffset) {
		if (!suppressRemoteUpdates) {
			RemoteSlot remoteSlot = remoteSlotsCollection.get(slotIndex);
			if (!remoteSlot.matches(slotStack)) {
				remoteSlot.force(slotStack);
				if (isStorageInventorySlot(slotIndex + slotIndexOffset)) {
					inventorySlotStackChanged = true;
				}
				if (synchronizer != null) {
					synchronizer.sendSlotChange(this, slotIndex + slotIndexOffset, slotStackCopy.get());
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
		addExtraSlots(extraSlots);
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

	private void reloadUpgradeControl(boolean removeOpenTabId) {
		if (!isUpdatingFromPacket && removeOpenTabId) {
			storageWrapper.removeOpenTabId();
		}
		NonNullList<ItemStack> previousLastUpgradeSlots = NonNullList.create();
		previousLastUpgradeSlots.addAll(lastUpgradeSlots);
		NonNullList<RemoteSlot> previousRemoteUpgradeSlots = NonNullList.create();
		previousRemoteUpgradeSlots.addAll(remoteUpgradeSlots);
		removeUpgradeSettingsSlots();
		upgradeContainers.clear();
		addUpgradeSettingsContainers(player);
		setPreviousLastAndRemoteSlots(previousLastUpgradeSlots, previousRemoteUpgradeSlots);
		onUpgradesChanged();
		sendEmptySlotIcons();
		sendAdditionalSlotInfo();
	}

	private void setPreviousLastAndRemoteSlots(NonNullList<ItemStack> previousLastUpgradeSlots, NonNullList<RemoteSlot> previousRemoteUpgradeSlots) {
		for (int i = 0; i < lastUpgradeSlots.size() && i < previousLastUpgradeSlots.size(); i++) {
			lastUpgradeSlots.set(i, previousLastUpgradeSlots.get(i));
		}
		for (int i = 0; i < remoteUpgradeSlots.size() && i < previousRemoteUpgradeSlots.size(); i++) {
			remoteUpgradeSlots.set(i, previousRemoteUpgradeSlots.get(i));
		}
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
	}

	@Override
	public void updateAdditionalSlotInfo(Set<Integer> inaccessibleSlots, Map<Integer, Integer> slotLimitOverrides, Set<Integer> infiniteSlots, Map<Integer, Holder<Item>> slotFilterItems) {
		this.inaccessibleSlots.clear();
		this.inaccessibleSlots.addAll(inaccessibleSlots);

		this.slotLimitOverrides.clear();
		this.slotLimitOverrides.putAll(slotLimitOverrides);

		this.infiniteSlots.clear();
		this.infiniteSlots.addAll(infiniteSlots);

		Set<Integer> noSort = getNoSortSlotIndexes();
		noSort.addAll(infiniteSlots);

		List<Slot> slotsToMakeIntoNoSort = new ArrayList<>();
		List<Slot> slotsToMakeSortable = new ArrayList<>();
		for (int i = 0; i < getNumberOfStorageInventorySlots(); i++) {
			Slot slot = realInventorySlots.get(i);
			if (noSort.contains(slot.index) && slots.contains(slot)) {
				slotsToMakeIntoNoSort.add(slot);
			} else if (!noSort.contains(slot.index) && !slots.contains(slot)) {
				slotsToMakeSortable.add(slot);
			}
		}

		slotsToMakeIntoNoSort.forEach(slots::remove);
		slots.addAll(slotsToMakeSortable);

		this.slotFilterItems.clear();
		slotFilterItems.forEach((slot, item) -> this.slotFilterItems.put(slot, new ItemStack(item)));
	}

	@Override
	public void updateEmptySlotIcons(Map<Identifier, Set<Integer>> emptySlotIcons) {
		this.emptySlotIcons.clear();
		emptySlotIcons.forEach((textureName, slots) -> slots.forEach(slot -> this.emptySlotIcons.put(slot, textureName)));
	}

	public ItemStack getSlotFilterItem(int slot) {
		return slotFilterItems.getOrDefault(slot, ItemStack.EMPTY);
	}

	public void updateSlotChangeError(UpgradeSlotChangeResult result) {
		errorUpgradeSlotChangeResult = result;
		if (!tryingToMergeUpgrade && columnsChange != 0) {
			actuallyUpdateColumnsTaken(columnsChange);
			if (player.level().isClientSide()) {
				onUpgradesChanged();
			}
		}
		columnsChange = 0;
		showUpgradeSlotChangeError();
	}

	private void showUpgradeSlotChangeError() {
		if (errorUpgradeSlotChangeResult == null || tryingToMergeUpgrade) {
			return;
		}
		if (player.level().isClientSide()) {
			if (!errorUpgradeSlotChangeResult.successful()) {
				errorResultExpirationTime = player.level().getGameTime() + 60;
			}
		} else {
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new SyncSlotChangeErrorPayload(errorUpgradeSlotChangeResult));
			}
		}
	}

	public void transferItemsToPlayerInventory(boolean filterByContents) {
		ClientPacketDistributor.sendToServer(new TransferItemsPayload(true, filterByContents));
	}

	public void transferItemsToStorage(boolean filterByContents) {
		ClientPacketDistributor.sendToServer(new TransferItemsPayload(false, filterByContents));
	}

	private record ReloadCheckResult(boolean reloadNeeded, boolean removeOpenTabId) {
		public static final ReloadCheckResult NO_RELOAD_NEEDED = new ReloadCheckResult(false, false);
		public static final ReloadCheckResult RELOAD_NEEDED = new ReloadCheckResult(true, true);
		public static final ReloadCheckResult RELOAD_NEEDED_KEEP_TAB = new ReloadCheckResult(true, false);
	}

	protected void onUpgradeChanged() {
		//noop by default
	}

	public class StorageUpgradeSlot extends StackCopySlot {
		private final int slotIndex;
		private final UpgradeHandler upgradeHandler;

		public StorageUpgradeSlot(UpgradeHandler upgradeHandler, int slotIndex) {
			super(-15, 0);
			this.slotIndex = slotIndex;
			this.upgradeHandler = upgradeHandler;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			if (stack.isEmpty() || !getResourceHandler().isValid(slotIndex, ItemResource.of(stack))) {
				return false;
			}
			UpgradeSlotChangeResult result;
			if (getItem().isEmpty()) {
				result = ((IUpgradeItem<?>) stack.getItem()).canAddUpgradeTo(storageWrapper, stack, isFirstLevelStorage(), player.level().isClientSide());
			} else if (stack.getCount() > 1) {
				return false;
			} else {
				result = ((IUpgradeItem<?>) getItem().getItem()).canSwapUpgradeFor(stack, slotIndex, storageWrapper, player.level().isClientSide());
			}

			updateSlotChangeError(result);
			return result.successful();
		}

		@Override
		public boolean mayPickup(Player player) {
			ItemResource resource = upgradeHandler.getResource(slotIndex);
			if (resource.isEmpty()) {
				return false;
			}
			try (Transaction tx = Transaction.openRoot()) {
				if (upgradeHandler.extract(slotIndex, resource, 1, tx) != 1) {
					return false;
				}
			}

			UpgradeSlotChangeResult result = ((IUpgradeItem<?>) getItem().getItem()).canRemoveUpgradeFrom(storageWrapper, player.level().isClientSide(), player);
			if (result.successful() && upgradeContainers.containsKey(slotIndex)) {
				Set<Integer> errorUpgradeSlots = upgradeContainers.get(slotIndex).getSlots()
						.stream().filter(slot -> !(slot instanceof IFilterSlot) && shouldSlotItemBeDroppedFromStorage(slot))
						.map(slot -> slot.getSlotIndex() + getNumberOfUpgradeSlots()).collect(Collectors.toSet());
				if (!errorUpgradeSlots.isEmpty()) {
					result = UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("remove.banned_item"), errorUpgradeSlots, Collections.emptySet(), Collections.emptySet());
				}
			}
			updateSlotChangeError(result);
			return result.successful();
		}

		@Nullable
		@Override
		public Identifier getNoItemIcon() {
			return StorageContainerMenuBase.EMPTY_UPGRADE_SLOT_BACKGROUND;
		}

		@Override
		protected ItemStack getStackCopy() {
			return this.upgradeHandler.getResource(slotIndex).toStack(this.upgradeHandler.getAmountAsInt(slotIndex));
		}

		@Override
		protected void setStackCopy(ItemStack stack) {
			boolean wasEmpty = upgradeHandler.getResource(slotIndex).isEmpty();

			upgradeHandler.setStackInSlot(slotIndex, stack);

			ReloadCheckResult reloadCheckResult = updateWrappersAndCheckForReloadNeeded(wasEmpty, stack);
			if (reloadCheckResult.reloadNeeded()) {
				reloadUpgradeControl(reloadCheckResult.removeOpenTabId());
				if (!isFirstLevelStorage()) {
					parentStorageWrapper.getUpgradeHandler().refreshUpgradeWrappers();
				}
				onUpgradeChanged();
			}
		}

		private ReloadCheckResult updateWrappersAndCheckForReloadNeeded(boolean wasEmpty, ItemStack stack) {
			if ((!isUpdatingFromPacket && wasEmpty != stack.isEmpty())) {
				return ReloadCheckResult.RELOAD_NEEDED;
			}

			int checkedContainersCount = 0;
			for (Map.Entry<Integer, IUpgradeWrapper> slotWrapper : storageWrapper.getUpgradeHandler().getSlotWrappers().entrySet()) {
				UpgradeContainerBase<?, ?> container = upgradeContainers.get(slotWrapper.getKey());
				if (slotWrapper.getValue().hideSettingsTab()) {
					if (container != null) {
						return ReloadCheckResult.RELOAD_NEEDED;
					}
				} else if (container == null || container.getUpgradeWrapper().isEnabled() != slotWrapper.getValue().isEnabled()) {
					return ReloadCheckResult.RELOAD_NEEDED;
				} else if (container.getUpgradeWrapper() != slotWrapper.getValue()) {
					if (!player.level().isClientSide() || container.getUpgradeWrapper().getUpgradeStack().getItem() != slotWrapper.getValue().getUpgradeStack().getItem()) {
						if (container.getUpgradeWrapper().getUpgradeStack().getItem() == slotWrapper.getValue().getUpgradeStack().getItem()) {
							return ReloadCheckResult.RELOAD_NEEDED_KEEP_TAB;
						} else {
							return ReloadCheckResult.RELOAD_NEEDED;
						}
					} else {
						container.setUpgradeWrapper(slotWrapper.getValue());
						checkedContainersCount++;
					}
				} else {
					checkedContainersCount++;
				}
			}

			return checkedContainersCount != upgradeContainers.size() ? ReloadCheckResult.RELOAD_NEEDED : ReloadCheckResult.NO_RELOAD_NEEDED;
		}

		@Override
		public void onQuickCraft(ItemStack oldStackIn, ItemStack newStackIn) {
		}

		@Override
		public int getMaxStackSize() {
			return this.upgradeHandler.getCapacityAsInt(slotIndex, ItemResource.EMPTY);
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return this.upgradeHandler.getCapacityAsInt(slotIndex, ItemResource.of(stack));
		}

		public ResourceHandler<ItemResource> getResourceHandler() {
			return this.upgradeHandler;
		}

		@Override
		public boolean isSameInventory(Slot other) {
			return other instanceof StorageContainerMenuBase<?>.StorageUpgradeSlot otherSlot
					&& otherSlot.upgradeHandler == this.upgradeHandler;
		}
	}
}
