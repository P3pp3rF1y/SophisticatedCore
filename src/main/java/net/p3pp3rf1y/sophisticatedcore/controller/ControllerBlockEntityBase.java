package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
	private static final int SEARCH_RANGE = 15;
	private List<BlockPos> storagePositions = new ArrayList<>();
	private List<Integer> baseIndexes = new ArrayList<>();
	private int totalSlots = 0;
	private final Map<ItemStackKey, Set<BlockPos>> stackStorages = new HashMap<>();
	private final Map<BlockPos, Set<ItemStackKey>> storageStacks = new HashMap<>();
	private final Set<BlockPos> emptySlotsStorages = new LinkedHashSet<>();

	private final Map<Item, Set<BlockPos>> memorizedItemStorages = new HashMap<>();
	private final Map<BlockPos, Set<Item>> storageMemorizedItems = new HashMap<>();

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide()) {
			stackStorages.clear();
			storageStacks.clear();
			emptySlotsStorages.clear();
			storagePositions.forEach(this::addStorageStacksAndRegisterListeners);
		}
	}

	public void searchAndAddStorages() {
		Set<BlockPos> positionsToCheck = new HashSet<>();
		for (Direction dir : Direction.values()) {
			positionsToCheck.add(getBlockPos().offset(dir.getNormal()));
		}
		searchAndAddStorages(positionsToCheck);
	}

	public void changeSlots(BlockPos storagePos, int newSlots, boolean hasEmptySlots) {
		updateBaseIndexesAndTotalSlots(storagePos, newSlots);
		updateEmptySlots(storagePos, hasEmptySlots);
	}

	private void updateEmptySlots(BlockPos storagePos, boolean hasEmptySlots) {
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
	}

	private int getStorageSlots(int index) {
		int previousBaseIndex = index == 0 ? 0 : baseIndexes.get(index - 1);
		return baseIndexes.get(index) - previousBaseIndex;
	}

	private void searchAndAddStorages(Set<BlockPos> positionsToCheck) {
		Set<BlockPos> positionsChecked = new HashSet<>();

		while (!positionsToCheck.isEmpty()) {
			Iterator<BlockPos> it = positionsToCheck.iterator();
			BlockPos posToCheck = it.next();
			it.remove();

			WorldHelper.getLoadedBlockEntity(level, posToCheck, IControllableStorage.class).ifPresentOrElse(storage -> {
				if (storage.canBeConnected()) {
					addStorageData(posToCheck);
				}
				if (storage.canConnectStorages()) {
					addUncheckedPositionsAround(positionsToCheck, positionsChecked, posToCheck);
				}
			}, () -> positionsChecked.add(posToCheck));
		}
	}

	private void addUncheckedPositionsAround(Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked, BlockPos currentPos) {
		for (Direction dir : Direction.values()) {
			BlockPos pos = currentPos.offset(dir.getNormal());
			if (!positionsChecked.contains(pos) && !storagePositions.contains(pos) && isWithinRange(pos)) {
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
			HashSet<BlockPos> positionsToCheck = new HashSet<>();
			positionsToCheck.add(storagePos);
			searchAndAddStorages(positionsToCheck);
		}
	}

	private void addStorageData(BlockPos storagePos) {
		storagePositions.add(storagePos);
		totalSlots += getInventoryHandlerValueFromHolder(storagePos, IItemHandler::getSlots).orElse(0);
		baseIndexes.add(totalSlots);
		setChanged();

		addStorageStacksAndRegisterListeners(storagePos);
	}

	public void addStorageStacksAndRegisterListeners(BlockPos storagePos) {
		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(storage -> {
			ITrackedContentsItemHandler handler = storage.getStorageWrapper().getInventoryForInputOutput();
			handler.getTrackedStacks().forEach(k -> addStorageStack(storagePos, k));
			if (handler.hasEmptySlots()) {
				emptySlotsStorages.add(storagePos);
			}
			storage.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getFilterItemSlots().keySet().forEach(i -> addStorageMemorizedItem(storagePos, i));
			storage.registerController(this);
		});
	}

	public void addStorageMemorizedItem(BlockPos storagePos, Item item) {
		memorizedItemStorages.computeIfAbsent(item, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageMemorizedItems.computeIfAbsent(storagePos, pos -> new HashSet<>()).add(item);
	}

	public void removeStorageMemorizedItem(BlockPos storagePos, Item item) {
		memorizedItemStorages.computeIfPresent(item, (i, storagePositions) -> {
			storagePositions.remove(storagePos);
			return storagePositions;
		});
		if (memorizedItemStorages.containsKey(item) && memorizedItemStorages.get(item).isEmpty()) {
			memorizedItemStorages.remove(item);
		}
		storageMemorizedItems.remove(storagePos);
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
	}

	public void removeStorageStack(BlockPos storagePos, ItemStackKey stackKey) {
		stackStorages.computeIfPresent(stackKey, (sk, storagePositions) -> {
			storagePositions.remove(storagePos);
			return storagePositions;
		});
		if (stackStorages.containsKey(stackKey) && stackStorages.get(stackKey).isEmpty()) {
			stackStorages.remove(stackKey);
		}
		storageStacks.remove(storagePos);
	}

	public void removeStorageStacks(BlockPos storagePos) {
		storageStacks.computeIfPresent(storagePos, (pos, stackKeys) -> {
			stackKeys.forEach(stackKey -> {
				Set<BlockPos> storages = stackStorages.get(stackKey);
				if (storages != null) {
					storages.remove(storagePos);
					if (storages.isEmpty()) {
						stackStorages.remove(stackKey);
					}
				}
			});
			return stackKeys;
		});
		storageStacks.remove(storagePos);
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
		setChanged();

		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController);
	}

	private void removeStorageInventoryData(BlockPos storagePos) {
		int idx = storagePositions.indexOf(storagePos);
		totalSlots -= getStorageSlots(idx);
		removeStorageStacks(storagePos);
		removeStorageMemorizedItems(storagePos);
		removeStorageWithEmptySlots(storagePos);
		storagePositions.remove(idx);
		removeBaseIndexAt(idx);
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

		toVerify.forEach(this::removeStorageInventoryDataAndUnregisterController);
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

	public ControllerBlockEntityBase(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
		super(blockEntityType, pos, state);
	}

	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return LazyOptional.of(() -> this).cast();
		}

		return super.getCapability(cap, side);
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

	@NotNull
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
		if  (handlerIndex < 0 || handlerIndex >= storagePositions.size()) {
			SophisticatedCore.LOGGER.debug("Invalid handler index calculated {} in controller's {} method. If you see many of these messages try replacing controller at {}", handlerIndex, methodName, getBlockPos().toShortString());
		} else {
			SophisticatedCore.LOGGER.debug("Invalid slot {} passed into controller's {} method for storage at {}. If you see many of these messages try replacing controller at {}", slot, methodName, storagePositions.get(handlerIndex).toShortString(), getBlockPos().toShortString());
		}

		return false;
	}

	@NotNull
	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		if (isSlotIndexInvalid(slot)) {
			return stack;
		}

		ItemStackKey stackKey = new ItemStackKey(stack);
		ItemStack remaining = stack;

		if (stackStorages.containsKey(stackKey)) {
			Set<BlockPos> storagePositions = stackStorages.get(stackKey);
			remaining = insertIntoStorages(storagePositions, remaining, simulate);
		}

		if (memorizedItemStorages.containsKey(stack.getItem())) {
			remaining = insertIntoStorages(memorizedItemStorages.get(stack.getItem()), remaining, simulate);
		}

		return insertIntoStorages(emptySlotsStorages, remaining, simulate);
	}

	private ItemStack insertIntoStorages(Set<BlockPos> positions, ItemStack stack, boolean simulate) {
		ItemStack remaining = stack;
		for (BlockPos storagePos : positions) {
			ItemStack finalRemaining = remaining;
			remaining = getInventoryHandlerValueFromHolder(storagePos, ins -> ins.insertItem(finalRemaining, simulate)).orElse(remaining);
			if (remaining.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	@NotNull
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
		detachFromStorages();
	}

	public void detachFromStorages() {
		storagePositions.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController));
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);

		NBTHelper.putList(tag, "storagePositions", storagePositions, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "baseIndexes", baseIndexes, IntTag::valueOf);
		tag.putInt("totalSlots", totalSlots);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);

		storagePositions = NBTHelper.getCollection(tag, "storagePositions", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), ArrayList::new).orElse(new ArrayList<>());
		baseIndexes = NBTHelper.getCollection(tag, "baseIndexes", Tag.TAG_INT, t -> Optional.of(((IntTag) t).getAsInt()), ArrayList::new).orElse(new ArrayList<>());
		totalSlots = tag.getInt("totalSlots");
	}

	public void addStorageWithEmptySlots(BlockPos storageBlockPos) {
		emptySlotsStorages.add(storageBlockPos);
	}

	public void removeStorageWithEmptySlots(BlockPos storageBlockPos) {
		emptySlotsStorages.remove(storageBlockPos);
	}
}
