package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotStackAccessor;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.TankPosition;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class UpgradeHandler extends ItemStacksResourceHandler implements ISlotStackAccessor, IndexModifier<ItemResource> {
	public static final String UPGRADE_INVENTORY_TAG = "upgradeInventory";
	private final IStorageWrapper storageWrapper;
	private final Runnable contentsSaveHandler;
	private final Runnable onInvalidateUpgradeCaches;
	private final ContainerContents.UpgradeData upgradeData;
	@Nullable
	private Runnable refreshCallBack = null;
	private final Map<Integer, IUpgradeWrapper> slotWrappers = new LinkedHashMap<>();
	private final Map<UpgradeType<? extends IUpgradeWrapper>, List<? extends IUpgradeWrapper>> typeWrappers = new HashMap<>();
	private boolean justSavingNbtChange = false;
	private boolean wrappersInitialized = false;
	private boolean typeWrappersInitialized = false;
	@Nullable
	private IUpgradeWrapperAccessor wrapperAccessor = null;
	private boolean persistent = true;
	private final Map<Class<? extends IUpgradeWrapper>, Consumer<? extends IUpgradeWrapper>> upgradeDefaultsHandlers = new HashMap<>();
	private final Set<Integer> runningOnBeforeRemovedOnSlots = new HashSet<>();

	public UpgradeHandler(int numberOfUpgradeSlots, IStorageWrapper storageWrapper, ContainerContents containerContents, Runnable contentsSaveHandler,
			Runnable onInvalidateUpgradeCaches) {
		super(numberOfUpgradeSlots);
		this.upgradeData = containerContents.upgrades();
		this.storageWrapper = storageWrapper;
		this.contentsSaveHandler = contentsSaveHandler;
		this.onInvalidateUpgradeCaches = onInvalidateUpgradeCaches;
		loadUpgradesFromData();
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER && storageWrapper.getRenderDataHandler().getUpgradeItems().size() != size()) {
			setRenderUpgradeItems();
		}
	}

	private void loadUpgradesFromData() {
		stacks = NonNullList.withSize(Math.max(stacks.size(), upgradeData.stacks().size()), ItemStack.EMPTY);
		for (int slot = 0; slot < upgradeData.stacks().size(); slot++) {
			ItemStack stack = upgradeData.stacks().get(slot);
			stacks.set(slot, stack);
		}
	}

	public <T extends IUpgradeWrapper> void registerUpgradeDefaultsHandler(Class<T> upgradeClass, Consumer<T> defaultsHandler) {
		upgradeDefaultsHandlers.put(upgradeClass, defaultsHandler);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return resource.isEmpty() || resource.getItem() instanceof IUpgradeItem<?>;
	}

	@Override
	protected void onContentsChanged(int slot, ItemStack previousContents) {
		super.onContentsChanged(slot, previousContents);
		if (persistent) {
			saveInventory();
			contentsSaveHandler.run();
		}
		ItemStack currentStack = getStackInSlot(slot);

		if (!justSavingNbtChange) {
			refreshUpgradeWrappers();
			setRenderUpgradeItems();
		}

		if (ItemStack.isSameItemSameComponents(previousContents, currentStack)) {
			return;
		}

		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER && !currentStack.isEmpty()) {
			onUpgradeAdded(slot);
		}
	}

	public void setRenderUpgradeItems() {
		List<ItemStack> upgradeItems = new ArrayList<>();
		InventoryHelper.iterate(this, (upgradeSlot, resource, amount) -> upgradeItems.add(resource.toStack(1)));
		storageWrapper.getRenderDataHandler().setUpgradeItems(upgradeItems);
	}

	@Override
	public void deserialize(ValueInput input) {
		input.read("stacks", this.codec).ifPresent(list -> {
			NonNullList<ItemStack> maxSized = list;
			if (stacks.size() != list.size()) {
				int newSize = Math.max(stacks.size(), list.size());
				maxSized = NonNullList.withSize(newSize, ItemStack.EMPTY);
				Collections.copy(maxSized, list);
			}
			this.stacks = maxSized;
		});
	}

	public void saveInventory() {
		NonNullList<ItemStack> stacksCopy = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
		for (int slot = 0; slot < stacks.size(); slot++) {
			stacksCopy.set(slot, stacks.get(slot));
		}
		upgradeData.setStacks(stacksCopy);
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public void setRefreshCallBack(Runnable refreshCallBack) {
		this.refreshCallBack = refreshCallBack;
	}

	public void removeRefreshCallback() {
		refreshCallBack = null;
	}

	private void initializeWrappers() {
		if (wrappersInitialized) {
			return;
		}
		wrappersInitialized = true;
		slotWrappers.clear();
		typeWrappers.clear();
		if (wrapperAccessor != null) {
			wrapperAccessor.clearCache();
		}

		InventoryHelper.iterate(this, (slot, stack) -> {
			if (stack.isEmpty() || !(stack.getItem() instanceof IUpgradeItem<?>)) {
				return;
			}
			UpgradeType<?> type = ((IUpgradeItem<?>) stack.getItem()).getType();
			IUpgradeWrapper wrapper = type.create(storageWrapper, stack, upgradeStack -> {
				justSavingNbtChange = true;
				setStackInSlot(slot, upgradeStack);
				justSavingNbtChange = false;
			});
			setUpgradeDefaults(wrapper);
			slotWrappers.put(slot, wrapper);
		});

		initRenderDataCallbacks(false);
	}

	private void onUpgradeAdded(int slot) {
		Map<Integer, IUpgradeWrapper> wrappers = getSlotWrappers();
		if (wrappers.containsKey(slot)) {
			wrappers.get(slot).onAdded();
		}
	}

	private void setUpgradeDefaults(IUpgradeWrapper wrapper) {
		getUpgradeDefaultsHandler(wrapper).accept(wrapper);
	}

	private <T extends IUpgradeWrapper> Consumer<T> getUpgradeDefaultsHandler(T wrapper) {
		// noinspection unchecked
		return (Consumer<T>) upgradeDefaultsHandlers.getOrDefault(wrapper.getClass(), w -> {
		});
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		setStackInSlot(index, resource.toStack(amount));
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		ItemStack originalStack = getStackInSlot(slot);
		Objects.checkIndex(slot, size());

		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER && !originalStack.isEmpty() && !runningOnBeforeRemovedOnSlots.contains(slot)) {
			runningOnBeforeRemovedOnSlots.add(slot);
			if (!ItemStack.isSameItemSameComponents(originalStack, stack)) {
				getSlotWrappers().get(slot).onBeforeRemoved();
			}
			runningOnBeforeRemovedOnSlots.remove(slot);
		}

		stacks.set(slot, stack);
		onContentsChanged(slot, originalStack);
	}

	private void initializeTypeWrappers() {
		if (typeWrappersInitialized) {
			return;
		}
		initializeWrappers();
		typeWrappersInitialized = true;

		typeWrappers.clear();
		slotWrappers.values().forEach(wrapper -> {
			if (wrapper.getUpgradeStack().getItem() instanceof IUpgradeItem<?> upgradeItem) {
				UpgradeType<?> type = upgradeItem.getType();
				if (wrapper.isEnabled() && (!(wrapper instanceof ITickableUpgrade) || storageWrapper.isUpgradeRunnable(wrapper.getUpgradeStack()))) {
					addTypeWrapper(type, wrapper);
				}
			}
		});

		initRenderDataCallbacks(false);
	}

	private <T extends IUpgradeWrapper> void addTypeWrapper(UpgradeType<?> type, T wrapper) {
		// noinspection unchecked
		((List<T>) typeWrappers.computeIfAbsent(type, t -> new ArrayList<>())).add(wrapper);
	}

	public <T extends IUpgradeWrapper> List<T> getTypeWrappers(UpgradeType<T> type) {
		initializeTypeWrappers();
		// noinspection unchecked
		return (List<T>) typeWrappers.getOrDefault(type, Collections.emptyList());
	}

	public <T extends IUpgradeWrapper> boolean hasUpgrade(UpgradeType<T> type) {
		return !getTypeWrappers(type).isEmpty();
	}

	public <T> List<T> getWrappersThatImplement(Class<T> upgradeClass) {
		initializeWrappers();
		return getWrapperAccessor().getWrappersThatImplement(upgradeClass);
	}

	private IUpgradeWrapperAccessor getWrapperAccessor() {
		if (wrapperAccessor == null) {
			IUpgradeWrapperAccessor accessor = new Accessor(this);
			for (IUpgradeAccessModifier upgrade : getListOfWrappersThatImplement(IUpgradeAccessModifier.class)) {
				accessor = upgrade.wrapAccessor(accessor);
			}
			wrapperAccessor = accessor;
		}
		return wrapperAccessor;
	}

	public <T> List<T> getWrappersThatImplementFromMainStorage(Class<T> upgradeClass) {
		initializeWrappers();
		return getWrapperAccessor().getWrappersThatImplementFromMainStorage(upgradeClass);
	}

	public <T> List<T> getListOfWrappersThatImplement(Class<T> uc) {
		List<T> ret = new ArrayList<>();
		for (IUpgradeWrapper wrapper : slotWrappers.values()) {
			if (wrapper.isEnabled() && uc.isInstance(wrapper)) {
				// noinspection unchecked
				ret.add((T) wrapper);
			}
		}
		return ret;
	}

	public Map<Integer, IUpgradeWrapper> getSlotWrappers() {
		initializeWrappers();
		return slotWrappers;
	}

	public void copyTo(UpgradeHandler otherHandler) {
		InventoryHelper.copyTo(this, otherHandler);
	}

	public void refreshWrappersThatImplementAndTypeWrappers() {
		typeWrappersInitialized = false;
		if (wrapperAccessor != null) {
			wrapperAccessor.clearCache();
		}
		if (refreshCallBack != null) {
			refreshCallBack.run();
		}
	}

	public void refreshUpgradeWrappers() {
		if (!wrappersInitialized && !typeWrappersInitialized) {
			return;
		}

		wrappersInitialized = false;
		typeWrappersInitialized = false;
		if (wrapperAccessor != null) {
			wrapperAccessor.onBeforeDeconstruct();
			wrapperAccessor = null;
		}
		if (refreshCallBack != null) {
			refreshCallBack.run();
		}
		onInvalidateUpgradeCaches.run();

		initRenderDataCallbacks(true);
	}

	private void initRenderDataCallbacks(boolean forceUpdateRenderData) {
		RenderDataHandler renderDataHandler = storageWrapper.getRenderDataHandler();
		if (forceUpdateRenderData) {
			renderDataHandler.resetUpgradeInfo(false);
		}

		initTankRenderDataCallbacks(forceUpdateRenderData, renderDataHandler);
		initBatteryRenderDataCallbacks(forceUpdateRenderData, renderDataHandler);
	}

	private void initBatteryRenderDataCallbacks(boolean forceUpdateRenderData, RenderDataHandler renderDataHandler) {
		getSlotWrappers().forEach((slot, wrapper) -> {
			if (wrapper instanceof IRenderedBatteryUpgrade batteryWrapper) {
				batteryWrapper.setBatteryRenderDataUpdateCallback(renderDataHandler::setBatteryRenderData);
				if (forceUpdateRenderData) {
					batteryWrapper.forceUpdateBatteryRenderData();
				}
			}
		});
	}

	private void initTankRenderDataCallbacks(boolean forceUpdateRenderData, RenderDataHandler renderDataHandler) {
		AtomicBoolean singleTankRight = new AtomicBoolean(false);
		List<IRenderedTankUpgrade> tankRenderWrappers = new ArrayList<>();
		int minRightSlot = size() / 2;
		getSlotWrappers().forEach((slot, wrapper) -> {
			if (wrapper instanceof IRenderedTankUpgrade tankUpgrade) {
				tankRenderWrappers.add(tankUpgrade);
				if (slot >= minRightSlot) {
					singleTankRight.set(true);
				}
			}
		});

		TankPosition currentTankPos = tankRenderWrappers.size() == 1 && singleTankRight.get() ? TankPosition.RIGHT : TankPosition.LEFT;
		for (IRenderedTankUpgrade tankRenderWrapper : tankRenderWrappers) {
			TankPosition finalCurrentTankPos = currentTankPos;
			tankRenderWrapper.setTankRenderDataUpdateCallback(data -> renderDataHandler.setTankRenderData(finalCurrentTankPos, data));
			if (forceUpdateRenderData) {
				tankRenderWrapper.forceUpdateTankRenderData();
			}
			currentTankPos = TankPosition.RIGHT;
		}
	}

	public void increaseSize(int diff) {
		NonNullList<ItemStack> previousStacks = stacks;
		stacks = NonNullList.withSize(previousStacks.size() + diff, ItemStack.EMPTY);
		for (int slot = 0; slot < previousStacks.size(); slot++) {
			stacks.set(slot, previousStacks.get(slot));
		}
		saveInventory();
		setRenderUpgradeItems();
	}

	@Override
	protected int getCapacity(int index, ItemResource resource) {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		Objects.checkIndex(slot, size());
		return stacks.get(slot);
	}

	private static class Accessor implements IUpgradeWrapperAccessor {
		private final Map<Class<?>, List<?>> interfaceWrappers = new ConcurrentHashMap<>();

		private final UpgradeHandler upgradeHandler;

		public Accessor(UpgradeHandler upgradeHandler) {
			this.upgradeHandler = upgradeHandler;
		}

		@Override
		public <T> List<T> getWrappersThatImplement(Class<T> upgradeClass) {
			// noinspection unchecked
			return (List<T>) interfaceWrappers.computeIfAbsent(upgradeClass, upgradeHandler::getListOfWrappersThatImplement);
		}

		@Override
		public <T> List<T> getWrappersThatImplementFromMainStorage(Class<T> upgradeClass) {
			// noinspection unchecked
			return (List<T>) interfaceWrappers.computeIfAbsent(upgradeClass, upgradeHandler::getListOfWrappersThatImplement);
		}

		@Override
		public void clearCache() {
			interfaceWrappers.clear();
		}
	}
}
