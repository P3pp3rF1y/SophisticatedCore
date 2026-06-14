package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
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

public abstract class ControllerBlockEntityBase extends BlockEntity implements IItemHandlerSimpleInserter, IInsertBlockOverride {
	private static final long INVALID_SLOT_LOG_INTERVAL_TICKS = 20;
	private static final int INVALID_SLOT_REFRESH_THRESHOLD = 2;
	private static final long INVALID_SLOT_REFRESH_COOLDOWN_TICKS = 40;

	private List<BlockPos> storagePositions = new ArrayList<>();
	private final Map<BlockPos, Integer> storagePositionIndexes = new HashMap<>();
	private List<Integer> baseIndexes = new ArrayList<>();
	private int totalSlots = 0;
	protected final Map<ItemStackKey, Set<BlockPos>> stackStorages = new HashMap<>();
	private final Map<BlockPos, Set<ItemStackKey>> storageStacks = new HashMap<>();
	protected final Map<Item, Set<ItemStackKey>> itemStackKeys = new HashMap<>();
	private final Comparator<BlockPos> distanceComparator = Comparator.<BlockPos>comparingDouble(p -> p.distSqr(getBlockPos())).thenComparing(Comparator.naturalOrder());
	protected final Set<BlockPos> emptySlotsStorages = new TreeSet<>(distanceComparator);
	protected final Set<BlockPos> filteredInputStorages = new TreeSet<>(distanceComparator);

	protected final Map<Item, Set<BlockPos>> memorizedItemStorages = new HashMap<>();
	private final Map<BlockPos, Set<Item>> storageMemorizedItems = new HashMap<>();
	protected final Map<Integer, Set<BlockPos>> memorizedStackStorages = new HashMap<>();
	private final Map<BlockPos, Set<Integer>> storageMemorizedStacks = new HashMap<>();
	protected final Map<Item, Set<BlockPos>> filterItemStorages = new HashMap<>();
	private final Map<BlockPos, Set<Item>> storageFilterItems = new HashMap<>();
	private Set<BlockPos> linkedBlocks = new TreeSet<>(distanceComparator);
	private Set<BlockPos> connectingBlocks = new TreeSet<>(distanceComparator);
	private Set<BlockPos> nonConnectingBlocks = new TreeSet<>(distanceComparator);

	private WeakReference<IItemHandlerModifiable>[] cachedHandlers = new WeakReference[0];
	private long lastInvalidSlotLogTime = -INVALID_SLOT_LOG_INTERVAL_TICKS;
	private long lastInvalidSlotRefreshTime = -INVALID_SLOT_REFRESH_COOLDOWN_TICKS;
	private int invalidSlotIncidentCount = 0;
	private boolean refreshingAfterInvalidSlots = false;

	public boolean addLinkedBlock(BlockPos linkedPos) {
		if (level != null && !level.isClientSide() && isWithinRange(linkedPos) && !linkedBlocks.contains(linkedPos) && !storagePositions.contains(linkedPos)) {

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
			WorldHelper.notifyBlockUpdate(this);
			return true;
		}
		return false;
	}

