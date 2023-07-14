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
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public abstract class ControllerBlockEntityBase extends BlockEntity implements IItemHandlerModifiable {
	public static final int SEARCH_RANGE = 15;
	private List<BlockPos> storagePositions = new ArrayList<>();
	private List<Integer> baseIndexes = new ArrayList<>();
	private int totalSlots = 0;
	private final Map<ItemStackKey, Set<BlockPos>> stackStorages = new HashMap<>();
	private final Map<BlockPos, Set<ItemStackKey>> storageStacks = new HashMap<>();
	private final Map<Item, Set<ItemStackKey>> itemStackKeys = new HashMap<>();
	private final Set<BlockPos> emptySlotsStorages = new LinkedHashSet<>();

	private final Map<Item, Set<BlockPos>> memorizedItemStorages = new HashMap<>();
	private final Map<BlockPos, Set<Item>> storageMemorizedItems = new HashMap<>();
	private final Map<Integer, Set<BlockPos>> memorizedStackStorages = new HashMap<>();
	private final Map<BlockPos, Set<Integer>> storageMemorizedStacks = new HashMap<>();
	private final Map<Item, Set<BlockPos>> filterItemStorages = new HashMap<>();
	private final Map<BlockPos, Set<Item>> storageFilterItems = new HashMap<>();
	private Set<BlockPos> linkedBlocks = new LinkedHashSet<>();

	@Nullable
	private LazyOptional<IItemHandler> itemHandlerCap;

	public boolean addLinkedBlock(BlockPos linkedPos) {
		if (level != null && !level.isClientSide() && isWithinRange(linkedPos) && !linkedBlocks.contains(linkedPos) && !storagePositions.contains(linkedPos)) {

			linkedBlocks.add(linkedPos);
			setChanged();

			WorldHelper.getBlockEntity(level, linkedPos, ILinkable.class).ifPresent(l -> {
				if (l.connectLinkedSelf()) {
					Set<BlockPos> positionsToCheck = new LinkedHashSet<>();
					positionsToCheck.add(linkedPos);
					searchAndAddStorages(positionsToCheck, true);
				}

				searchAndAddStorages(new LinkedHashSet<>(l.getConnectablePositions()), false);
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
			storagePositions.forEach(this::addStorageStacksAndRegisterListeners);
		}
	}

	public void searchAndAddStorages() {
		Set<BlockPos> positionsToCheck = new HashSet<>();
		for (Direction dir : Direction.values()) {
			positionsToCheck.add(getBlockPos().offset(dir.getNormal()));
		}
		searchAndAddStorages(positionsToCheck, false);
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

	private void searchAndAddStorages(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf) {
		Set<BlockPos> positionsChecked = new HashSet<>();

		boolean first = true;
		while (!positionsToCheck.isEmpty()) {
			Iterator<BlockPos> it = positionsToCheck.iterator();
			BlockPos posToCheck = it.next();
			it.remove();

			final boolean finalFirst = first;
			WorldHelper.getLoadedBlockEntity(level, posToCheck, IControllableStorage.class).ifPresentOrElse(storage ->
							tryToConnectStorageAndAddPositionsToCheckAround(positionsToCheck, addingLinkedSelf, positionsChecked, posToCheck, finalFirst, storage),
					() -> positionsChecked.add(posToCheck)
			);
			first = false;
		}
	}

	private void tryToConnectStorageAndAddPositionsToCheckAround(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf, Set<BlockPos> positionsChecked, BlockPos posToCheck, boolean finalFirst, IControllableStorage storage) {
		if (storage.canBeConnected() || (addingLinkedSelf && finalFirst)) {
			if (storage instanceof ILinkable linkable && linkable.isLinked() && (!addingLinkedSelf || !finalFirst)) {
				linkedBlocks.remove(posToCheck);
				linkable.setNotLinked();
			} else {
				addStorageData(posToCheck);
			}
			if (storage.canConnectStorages()) {
				addUncheckedPositionsAround(positionsToCheck, positionsChecked, posToCheck);
			}
		}
	}

	private void addUncheckedPositionsAround(Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked, BlockPos currentPos) {
		for (Direction dir : Direction.values()) {
			BlockPos pos = currentPos.offset(dir.getNormal());
			if (!positionsChecked.contains(pos) && (!storagePositions.contains(pos) || linkedBlocks.contains(pos)) && isWithinRange(pos)) {
				positionsToCheck.add(pos);
			}
		}
	}

	private boolean isWithinRange(BlockPos pos) {
		return Math.abs(pos.getX() - getBlockPos().getX()) <= SEARCH_RANGE && Math.abs(pos.getY() - getBlockPos().getY()) <= SEARCH_RANGE && Math.abs(pos.getZ() - getBlockPos().getZ()) <= SEARCH_RANGE;
	}

	public void addStorage(BlockPos storagePos) {
		if (storagePositions.contains(storagePos)) {
			removeStorageInventoryData(storagePos);
		}

		if (isWithinRange(storagePos)) {
			HashSet<BlockPos> positionsToCheck = new LinkedHashSet<>();
			positionsToCheck.add(storagePos);
			searchAndAddStorages(positionsToCheck, false);
		}
	}

	private void addStorageData(BlockPos storagePos) {
		storagePositions.add(storagePos);
		totalSlots += getInventoryHandlerValueFromHolder(storagePos, IItemHandler::getSlots).orElse(0);
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

	private <T> Optional<T> getInventoryHandlerValueFromHolder(BlockPos storagePos, Function<IItemHandlerSimpleInserter, T> valueGetter) {
		return getWrapperValueFromHolder(storagePos, wrapper -> valueGetter.apply(wrapper.getInventoryForInputOutput()));
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
		return memorizedItemStorages.containsKey(stack.getItem()) || memorizedStackStorages.containsKey(ItemStackKey.getHashCode(stack));
	}

	protected boolean isFilterItem(Item item) {
		return filterItemStorages.containsKey(item);
	}

	public void removeStorage(BlockPos storagePos) {
		removeStorageInventoryDataAndUnregisterController(storagePos);
		verifyStoragesConnected();
	}

	private void removeStorageInventoryDataAndUnregisterController(BlockPos storagePos) {
		if (!storagePositions.contains(storagePos)) {
			return;
		}
		removeStorageInventoryData(storagePos);
		linkedBlocks.remove(storagePos);

		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController);

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
		storagePositions.remove(idx);
		removeBaseIndexAt(idx);
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

		Set<BlockPos> positionsToCheck = new HashSet<>();
		for (Direction dir : Direction.values()) {
			BlockPos offsetPos = getBlockPos().offset(dir.getNormal());
			if (toVerify.contains(offsetPos)) {
				positionsToCheck.add(offsetPos);
			}
		}
		Set<BlockPos> positionsChecked = new HashSet<>();

		verifyDirectlyConnected(toVerify, positionsToCheck, positionsChecked);

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

		verifyDirectlyConnected(toVerify, positionsToCheck, positionsChecked);

		toVerify.forEach(this::removeStorageInventoryDataAndUnregisterController);
	}

	private void verifyDirectlyConnected(HashSet<BlockPos> toVerify, Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked) {
		while (!positionsToCheck.isEmpty()) {
			Iterator<BlockPos> it = positionsToCheck.iterator();
			BlockPos posToCheck = it.next();
			it.remove();

			positionsChecked.add(posToCheck);
			WorldHelper.getLoadedBlockEntity(level, posToCheck, IControllableStorage.class).ifPresent(h -> {
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

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (itemHandlerCap == null) {
				itemHandlerCap = LazyOptional.of(() -> this);
			}
			return itemHandlerCap.cast();
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
		return getWrapperValueFromHolder(storagePositions.get(index), wrapper -> (IItemHandlerModifiable) wrapper.getInventoryForInputOutput()).orElse((IItemHandlerModifiable) EmptyHandler.INSTANCE);
	}

	protected int getSlotFromIndex(int slot, int index) {
		if (index <= 0 || index >= baseIndexes.size()) {
			return slot;
		}
		return slot - baseIndexes.get(index - 1);
	}

	@Nonnull
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
		if (handlerIndex < 0 || handlerIndex >= storagePositions.size()) {
			SophisticatedCore.LOGGER.debug("Invalid handler index calculated {} in controller's {} method. If you see many of these messages try replacing controller at {}", () -> handlerIndex, () -> methodName, () -> getBlockPos().toShortString());
		} else {
			SophisticatedCore.LOGGER.debug("Invalid slot {} passed into controller's {} method for storage at {}. If you see many of these messages try replacing controller at {}", () -> slot, () -> methodName, () -> storagePositions.get(handlerIndex).toShortString(), () -> getBlockPos().toShortString());
		}

		return false;
	}

	@Nonnull
	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (isSlotIndexInvalid(slot)) {
			return stack;
		}

		return insertItem(stack, simulate, true);
	}

	protected ItemStack insertItem(ItemStack stack, boolean simulate, boolean insertIntoAnyEmpty) {
		ItemStackKey stackKey = new ItemStackKey(stack);
		ItemStack remaining = stack;

		if (stackStorages.containsKey(stackKey)) {
			Set<BlockPos> positions = stackStorages.get(stackKey);
			remaining = insertIntoStorages(positions, remaining, simulate);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		if (itemStackKeys.containsKey(stack.getItem())) {
			for (ItemStackKey key : itemStackKeys.get(stack.getItem())) {
				if (stackStorages.containsKey(key)) {
					Set<BlockPos> positions = stackStorages.get(key);
					remaining = insertIntoStorages(positions, remaining, simulate);
					if (remaining.isEmpty()) {
						break;
					}
				}
			}
		}

		if (memorizedItemStorages.containsKey(stack.getItem())) {
			remaining = insertIntoStorages(memorizedItemStorages.get(stack.getItem()), remaining, simulate);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}
		int stackHash = ItemStackKey.getHashCode(stack);
		if (memorizedStackStorages.containsKey(stackHash)) {
			remaining = insertIntoStorages(memorizedStackStorages.get(stackHash), remaining, simulate);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		if (filterItemStorages.containsKey(stack.getItem())) {
			remaining = insertIntoStorages(filterItemStorages.get(stack.getItem()), remaining, simulate);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		return insertIntoAnyEmpty ? insertIntoStorages(emptySlotsStorages, remaining, simulate) : remaining;
	}

	private ItemStack insertIntoStorages(Set<BlockPos> positions, ItemStack stack, boolean simulate) {
		ItemStack remaining = stack;
		Set<BlockPos> positionsCopy = new HashSet<>(positions); //to prevent CME if stack insertion actually causes set of positions to change
		for (BlockPos storagePos : positionsCopy) {
			ItemStack finalRemaining = remaining;
			remaining = getInventoryHandlerValueFromHolder(storagePos, ins -> ins.insertItem(finalRemaining, simulate)).orElse(remaining);
			if (remaining.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	@Nonnull
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
		new HashSet<>(linkedBlocks).forEach(linkedPos -> WorldHelper.getLoadedBlockEntity(level, linkedPos, ILinkable.class).ifPresent(ILinkable::unlinkFromController)); //copying into new hashset to prevent CME when these are removed
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);

		saveData(tag);
	}

	private CompoundTag saveData(CompoundTag tag) {
		NBTHelper.putList(tag, "storagePositions", storagePositions, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "linkedBlocks", linkedBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "baseIndexes", baseIndexes, IntTag::valueOf);
		tag.putInt("totalSlots", totalSlots);

		return tag;
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);

		storagePositions = NBTHelper.getCollection(tag, "storagePositions", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), ArrayList::new).orElseGet(ArrayList::new);
		baseIndexes = NBTHelper.getCollection(tag, "baseIndexes", Tag.TAG_INT, t -> Optional.of(((IntTag) t).getAsInt()), ArrayList::new).orElseGet(ArrayList::new);
		totalSlots = tag.getInt("totalSlots");
		linkedBlocks = NBTHelper.getCollection(tag, "linkedBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new).orElseGet(LinkedHashSet::new);
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
}
