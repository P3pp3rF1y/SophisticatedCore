package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.EmptyHandler;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IIOFilterUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.*;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings("PMD.UnnecessaryImport")
public abstract class ControllerBlockEntityBase extends BlockEntity implements IItemHandlerSimpleInserter, IInsertBlockOverride {
	private static final long INVALID_SLOT_LOG_INTERVAL_TICKS = 20;
	private static final int INVALID_SLOT_REFRESH_THRESHOLD = 2;
	private static final long INVALID_SLOT_REFRESH_COOLDOWN_TICKS = 40;

	private static final String STORAGE_KEYS_TAG = "storageKeys";
	private static final String STORAGE_MEMBER_KEYS_TAG = "storageMemberKeys";

	private final List<BlockPos> storagePositions = new ArrayList<>();
	private final List<ControllerStorageKey> storageKeys = new ArrayList<>();
	private final Map<ControllerStorageKey, Integer> storageKeyIndexes = new HashMap<>();
	private final Map<BlockPos, ControllerStorageKey> storageKeysByPosition = new HashMap<>();
	private final Map<ControllerStorageKey, BlockPos> listenerSourcePositions = new HashMap<>();
	private List<Integer> baseIndexes = new ArrayList<>();
	private int totalSlots = 0;
	private final Map<ItemStackKey, Set<ControllerStorageKey>> stackStorageKeys = new HashMap<>();
	protected final Map<ItemStackKey, Set<BlockPos>> stackStorages = new HashMap<>();
	private final Map<ControllerStorageKey, Set<ItemStackKey>> storageStacks = new HashMap<>();
	protected final Map<Item, Set<ItemStackKey>> itemStackKeys = new HashMap<>();
	private final Comparator<BlockPos> distanceComparator = Comparator.<BlockPos>comparingDouble(p -> p.distSqr(getBlockPos()))
			.thenComparing(Comparator.naturalOrder());
	private final Comparator<ControllerStorageKey> storageKeyDistanceComparator = Comparator.comparing(ControllerStorageKey::position, distanceComparator);
	private final Set<ControllerStorageKey> emptySlotStorageKeys = new TreeSet<>(storageKeyDistanceComparator);
	protected final Set<BlockPos> emptySlotsStorages = new TreeSet<>(distanceComparator);
	private final Set<ControllerStorageKey> filteredInputStorageKeys = new TreeSet<>(storageKeyDistanceComparator);
	protected final Set<BlockPos> filteredInputStorages = new TreeSet<>(distanceComparator);

	private final Map<Item, Set<ControllerStorageKey>> memorizedItemStorageKeys = new HashMap<>();
	protected final Map<Item, Set<BlockPos>> memorizedItemStorages = new HashMap<>();
	private final Map<ControllerStorageKey, Set<Item>> storageMemorizedItems = new HashMap<>();
	private final Map<Integer, Set<ControllerStorageKey>> memorizedStackStorageKeys = new HashMap<>();
	protected final Map<Integer, Set<BlockPos>> memorizedStackStorages = new HashMap<>();
	private final Map<ControllerStorageKey, Set<Integer>> storageMemorizedStacks = new HashMap<>();
	private final Map<Item, Set<ControllerStorageKey>> filterItemStorageKeys = new HashMap<>();
	protected final Map<Item, Set<BlockPos>> filterItemStorages = new HashMap<>();
	private final Map<ControllerStorageKey, Set<Item>> storageFilterItems = new HashMap<>();
	private Set<BlockPos> linkedBlocks = new TreeSet<>(distanceComparator);
	private Set<BlockPos> connectingBlocks = new TreeSet<>(distanceComparator);
	private Set<BlockPos> nonConnectingBlocks = new TreeSet<>(distanceComparator);

	@Nullable
	private LazyOptional<IItemHandler> itemHandlerCap;
	@Nullable
	private LazyOptional<IItemHandler> noSideItemHandlerCap;

	private WeakReference<IItemHandlerModifiable>[] cachedHandlers = new WeakReference[0];
	private long lastInvalidSlotLogTime = -INVALID_SLOT_LOG_INTERVAL_TICKS;
	private long lastInvalidSlotRefreshTime = -INVALID_SLOT_REFRESH_COOLDOWN_TICKS;
	private int invalidSlotIncidentCount = 0;
	private boolean refreshingAfterInvalidSlots = false;

	public boolean addLinkedBlock(BlockPos linkedPos) {
		if (level != null && !level.isClientSide() && isWithinRange(linkedPos) && !linkedBlocks.contains(linkedPos)
				&& !storageKeysByPosition.containsKey(linkedPos)) {

			linkedBlocks.add(linkedPos);
			setChanged();

			WorldHelper.getBlockEntity(level, linkedPos, ILinkable.class).ifPresent(l -> {
				if (l.connectLinkedSelf()) {
					Set<BlockPos> positionsToCheck = new LinkedHashSet<>();
					positionsToCheck.add(linkedPos);
					searchAndAddBoundables(positionsToCheck, true);
				}

				searchAndAddBoundables(new LinkedHashSet<>(l.getConnectablePositions()), false);
			});
			WorldHelper.notifyBlockEntityUpdate(this);
			return true;
		}
		return false;
	}

