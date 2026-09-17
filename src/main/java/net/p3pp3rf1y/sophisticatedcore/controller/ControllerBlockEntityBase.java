package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IIOFilterUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInsertBlockOverride;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ControllerBlockEntityBase extends BlockEntity implements ResourceHandler<ItemResource>, IInsertBlockOverride {
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
	protected final Set<ControllerStorageKey> emptySlotStorageKeys = new TreeSet<>(storageKeyDistanceComparator);
	protected final Set<BlockPos> emptySlotsStorages = new TreeSet<>(distanceComparator);
	protected final Set<ControllerStorageKey> filteredInputStorageKeys = new TreeSet<>(storageKeyDistanceComparator);
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

	private WeakReference<ResourceHandler<ItemResource>>[] cachedHandlers = new WeakReference[0];
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
			positionsToCheck.add(getBlockPos().offset(dir.getUnitVec3i()));
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
			changeSlots(storagePos, storage.getStorageWrapper().getInventoryForInputOutput().size(),
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
			BlockPos pos = currentPos.offset(dir.getUnitVec3i());
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
		totalSlots += getHandlerFromIndex(index).size();
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
			ITrackedContentsItemResourceHandler handler = storage.getStorageWrapper().getInventoryForInputOutput();
			handler.getTrackedStacks().forEach(k -> addStorageStack(storagePos, k));
			if (handler.hasEmptySlots()) {
				emptySlotStorageKeys.add(storageKey);
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
		return holders.stream().filter(IControllableStorage::isControllerStorageAccessible).findFirst().or(() -> holders.stream().findFirst())
				.map(holder -> valueGetter.apply(holder.getStorageWrapper()));
	}

	public void addStorageStack(BlockPos storagePos, ItemStackKey itemStackKey) {
		ControllerStorageKey storageKey = getStorageKey(storagePos);
		stackStorageKeys.computeIfAbsent(itemStackKey, stackKey -> new LinkedHashSet<>()).add(storageKey);
		stackStorages.computeIfAbsent(itemStackKey, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageStacks.computeIfAbsent(storageKey, key -> new HashSet<>()).add(itemStackKey);
		itemStackKeys.computeIfAbsent(itemStackKey.stack().getItem(), item -> new LinkedHashSet<>()).add(itemStackKey);
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

			itemStackKeys.computeIfPresent(stackKey.stack().getItem(), (i, stackKeys) -> {
				stackKeys.remove(stackKey);
				return stackKeys;
			});
			if (itemStackKeys.containsKey(stackKey.stack().getItem())) {
				if (itemStackKeys.get(stackKey.stack().getItem()).isEmpty()) {
					itemStackKeys.remove(stackKey.stack().getItem());
				}
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
						itemStackKeys.computeIfPresent(stackKey.stack().getItem(), (i, positions) -> {
							positions.remove(stackKey);
							return positions;
						});
						if (itemStackKeys.containsKey(stackKey.stack().getItem())) {
							if (itemStackKeys.get(stackKey.stack().getItem()).isEmpty()) {
								itemStackKeys.remove(stackKey.stack().getItem());
							}
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
		return memorizedItemStorageKeys.containsKey(stack.getItem()) || memorizedStackStorageKeys.containsKey(ItemStack.hashItemAndComponents(stack));
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
			BlockPos offsetPos = getBlockPos().offset(dir.getUnitVec3i());
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
						BlockPos pos = posToCheck.offset(dir.getUnitVec3i());
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
	public int size() {
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

	protected ResourceHandler<ItemResource> getHandlerFromIndex(int index) {
		if (index < 0 || index >= storageKeys.size()) {
			return EmptyResourceHandler.instance();
		}
		if (index >= cachedHandlers.length) {
			cachedHandlers = Arrays.copyOf(cachedHandlers, index + 1);
		}

		if (cachedHandlers[index] != null) {
			ResourceHandler<ItemResource> handler = cachedHandlers[index].get();
			if (handler != null) {
				return handler;
			}
		}

		ResourceHandler<ItemResource> handler = getWrapperValueFromHolder(storageKeys.get(index),
				storageWrapper -> (ResourceHandler<ItemResource>) storageWrapper.getInventoryForInputOutput()).orElse(EmptyResourceHandler.instance());
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
	public ItemResource getResource(int slot) {
		if (isSlotIndexInvalid(slot)) {
			return ItemResource.EMPTY;
		}
		int handlerIndex = getIndexForSlot(slot);
		ResourceHandler<ItemResource> handler = getHandlerFromIndex(handlerIndex);
		slot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, slot, "getResource")) {
			return handler.getResource(slot);
		}
		return ItemResource.EMPTY;
	}

	@Override
	public long getAmountAsLong(int i) {
		if (isSlotIndexInvalid(i)) {
			return 0;
		}
		int handlerIndex = getIndexForSlot(i);
		ResourceHandler<ItemResource> handler = getHandlerFromIndex(handlerIndex);
		i = getSlotFromIndex(i, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, i, "getAmountAsLong")) {
			return handler.getAmountAsLong(i);
		}
		return 0;
	}

	private boolean isSlotIndexInvalid(int slot) {
		return slot < 0 || slot >= totalSlots;
	}

	private boolean validateHandlerSlotIndex(ResourceHandler<ItemResource> handler, int handlerIndex, int slot, String methodName) {
		if (slot >= 0 && slot < handler.size()) {
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
		filteredInputStorageKeys.clear();
		memorizedItemStorageKeys.clear();
		memorizedItemStorages.clear();
		storageMemorizedItems.clear();
		memorizedStackStorageKeys.clear();
		memorizedStackStorages.clear();
		storageMemorizedStacks.clear();
		filterItemStorageKeys.clear();
		filterItemStorages.clear();
		storageFilterItems.clear();
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
	public int insert(int index, ItemResource resource, int amount, TransactionContext transactionContext) {
		if (isSlotIndexInvalid(index) || resource.isEmpty() || amount <= 0) {
			return 0;
		}
		int handlerIndex = getIndexForSlot(index);
		ResourceHandler<ItemResource> handler = getHandlerFromIndex(handlerIndex);
		index = getSlotFromIndex(index, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, index,
				"insert(int index, ItemResource resource, int amount, TransactionContext transactionContext)")) {
			return handler.insert(index, resource, amount, transactionContext);
		}
		return 0;
	}

	@Override
	public int insert(ItemResource resource, int amount, TransactionContext transaction) {
		return insertItem(resource, amount, transaction, true);
	}

	protected int insertItem(ItemStack stack, TransactionContext tx, boolean insertIntoAnyEmpty) {
		return insertItem(ItemResource.of(stack), stack.getCount(), tx, insertIntoAnyEmpty);
	}

	protected int insertItem(ItemResource resource, int amount, TransactionContext tx, boolean insertIntoAnyEmpty) {
		ItemStackKey stackKey = ItemStackKey.of(resource);
		int inserted = 0;

		inserted += insertIntoStoragesThatMatchStack(resource, amount, stackKey, tx);
		if (inserted >= amount) {
			return inserted;
		}

		int stackHash = stackKey.hashCode();
		if (memorizedStackStorageKeys.containsKey(stackHash)) {
			inserted += insertIntoStorages(memorizedStackStorageKeys.get(stackHash), resource, amount - inserted, tx, false);
			if (inserted >= amount) {
				return inserted;
			}
		}

		inserted += insertIntoStoragesThatMatchItem(resource, amount - inserted, tx);
		if (inserted >= amount) {
			return inserted;
		}

		if (memorizedItemStorageKeys.containsKey(resource.getItem())) {
			inserted += insertIntoStorages(memorizedItemStorageKeys.get(resource.getItem()), resource, amount - inserted, tx, false);
			if (inserted >= amount) {
				return inserted;
			}
		}

		if (filterItemStorageKeys.containsKey(resource.getItem())) {
			inserted += insertIntoStorages(filterItemStorageKeys.get(resource.getItem()), resource, amount - inserted, tx, false);
			if (inserted >= amount) {
				return inserted;
			}
		}

		inserted += insertIntoStorages(filteredInputStorageKeys, resource, amount - inserted, tx, true);
		if (inserted >= amount || !insertIntoAnyEmpty) {
			return inserted;
		}

		return inserted + insertIntoStorages(emptySlotStorageKeys, filteredInputStorageKeys, resource, amount - inserted, tx, false);
	}

	private int insertIntoStoragesThatMatchStack(ItemResource resource, int amount, ItemStackKey stackKey, TransactionContext tx) {
		if (stackStorageKeys.containsKey(stackKey)) {
			return insertIntoStorages(stackStorageKeys.get(stackKey), resource, amount, tx, false);
		}
		return 0;
	}

	private int insertIntoStoragesThatMatchItem(ItemResource resource, int amount, TransactionContext tx) {
		int inserted = 0;
		if (!emptySlotStorageKeys.isEmpty() && itemStackKeys.containsKey(resource.getItem())) {
			Set<ItemStackKey> matchingStackKeys = itemStackKeys.get(resource.getItem());
			if (amount > resource.getMaxStackSize()) {
				matchingStackKeys = new LinkedHashSet<>(matchingStackKeys); // to prevent CME when larger than maxStackSize stack causes new key to be added to
																			// set which then continues to be iterated on
			}

			for (ItemStackKey key : matchingStackKeys) {
				if (stackStorageKeys.containsKey(key)) {
					inserted += insertIntoStorages(stackStorageKeys.get(key), resource, amount - inserted, tx, true);
					if (inserted >= amount) {
						break;
					}
				}
			}
		}
		return inserted;
	}

	private int insertIntoStorages(Set<ControllerStorageKey> storageKeys, ItemResource resource, int amount, TransactionContext tx,
			boolean checkHasEmptySlotFirst) {
		return insertIntoStorages(storageKeys, Collections.emptySet(), resource, amount, tx, checkHasEmptySlotFirst);
	}

	private int insertIntoStorages(Set<ControllerStorageKey> storageKeys, Set<ControllerStorageKey> storageKeysToSkip, ItemResource resource, int amount,
			TransactionContext tx, boolean checkHasEmptySlotFirst) {
		int inserted = 0;
		Set<ControllerStorageKey> storageKeysCopy = new LinkedHashSet<>(storageKeys); // to prevent CME if stack insertion changes tracked storage keys
		for (ControllerStorageKey storageKey : storageKeysCopy) {
			if (storageKeysToSkip.contains(storageKey)) {
				continue;
			}
			if (checkHasEmptySlotFirst && !emptySlotStorageKeys.contains(storageKey)) {
				continue;
			}
			inserted += insertIntoStorage(storageKey, resource, amount - inserted, tx);
			if (inserted >= amount) {
				return amount;
			}
		}
		return inserted;
	}

	protected int insertIntoStorage(BlockPos storagePos, ItemResource resource, int amount, TransactionContext tx) {
		return insertIntoStorage(getStorageKey(storagePos), resource, amount, tx);
	}

	private int insertIntoStorage(ControllerStorageKey storageKey, ItemResource resource, int amount, TransactionContext tx) {
		Integer idx = storageKeyIndexes.get(storageKey);
		if (idx == null) {
			return 0;
		}

		ResourceHandler<ItemResource> handler = getHandlerFromIndex(idx);
		return handler.insert(resource, amount, tx);
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
		if (isSlotIndexInvalid(index)) {
			return 0;
		}
		int handlerIndex = getIndexForSlot(index);
		ResourceHandler<ItemResource> handler = getHandlerFromIndex(handlerIndex);
		index = getSlotFromIndex(index, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, index, "extract(int index, ItemResource resource, int amount, TransactionContext tx)")) {
			return handler.extract(index, resource, amount, tx);
		}
		return 0;
	}

	@Override
	public int extract(ItemResource resource, int amount, TransactionContext tx) {
		if (resource.isEmpty() || amount <= 0) {
			return 0;
		}
		int extracted = 0;
		ItemStackKey stackKey = ItemStackKey.of(resource);
		if (stackStorageKeys.containsKey(stackKey)) {
			extracted += extractFromStorages(stackKey, resource, amount, tx);
			if (extracted >= amount) {
				return extracted;
			}
		}

		return extracted;
	}

	private int extractFromStorages(ItemStackKey stackKey, ItemResource resource, int amount, TransactionContext tx) {
		int extracted = 0;
		Set<ControllerStorageKey> storageKeysCopy = new LinkedHashSet<>(stackStorageKeys.get(stackKey)); // to prevent CME if stack extraction changes tracked
																											// storage keys
		for (ControllerStorageKey storageKey : storageKeysCopy) {
			extracted += extractFromStorage(storageKey, resource, amount - extracted, tx);
			if (extracted >= amount) {
				return amount;
			}
		}
		return extracted;
	}

	private int extractFromStorage(BlockPos pos, ItemResource resource, int amount, TransactionContext tx) {
		return extractFromStorage(getStorageKey(pos), resource, amount, tx);
	}

	private int extractFromStorage(ControllerStorageKey storageKey, ItemResource resource, int amount, TransactionContext tx) {
		Integer idx = storageKeyIndexes.get(storageKey);
		if (idx == null) {
			return 0;
		}
		return getHandlerFromIndex(idx).extract(resource, amount, tx);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		if (isSlotIndexInvalid(index)) {
			return 0;
		}
		int handlerIndex = getIndexForSlot(index);
		ResourceHandler<ItemResource> handler = getHandlerFromIndex(handlerIndex);
		index = getSlotFromIndex(index, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, index, "getCapacityAsLong(int index, ItemResource resource)")) {
			return handler.getCapacityAsLong(index, resource);
		}
		return 0;
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		if (isSlotIndexInvalid(index)) {
			return false;
		}
		int handlerIndex = getIndexForSlot(index);
		ResourceHandler<ItemResource> handler = getHandlerFromIndex(handlerIndex);
		index = getSlotFromIndex(index, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, index, "isValid(int index, ItemResource resource)")) {
			return handler.isValid(index, resource);
		}
		return false;
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
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);

		saveData(out);
	}

	private void saveData(ValueOutput out) {
		ValueIOHelper.saveList(out, STORAGE_KEYS_TAG, storageKeys, ControllerStorageKey.CODEC);
		ValueIOHelper.saveList(out, STORAGE_MEMBER_KEYS_TAG, storageKeysByPosition.entrySet().stream().filter(entry -> entry.getValue().groupId() != null)
				.map(entry -> new ControllerStorageKey(entry.getKey(), entry.getValue().groupId())).toList(), ControllerStorageKey.CODEC);
		ValueIOHelper.saveList(out, "connectingBlocks", connectingBlocks, BlockPos.CODEC);
		ValueIOHelper.saveList(out, "nonConnectingBlocks", nonConnectingBlocks, BlockPos.CODEC);
		ValueIOHelper.saveList(out, "linkedBlocks", linkedBlocks, BlockPos.CODEC);
		ValueIOHelper.saveList(out, "baseIndexes", baseIndexes, ExtraCodecs.POSITIVE_INT);
		out.putInt("totalSlots", totalSlots);
	}

	@Override
	public void loadAdditional(ValueInput in) {
		super.loadAdditional(in);

		storageKeys.clear();
		storageKeys.addAll(in.listOrEmpty(STORAGE_KEYS_TAG, ControllerStorageKey.CODEC).stream().toList());
		setupStorageKeyIndexes();
		List<ControllerStorageKey> storageMemberKeys = in.listOrEmpty(STORAGE_MEMBER_KEYS_TAG, ControllerStorageKey.CODEC).stream().toList();
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
		connectingBlocks = in.listOrEmpty("connectingBlocks", BlockPos.CODEC).stream().collect(Collectors.toCollection(LinkedHashSet::new));
		nonConnectingBlocks = in.listOrEmpty("nonConnectingBlocks", BlockPos.CODEC).stream().collect(Collectors.toCollection(LinkedHashSet::new));
		baseIndexes = in.listOrEmpty("baseIndexes", ExtraCodecs.POSITIVE_INT).stream().collect(Collectors.toCollection(ArrayList::new));
		totalSlots = in.getIntOr("totalSlots", 0);
		linkedBlocks = in.listOrEmpty("linkedBlocks", BlockPos.CODEC).stream().collect(Collectors.toCollection(LinkedHashSet::new));
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
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return super.getUpdateTag(registries).merge(ValueIOHelper.collectOutputToTag(registries, this::saveData));
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

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		detachFromStoragesAndUnlinkBlocks();
	}

	public boolean hasMatchingStack(ItemStackKey stackKey) {
		return stackStorageKeys.containsKey(stackKey) || memorizedStackStorageKeys.containsKey(stackKey.hashCode());
	}

	public boolean hasMatchingItem(Item item) {
		return itemStackKeys.containsKey(item) || memorizedItemStorageKeys.containsKey(item) || filterItemStorageKeys.containsKey(item);
	}

	public boolean hasMatchingFilter(ItemStack stack) {
		try (Transaction tx = Transaction.openRoot()) {
			return hasMatchingFilter(stack, tx);
		}
	}

	public boolean hasMatchingFilter(ItemStack stack, TransactionContext tx) {
		ItemResource resource = ItemResource.of(stack);
		Set<ControllerStorageKey> storageKeysCopy = new LinkedHashSet<>(filteredInputStorageKeys);
		for (ControllerStorageKey storageKey : storageKeysCopy) {
			if (!emptySlotStorageKeys.contains(storageKey)) {
				continue;
			}
			try (Transaction nestedTx = Transaction.open(tx)) {
				if (insertIntoStorage(storageKey.position(), resource, 1, nestedTx) == 1) {
					return true;
				}
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
