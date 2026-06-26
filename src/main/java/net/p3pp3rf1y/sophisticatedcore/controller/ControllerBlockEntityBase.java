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

	private List<BlockPos> storagePositions = new ArrayList<>();
	private final Map<BlockPos, Integer> storagePositionIndexes = new HashMap<>();
	private List<Integer> baseIndexes = new ArrayList<>();
	private int totalSlots = 0;
	protected final Map<ItemStackKey, Set<BlockPos>> stackStorages = new HashMap<>();
	private final Map<BlockPos, Set<ItemStackKey>> storageStacks = new HashMap<>();
	protected final Map<Item, Set<ItemStackKey>> itemStackKeys = new HashMap<>();
	private final Comparator<BlockPos> distanceComparator = Comparator.<BlockPos>comparingDouble(p -> p.distSqr(getBlockPos()))
			.thenComparing(Comparator.naturalOrder());
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

	private WeakReference<ResourceHandler<ItemResource>>[] cachedHandlers = new WeakReference[0];
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
			positionsToCheck.add(getBlockPos().offset(dir.getUnitVec3i()));
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
			WorldHelper.getLoadedBlockEntity(level, posToCheck, IControllerBoundable.class)
					.ifPresentOrElse(boundable -> tryToConnectStorageAndAddPositionsToCheckAround(positionsToCheck, addingLinkedSelf, positionsChecked,
							posToCheck, finalFirst, boundable), () -> positionsChecked.add(posToCheck));
			first = false;
		}
	}

	private void tryToConnectStorageAndAddPositionsToCheckAround(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf, Set<BlockPos> positionsChecked,
			BlockPos posToCheck, boolean finalFirst, IControllerBoundable boundable) {
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

		getWrapperValueFromHolder(storagePos, this::hasInputFilter).ifPresentOrElse(hasInputFilter -> setStorageInputFilter(storagePos, hasInputFilter),
				() -> filteredInputStorages.remove(storagePos));
	}

	private boolean hasInputFilter(IStorageWrapper storageWrapper) {
		return storageWrapper.getUpgradeHandler().getWrappersThatImplement(IIOFilterUpgrade.class).stream()
				.anyMatch(wrapper -> wrapper.getInputFilter().isPresent());
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
			BlockPos pos = currentPos.offset(dir.getUnitVec3i());
			if (!positionsChecked.contains(pos) && ((!storagePositions.contains(pos) && !connectingBlocks.contains(pos) && !nonConnectingBlocks.contains(pos))
					|| linkedBlocks.contains(pos)) && isWithinRange(pos)) {
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
		if (storagePositions.contains(storagePos)) {
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
		WorldHelper.notifyBlockUpdate(this);
	}

	private void addStorageData(BlockPos storagePos) {
		if (storagePositions.contains(storagePos)) {
			return;
		}

		storagePositions.add(storagePos);
		int index = storagePositions.size() - 1;
		storagePositionIndexes.put(storagePos, index);
		totalSlots += getHandlerFromIndex(index).size();
		baseIndexes.add(totalSlots);
		addStorageStacksAndRegisterListeners(storagePos);

		setChanged();
		WorldHelper.notifyBlockUpdate(this);
	}

	public void addStorageStacksAndRegisterListeners(BlockPos storagePos) {
		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(storage -> {
			ITrackedContentsItemResourceHandler handler = storage.getStorageWrapper().getInventoryForInputOutput();
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
		itemStackKeys.computeIfAbsent(itemStackKey.stack().getItem(), item -> new LinkedHashSet<>()).add(itemStackKey);
	}

	public void removeStorageStack(BlockPos storagePos, ItemStackKey stackKey) {
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
		if (removedIndex == null)
			return;

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
		if (index < 0 || index >= storagePositions.size()) {
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

		ResourceHandler<ItemResource> handler = getWrapperValueFromHolder(storagePositions.get(index),
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
		if (handlerIndex < 0 || handlerIndex >= storagePositions.size()) {
			SophisticatedCore.LOGGER.debug(
					"Invalid handler index calculated {} in controller's {} method. If you see many of these messages try replacing controller at {}",
					() -> handlerIndex, () -> methodName, () -> getBlockPos().toShortString());
		} else {
			SophisticatedCore.LOGGER.debug(
					"Invalid slot {} passed into controller's {} method for storage at {}. If you see many of these messages try replacing controller at {}",
					() -> slot, () -> methodName, () -> storagePositions.get(handlerIndex).toShortString(), () -> getBlockPos().toShortString());
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
		WorldHelper.notifyBlockUpdate(this);
	}

	private void unregisterCurrentConnections() {
		storagePositions
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController));
		connectingBlocks
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		nonConnectingBlocks
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
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
		if (memorizedStackStorages.containsKey(stackHash)) {
			inserted += insertIntoStorages(memorizedStackStorages.get(stackHash), resource, amount - inserted, tx, false);
			if (inserted >= amount) {
				return inserted;
			}
		}

		inserted += insertIntoStoragesThatMatchItem(resource, amount - inserted, tx);
		if (inserted >= amount) {
			return inserted;
		}

		if (memorizedItemStorages.containsKey(resource.getItem())) {
			inserted += insertIntoStorages(memorizedItemStorages.get(resource.getItem()), resource, amount - inserted, tx, false);
			if (inserted >= amount) {
				return inserted;
			}
		}

		if (filterItemStorages.containsKey(resource.getItem())) {
			inserted += insertIntoStorages(filterItemStorages.get(resource.getItem()), resource, amount - inserted, tx, false);
			if (inserted >= amount) {
				return inserted;
			}
		}

		inserted += insertIntoStorages(filteredInputStorages, resource, amount - inserted, tx, true);
		if (inserted >= amount || !insertIntoAnyEmpty) {
			return inserted;
		}

		return inserted + insertIntoStorages(emptySlotsStorages, filteredInputStorages, resource, amount - inserted, tx, false);
	}

	private int insertIntoStoragesThatMatchStack(ItemResource resource, int amount, ItemStackKey stackKey, TransactionContext tx) {
		if (stackStorages.containsKey(stackKey)) {
			Set<BlockPos> positions = stackStorages.get(stackKey);
			return insertIntoStorages(positions, resource, amount, tx, false);
		}
		return 0;
	}

	private int insertIntoStoragesThatMatchItem(ItemResource resource, int amount, TransactionContext tx) {
		int inserted = 0;
		if (!emptySlotsStorages.isEmpty() && itemStackKeys.containsKey(resource.getItem())) {
			Set<ItemStackKey> matchingStackKeys = itemStackKeys.get(resource.getItem());
			if (amount > resource.getMaxStackSize()) {
				matchingStackKeys = new LinkedHashSet<>(matchingStackKeys); // to prevent CME when larger than maxStackSize stack causes new key to be added to
																			// set which then continues to be iterated on
			}

			for (ItemStackKey key : matchingStackKeys) {
				if (stackStorages.containsKey(key)) {
					Set<BlockPos> positions = stackStorages.get(key);
					inserted += insertIntoStorages(positions, resource, amount - inserted, tx, true);
					if (inserted >= amount) {
						break;
					}
				}
			}
		}
		return inserted;
	}

	private int insertIntoStorages(Set<BlockPos> positions, ItemResource resource, int amount, TransactionContext tx, boolean checkHasEmptySlotFirst) {
		return insertIntoStorages(positions, Collections.emptySet(), resource, amount, tx, checkHasEmptySlotFirst);
	}

	private int insertIntoStorages(Set<BlockPos> positions, Set<BlockPos> positionsToSkip, ItemResource resource, int amount, TransactionContext tx,
			boolean checkHasEmptySlotFirst) {
		int inserted = 0;
		Set<BlockPos> positionsCopy = new LinkedHashSet<>(positions); // to prevent CME if stack insertion actually causes set of positions to change
		for (BlockPos storagePos : positionsCopy) {
			if (positionsToSkip.contains(storagePos)) {
				continue;
			}
			if (checkHasEmptySlotFirst && !emptySlotsStorages.contains(storagePos)) {
				continue;
			}
			inserted += insertIntoStorage(storagePos, resource, amount - inserted, tx);
			if (inserted >= amount) {
				return amount;
			}
		}
		return inserted;
	}

	protected int insertIntoStorage(BlockPos storagePos, ItemResource resource, int amount, TransactionContext tx) {
		Integer idx = storagePositionIndexes.get(storagePos);
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
		if (stackStorages.containsKey(stackKey)) {
			extracted += extractFromStorages(stackKey, resource, amount, tx);
			if (extracted >= amount) {
				return extracted;
			}
		}

		return extracted;
	}

	private int extractFromStorages(ItemStackKey stackKey, ItemResource resource, int amount, TransactionContext tx) {
		int extracted = 0;
		Set<BlockPos> positionsCopy = new LinkedHashSet<>(stackStorages.get(stackKey)); // to prevent CME if stack extraction actually causes set of positions
																						// to change
		for (BlockPos storagePos : positionsCopy) {
			extracted += extractFromStorage(storagePos, resource, amount - extracted, tx);
			if (extracted >= amount) {
				return amount;
			}
		}
		return extracted;
	}

	private int extractFromStorage(BlockPos pos, ItemResource resource, int amount, TransactionContext tx) {
		Integer idx = storagePositionIndexes.get(pos);
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
		storagePositions
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController));
		connectingBlocks
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		nonConnectingBlocks
				.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		new HashSet<>(linkedBlocks)
				.forEach(linkedPos -> WorldHelper.getLoadedBlockEntity(level, linkedPos, ILinkable.class).ifPresent(ILinkable::unlinkFromController)); // copying
																																						// into
																																						// new
																																						// hashset
																																						// to
																																						// prevent
																																						// CME
																																						// when
																																						// these
																																						// are
																																						// removed
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);

		saveData(out);
	}

	private void saveData(ValueOutput out) {
		ValueIOHelper.saveList(out, "storagePositions", storagePositions, BlockPos.CODEC);
		ValueIOHelper.saveList(out, "connectingBlocks", connectingBlocks, BlockPos.CODEC);
		ValueIOHelper.saveList(out, "nonConnectingBlocks", nonConnectingBlocks, BlockPos.CODEC);
		ValueIOHelper.saveList(out, "linkedBlocks", linkedBlocks, BlockPos.CODEC);
		ValueIOHelper.saveList(out, "baseIndexes", baseIndexes, ExtraCodecs.POSITIVE_INT);
		out.putInt("totalSlots", totalSlots);
	}

	@Override
	public void loadAdditional(ValueInput in) {
		super.loadAdditional(in);

		storagePositions = in.listOrEmpty("storagePositions", BlockPos.CODEC).stream().collect(Collectors.toCollection(ArrayList::new));
		setupStoragePositionIndexes();
		connectingBlocks = in.listOrEmpty("connectingBlocks", BlockPos.CODEC).stream().collect(Collectors.toCollection(LinkedHashSet::new));
		nonConnectingBlocks = in.listOrEmpty("nonConnectingBlocks", BlockPos.CODEC).stream().collect(Collectors.toCollection(LinkedHashSet::new));
		baseIndexes = in.listOrEmpty("baseIndexes", ExtraCodecs.POSITIVE_INT).stream().collect(Collectors.toCollection(ArrayList::new));
		totalSlots = in.getIntOr("totalSlots", 0);
		linkedBlocks = in.listOrEmpty("linkedBlocks", BlockPos.CODEC).stream().collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private void setupStoragePositionIndexes() {
		storagePositionIndexes.clear();
		for (int i = 0; i < storagePositions.size(); i++) {
			storagePositionIndexes.put(storagePositions.get(i), i);
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

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		detachFromStoragesAndUnlinkBlocks();
	}

	public boolean hasMatchingStack(ItemStackKey stackKey) {
		return stackStorages.containsKey(stackKey) || memorizedStackStorages.containsKey(stackKey.hashCode());
	}

	public boolean hasMatchingItem(Item item) {
		return itemStackKeys.containsKey(item) || memorizedItemStorages.containsKey(item) || filterItemStorages.containsKey(item);
	}

	public boolean hasMatchingFilter(ItemStack stack) {
		try (Transaction tx = Transaction.openRoot()) {
			return hasMatchingFilter(stack, tx);
		}
	}

	public boolean hasMatchingFilter(ItemStack stack, TransactionContext tx) {
		ItemResource resource = ItemResource.of(stack);
		Set<BlockPos> positionsCopy = new LinkedHashSet<>(filteredInputStorages);
		for (BlockPos storagePos : positionsCopy) {
			if (!emptySlotsStorages.contains(storagePos)) {
				continue;
			}
			try (Transaction nestedTx = Transaction.open(tx)) {
				if (insertIntoStorage(storagePos, resource, 1, nestedTx) == 1) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isInsertBlocked() {
		return storagePositions.stream()
				.allMatch(pos -> getWrapperValueFromHolder(pos, storageWrapper -> storageWrapper.getInventoryHandler().isInsertBlocked()).orElse(true));
	}
}