	public void removeLinkedBlock(BlockPos storageBlockPos) {
		linkedBlocks.remove(storageBlockPos);
		setChanged();
		verifyStoragesConnected();

		WorldHelper.notifyBlockEntityUpdate(this);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide()) {
			stackStorageKeys.clear();
			stackStorages.clear();
			storageStacks.clear();
			itemStackKeys.clear();
			emptySlotStorageKeys.clear();
			emptySlotsStorages.clear();
			filteredInputStorageKeys.clear();
			filteredInputStorages.clear();
			memorizedItemStorageKeys.clear();
			memorizedItemStorages.clear();
			storageMemorizedItems.clear();
			memorizedStackStorageKeys.clear();
			memorizedStackStorages.clear();
			storageMemorizedStacks.clear();
			filterItemStorageKeys.clear();
			filterItemStorages.clear();
			storageFilterItems.clear();
			listenerSourcePositions.clear();
			storageKeys.forEach(storageKey -> getStorageMemberPositions(storageKey).stream()
					.filter(storagePos -> WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).isPresent()).findFirst()
					.ifPresent(this::addStorageStacksAndRegisterListeners));
		}
	}

	public boolean isStorageConnected(BlockPos storagePos) {
		return storageKeysByPosition.containsKey(storagePos);
	}

	public void searchAndAddBoundables() {
		Set<BlockPos> positionsToCheck = new HashSet<>();
		for (Direction dir : Direction.values()) {
			positionsToCheck.add(getBlockPos().offset(dir.getNormal()));
		}
		searchAndAddBoundables(positionsToCheck, false);
	}

	public void changeSlots(BlockPos storagePos, int newSlots, boolean hasEmptySlots) {
		updateBaseIndexesAndTotalSlots(storagePos, newSlots);
		updateEmptySlots(storagePos, hasEmptySlots);
	}

	public void rebindStorage(BlockPos storagePos) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		if (!storagePos.equals(listenerSourcePositions.get(storageKey))) {
			clearCachedHandler(storagePos);
			updateStorageInputFilter(storagePos);
			return;
		}

		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(storage -> {
			removeStorageRoutingData(storagePos);
			storage.unregisterControllerListeners();
			listenerSourcePositions.remove(storageKey);
			addStorageStacksAndRegisterListeners(storagePos);
			changeSlots(storagePos, storage.getStorageWrapper().getInventoryForInputOutput().getSlots(),
					storage.getStorageWrapper().getInventoryForInputOutput().hasEmptySlots());
			clearCachedHandler(storagePos);
			setChanged();
			WorldHelper.notifyBlockEntityUpdate(this);
		});
	}

	public void updateEmptySlots(BlockPos storagePos, boolean hasEmptySlots) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		if (emptySlotStorageKeys.contains(storageKey) && !hasEmptySlots) {
			emptySlotStorageKeys.remove(storageKey);
			emptySlotsStorages.remove(storagePos);
		} else if (!emptySlotStorageKeys.contains(storageKey) && hasEmptySlots) {
			emptySlotStorageKeys.add(storageKey);
			emptySlotsStorages.add(storagePos);
		}
	}

	private void updateBaseIndexesAndTotalSlots(BlockPos storagePos, int newSlots) {
		Integer index = storageKeyIndexes.get(getStorageKey(storagePos));
		if (index == null) {
			return;
		}
		int originalSlots = getStorageSlots(index);

		int diff = newSlots - originalSlots;

		for (int i = index; i < baseIndexes.size(); i++) {
			baseIndexes.set(i, baseIndexes.get(i) + diff);
		}

		totalSlots += diff;
		WorldHelper.notifyBlockEntityUpdate(this);
	}

	private int getStorageSlots(int index) {
		int previousBaseIndex = index == 0 ? 0 : baseIndexes.get(index - 1);
		return baseIndexes.get(index) - previousBaseIndex;
	}

	public int getSlots(int storageIndex) {
		if (storageIndex < 0 || storageIndex >= baseIndexes.size()) {
			return 0;
		}
		return getStorageSlots(storageIndex);
	}

	private void searchAndAddBoundables(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf) {
		Set<BlockPos> positionsChecked = new HashSet<>();

		boolean first = true;
		while (!positionsToCheck.isEmpty()) {
			Iterator<BlockPos> it = positionsToCheck.iterator();
			BlockPos posToCheck = it.next();
			it.remove();
			positionsChecked.add(posToCheck);

			final boolean finalFirst = first;
			WorldHelper.getLoadedBlockEntity(level, posToCheck, IControllerBoundable.class)
					.ifPresent(boundable -> tryToConnectStorageAndAddPositionsToCheckAround(positionsToCheck, addingLinkedSelf, positionsChecked,
							posToCheck, finalFirst, boundable));
			first = false;
		}
	}

	private void tryToConnectStorageAndAddPositionsToCheckAround(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf, Set<BlockPos> positionsChecked,
			BlockPos posToCheck, boolean finalFirst, IControllerBoundable boundable) {
		if (boundable.canBeConnected() || isConnectedToThisController(boundable) || (addingLinkedSelf && finalFirst)) {
			if (boundable instanceof ILinkable linkable && linkable.isLinked() && (!addingLinkedSelf || !finalFirst)) {
				linkedBlocks.remove(posToCheck);
				linkable.setNotLinked();
				clearCachedHandlers();
			} else if (boundable instanceof IControllableStorage storage && storage.hasStorageData()) {
				addStorageData(storage);
			} else {
				if (boundable.canConnectStorages()) {
					connectingBlocks.add(posToCheck);
				} else {
					nonConnectingBlocks.add(posToCheck);
				}
				boundable.registerController(this);
			}
			if (boundable.canConnectStorages()) {
				addUncheckedPositionsAround(positionsToCheck, positionsChecked, posToCheck);
			}
		}
	}

	private boolean isConnectedToThisController(IControllerBoundable boundable) {
		return boundable.getControllerPos().filter(getBlockPos()::equals).isPresent();
	}

	private void clearCachedHandlers() {
		cachedHandlers = new WeakReference[storageKeys.size()];
	}

	public void clearCachedHandler(BlockPos storagePos) {
		Integer index = storageKeyIndexes.get(getStorageKey(storagePos));
		if (index != null && index < cachedHandlers.length) {
			cachedHandlers[index] = null;
		}
	}

	private ControllerStorageKey getStorageKey(BlockPos storagePos) {
		return storageKeysByPosition.getOrDefault(storagePos, new ControllerStorageKey(storagePos));
	}

	public void updateStorageInputFilter(BlockPos storagePos) {
		if (!storageKeysByPosition.containsKey(storagePos)) {
			filteredInputStorageKeys.remove(getStorageKey(storagePos));
			filteredInputStorages.remove(storagePos);
			return;
		}

		getWrapperValueFromHolder(storagePos, this::hasInputFilter).ifPresentOrElse(hasInputFilter -> setStorageInputFilter(storagePos, hasInputFilter),
				() -> setStorageInputFilter(storagePos, false));
	}

	private boolean hasInputFilter(IStorageWrapper storageWrapper) {
		return storageWrapper.getUpgradeHandler().getWrappersThatImplement(IIOFilterUpgrade.class).stream()
				.anyMatch(wrapper -> wrapper.getInputFilter().isPresent());
	}

	private void setStorageInputFilter(BlockPos storagePos, boolean hasInputFilter) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		if (hasInputFilter) {
			filteredInputStorageKeys.add(storageKey);
			filteredInputStorages.add(storagePos);
		} else {
			filteredInputStorageKeys.remove(storageKey);
			filteredInputStorages.remove(storagePos);
		}
	}

	private void addUncheckedPositionsAround(Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked, BlockPos currentPos) {
		for (Direction dir : Direction.values()) {
			BlockPos pos = currentPos.offset(dir.getNormal());
			if (!positionsChecked.contains(pos) && isWithinRange(pos)) {
				positionsToCheck.add(pos);
			}
		}
	}

	private boolean isWithinRange(BlockPos pos) {
		return Math.abs(pos.getX() - getBlockPos().getX()) <= getSearchRange() && Math.abs(pos.getY() - getBlockPos().getY()) <= getSearchRange()
				&& Math.abs(pos.getZ() - getBlockPos().getZ()) <= getSearchRange();
	}

	protected abstract int getSearchRange();

	public void addStorage(BlockPos storagePos) {
		if (storageKeysByPosition.containsKey(storagePos)) {
			if (level != null) {
				WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class)
						.ifPresent(storage -> storage.getControllerPos().filter(getBlockPos()::equals).ifPresent(pos -> storage.unregisterController()));
			}
			removeStorageInventoryData(storagePos);
			clearCachedHandlers();
		}

		if (isWithinRange(storagePos)) {
			HashSet<BlockPos> positionsToCheck = new LinkedHashSet<>();
			positionsToCheck.add(storagePos);
			searchAndAddBoundables(positionsToCheck, false);
		}
		WorldHelper.notifyBlockEntityUpdate(this);
	}

	private void addStorageData(IControllableStorage storage) {
		ControllerStorageKey storageKey = storage.getControllerStorageKey();
		BlockPos storagePos = storage.getControlledStorageBlockPos();
		if (storageKeyIndexes.containsKey(storageKey)) {
			storageKeysByPosition.put(storagePos, storageKey);
			storage.registerControllerMembership(this);
			clearCachedHandlers();
			setChanged();
			WorldHelper.notifyBlockEntityUpdate(this);
			return;
		}
		addStorageData(storageKey);
	}

	private void addStorageData(ControllerStorageKey storageKey) {
		if (storageKeyIndexes.containsKey(storageKey) || storageKeysByPosition.containsKey(storageKey.position())) {
			return;
		}

		storageKeys.add(storageKey);
		storagePositions.add(storageKey.position());
		int index = storageKeys.size() - 1;
		storageKeyIndexes.put(storageKey, index);
		storageKeysByPosition.put(storageKey.position(), storageKey);
		totalSlots += getHandlerFromIndex(index).getSlots();
		baseIndexes.add(totalSlots);
		addStorageStacksAndRegisterListeners(storageKey.position());

		setChanged();
		WorldHelper.notifyBlockEntityUpdate(this);
	}

	public void addStorageStacksAndRegisterListeners(BlockPos storagePos) {
		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(storage -> {
			ControllerStorageKey storageKey = getStorageKey(storagePos);
			if (listenerSourcePositions.containsKey(storageKey)) {
				storage.registerControllerMembership(this);
				return;
			}
			ITrackedContentsItemHandler handler = storage.getStorageWrapper().getInventoryForInputOutput();
			handler.getTrackedStacks().forEach(k -> addStorageStack(storagePos, k));
			if (handler.hasEmptySlots()) {
				emptySlotStorageKeys.add(getStorageKey(storagePos));
				emptySlotsStorages.add(storagePos);
			}
			MemorySettingsCategory memorySettings = storage.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			memorySettings.getFilterItemSlots().keySet().forEach(i -> addStorageMemorizedItem(storagePos, i));
			memorySettings.getFilterStackSlots().keySet().forEach(stackHash -> addStorageMemorizedStack(storagePos, stackHash));

			setStorageFilterItems(storagePos, storage.getStorageWrapper().getInventoryHandler().getFilterItems());
			setStorageInputFilter(storagePos, hasInputFilter(storage.getStorageWrapper()));

			storage.registerController(this);
			listenerSourcePositions.put(storageKey, storagePos);
		});
	}

	public void addStorageMemorizedItem(BlockPos storagePos, Item item) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		memorizedItemStorageKeys.computeIfAbsent(item, stackKey -> new LinkedHashSet<>()).add(storageKey);
		memorizedItemStorages.computeIfAbsent(item, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageMemorizedItems.computeIfAbsent(storageKey, key -> new HashSet<>()).add(item);
	}

	public void addStorageMemorizedStack(BlockPos storagePos, int stackHash) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		memorizedStackStorageKeys.computeIfAbsent(stackHash, stackKey -> new LinkedHashSet<>()).add(storageKey);
		memorizedStackStorages.computeIfAbsent(stackHash, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageMemorizedStacks.computeIfAbsent(storageKey, key -> new HashSet<>()).add(stackHash);
	}

	public void removeStorageMemorizedItem(BlockPos storagePos, Item item) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		memorizedItemStorageKeys.computeIfPresent(item, (i, storageKeys) -> {
			storageKeys.remove(storageKey);
			return storageKeys;
		});
		if (memorizedItemStorageKeys.containsKey(item) && memorizedItemStorageKeys.get(item).isEmpty()) {
			memorizedItemStorageKeys.remove(item);
		}
		memorizedItemStorages.computeIfPresent(item, (i, positions) -> {
			positions.remove(storagePos);
			return positions;
		});
		if (memorizedItemStorages.containsKey(item) && memorizedItemStorages.get(item).isEmpty()) {
			memorizedItemStorages.remove(item);
		}
		storageMemorizedItems.remove(storageKey);
	}

	public void removeStorageMemorizedStack(BlockPos storagePos, int stackHash) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		memorizedStackStorageKeys.computeIfPresent(stackHash, (i, storageKeys) -> {
			storageKeys.remove(storageKey);
			return storageKeys;
		});
		if (memorizedStackStorageKeys.containsKey(stackHash) && memorizedStackStorageKeys.get(stackHash).isEmpty()) {
			memorizedStackStorageKeys.remove(stackHash);
		}
		memorizedStackStorages.computeIfPresent(stackHash, (i, positions) -> {
			positions.remove(storagePos);
			return positions;
		});
		if (memorizedStackStorages.containsKey(stackHash) && memorizedStackStorages.get(stackHash).isEmpty()) {
			memorizedStackStorages.remove(stackHash);
		}
		storageMemorizedStacks.remove(storageKey);
	}

	private <T> Optional<T> getWrapperValueFromHolder(BlockPos storagePos, Function<IStorageWrapper, T> valueGetter) {
		return getWrapperValueFromHolder(getStorageKey(storagePos), valueGetter);
	}

	private <T> Optional<T> getWrapperValueFromHolder(ControllerStorageKey storageKey, Function<IStorageWrapper, T> valueGetter) {
		if (storageKey.groupId() == null) {
			return WorldHelper.getLoadedBlockEntity(level, storageKey.position(), IControllableStorage.class)
					.map(holder -> valueGetter.apply(holder.getStorageWrapper()));
		}

		List<IControllableStorage> holders = storageKeysByPosition.entrySet().stream().filter(entry -> entry.getValue().equals(storageKey))
				.map(Map.Entry::getKey).map(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class)).flatMap(Optional::stream).toList();
		// An unlocked endpoint permits general I/O; a locked one retains matching-item filtering.
		return holders.stream().filter(IControllableStorage::isControllerStorageAccessible).findFirst().or(() -> holders.stream().findFirst())
				.map(holder -> valueGetter.apply(holder.getStorageWrapper()));
	}

	public void addStorageStack(BlockPos storagePos, ItemStackKey itemStackKey) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		stackStorageKeys.computeIfAbsent(itemStackKey, stackKey -> new LinkedHashSet<>()).add(storageKey);
		stackStorages.computeIfAbsent(itemStackKey, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageStacks.computeIfAbsent(storageKey, key -> new HashSet<>()).add(itemStackKey);
		itemStackKeys.computeIfAbsent(itemStackKey.getStack().getItem(), item -> new LinkedHashSet<>()).add(itemStackKey);
	}

	public void removeStorageStack(BlockPos storagePos, ItemStackKey stackKey) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		stackStorageKeys.computeIfPresent(stackKey, (sk, storageKeys) -> {
			storageKeys.remove(storageKey);
			return storageKeys;
		});
		if (stackStorageKeys.containsKey(stackKey) && stackStorageKeys.get(stackKey).isEmpty()) {
			stackStorageKeys.remove(stackKey);
		}
		stackStorages.computeIfPresent(stackKey, (sk, positions) -> {
			positions.remove(storagePos);
			return positions;
		});
		if (stackStorages.containsKey(stackKey) && stackStorages.get(stackKey).isEmpty()) {
			stackStorages.remove(stackKey);

			itemStackKeys.computeIfPresent(stackKey.getStack().getItem(), (i, stackKeys) -> {
				stackKeys.remove(stackKey);
				return stackKeys;
			});
			if (itemStackKeys.containsKey(stackKey.getStack().getItem()) && itemStackKeys.get(stackKey.getStack().getItem()).isEmpty()) {
				itemStackKeys.remove(stackKey.getStack().getItem());
			}
		}
		storageStacks.computeIfPresent(storageKey, (key, stackKeys) -> {
			stackKeys.remove(stackKey);
			return stackKeys;
		});
		if (storageStacks.containsKey(storageKey) && storageStacks.get(storageKey).isEmpty()) {
			storageStacks.remove(storageKey);
		}
	}

	public void removeStorageStacks(BlockPos storagePos) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		storageStacks.computeIfPresent(storageKey, (key, stackKeys) -> {
			stackKeys.forEach(stackKey -> {
				Set<ControllerStorageKey> storageKeys = stackStorageKeys.get(stackKey);
				if (storageKeys != null) {
					storageKeys.remove(storageKey);
					stackStorages.computeIfPresent(stackKey, (sk, positions) -> {
						positions.remove(storagePos);
						return positions;
					});
					if (storageKeys.isEmpty()) {
						stackStorageKeys.remove(stackKey);
						stackStorages.remove(stackKey);
						itemStackKeys.computeIfPresent(stackKey.getStack().getItem(), (i, positions) -> {
							positions.remove(stackKey);
							return positions;
						});
						if (itemStackKeys.containsKey(stackKey.getStack().getItem()) && itemStackKeys.get(stackKey.getStack().getItem()).isEmpty()) {
							itemStackKeys.remove(stackKey.getStack().getItem());
						}
					}
				}
			});
			return stackKeys;
		});
		storageStacks.remove(storageKey);
	}

	protected boolean hasItem(Item item) {
		return itemStackKeys.containsKey(item);
	}

	protected boolean isMemorizedItem(ItemStack stack) {
		return memorizedItemStorages.containsKey(stack.getItem()) || memorizedStackStorages.containsKey(ItemStackKey.getHashCode(stack));
	}

	protected boolean isFilterItem(Item item) {
		return filterItemStorageKeys.containsKey(item);
	}

	public void removeBoundable(BlockPos boundablePos) {
		removeConnectingBlock(boundablePos);
		verifyStoragesConnected();
	}

	public void removeStorage(BlockPos storagePos) {
		removeConnectingBlock(storagePos);
		removeStorageInventoryDataAndUnregisterController(storagePos);
		verifyStoragesConnected();
	}

	private void removeConnectingBlock(BlockPos storagePos) {
		if (connectingBlocks.remove(storagePos)) {
			WorldHelper.getLoadedBlockEntity(level, storagePos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController);
		}
	}

	public void removeNonConnectingBlock(BlockPos storagePos) {
		if (nonConnectingBlocks.remove(storagePos)) {
			WorldHelper.getLoadedBlockEntity(level, storagePos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController);
		}
	}

	private void removeStorageInventoryDataAndUnregisterController(BlockPos storagePos) {
		if (!storageKeysByPosition.containsKey(storagePos)) {
			return;
		}
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		if (storageKey.groupId() != null && getStorageMemberPositions(storageKey).size() > 1) {
			boolean isListenerSource = storagePos.equals(listenerSourcePositions.get(storageKey));
			if (isListenerSource) {
				removeStorageRoutingData(storagePos);
				listenerSourcePositions.remove(storageKey);
			}
			storageKeysByPosition.remove(storagePos);
			WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(storage -> {
				if (isListenerSource) {
					storage.unregisterController();
				} else {
					storage.unregisterControllerMembership();
				}
			});
			if (isListenerSource) {
				getStorageMemberPositions(storageKey).stream().findFirst().ifPresent(newSourcePos -> {
					updateStorageKeyAnchor(storageKey, newSourcePos);
					addStorageStacksAndRegisterListeners(newSourcePos);
				});
			}
			clearCachedHandlers();
			setChanged();
			WorldHelper.notifyBlockEntityUpdate(this);
			return;
		}
		removeStorageInventoryData(storagePos);
		linkedBlocks.remove(storagePos);

		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController);

		clearCachedHandlers();
		setChanged();
		WorldHelper.notifyBlockEntityUpdate(this);
	}

	private void removeStorageInventoryData(BlockPos storagePos) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		Integer idx = storageKeyIndexes.get(storageKey);
		if (idx == null) {
			return;
		}
		totalSlots -= getStorageSlots(idx);
		removeStorageRoutingData(storagePos);
		listenerSourcePositions.remove(storageKey);
		storagePositions.remove((int) idx);
		storageKeys.remove((int) idx);
		removeStorageKeyIndex(storageKey);
		storageKeysByPosition.remove(storagePos);
		removeBaseIndexAt(idx);
	}

	private void removeStorageRoutingData(BlockPos storagePos) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		removeStorageStacks(storagePos);
		removeStorageMemorizedItems(storagePos);
		removeStorageMemorizedStacks(storagePos);
		removeStorageWithEmptySlots(storagePos);
		removeStorageFilterItems(storagePos);
		filteredInputStorageKeys.remove(storageKey);
		filteredInputStorages.remove(storagePos);
	}

	private void updateStorageKeyAnchor(ControllerStorageKey storageKey, BlockPos anchorPos) {
		Integer index = storageKeyIndexes.get(storageKey);
		if (index == null || storageKey.position().equals(anchorPos)) {
			return;
		}
		ControllerStorageKey updatedKey = new ControllerStorageKey(anchorPos, storageKey.groupId());
		storageKeys.set(index, updatedKey);
		storageKeyIndexes.remove(storageKey);
		storageKeyIndexes.put(updatedKey, index);
		storagePositions.set(index, anchorPos);
	}

	private Set<BlockPos> getStorageMemberPositions(ControllerStorageKey storageKey) {
		return storageKeysByPosition.entrySet().stream().filter(entry -> entry.getValue().equals(storageKey)).map(Map.Entry::getKey).collect(LinkedHashSet::new,
				Set::add, Set::addAll);
	}

	protected Set<BlockPos> getStorageMemberPositions(BlockPos storagePos) {
		return getStorageMemberPositions(getStorageKey(storagePos));
	}

	private void removeStorageKeyIndex(ControllerStorageKey storageKey) {
		Integer removedIndex = storageKeyIndexes.remove(storageKey);
		if (removedIndex == null)
			return;

		for (Map.Entry<ControllerStorageKey, Integer> entry : storageKeyIndexes.entrySet()) {
			int index = entry.getValue();
			if (index > removedIndex) {
				entry.setValue(index - 1);
			}
		}
	}

	private void removeStorageFilterItems(BlockPos storagePos) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		storageFilterItems.computeIfPresent(storageKey, (key, items) -> {
			items.forEach(item -> {
				Set<ControllerStorageKey> storageKeys = filterItemStorageKeys.get(item);
				if (storageKeys != null) {
					storageKeys.remove(storageKey);
					filterItemStorages.computeIfPresent(item, (i, positions) -> {
						positions.remove(storagePos);
						return positions;
					});
					if (storageKeys.isEmpty()) {
						filterItemStorageKeys.remove(item);
						filterItemStorages.remove(item);
					}
				}
			});
			return items;
		});
		storageFilterItems.remove(storageKey);
	}

	private void removeStorageMemorizedItems(BlockPos storagePos) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		storageMemorizedItems.computeIfPresent(storageKey, (key, items) -> {
			items.forEach(item -> {
				Set<ControllerStorageKey> storageKeys = memorizedItemStorageKeys.get(item);
				if (storageKeys != null) {
					storageKeys.remove(storageKey);
					memorizedItemStorages.computeIfPresent(item, (i, positions) -> {
						positions.remove(storagePos);
						return positions;
					});
					if (storageKeys.isEmpty()) {
						memorizedItemStorageKeys.remove(item);
						memorizedItemStorages.remove(item);
					}
				}
			});
			return items;
		});
		storageMemorizedItems.remove(storageKey);
	}

	private void removeStorageMemorizedStacks(BlockPos storagePos) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		storageMemorizedStacks.computeIfPresent(storageKey, (key, items) -> {
			items.forEach(stackHash -> {
				Set<ControllerStorageKey> storageKeys = memorizedStackStorageKeys.get(stackHash);
				if (storageKeys != null) {
					storageKeys.remove(storageKey);
					memorizedStackStorages.computeIfPresent(stackHash, (i, positions) -> {
						positions.remove(storagePos);
						return positions;
					});
					if (storageKeys.isEmpty()) {
						memorizedStackStorageKeys.remove(stackHash);
						memorizedStackStorages.remove(stackHash);
					}
				}
			});
			return items;
		});
		storageMemorizedStacks.remove(storageKey);
	}

	private void verifyStoragesConnected() {
		HashSet<BlockPos> toVerify = new HashSet<>(storageKeysByPosition.keySet());
		toVerify.addAll(connectingBlocks);
		toVerify.addAll(nonConnectingBlocks);

		Set<BlockPos> positionsToCheck = new HashSet<>();
		for (Direction dir : Direction.values()) {
			BlockPos offsetPos = getBlockPos().offset(dir.getNormal());
			if (toVerify.contains(offsetPos)) {
				positionsToCheck.add(offsetPos);
			}
		}
		Set<BlockPos> positionsChecked = new HashSet<>();

		verifyConnected(toVerify, positionsToCheck, positionsChecked);

		linkedBlocks.forEach(linkedPosition -> WorldHelper.getBlockEntity(getLevel(), linkedPosition, ILinkable.class).ifPresent(l -> {
			if (l.connectLinkedSelf() && toVerify.contains(linkedPosition)) {
				positionsToCheck.add(linkedPosition);
			}
			l.getConnectablePositions().forEach(p -> {
				if (toVerify.contains(p)) {
					positionsToCheck.add(p);
				}
			});
		}));

		verifyConnected(toVerify, positionsToCheck, positionsChecked);

		toVerify.forEach(storagePos -> {
			removeConnectingBlock(storagePos);
			removeNonConnectingBlock(storagePos);
			removeStorageInventoryDataAndUnregisterController(storagePos);
		});

		clearCachedHandlers();
	}

	private void verifyConnected(HashSet<BlockPos> toVerify, Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked) {
		while (!positionsToCheck.isEmpty()) {
			Iterator<BlockPos> it = positionsToCheck.iterator();
			BlockPos posToCheck = it.next();
			it.remove();

			positionsChecked.add(posToCheck);
			WorldHelper.getLoadedBlockEntity(level, posToCheck, IControllerBoundable.class).ifPresent(h -> {
				toVerify.remove(posToCheck);
				if (h.canConnectStorages()) {
					for (Direction dir : Direction.values()) {
						BlockPos pos = posToCheck.offset(dir.getNormal());
						if (!positionsChecked.contains(pos) && toVerify.contains(pos)) {
							positionsToCheck.add(pos);
						}
					}
				}
			});
		}
	}

	private void removeBaseIndexAt(int idx) {
		if (idx >= baseIndexes.size()) {
			return;
		}
		int slotsRemoved = getStorageSlots(idx);
		baseIndexes.remove(idx);
		for (int i = idx; i < baseIndexes.size(); i++) {
			baseIndexes.set(i, baseIndexes.get(i) - slotsRemoved);
		}
	}

	protected ControllerBlockEntityBase(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
		super(blockEntityType, pos, state);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (side == null) {
				if (noSideItemHandlerCap == null) {
					noSideItemHandlerCap = LazyOptional.of(() -> this).cast();
				}
				return noSideItemHandlerCap.cast();
			} else {
				if (itemHandlerCap == null) {
					itemHandlerCap = LazyOptional.of(() -> new CachedFailedInsertInventoryHandler(() -> this, () -> level != null ? level.getGameTime() : 0));
				}
				return itemHandlerCap.cast();
			}
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		if (itemHandlerCap != null) {
			itemHandlerCap.invalidate();
			itemHandlerCap = null;
		}
		if (noSideItemHandlerCap != null) {
			noSideItemHandlerCap.invalidate();
			noSideItemHandlerCap = null;
		}
	}

	@Override
	public int getSlots() {
		return totalSlots;
	}

	private int getIndexForSlot(int slot) {
		if (slot < 0) {
			return -1;
		}

		for (int i = 0; i < baseIndexes.size(); i++) {
			if (slot - baseIndexes.get(i) < 0) {
				return i;
			}
		}
		return -1;
	}

	protected IItemHandlerModifiable getHandlerFromIndex(int index) {
		if (index < 0 || index >= storagePositions.size()) {
			return (IItemHandlerModifiable) EmptyHandler.INSTANCE;
		}
		if (index >= cachedHandlers.length) {
			cachedHandlers = Arrays.copyOf(cachedHandlers, index + 1);
		}

		if (cachedHandlers[index] != null) {
			IItemHandlerModifiable handler = cachedHandlers[index].get();
			if (handler != null) {
				return handler;
			}
		}

		IItemHandlerModifiable handler = getWrapperValueFromHolder(storageKeys.get(index),
				wrapper -> (IItemHandlerModifiable) wrapper.getInventoryForInputOutput()).orElse((IItemHandlerModifiable) EmptyHandler.INSTANCE);
		cachedHandlers[index] = new WeakReference<>(handler);

		return handler;
	}

	protected int getSlotFromIndex(int slot, int index) {
		if (index <= 0 || index >= baseIndexes.size()) {
			return slot;
		}
		return slot - baseIndexes.get(index - 1);
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (isSlotIndexInvalid(slot)) {
			return ItemStack.EMPTY;
		}
		int handlerIndex = getIndexForSlot(slot);
		IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
		slot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, slot, "getStackInSlot")) {
			return handler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}

	private boolean isSlotIndexInvalid(int slot) {
		return slot < 0 || slot >= totalSlots;
	}

	private boolean validateHandlerSlotIndex(IItemHandler handler, int handlerIndex, int slot, String methodName) {
		if (slot >= 0 && slot < handler.getSlots()) {
			return true;
		}
		handleInvalidSlotAccess(handlerIndex, slot, methodName);

		return false;
	}

	private void handleInvalidSlotAccess(int handlerIndex, int slot, String methodName) {
		if (level == null) {
			return;
		}

		long gameTime = level.getGameTime();
		if (gameTime - lastInvalidSlotLogTime < INVALID_SLOT_LOG_INTERVAL_TICKS) {
			return;
		}

		lastInvalidSlotLogTime = gameTime;
		invalidSlotIncidentCount++;
		if (handlerIndex < 0 || handlerIndex >= storageKeys.size()) {
			SophisticatedCore.LOGGER.debug(
					"Invalid handler index calculated {} in controller's {} method. If you see many of these messages try replacing controller at {}",
					() -> handlerIndex, () -> methodName, () -> getBlockPos().toShortString());
		} else {
			SophisticatedCore.LOGGER.debug(
					"Invalid slot {} passed into controller's {} method for storage at {}. If you see many of these messages try replacing controller at {}",
					() -> slot, () -> methodName, () -> storageKeys.get(handlerIndex).position().toShortString(), () -> getBlockPos().toShortString());
		}

		if (!refreshingAfterInvalidSlots && invalidSlotIncidentCount >= INVALID_SLOT_REFRESH_THRESHOLD
				&& gameTime - lastInvalidSlotRefreshTime >= INVALID_SLOT_REFRESH_COOLDOWN_TICKS) {
			lastInvalidSlotRefreshTime = gameTime;
			refreshingAfterInvalidSlots = true;
			SophisticatedCore.LOGGER.debug("Refreshing controller at {} after {} invalid slot incidents were logged", () -> getBlockPos().toShortString(),
					() -> invalidSlotIncidentCount);
			refreshConnectedStoragesAfterRepeatedInvalidSlots();
			invalidSlotIncidentCount = 0;
			refreshingAfterInvalidSlots = false;
		}
	}

	private void refreshConnectedStoragesAfterRepeatedInvalidSlots() {
		unregisterCurrentConnections();
		clearControllerStateForRefresh();
		searchAndAddBoundables();
		rebuildLinkedBlockConnections();
		setChanged();
		WorldHelper.notifyBlockEntityUpdate(this);
	}

	private void unregisterCurrentConnections() {
		unregisterStorageMemberships();
		connectingBlocks
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		nonConnectingBlocks
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
	}

	private void clearControllerStateForRefresh() {
		storagePositions.clear();
		storageKeys.clear();
		storageKeyIndexes.clear();
		storageKeysByPosition.clear();
		listenerSourcePositions.clear();
		baseIndexes.clear();
		totalSlots = 0;
		stackStorageKeys.clear();
		stackStorages.clear();
		storageStacks.clear();
		itemStackKeys.clear();
		emptySlotStorageKeys.clear();
		emptySlotsStorages.clear();
		memorizedItemStorageKeys.clear();
		memorizedItemStorages.clear();
		storageMemorizedItems.clear();
		memorizedStackStorageKeys.clear();
		memorizedStackStorages.clear();
		storageMemorizedStacks.clear();
		filterItemStorageKeys.clear();
		filterItemStorages.clear();
		storageFilterItems.clear();
		filteredInputStorageKeys.clear();
		filteredInputStorages.clear();
		connectingBlocks.clear();
		nonConnectingBlocks.clear();
		cachedHandlers = new WeakReference[0];
	}

	private void rebuildLinkedBlockConnections() {
		for (BlockPos linkedPos : new ArrayList<>(linkedBlocks)) {
			WorldHelper.getBlockEntity(level, linkedPos, ILinkable.class).ifPresent(l -> {
				if (l.connectLinkedSelf()) {
					Set<BlockPos> positionsToCheck = new LinkedHashSet<>();
					positionsToCheck.add(linkedPos);
					searchAndAddBoundables(positionsToCheck, true);
				}

				searchAndAddBoundables(new LinkedHashSet<>(l.getConnectablePositions()), false);
			});
		}
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (isSlotIndexInvalid(slot)) {
			return stack;
		}

		if (simulate) {
			int handlerIndex = getIndexForSlot(slot);
			IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
			slot = getSlotFromIndex(slot, handlerIndex);
			if (validateHandlerSlotIndex(handler, handlerIndex, slot, "insertItem")) {
				return handler.insertItem(slot, stack, true);
			}
		}

		return insertItem(stack, simulate, true);
	}

	@Override
	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		return insertItem(stack, simulate, true);
	}

	protected ItemStack insertItem(ItemStack stack, boolean simulate, boolean insertIntoAnyEmpty) {
		ItemStackKey stackKey = ItemStackKey.of(stack);
		ItemStack remaining = stack;

		remaining = insertIntoStoragesThatMatchStack(remaining, stackKey, simulate);
		if (remaining.isEmpty()) {
			return remaining;
		}

		int stackHash = stackKey.hashCode();
		if (memorizedStackStorageKeys.containsKey(stackHash)) {
			remaining = insertIntoStorages(memorizedStackStorageKeys.get(stackHash), remaining, simulate, false);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		remaining = insertIntoStoragesThatMatchItem(remaining, simulate);
		if (remaining.isEmpty()) {
			return remaining;
		}

		if (memorizedItemStorageKeys.containsKey(stack.getItem())) {
			remaining = insertIntoStorages(memorizedItemStorageKeys.get(stack.getItem()), remaining, simulate, false);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		if (filterItemStorageKeys.containsKey(stack.getItem())) {
			remaining = insertIntoStorages(filterItemStorageKeys.get(stack.getItem()), remaining, simulate, false);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		remaining = insertIntoStorages(filteredInputStorageKeys, remaining, simulate, true);
		if (remaining.isEmpty() || !insertIntoAnyEmpty) {
			return remaining;
		}

		return insertIntoStorages(emptySlotStorageKeys, filteredInputStorageKeys, remaining, simulate, false);
	}

	private ItemStack insertIntoStoragesThatMatchStack(ItemStack remaining, ItemStackKey stackKey, boolean simulate) {
		if (stackStorageKeys.containsKey(stackKey)) {
			remaining = insertIntoStorages(stackStorageKeys.get(stackKey), remaining, simulate, false);
		}
		return remaining;
	}

	private ItemStack insertIntoStoragesThatMatchItem(ItemStack remaining, boolean simulate) {
		if (!emptySlotStorageKeys.isEmpty() && itemStackKeys.containsKey(remaining.getItem())) {
			Set<ItemStackKey> matchingStackKeys = itemStackKeys.get(remaining.getItem());
			if (remaining.getCount() > remaining.getMaxStackSize()) {
				matchingStackKeys = new LinkedHashSet<>(matchingStackKeys); // to prevent CME when larger than maxStackSize stack causes new key to be added to
																			// set which then continues to be iterated on
			}

			for (ItemStackKey key : matchingStackKeys) {
				if (stackStorageKeys.containsKey(key)) {
					remaining = insertIntoStorages(stackStorageKeys.get(key), remaining, simulate, true);
					if (remaining.isEmpty()) {
						return ItemStack.EMPTY;
					}
				}
			}
		}
		return remaining;
	}

	private ItemStack insertIntoStorages(Set<ControllerStorageKey> storageKeys, ItemStack stack, boolean simulate, boolean checkHasEmptySlotFirst) {
		return insertIntoStorages(storageKeys, Collections.emptySet(), stack, simulate, checkHasEmptySlotFirst);
	}

	private ItemStack insertIntoStorages(Set<ControllerStorageKey> storageKeys, Set<ControllerStorageKey> storageKeysToSkip, ItemStack stack, boolean simulate,
			boolean checkHasEmptySlotFirst) {
		ItemStack remaining = stack;
		Set<ControllerStorageKey> storageKeysCopy = new LinkedHashSet<>(storageKeys); // to prevent CME if stack insertion changes tracked storage keys
		for (ControllerStorageKey storageKey : storageKeysCopy) {
			if (storageKeysToSkip.contains(storageKey)) {
				continue;
			}
			if (checkHasEmptySlotFirst && !emptySlotStorageKeys.contains(storageKey)) {
				continue;
			}
			remaining = insertIntoStorage(storageKey, remaining, simulate);
			if (remaining.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	protected ItemStack insertIntoStorage(BlockPos storagePos, ItemStack remaining, boolean simulate) {
		return insertIntoStorage(getStorageKey(storagePos), remaining, simulate);
	}

	private ItemStack insertIntoStorage(ControllerStorageKey storageKey, ItemStack remaining, boolean simulate) {
		Integer idx = storageKeyIndexes.get(storageKey);
		if (idx == null) {
			return remaining;
		}

		IItemHandlerModifiable handler = getHandlerFromIndex(idx);

		if (handler instanceof IItemHandlerSimpleInserter simpleInserter) {
			return simpleInserter.insertItem(remaining, simulate);
		}

		return remaining;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (isSlotIndexInvalid(slot)) {
			return ItemStack.EMPTY;
		}

		int handlerIndex = getIndexForSlot(slot);
		IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
		slot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, slot, "extractItem(int slot, int amount, boolean simulate)")) {
			return handler.extractItem(slot, amount, simulate);
		}

		return ItemStack.EMPTY;
	}

	public ItemStack extractItem(ItemStack stack, boolean simulate) {
		ItemStackKey stackKey = ItemStackKey.of(stack);
		if (!stackStorageKeys.containsKey(stackKey)) {
			return ItemStack.EMPTY;
		}

		Set<ControllerStorageKey> storageKeysCopy = new LinkedHashSet<>(stackStorageKeys.get(stackKey));

		ItemStack remaining = stack;

		for (ControllerStorageKey storageKey : storageKeysCopy) {
			Integer idx = storageKeyIndexes.get(storageKey);
			if (idx == null) {
				continue;
			}

			IItemHandlerModifiable handler = getHandlerFromIndex(idx);
			if (handler instanceof IItemHandlerSimpleExtractor simpleExtractor) {
				ItemStack extracted = simpleExtractor.extractItem(stack, simulate);
				if (extracted.getCount() > 0) {
					remaining = stack.copyWithCount(remaining.getCount() - extracted.getCount());
				}
				if (remaining.isEmpty()) {
					break;
				}
			}
		}

		return stack.getCount() == remaining.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - remaining.getCount());
	}

	@Override
	public int getSlotLimit(int slot) {
		if (isSlotIndexInvalid(slot)) {
			return 0;
		}
		int handlerIndex = getIndexForSlot(slot);
		IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
		int localSlot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, localSlot, "getSlotLimit(int slot)")) {
			return handler.getSlotLimit(localSlot);
		}
		return 0;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		if (isSlotIndexInvalid(slot)) {
			return false;
		}
		int handlerIndex = getIndexForSlot(slot);
		IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
		int localSlot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, localSlot, "isItemValid(int slot, ItemStack stack)")) {
			return handler.isItemValid(localSlot, stack);
		}
		return false;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		if (isSlotIndexInvalid(slot)) {
			return;
		}
		int handlerIndex = getIndexForSlot(slot);
		IItemHandlerModifiable handler = getHandlerFromIndex(handlerIndex);
		slot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, slot, "setStackInSlot(int slot, ItemStack stack)")) {
			handler.setStackInSlot(slot, stack);
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		detachFromStoragesAndUnlinkBlocks();
	}

	public void detachFromStoragesAndUnlinkBlocks() {
		unregisterStorageMemberships();
		connectingBlocks
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		nonConnectingBlocks
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		// copying into new hashset to prevent CME when these are removed
		new HashSet<>(linkedBlocks)
				.forEach(linkedPos -> WorldHelper.getLoadedBlockEntity(level, linkedPos, ILinkable.class).ifPresent(ILinkable::unlinkFromController));
	}

	private void unregisterStorageMemberships() {
		Set<BlockPos> listenerSources = new HashSet<>(listenerSourcePositions.values());
		storageKeysByPosition.keySet()
				.forEach(storagePos -> WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(storage -> {
					if (listenerSources.contains(storagePos)) {
						storage.unregisterController();
					} else {
						storage.unregisterControllerMembership();
					}
				}));
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);

		saveData(tag);
	}

	private CompoundTag saveData(CompoundTag tag) {
		NBTHelper.putList(tag, "storagePositions", storagePositions, p -> LongTag.valueOf(p.asLong()));
		if (storageKeys.stream().anyMatch(storageKey -> storageKey.groupId() != null)) {
			NBTHelper.putList(tag, STORAGE_KEYS_TAG, storageKeys, ControllerStorageKey::save);
			NBTHelper.putList(tag, STORAGE_MEMBER_KEYS_TAG, storageKeysByPosition.entrySet().stream().filter(entry -> entry.getValue().groupId() != null)
					.map(entry -> new ControllerStorageKey(entry.getKey(), entry.getValue().groupId())).toList(), ControllerStorageKey::save);
		} else {
			tag.remove(STORAGE_KEYS_TAG);
			tag.remove(STORAGE_MEMBER_KEYS_TAG);
		}
		NBTHelper.putList(tag, "connectingBlocks", connectingBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "nonConnectingBlocks", nonConnectingBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "linkedBlocks", linkedBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "baseIndexes", baseIndexes, IntTag::valueOf);
		tag.putInt("totalSlots", totalSlots);

		return tag;
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);

		storageKeys.clear();
		if (tag.contains(STORAGE_KEYS_TAG, Tag.TAG_LIST)) {
			storageKeys.addAll(NBTHelper
					.getCollection(tag, STORAGE_KEYS_TAG, Tag.TAG_COMPOUND, t -> Optional.of(ControllerStorageKey.load((CompoundTag) t)), ArrayList::new)
					.orElseGet(ArrayList::new));
		} else {
			NBTHelper.getCollection(tag, "storagePositions", Tag.TAG_LONG, t -> Optional.of(new ControllerStorageKey(BlockPos.of(((LongTag) t).getAsLong()))),
					ArrayList::new).ifPresent(storageKeys::addAll);
		}
		setupStorageKeyIndexes();
		if (tag.contains(STORAGE_MEMBER_KEYS_TAG, Tag.TAG_LIST)) {
			List<ControllerStorageKey> storageMemberKeys = NBTHelper
					.getCollection(tag, STORAGE_MEMBER_KEYS_TAG, Tag.TAG_COMPOUND, t -> Optional.of(ControllerStorageKey.load((CompoundTag) t)), ArrayList::new)
					.orElseGet(ArrayList::new);
			storageMemberKeys.forEach(memberKey -> storageKeyIndexes.keySet().stream().filter(memberKey::equals).findFirst()
					.ifPresent(storageKey -> storageKeysByPosition.put(memberKey.position(), storageKey)));
			storageKeys.stream().filter(storageKey -> storageKey.groupId() != null).forEach(storageKey -> {
				boolean hasPersistedMember = storageMemberKeys.stream().anyMatch(storageKey::equals);
				boolean anchorIsPersistedMember = storageMemberKeys.stream()
						.anyMatch(memberKey -> storageKey.equals(memberKey) && storageKey.position().equals(memberKey.position()));
				if (hasPersistedMember && !anchorIsPersistedMember) {
					storageKeysByPosition.remove(storageKey.position());
				}
			});
		}
		connectingBlocks = NBTHelper
				.getCollection(tag, "connectingBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new)
				.orElseGet(LinkedHashSet::new);
		nonConnectingBlocks = NBTHelper
				.getCollection(tag, "nonConnectingBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new)
				.orElseGet(LinkedHashSet::new);
		baseIndexes = NBTHelper.getCollection(tag, "baseIndexes", Tag.TAG_INT, t -> Optional.of(((IntTag) t).getAsInt()), ArrayList::new)
				.orElseGet(ArrayList::new);
		totalSlots = tag.getInt("totalSlots");
		linkedBlocks = NBTHelper.getCollection(tag, "linkedBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new)
				.orElseGet(LinkedHashSet::new);
	}

	private void setupStorageKeyIndexes() {
		storagePositions.clear();
		storageKeyIndexes.clear();
		storageKeysByPosition.clear();
		for (int i = 0; i < storageKeys.size(); i++) {
			ControllerStorageKey storageKey = storageKeys.get(i);
			storagePositions.add(storageKey.position());
			storageKeyIndexes.put(storageKey, i);
			storageKeysByPosition.put(storageKey.position(), storageKey);
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		return saveData(super.getUpdateTag());
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public void addStorageWithEmptySlots(BlockPos storageBlockPos) {
		emptySlotStorageKeys.add(getStorageKey(storageBlockPos));
		emptySlotsStorages.add(storageBlockPos);
	}

	public void removeStorageWithEmptySlots(BlockPos storageBlockPos) {
		emptySlotStorageKeys.remove(getStorageKey(storageBlockPos));
		emptySlotsStorages.remove(storageBlockPos);
	}

	public Set<BlockPos> getLinkedBlocks() {
		return linkedBlocks;
	}

	public List<BlockPos> getStoragePositions() {
		return storagePositions;
	}

	public void setStorageFilterItems(BlockPos storagePos, Set<Item> filterItems) {
		removeStorageFilterItems(storagePos);
		if (filterItems.isEmpty()) {
			return;
		}

		ControllerStorageKey storageKey = getStorageKey(storagePos);
		for (Item item : filterItems) {
			filterItemStorageKeys.computeIfAbsent(item, stackKey -> new LinkedHashSet<>()).add(storageKey);
			filterItemStorages.computeIfAbsent(item, stackKey -> new LinkedHashSet<>()).add(storagePos);
		}
		storageFilterItems.put(storageKey, new LinkedHashSet<>(filterItems));
	}

	public boolean hasMatchingStack(ItemStackKey stackKey) {
		return stackStorageKeys.containsKey(stackKey) || memorizedStackStorageKeys.containsKey(stackKey.hashCode());
	}

	public boolean hasMatchingItem(Item item) {
		return itemStackKeys.containsKey(item) || memorizedItemStorageKeys.containsKey(item) || filterItemStorageKeys.containsKey(item);
	}

	public boolean hasMatchingFilter(ItemStack stack) {
		ItemStack singleItemStack = stack.copyWithCount(1);
		Set<ControllerStorageKey> storageKeysCopy = new LinkedHashSet<>(filteredInputStorageKeys);
		for (ControllerStorageKey storageKey : storageKeysCopy) {
			if (!emptySlotStorageKeys.contains(storageKey)) {
				continue;
			}
			if (insertIntoStorage(storageKey, singleItemStack, true).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isInsertBlocked() {
		return storageKeys.stream().allMatch(
				storageKey -> getWrapperValueFromHolder(storageKey, storageWrapper -> storageWrapper.getInventoryHandler().isInsertBlocked()).orElse(true));
	}
}