	public void removeLinkedBlock(BlockPos storageBlockPos) {
		linkedBlocks.remove(storageBlockPos);
		setChanged();
		verifyStoragesConnected();

		WorldHelper.notifyBlockUpdate(this);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide()) {
			stackStorages.clear();
			storageStacks.clear();
			itemStackKeys.clear();
			emptySlotsStorages.clear();
			filteredInputStorages.clear();
			storagePositions.forEach(this::addStorageStacksAndRegisterListeners);
		}
	}

	public boolean isStorageConnected(BlockPos storagePos) {
		return storagePositions.contains(storagePos);
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

	public void updateEmptySlots(BlockPos storagePos, boolean hasEmptySlots) {
		if (emptySlotsStorages.contains(storagePos) && !hasEmptySlots) {
			emptySlotsStorages.remove(storagePos);
		} else if (!emptySlotsStorages.contains(storagePos) && hasEmptySlots) {
			emptySlotsStorages.add(storagePos);
		}
	}

	private void updateBaseIndexesAndTotalSlots(BlockPos storagePos, int newSlots) {
		int index = storagePositions.indexOf(storagePos);
		int originalSlots = getStorageSlots(index);

		int diff = newSlots - originalSlots;

		for (int i = index; i < baseIndexes.size(); i++) {
			baseIndexes.set(i, baseIndexes.get(i) + diff);
		}

		totalSlots += diff;
		WorldHelper.notifyBlockUpdate(this);
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

			final boolean finalFirst = first;
			WorldHelper.getLoadedBlockEntity(level, posToCheck, IControllerBoundable.class).ifPresentOrElse(boundable ->
							tryToConnectStorageAndAddPositionsToCheckAround(positionsToCheck, addingLinkedSelf, positionsChecked, posToCheck, finalFirst, boundable),
					() -> positionsChecked.add(posToCheck)
			);
			first = false;
		}
	}

	private void tryToConnectStorageAndAddPositionsToCheckAround(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf, Set<BlockPos> positionsChecked, BlockPos posToCheck, boolean finalFirst, IControllerBoundable boundable) {
		if (boundable.canBeConnected() || (addingLinkedSelf && finalFirst)) {
			if (boundable instanceof ILinkable linkable && linkable.isLinked() && (!addingLinkedSelf || !finalFirst)) {
				linkedBlocks.remove(posToCheck);
				linkable.setNotLinked();
				clearCachedHandlers();
			} else if (boundable instanceof IControllableStorage storage && storage.hasStorageData()) {
				addStorageData(storage.getControlledStorageBlockPos());
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

	private void clearCachedHandlers() {
		cachedHandlers = new WeakReference[storagePositions.size()];
	}

	public void clearCachedHandler(BlockPos storagePos) {
		Integer index = storagePositionIndexes.get(storagePos);
		if (index != null && index < cachedHandlers.length) {
			cachedHandlers[index] = null;
		}
	}

	public void updateStorageInputFilter(BlockPos storagePos) {
		if (!storagePositions.contains(storagePos)) {
			filteredInputStorages.remove(storagePos);
			return;
		}

		getWrapperValueFromHolder(storagePos, this::hasInputFilter)
				.ifPresentOrElse(hasInputFilter -> setStorageInputFilter(storagePos, hasInputFilter), () -> filteredInputStorages.remove(storagePos));
	}

	private boolean hasInputFilter(IStorageWrapper storageWrapper) {
		return storageWrapper.getUpgradeHandler().getWrappersThatImplement(IIOFilterUpgrade.class).stream().anyMatch(wrapper -> wrapper.getInputFilter().isPresent());
	}

	private void setStorageInputFilter(BlockPos storagePos, boolean hasInputFilter) {
		if (hasInputFilter) {
			filteredInputStorages.add(storagePos);
		} else {
			filteredInputStorages.remove(storagePos);
		}
	}

	private void addUncheckedPositionsAround(Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked, BlockPos currentPos) {
		for (Direction dir : Direction.values()) {
			BlockPos pos = currentPos.offset(dir.getNormal());
			if (!positionsChecked.contains(pos) && ((!storagePositions.contains(pos) && !connectingBlocks.contains(pos) && !nonConnectingBlocks.contains(pos)) || linkedBlocks.contains(pos)) && isWithinRange(pos)) {
				positionsToCheck.add(pos);
			}
		}
	}

	private boolean isWithinRange(BlockPos pos) {
		return Math.abs(pos.getX() - getBlockPos().getX()) <= getSearchRange() && Math.abs(pos.getY() - getBlockPos().getY()) <= getSearchRange() && Math.abs(pos.getZ() - getBlockPos().getZ()) <= getSearchRange();
	}

	protected abstract int getSearchRange();

	public void addStorage(BlockPos storagePos) {
		if (storagePositions.contains(storagePos)) {
			if (level != null) {
				WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(storage ->
						storage.getControllerPos()
								.filter(getBlockPos()::equals)
								.ifPresent(pos -> storage.unregisterController())
				);
			}
			removeStorageInventoryData(storagePos);
			clearCachedHandlers();
		}

		if (isWithinRange(storagePos)) {
			HashSet<BlockPos> positionsToCheck = new LinkedHashSet<>();
			positionsToCheck.add(storagePos);
			searchAndAddBoundables(positionsToCheck, false);
		}
		WorldHelper.notifyBlockUpdate(this);
	}

	private void addStorageData(BlockPos storagePos) {
		if (storagePositions.contains(storagePos)) {
			return;
		}

		storagePositions.add(storagePos);
		int index = storagePositions.size() - 1;
		storagePositionIndexes.put(storagePos, index);
		totalSlots += getHandlerFromIndex(index).getSlots();
		baseIndexes.add(totalSlots);
		addStorageStacksAndRegisterListeners(storagePos);

		setChanged();
		WorldHelper.notifyBlockUpdate(this);
	}

	public void addStorageStacksAndRegisterListeners(BlockPos storagePos) {
		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(storage -> {
			ITrackedContentsItemHandler handler = storage.getStorageWrapper().getInventoryForInputOutput();
			handler.getTrackedStacks().forEach(k -> addStorageStack(storagePos, k));
			if (handler.hasEmptySlots()) {
				emptySlotsStorages.add(storagePos);
			}
			MemorySettingsCategory memorySettings = storage.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			memorySettings.getFilterItemSlots().keySet().forEach(i -> addStorageMemorizedItem(storagePos, i));
			memorySettings.getFilterStackSlots().keySet().forEach(stackHash -> addStorageMemorizedStack(storagePos, stackHash));

			setStorageFilterItems(storagePos, storage.getStorageWrapper().getInventoryHandler().getFilterItems());
			setStorageInputFilter(storagePos, hasInputFilter(storage.getStorageWrapper()));

			storage.registerController(this);
		});
	}

	public void addStorageMemorizedItem(BlockPos storagePos, Item item) {
		memorizedItemStorages.computeIfAbsent(item, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageMemorizedItems.computeIfAbsent(storagePos, pos -> new HashSet<>()).add(item);
	}

	public void addStorageMemorizedStack(BlockPos storagePos, int stackHash) {
		memorizedStackStorages.computeIfAbsent(stackHash, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageMemorizedStacks.computeIfAbsent(storagePos, pos -> new HashSet<>()).add(stackHash);
	}

	public void removeStorageMemorizedItem(BlockPos storagePos, Item item) {
		memorizedItemStorages.computeIfPresent(item, (i, positions) -> {
			positions.remove(storagePos);
			return positions;
		});
		if (memorizedItemStorages.containsKey(item) && memorizedItemStorages.get(item).isEmpty()) {
			memorizedItemStorages.remove(item);
		}
		storageMemorizedItems.remove(storagePos);
	}

	public void removeStorageMemorizedStack(BlockPos storagePos, int stackHash) {
		memorizedStackStorages.computeIfPresent(stackHash, (i, positions) -> {
			positions.remove(storagePos);
			return positions;
		});
		if (memorizedStackStorages.containsKey(stackHash) && memorizedStackStorages.get(stackHash).isEmpty()) {
			memorizedStackStorages.remove(stackHash);
		}
		storageMemorizedStacks.remove(storagePos);
	}

	private <T> Optional<T> getWrapperValueFromHolder(BlockPos storagePos, Function<IStorageWrapper, T> valueGetter) {
		return WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).map(holder -> valueGetter.apply(holder.getStorageWrapper()));
	}

	public void addStorageStack(BlockPos storagePos, ItemStackKey itemStackKey) {
		stackStorages.computeIfAbsent(itemStackKey, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageStacks.computeIfAbsent(storagePos, pos -> new HashSet<>()).add(itemStackKey);
		itemStackKeys.computeIfAbsent(itemStackKey.getStack().getItem(), item -> new LinkedHashSet<>()).add(itemStackKey);
	}

	public void removeStorageStack(BlockPos storagePos, ItemStackKey stackKey) {
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
		storageStacks.computeIfPresent(storagePos, (pos, stackKeys) -> {
			stackKeys.remove(stackKey);
			return stackKeys;
		});
		if (storageStacks.containsKey(storagePos) && storageStacks.get(storagePos).isEmpty()) {
			storageStacks.remove(storagePos);
		}
	}

	public void removeStorageStacks(BlockPos storagePos) {
		storageStacks.computeIfPresent(storagePos, (pos, stackKeys) -> {
			stackKeys.forEach(stackKey -> {
				Set<BlockPos> storages = stackStorages.get(stackKey);
				if (storages != null) {
					storages.remove(storagePos);
					if (storages.isEmpty()) {
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
		storageStacks.remove(storagePos);
	}

	protected boolean hasItem(Item item) {
		return itemStackKeys.containsKey(item);
	}

	protected boolean isMemorizedItem(ItemStack stack) {
		return memorizedItemStorages.containsKey(stack.getItem()) || memorizedStackStorages.containsKey(ItemStack.hashItemAndComponents(stack));
	}

	protected boolean isFilterItem(Item item) {
		return filterItemStorages.containsKey(item);
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
		if (!storagePositions.contains(storagePos)) {
			return;
		}
		removeStorageInventoryData(storagePos);
		linkedBlocks.remove(storagePos);

		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController);

		clearCachedHandlers();
		setChanged();
		WorldHelper.notifyBlockUpdate(this);
	}

	private void removeStorageInventoryData(BlockPos storagePos) {
		int idx = storagePositions.indexOf(storagePos);
		totalSlots -= getStorageSlots(idx);
		removeStorageStacks(storagePos);
		removeStorageMemorizedItems(storagePos);
		removeStorageMemorizedStacks(storagePos);
		removeStorageWithEmptySlots(storagePos);
		removeStorageFilterItems(storagePos);
		filteredInputStorages.remove(storagePos);
		storagePositions.remove(idx);
		removeStoragePositionIndex(storagePos);
		removeBaseIndexAt(idx);
	}

	private void removeStoragePositionIndex(BlockPos storagePos) {
		Integer removedIndex = storagePositionIndexes.remove(storagePos);
		if (removedIndex == null) return;

		for (Map.Entry<BlockPos, Integer> entry : storagePositionIndexes.entrySet()) {
			int index = entry.getValue();
			if (index > removedIndex) {
				entry.setValue(index - 1);
			}
		}
	}

	private void removeStorageFilterItems(BlockPos storagePos) {
		storageFilterItems.computeIfPresent(storagePos, (pos, items) -> {
			items.forEach(item -> {
				Set<BlockPos> storages = filterItemStorages.get(item);
				if (storages != null) {
					storages.remove(storagePos);
					if (storages.isEmpty()) {
						filterItemStorages.remove(item);
					}
				}
			});
			return items;
		});
		storageFilterItems.remove(storagePos);
	}

	private void removeStorageMemorizedItems(BlockPos storagePos) {
		storageMemorizedItems.computeIfPresent(storagePos, (pos, items) -> {
			items.forEach(item -> {
				Set<BlockPos> storages = memorizedItemStorages.get(item);
				if (storages != null) {
					storages.remove(storagePos);
					if (storages.isEmpty()) {
						memorizedItemStorages.remove(item);
					}
				}
			});
			return items;
		});
		storageMemorizedItems.remove(storagePos);
	}

	private void removeStorageMemorizedStacks(BlockPos storagePos) {
		storageMemorizedStacks.computeIfPresent(storagePos, (pos, items) -> {
			items.forEach(stackHash -> {
				Set<BlockPos> storages = memorizedStackStorages.get(stackHash);
				if (storages != null) {
					storages.remove(storagePos);
					if (storages.isEmpty()) {
						memorizedStackStorages.remove(stackHash);
					}
				}
			});
			return items;
		});
		storageMemorizedStacks.remove(storagePos);
	}

	private void verifyStoragesConnected() {
		HashSet<BlockPos> toVerify = new HashSet<>(storagePositions);
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
			return (IItemHandlerModifiable) EmptyItemHandler.INSTANCE;
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

		IItemHandlerModifiable handler = getWrapperValueFromHolder(storagePositions.get(index), wrapper -> (IItemHandlerModifiable) wrapper.getInventoryForInputOutput()).orElse((IItemHandlerModifiable) EmptyItemHandler.INSTANCE);
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
		if (handlerIndex < 0 || handlerIndex >= storagePositions.size()) {
			SophisticatedCore.LOGGER.debug("Invalid handler index calculated {} in controller's {} method. If you see many of these messages try replacing controller at {}", () -> handlerIndex, () -> methodName, () -> getBlockPos().toShortString());
		} else {
			SophisticatedCore.LOGGER.debug("Invalid slot {} passed into controller's {} method for storage at {}. If you see many of these messages try replacing controller at {}", () -> slot, () -> methodName, () -> storagePositions.get(handlerIndex).toShortString(), () -> getBlockPos().toShortString());
		}

		if (!refreshingAfterInvalidSlots && invalidSlotIncidentCount >= INVALID_SLOT_REFRESH_THRESHOLD && gameTime - lastInvalidSlotRefreshTime >= INVALID_SLOT_REFRESH_COOLDOWN_TICKS) {
			lastInvalidSlotRefreshTime = gameTime;
			refreshingAfterInvalidSlots = true;
			SophisticatedCore.LOGGER.debug("Refreshing controller at {} after {} invalid slot incidents were logged", () -> getBlockPos().toShortString(), () -> invalidSlotIncidentCount);
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
		WorldHelper.notifyBlockUpdate(this);
	}

	private void unregisterCurrentConnections() {
		storagePositions.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController));
		connectingBlocks.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		nonConnectingBlocks.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
	}

	private void clearControllerStateForRefresh() {
		storagePositions.clear();
		storagePositionIndexes.clear();
		baseIndexes.clear();
		totalSlots = 0;
		stackStorages.clear();
		storageStacks.clear();
		itemStackKeys.clear();
		emptySlotsStorages.clear();
		memorizedItemStorages.clear();
		storageMemorizedItems.clear();
		memorizedStackStorages.clear();
		storageMemorizedStacks.clear();
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
		if (memorizedStackStorages.containsKey(stackHash)) {
			remaining = insertIntoStorages(memorizedStackStorages.get(stackHash), remaining, simulate, false);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		remaining = insertIntoStoragesThatMatchItem(remaining, simulate);
		if (remaining.isEmpty()) {
			return remaining;
		}

		if (memorizedItemStorages.containsKey(stack.getItem())) {
			remaining = insertIntoStorages(memorizedItemStorages.get(stack.getItem()), remaining, simulate, false);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		if (filterItemStorages.containsKey(stack.getItem())) {
			remaining = insertIntoStorages(filterItemStorages.get(stack.getItem()), remaining, simulate, false);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		remaining = insertIntoStorages(filteredInputStorages, remaining, simulate, true);
		if (remaining.isEmpty() || !insertIntoAnyEmpty) {
			return remaining;
		}

		return insertIntoStorages(emptySlotsStorages, filteredInputStorages, remaining, simulate, false);
	}

	private ItemStack insertIntoStoragesThatMatchStack(ItemStack remaining, ItemStackKey stackKey, boolean simulate) {
		if (stackStorages.containsKey(stackKey)) {
			Set<BlockPos> positions = stackStorages.get(stackKey);
			remaining = insertIntoStorages(positions, remaining, simulate, false);
		}
		return remaining;
	}

	private ItemStack insertIntoStoragesThatMatchItem(ItemStack remaining, boolean simulate) {
		if (!emptySlotsStorages.isEmpty() && itemStackKeys.containsKey(remaining.getItem())) {
			Set<ItemStackKey> matchingStackKeys = itemStackKeys.get(remaining.getItem());
			if (remaining.getCount() > remaining.getMaxStackSize()) {
				matchingStackKeys = new LinkedHashSet<>(matchingStackKeys); //to prevent CME when larger than maxStackSize stack causes new key to be added to set which then continues to be iterated on
			}

			for (ItemStackKey key : matchingStackKeys) {
				if (stackStorages.containsKey(key)) {
					Set<BlockPos> positions = stackStorages.get(key);
					remaining = insertIntoStorages(positions, remaining, simulate, true);
					if (remaining.isEmpty()) {
						return ItemStack.EMPTY;
					}
				}
			}
		}
		return remaining;
	}

	private ItemStack insertIntoStorages(Set<BlockPos> positions, ItemStack stack, boolean simulate, boolean checkHasEmptySlotFirst) {
		return insertIntoStorages(positions, Collections.emptySet(), stack, simulate, checkHasEmptySlotFirst);
	}

	private ItemStack insertIntoStorages(Set<BlockPos> positions, Set<BlockPos> positionsToSkip, ItemStack stack, boolean simulate, boolean checkHasEmptySlotFirst) {
		ItemStack remaining = stack;
		Set<BlockPos> positionsCopy = new LinkedHashSet<>(positions); //to prevent CME if stack insertion actually causes set of positions to change
		for (BlockPos storagePos : positionsCopy) {
			if (positionsToSkip.contains(storagePos)) {
				continue;
			}
			if (checkHasEmptySlotFirst && !emptySlotsStorages.contains(storagePos)) {
				continue;
			}
			remaining = insertIntoStorage(storagePos, remaining, simulate);
			if (remaining.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	protected ItemStack insertIntoStorage(BlockPos storagePos, ItemStack remaining, boolean simulate) {
		Integer idx = storagePositionIndexes.get(storagePos);
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
		if (!stackStorages.containsKey(stackKey)) {
			return ItemStack.EMPTY;
		}

		Set<BlockPos> positionsCopy = new LinkedHashSet<>(stackStorages.get(stackKey));

		ItemStack remaining = stack;

		for (BlockPos storagePos : positionsCopy) {
			Integer idx = storagePositionIndexes.get(storagePos);
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
		storagePositions.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController));
		connectingBlocks.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		nonConnectingBlocks.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		new HashSet<>(linkedBlocks).forEach(linkedPos -> WorldHelper.getLoadedBlockEntity(level, linkedPos, ILinkable.class).ifPresent(ILinkable::unlinkFromController)); //copying into new hashset to prevent CME when these are removed
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);

		saveData(tag);
	}

	private CompoundTag saveData(CompoundTag tag) {
		NBTHelper.putList(tag, "storagePositions", storagePositions, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "connectingBlocks", connectingBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "nonConnectingBlocks", nonConnectingBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "linkedBlocks", linkedBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "baseIndexes", baseIndexes, IntTag::valueOf);
		tag.putInt("totalSlots", totalSlots);

		return tag;
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);

		storagePositions = NBTHelper.getCollection(tag, "storagePositions", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), ArrayList::new).orElseGet(ArrayList::new);
		setupStoragePositionIndexes();
		connectingBlocks = NBTHelper.getCollection(tag, "connectingBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new).orElseGet(LinkedHashSet::new);
		nonConnectingBlocks = NBTHelper.getCollection(tag, "nonConnectingBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new).orElseGet(LinkedHashSet::new);
		baseIndexes = NBTHelper.getCollection(tag, "baseIndexes", Tag.TAG_INT, t -> Optional.of(((IntTag) t).getAsInt()), ArrayList::new).orElseGet(ArrayList::new);
		totalSlots = tag.getInt("totalSlots");
		linkedBlocks = NBTHelper.getCollection(tag, "linkedBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new).orElseGet(LinkedHashSet::new);
	}

	private void setupStoragePositionIndexes() {
		storagePositionIndexes.clear();
		for (int i = 0; i < storagePositions.size(); i++) {
			storagePositionIndexes.put(storagePositions.get(i), i);
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveData(super.getUpdateTag(registries));
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public void addStorageWithEmptySlots(BlockPos storageBlockPos) {
		emptySlotsStorages.add(storageBlockPos);
	}

	public void removeStorageWithEmptySlots(BlockPos storageBlockPos) {
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

		for (Item item : filterItems) {
			filterItemStorages.computeIfAbsent(item, stackKey -> new LinkedHashSet<>()).add(storagePos);
		}
		storageFilterItems.put(storagePos, new LinkedHashSet<>(filterItems));
	}

	public boolean hasMatchingStack(ItemStackKey stackKey) {
		return stackStorages.containsKey(stackKey) || memorizedStackStorages.containsKey(stackKey.hashCode());
	}

	public boolean hasMatchingItem(Item item) {
		return itemStackKeys.containsKey(item) || memorizedItemStorages.containsKey(item) || filterItemStorages.containsKey(item);
	}

	public boolean hasMatchingFilter(ItemStack stack) {
		ItemStack singleItemStack = stack.copyWithCount(1);
		Set<BlockPos> positionsCopy = new LinkedHashSet<>(filteredInputStorages);
		for (BlockPos storagePos : positionsCopy) {
			if (!emptySlotsStorages.contains(storagePos)) {
				continue;
			}
			if (insertIntoStorage(storagePos, singleItemStack, true).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isInsertBlocked() {
		return storagePositions.stream().allMatch(pos -> getWrapperValueFromHolder(pos, storageWrapper -> storageWrapper.getInventoryHandler().isInsertBlocked()).orElse(true));
	}
}
