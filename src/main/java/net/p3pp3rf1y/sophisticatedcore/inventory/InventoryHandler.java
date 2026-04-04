package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IExtractResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public abstract class InventoryHandler extends ItemStackHandler implements ITrackedContentsItemHandler, IInsertBlockOverride {
	public static final String INVENTORY_TAG = "inventory";
	private static final String PARTITIONER_TAG = "partitioner";
	protected final IStorageWrapper storageWrapper;
	private final CompoundTag contentsNbt;
	private final Runnable saveHandler;
	private final List<IntConsumer> onContentsChangedListeners = new ArrayList<>();
	private boolean persistent = true;
	private final Map<Integer, Tag> stackNbts = new LinkedHashMap<>();

	private ISlotTracker slotTracker = new ISlotTracker.Noop();

	private int baseSlotLimit;
	private double maxStackSizeMultiplier;
	private boolean isInitializing;
	private final StackUpgradeConfig stackUpgradeConfig;
	private final InventoryPartitioner inventoryPartitioner;
	private Consumer<Set<Item>> filterItemsChangeListener = s -> {
	};
	private final SlotValueMap<Item> filterItemSlots = new SlotValueMap<>();
	private BooleanSupplier shouldInsertIntoEmpty = () -> true;
	private boolean voidUpgradeInfoInitialized = false;
	private boolean hasVoidUpgrade = false;

	protected InventoryHandler(int numberOfInventorySlots, IStorageWrapper storageWrapper, CompoundTag contentsNbt, Runnable saveHandler, int baseSlotLimit, StackUpgradeConfig stackUpgradeConfig) {
		super(numberOfInventorySlots);
		this.stackUpgradeConfig = stackUpgradeConfig;
		isInitializing = true;
		this.storageWrapper = storageWrapper;
		this.contentsNbt = contentsNbt;
		this.saveHandler = saveHandler;
		setBaseSlotLimit(baseSlotLimit);
		RegistryHelper.getRegistryAccess().ifPresent(registryAccess -> deserializeNBT(registryAccess, contentsNbt.getCompound(INVENTORY_TAG)));
		inventoryPartitioner = new InventoryPartitioner(contentsNbt.getCompound(PARTITIONER_TAG), this, () -> storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class));
		initStackNbts();
		getSlotTracker().refreshSlotIndexesFrom(this);

		isInitializing = false;
	}

	public ISlotTracker getSlotTracker() {
		initSlotTracker();
		return slotTracker;
	}

	@Override
	public void setSize(int size) {
		super.setSize(stacks.size());
	}

	private void initStackNbts() {
		stackNbts.clear();
		for (int slot = 0; slot < stacks.size(); slot++) {
			ItemStack slotStack = stacks.get(slot);
			if (!slotStack.isEmpty()) {
				stackNbts.put(slot, getSlotsStackNbt(slot, slotStack));
			}
		}
	}

	@Override
	public void onContentsChanged(int slot) {
		super.onContentsChanged(slot);
		inventoryPartitioner.getPartBySlot(slot).onContentsChanged(slot, super::setStackInSlot);
		if (persistent && updateSlotNbt(slot)) {
			saveInventory();
			triggerOnChangeListeners(slot);
		}
	}

	public void triggerOnChangeListeners(int slot) {
		for (IntConsumer onContentsChangedListener : onContentsChangedListeners) {
			onContentsChangedListener.accept(slot);
		}
	}

	@SuppressWarnings("java:S3824")
	//compute use here would be difficult as then there's no way of telling that value was newly created vs different from the one that needs to be set
	private boolean updateSlotNbt(int slot) {
		ItemStack slotStack = getSlotStack(slot);
		if (slotStack.isEmpty()) {
			if (stackNbts.containsKey(slot)) {
				stackNbts.remove(slot);
				return true;
			}
		} else {
			Tag itemTag = getSlotsStackNbt(slot, slotStack);
			if (!stackNbts.containsKey(slot) || !stackNbts.get(slot).equals(itemTag)) {
				stackNbts.put(slot, itemTag);
				return true;
			}
		}
		return false;
	}

	private Tag getSlotsStackNbt(int slot, ItemStack slotStack) {
		CompoundTag itemTag = new CompoundTag();
		itemTag.putInt("Slot", slot);
		return RegistryHelper.getRegistryAccess().map(registryAccess -> CodecHelper.OVERSIZED_ITEM_STACK_CODEC.encode(slotStack, registryAccess.createSerializationContext(NbtOps.INSTANCE), itemTag).getOrThrow()).orElse(itemTag);
	}

	private Optional<ItemStack> getStackFromNbt(int slot, Tag itemTag, RegistryAccess registryAccess) {
		try {
			return CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), itemTag)
					.resultOrPartial(errorMessage -> SophisticatedCore.LOGGER.error(
							"Failed to deserialize stored item in storage '{}' slot {} - {}. Raw item data: {}",
							getStorageLogName(), slot, errorMessage, itemTag
					));
		} catch (Exception e) {
			SophisticatedCore.LOGGER.error(
					"Error deserializing stored item in storage '{}' slot {}. Raw item data: {}",
					getStorageLogName(), slot, itemTag, e
			);
			return Optional.empty();
		}
	}

	private String getStorageLogName() {
		String displayName = Optional.ofNullable(storageWrapper.getDisplayName()).map(Component::getString).orElse("");
		if (!displayName.isEmpty()) {
			return storageWrapper.getStorageType() + " (" + displayName + ")";
		}
		return storageWrapper.getStorageType();
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {
		slotTracker.clear();
		setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.size());
		ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
		RegistryHelper.getRegistryAccess().ifPresent(registryAccess -> {
			for (int i = 0; i < tagList.size(); i++) {
				CompoundTag itemTag = tagList.getCompound(i);
				int slot = itemTag.getInt("Slot");
				if (slot >= 0 && slot < stacks.size()) {
					getStackFromNbt(slot, itemTag, registryAccess).ifPresent(stack -> stacks.set(slot, stack));
				}
			}
		});
		slotTracker.refreshSlotIndexesFrom(this);
		onLoad();
	}

	public int getBaseSlotLimit() {
		return baseSlotLimit;
	}

	@Override
	public int getSlotLimit(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).getSlotLimit(slot);
	}

	public int getBaseStackLimit(ItemStack stack) {
		if (!stackUpgradeConfig.canStackItem(stack.getItem())) {
			return stack.getMaxStackSize();
		}
		int maxStackSize = stack.isEmpty() ? getBaseSlotLimit() : stack.getMaxStackSize();

		if (baseSlotLimit < 64) {
			return (int) Math.max(1, (double) maxStackSize * baseSlotLimit / 64);
		}

		int limit = MathHelper.intMaxCappedMultiply(maxStackSize, baseSlotLimit / 64);
		int remainder = baseSlotLimit % 64;
		if (remainder > 0) {
			limit = MathHelper.intMaxCappedAddition(limit, remainder * maxStackSize / 64);
		}
		return limit;
	}

	@Override
	public int getStackLimit(int slot, ItemStack stack) {
		return inventoryPartitioner.getPartBySlot(slot).getStackLimit(slot, stack);
	}

	public Item getFilterItem(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).getFilterItem(slot);
	}

	public boolean isFilterItem(Item item) {
		return inventoryPartitioner.isFilterItem(item);
	}

	public void setBaseSlotLimit(int baseSlotLimit) {
		voidUpgradeInfoInitialized = false; // not the most ideal of places to do this, but base slot limit is set when upgrades change and that's when slot limit needs to be reinitialized as well
		this.baseSlotLimit = baseSlotLimit;
		maxStackSizeMultiplier = baseSlotLimit / 64f;

		if (inventoryPartitioner != null) {
			inventoryPartitioner.onSlotLimitChange();
		}

		if (!isInitializing) {
			slotTracker.refreshSlotIndexesFrom(this);
		}
	}

	public ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
		if (amount == 0) {
			return ItemStack.EMPTY;
		}

		validateSlotIndex(slot);
		ItemStack existing = getSlotStack(slot);

		if (existing.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int toExtract = Math.min(amount, existing.getMaxStackSize());

		if (existing.getCount() <= toExtract) {
			if (!simulate) {
				setSlotStack(slot, ItemStack.EMPTY);
				runOnAfterExtract(slot, this, existing);
				return existing;
			} else {
				return existing.copy();
			}
		} else {
			if (!simulate) {
				setSlotStack(slot, existing.copyWithCount(existing.getCount() - toExtract));
				runOnAfterExtract(slot, this, existing);
			}

			return existing.copyWithCount(toExtract);
		}
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return inventoryPartitioner.getPartBySlot(slot).extractItem(slot, amount, simulate);
	}

	@Override
	public void validateSlotIndex(int slot) {
		super.validateSlotIndex(slot);
	}

	public ItemStack getSlotStack(int slot) {
		return stacks.get(slot);
	}

	public void setSlotStack(int slot, ItemStack stack) {
		stacks.set(slot, stack);
		getSlotTracker().removeAndSetSlotIndexes(this, slot, stack);
		onContentsChanged(slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		return getSlotTracker().insertItemIntoHandler(this, this::runOnBeforeInsert, this::insertItemInternal, this::triggerSlotOverflowUpgrades, this::triggerStorageOverflowUpgrades, slot, stack, simulate);
	}

	public ItemStack insertItemOnlyToSlot(int slot, ItemStack stack, boolean simulate) {
		initSlotTracker();
		if (ItemStack.isSameItemSameComponents(getStackInSlot(slot), stack)) {
			return triggerSlotOverflowUpgrades(insertItemInternal(slot, stack, simulate));
		}

		return insertItemInternal(slot, stack, simulate);
	}

	private void initSlotTracker() {
		if (!(slotTracker instanceof InventoryHandlerSlotTracker)) {
			slotTracker = new InventoryHandlerSlotTracker(storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class), filterItemSlots);
			slotTracker.refreshSlotIndexesFrom(this);
			slotTracker.setShouldInsertIntoEmpty(shouldInsertIntoEmpty);
		}
	}

	private ItemStack insertItemInternal(int slot, ItemStack stack, boolean simulate) {
		ItemStack ret = runOnBeforeInsert(slot, stack, simulate, this, storageWrapper);
		if (ret.isEmpty()) {
			return ret;
		}

		ret = inventoryPartitioner.getPartBySlot(slot).insertItem(slot, ret, simulate, super::insertItem);

		if (!simulate) {
			getSlotTracker().removeAndSetSlotIndexes(this, slot, getStackInSlot(slot));
		}

		if (ret == stack) {
			return ret;
		}

		runOnAfterInsert(slot, simulate, this, storageWrapper);

		return ret;
	}

	private ItemStack triggerSlotOverflowUpgrades(ItemStack ret) {
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplement(IOverflowResponseUpgrade.class)) {
			ret = overflowUpgrade.onSlotOverflow(ret);
			if (ret.isEmpty()) {
				break;
			}
		}
		return ret;
	}

	private ItemStack triggerStorageOverflowUpgrades(ItemStack ret) {
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)) {
			ret = overflowUpgrade.onStorageOverflow(ret);
			if (ret.isEmpty()) {
				break;
			}
		}
		return ret;
	}

	private void runOnAfterInsert(int slot, boolean simulate, IItemHandlerSimpleInserter handler, IStorageWrapper storageWrapper) {
		if (!simulate) {
			storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class).forEach(u -> u.onAfterInsert(handler, slot));
		}
	}

	private ItemStack runOnBeforeInsert(ItemStack stack, boolean simulate) {
		List<IInsertResponseUpgrade> wrappers = storageWrapper.getUpgradeHandler().getWrappersThatImplement(IInsertResponseUpgrade.class);
		ItemStack remaining = stack;
		for (IInsertResponseUpgrade upgrade : wrappers) {
			remaining = upgrade.onBeforeInsert(storageWrapper.getInventoryHandler(), remaining, simulate);
			if (remaining.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	private ItemStack runOnBeforeInsert(int slot, ItemStack stack, boolean simulate, InventoryHandler handler, IStorageWrapper storageWrapper) {
		List<IInsertResponseUpgrade> wrappers = storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class);
		ItemStack remaining = stack;
		for (IInsertResponseUpgrade upgrade : wrappers) {
			remaining = upgrade.onBeforeInsert(handler, slot, remaining, simulate);
			if (remaining.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	private void runOnAfterExtract(int slot, IItemHandlerSimpleInserter handler, ItemStack originalContents) {
		List<IExtractResponseUpgrade> wrappers = storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IExtractResponseUpgrade.class);
		for (IExtractResponseUpgrade upgrade : wrappers) {
			upgrade.onAfterExtract(handler, slot, originalContents);
		}
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		inventoryPartitioner.getPartBySlot(slot).setStackInSlot(slot, stack, super::setStackInSlot);
		getSlotTracker().removeAndSetSlotIndexes(this, slot, stack);
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public boolean isItemValid(int slot, ItemStack stack, @Nullable Player player) {
		return inventoryPartitioner.getPartBySlot(slot).isItemValid(slot, stack, player, super::isItemValid)
				&& isAllowed(stack) && storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(slot, stack);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return isItemValid(slot, stack, null);
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).getStackInSlot(slot, super::getStackInSlot);
	}

	protected abstract boolean isAllowed(ItemStack stack);

	public void saveInventory() {
		RegistryHelper.getRegistryAccess().ifPresent(registryAccess -> contentsNbt.put(INVENTORY_TAG, serializeNBT(registryAccess)));
		if (inventoryPartitioner != null) {
			//inventory parts may affect inventory slots during their initialization in Inventory Partitioner deserialize,
			// but there's no reason to serialize partitioner at that point as its nbt can't during init/deserialization.
			contentsNbt.put(PARTITIONER_TAG, inventoryPartitioner.serializeNBT());
		}
		saveHandler.run();
	}

	@Nullable
	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon(int slotIndex) {
		return inventoryPartitioner.getNoItemIcon(slotIndex);
	}

	public void copyStacksTo(InventoryHandler otherHandler) {
		InventoryHelper.copyTo(this, otherHandler);
	}

	public void addListener(IntConsumer onContentsChanged) {
		onContentsChangedListeners.add(onContentsChanged);
	}

	public void clearListeners() {
		onContentsChangedListeners.clear();
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider registries) {
		ListTag nbtTagList = new ListTag();
		nbtTagList.addAll(stackNbts.values());
		CompoundTag nbt = new CompoundTag();
		nbt.put("Items", nbtTagList);
		nbt.putInt("Size", getSlots());
		return nbt;
	}

	public double getStackSizeMultiplier() {
		return maxStackSizeMultiplier;
	}

	@Override
	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		return getSlotTracker().insertItemIntoHandler(this, this::runOnBeforeInsert, this::insertItemInternal, this::triggerSlotOverflowUpgrades, this::triggerStorageOverflowUpgrades , stack, simulate);
	}

	@Override
	public ItemStack extractItem(ItemStack stack, boolean simulate) {
		return getSlotTracker().extractItemFromHandler(this, this::extractItemInternal, stack, simulate);
	}

	public void changeSlots(int diff) {
		NonNullList<ItemStack> previousStacks = stacks;
		stacks = NonNullList.withSize(previousStacks.size() + diff, ItemStack.EMPTY);
		for (int slot = 0; slot < previousStacks.size() && slot < stacks.size(); slot++) {
			stacks.set(slot, previousStacks.get(slot));
		}
		initStackNbts();
		saveInventory();
		getSlotTracker().refreshSlotIndexesFrom(this);
	}

	@Override
	public Set<ItemStackKey> getTrackedStacks() {
		initSlotTracker();
		HashSet<ItemStackKey> ret = new HashSet<>(slotTracker.getFullStacks());
		ret.addAll(slotTracker.getPartialStacks());
		return ret;
	}

	@Override
	public void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
		getSlotTracker().registerListeners(onAddStackKey, onRemoveStackKey, onAddFirstEmptySlot, onRemoveLastEmptySlot);
	}

	@Override
	public void unregisterStackKeyListeners() {
		slotTracker.unregisterStackKeyListeners();
	}

	@Override
	public boolean hasEmptySlots() {
		return slotTracker.hasEmptySlots();
	}

	public InventoryPartitioner getInventoryPartitioner() {
		return inventoryPartitioner;
	}

	public boolean isSlotAccessible(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).isSlotAccessible(slot);
	}

	public Set<Integer> getNoSortSlots() {
		return inventoryPartitioner.getNoSortSlots();
	}

	public void onSlotFilterChanged(int slot) {
		inventoryPartitioner.getPartBySlot(slot).onSlotFilterChanged(slot);
	}

	public void registerFilterItemsChangeListener(Consumer<Set<Item>> listener) {
		filterItemsChangeListener = listener;
	}

	public void unregisterFilterItemsChangeListener() {
		filterItemsChangeListener = s -> {
		};
	}

	public void initFilterItems() {
		inventoryPartitioner.getFilterItems().forEach((item, slots) -> slots.forEach(slot -> filterItemSlots.add(slot, item)));
	}

	public void onFilterItemsChanged() {
		slotTracker.refreshSlotIndexesFrom(this);
		if (inventoryPartitioner == null) {
			return;
		}
		filterItemSlots.clear();
		inventoryPartitioner.getFilterItems().forEach((item, slots) -> slots.forEach(slot -> filterItemSlots.add(slot, item)));

		filterItemsChangeListener.accept(filterItemSlots.keySet());
	}

	public Set<Item> getFilterItems() {
		return filterItemSlots.keySet();
	}

	public void onInit() {
		if (inventoryPartitioner == null) {
			return;
		}
		inventoryPartitioner.onInit();
		slotTracker = new ISlotTracker.Noop();
	}

	public void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty) {
		this.shouldInsertIntoEmpty = shouldInsertIntoEmpty;
		getSlotTracker().setShouldInsertIntoEmpty(shouldInsertIntoEmpty);
	}

	public boolean isInfinite(int slot) {
		return inventoryPartitioner.isInfinite(slot);
	}

	private boolean hasVoidUpgrade() {
		if (!voidUpgradeInfoInitialized) {
			hasVoidUpgrade = !storageWrapper.getUpgradeHandler().getTypeWrappers(VoidUpgradeItem.TYPE).isEmpty();
			voidUpgradeInfoInitialized = true;
		}
		return hasVoidUpgrade;
	}

	@Override
	public boolean isInsertBlocked() {
		if (hasVoidUpgrade()) {
			return false;
		}

		for (int slot = 0; slot < stacks.size(); ++slot) {
			ItemStack stack = stacks.get(slot);
			if (stack.getCount() < getStackLimit(slot, stack)) {
				return false;
			}
		}
		return true;
	}
}
