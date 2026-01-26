package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IExtractResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.MathHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotValueMap;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public abstract class InventoryHandler extends ItemStacksResourceHandler implements ITrackedContentsItemResourceHandler, IndexModifier<ItemResource>, IInsertBlockOverride {
	protected final IStorageWrapper storageWrapper;
	private final ContainerContents.InventoryData inventoryData;
	private final Runnable saveHandler;
	private final List<IntConsumer> onContentsChangedListeners = new ArrayList<>();
	private boolean persistent = true;

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
	private final SlotTrackerJournal slotTrackerJournal = new SlotTrackerJournal();

	protected InventoryHandler(int numberOfInventorySlots, IStorageWrapper storageWrapper, ContainerContents containerContents, Runnable saveHandler, int baseSlotLimit, StackUpgradeConfig stackUpgradeConfig) {
		super(numberOfInventorySlots);
		this.stackUpgradeConfig = stackUpgradeConfig;
		isInitializing = true;
		this.storageWrapper = storageWrapper;
		this.inventoryData = containerContents.inventory();
		this.saveHandler = saveHandler;
		setBaseSlotLimit(baseSlotLimit);
		loadStacksFromData();
		inventoryPartitioner = new InventoryPartitioner(containerContents.partitioner(), this, () -> storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class));
		getSlotTracker().refreshSlotIndexesFrom(this);

		isInitializing = false;
	}

	protected final void directSet(int index, ItemResource resource, int amount) {
		if (amount == 0) {
			super.set(index, ItemResource.EMPTY, 0);
			return;
		}
		super.set(index, resource, amount);
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		inventoryPartitioner.getPartBySlot(index).set(index, resource, amount, this::directSet);
	}

	public ISlotTracker getSlotTracker() {
		initSlotTracker();
		return slotTracker;
	}

	@Override
	protected void onContentsChanged(int index, ItemStack previousContents) {
		super.onContentsChanged(index, previousContents);

		ItemStack current = getInternalStack(index);
		getSlotTracker().removeAndSetSlotIndexes(this, index, current);

		if (persistent && updateSlotStack(index)) {
			saveInventory();
			triggerOnChangeListeners(index);
		}
	}

	private void runOnAfterInsert(int index, TransactionContext tx) {
		storageWrapper.getUpgradeHandler()
				.getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class)
				.forEach(u -> u.onAfterInsert(this, index, tx));
	}

	public void triggerOnChangeListeners(int slot) {
		for (IntConsumer onContentsChangedListener : onContentsChangedListeners) {
			onContentsChangedListener.accept(slot);
		}
	}

	@SuppressWarnings("java:S3824")
	//compute use here would be difficult as then there's no way of telling that value was newly created vs different from the one that needs to be set
	private boolean updateSlotStack(int slot) {
		ItemStack slotStack = getInternalStack(slot);
		if (inventoryData.stacks().size() > slot && (!ItemStack.isSameItemSameComponents(inventoryData.stacks().get(slot), slotStack) || inventoryData.stacks().get(slot).getCount() != slotStack.getCount())) {
			inventoryData.stacks().set(slot, slotStack.copy());
			return true;
		}
		return false;
	}

	private void loadStacksFromData() {
		slotTracker.clear();
		if (inventoryData.stacks().size() < stacks.size()) {
			inventoryData.resize(stacks.size());
		}

		for (int slot = 0; slot < stacks.size() && slot < inventoryData.stacks().size(); slot++) {
			ItemStack stack = inventoryData.stacks().get(slot);
			stacks.set(slot, stack.copy());
		}
		slotTracker.refreshSlotIndexesFrom(this);
	}

	public int getBaseSlotLimit() {
		return baseSlotLimit;
	}

	@Override
	public int getInternalSlotLimit(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).getSlotLimit(slot);
	}

	protected int getCapacityNoInit(int index, ItemResource resource) {
		return inventoryPartitioner.getPartBySlot(index).getCapacity(index, resource);
	}

	@Override
	protected int getCapacity(int index, ItemResource resource) {
		return inventoryPartitioner.getPartBySlot(index).getCapacity(index, resource);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		Objects.checkIndex(index, this.size());
		return getCapacity(index, resource);
	}

	public int getBaseCapacity(ItemResource resource) {
		if (!stackUpgradeConfig.canStackItem(resource.getItem())) {
			return resource.getMaxStackSize();
		}
		int maxStackSize = resource.isEmpty() ? getBaseSlotLimit() : resource.getMaxStackSize();

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

	@Override
	public int extract(ItemResource resource, int amount, TransactionContext transaction) {
		ISlotTracker tracker = getSlotTracker();
		int extracted = 0;

		ItemStackKey stackKey = ItemStackKey.of(resource);
		int originalSize = tracker.getFullSlots(stackKey).size();
		int i = 0;

		while (extracted < amount && i++ < originalSize) {
			int slot = tracker.getFullSlots(stackKey).iterator().next();
			extracted += extract(slot, resource, amount - extracted, transaction);
		}

		if (extracted >= amount) {
			return extracted;
		}

		originalSize = tracker.getPartialSlots(stackKey).size();
		i = 0;

		while (extracted < amount && i++ < originalSize) {
			int slot = tracker.getPartialSlots(stackKey).iterator().next();
			extracted += extract(slot, resource, amount - extracted, transaction);
		}

		return extracted;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		int result = inventoryPartitioner.getPartBySlot(index).extract(index, resource, amount, transaction, super::extract);
		if (result > 0) {
			slotTrackerJournal.updateSnapshots(transaction);
			getSlotTracker().removeAndSetSlotIndexes(this, index, getStackInSlot(index));

			runOnAfterExtract(index, resource);
		}
		return result;
	}

	@Override
	public ItemResource getResource(int index) {
		return inventoryPartitioner.getPartBySlot(index).getResource(index, super::getResource);
	}

	@Override
	public long getAmountAsLong(int index) {
		return inventoryPartitioner.getPartBySlot(index).getAmountAsLong(index, super::getAmountAsLong);
	}

	public ItemStack getInternalStack(int slot) {
		return stacks.get(slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		inventoryPartitioner.getPartBySlot(slot).setStackInSlot(slot, stack, this::setStackInSlotInternal);
	}

	public void setStackInSlotInternal(int slot, ItemStack stack) {
		ItemStack previousContents = stacks.get(slot);
		stacks.set(slot, stack);
		getSlotTracker().removeAndSetSlotIndexes(this, slot, stack);
		onContentsChanged(slot, previousContents);
	}

	@Override
	public int insert(ItemResource resource, int amount, TransactionContext tx) {
		ISlotTracker tracker = getSlotTracker();
		MemorySettingsCategory mem = storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
		Item item = resource.getItem();
		ItemStackKey key = ItemStackKey.of(resource);
		int moved = 0;

		moved += runOnBeforeInsert(resource, amount, storageWrapper);

		if (moved >= amount) {
			return moved;
		}

		moved += handleOverflow(resource, amount - moved);

		if (moved >= amount) {
			return moved;
		}

		if (!tracker.getPartialSlots(key).isEmpty()) {
			int sizeBefore = tracker.getPartialSlots(key).size();
			int i = 0;
			while (moved < amount && i++ < sizeBefore) {
				int slot = tracker.getPartialSlots(key).iterator().next();
				if (slot == -1) {
					break;
				}
				moved += insert(slot, resource, amount - moved, tx);
			}
		}

		if (moved >= amount) {
			return moved;
		}

		for (int slot : mem.getFilterItemSlots().getOrDefault(item, Collections.emptySet())) {
			if (moved >= amount) {
				break;
			}
			if (tracker.getEmptySlots().contains(slot)) {
				moved += insert(slot, resource, amount - moved, tx);
			}
		}

		if (moved >= amount) {
			return moved;
		}

		for (int slot : mem.getFilterStackSlots().getOrDefault(key.hashCode(), Collections.emptySet())) {
			if (moved >= amount) {
				break;
			}
			if (tracker.getEmptySlots().contains(slot)) {
				moved += insert(slot, resource, amount - moved, tx);
			}
		}

		if (moved >= amount) {
			return moved;
		}

		for (int slot : filterItemSlots.getSlots(item)) {
			if (moved >= amount) {
				break;
			}
			if (tracker.getEmptySlots().contains(slot)) {
				moved += insert(slot, resource, amount - moved, tx);
			}
		}

		if (moved >= amount) {
			return moved;
		}

		if (shouldInsertIntoEmpty.getAsBoolean()) {
			int sizeBefore = tracker.getEmptySlots().size();
			int i = 0;
			while (moved < amount && i++ < sizeBefore) {
				int slot = pickNextPlaceableEmptySlot(tracker.getEmptySlots(), mem, resource);
				if (slot == -1) {
					break;
				}
				moved += insert(slot, resource, amount - moved, tx);
			}
		}

		moved += handleOverflow(resource, amount - moved);

		return moved;
	}

	private int pickNextPlaceableEmptySlot(Set<Integer> slots, MemorySettingsCategory mem, ItemResource resource) {
		for (int slot : slots) {
			if ((mem.isSlotSelected(slot) && !mem.matchesFilter(slot, resource)
					|| filterItemSlots.containsSlot(slot) && !filterItemSlots.getSlots(resource.getItem()).contains(slot))) {
				continue;
			}
			return slot;
		}
		return -1;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
		int inserted = runOnBeforeInsert(resource, amount, storageWrapper);
		inserted = runOnBeforeInsert(index, resource, amount - inserted, storageWrapper);
		if (inserted >= amount) {
			return amount;
		}

		inserted += handleOverflow(resource, amount - inserted);

		int result = inventoryPartitioner.getPartBySlot(index).insert(index, resource, amount - inserted, tx, super::insert);
		if (result > 0) {
			slotTrackerJournal.updateSnapshots(tx);
			getSlotTracker().removeAndSetSlotIndexes(this, index, getStackInSlot(index));
		}

		inserted += result;

		inserted += handleOverflow(resource, amount - inserted);

		runOnAfterInsert(index, tx);

		return inserted;
	}

	public ItemStack insertItemOnlyToSlot(int slot, ItemStack stack) {
		initSlotTracker();
		ItemResource resource = getResource(slot);
		if (resource.matches(stack)) {
			return triggerSlotOverflowUpgrades(insertItem(slot, stack));
		}

		return insertItem(slot, stack);
	}

	private void initSlotTracker() {
		if (!(slotTracker instanceof InventoryHandlerSlotTracker)) {
			slotTracker = new InventoryHandlerSlotTracker(storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class), filterItemSlots);
			slotTracker.refreshSlotIndexesFrom(this);
			slotTracker.setShouldInsertIntoEmpty(shouldInsertIntoEmpty);
		}
	}

	private ItemStack insertItem(int slot, ItemStack stack) {
		ItemResource resource = ItemResource.of(stack);
		int amount = stack.getCount();
		int inserted;
		try (Transaction tx = Transaction.openRoot()) {
			inserted = insert(slot, resource, amount, tx);
			if (inserted > 0) {
				tx.commit();
			}
		}

		if (inserted == 0) {
			return stack;
		} else if (inserted >= amount) {
			return ItemStack.EMPTY;
		}

		return stack.copyWithCount(amount - inserted);
	}

	private int handleOverflow(ItemResource resource, int amount) {
		if (!hasVoidUpgrade()) {
			return 0;
		}

		ItemStackKey stackKey = ItemStackKey.of(resource);
		if (hasOneFullStackOfItem(stackKey)) {
			int inserted = 0;
			if (getSlotTracker().getEmptySlots().isEmpty() && !getSlotTracker().getPartialStacks().contains(stackKey)) {
				inserted = triggerStorageOverflowUpgrades(resource, amount);
				if (inserted >= amount) {
					return amount;
				}
			}
			return triggerSlotOverflowUpgrades(resource, amount - inserted);
		}
		return 0;
	}

	private boolean hasOneFullStackOfItem(ItemStackKey stackKey) {
		return getSlotTracker().getFullStacks().contains(stackKey) && !getSlotTracker().getFullSlots(stackKey).isEmpty();
	}

	private int triggerStorageOverflowUpgrades(ItemResource resource, int amount) {
		int ret = 0;
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)) {
			ret = overflowUpgrade.onStorageOverflow(resource, amount);
			if (ret >= amount) {
				break;
			}
		}
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

	private int triggerSlotOverflowUpgrades(ItemResource resource, int amount) {
		int ret = 0;
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplement(IOverflowResponseUpgrade.class)) {
			ret = overflowUpgrade.onSlotOverflow(resource, amount);
			if (ret >= amount) {
				break;
			}
		}
		return ret;
	}

	private int runOnBeforeInsert(ItemResource resource, int amount, IStorageWrapper storageWrapper) {
		List<IInsertResponseUpgrade> wrappers = storageWrapper.getUpgradeHandler().getWrappersThatImplement(IInsertResponseUpgrade.class);
		int moved = 0;
		for (IInsertResponseUpgrade upgrade : wrappers) {
			moved += upgrade.onBeforeInsert(this, resource, amount - moved);
			if (moved == amount) {
				return amount;
			}
		}
		return moved;
	}

	private int runOnBeforeInsert(int slot, ItemResource resource, int amount, IStorageWrapper storageWrapper) {
		List<IInsertResponseUpgrade> wrappers = storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class);
		int moved = 0;
		for (IInsertResponseUpgrade upgrade : wrappers) {
			moved += upgrade.onBeforeInsert(this, slot, resource, amount - moved);
			if (moved == amount) {
				return amount;
			}
		}
		return moved;
	}

	private void runOnAfterExtract(int slot, ItemResource originalResource) {
		List<IExtractResponseUpgrade> wrappers = storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IExtractResponseUpgrade.class);
		for (IExtractResponseUpgrade upgrade : wrappers) {
			upgrade.onAfterExtract(this, slot, originalResource);
		}
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}


	public boolean isItemValid(int slot, ItemStack stack) {
		return isItemValid(slot, stack, null);
	}

	public boolean isItemValid(int slot, ItemStack stack, @Nullable Player player) {
		return isItemValid(slot, ItemResource.of(stack), player);
	}

	public boolean isItemValid(int slot, ItemResource resource, @Nullable Player player) {
		return inventoryPartitioner.getPartBySlot(slot).isValid(slot, resource, player, super::isValid)
				&& isAllowed(resource) && storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(slot, resource);
	}

	@Override
	public boolean isValid(int slot, ItemResource resource) {
		return isItemValid(slot, resource, null);
	}

	protected abstract boolean isAllowed(ItemResource resource);

	public void saveInventory() {
		saveHandler.run();
	}

	@Nullable
	public Identifier getNoItemIcon(int slotIndex) {
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

	public double getStackSizeMultiplier() {
		return maxStackSizeMultiplier;
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

	public ItemStack getStackInSlot(int index) {
		Objects.checkIndex(index, this.size());
		return inventoryPartitioner.getPartBySlot(index).getStackInSlot(index, slot -> stacks.get(slot));
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

		for(int i = 0; i < stacks.size(); ++i) {
			ItemStack stack = stacks.get(i);
			ItemResource resource = ItemResource.of(stack);
			if (stack.getCount() < getCapacity(i, resource)) {
				return false;
			}
		}
		return true;
	}

	private class SlotTrackerJournal extends SnapshotJournal<Void> {
		@Override
		protected Void createSnapshot() {
			return null;
		}

		@Override
		protected void revertToSnapshot(Void unused) {
			getSlotTracker().refreshSlotIndexesFrom(InventoryHandler.this);
		}
	}
}
