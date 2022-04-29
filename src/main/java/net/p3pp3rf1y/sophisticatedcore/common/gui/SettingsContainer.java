package net.p3pp3rf1y.sophisticatedcore.common.gui;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsContainerBase;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.globaloverridable.GlobalOverridableSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.globaloverridable.GlobalOverridableSettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class SettingsContainer<S extends IStorageWrapper> extends AbstractContainerMenu implements ISyncedContainer {
	private static final Map<String, ISettingsContainerFactory<?, ?>> SETTINGS_CONTAINER_FACTORIES;

	static {
		ImmutableMap.Builder<String, ISettingsContainerFactory<?, ?>> builder = new ImmutableMap.Builder<>();
		addFactory(builder, GlobalOverridableSettingsCategory.NAME, GlobalOverridableSettingsContainer::new);
		addFactory(builder, NoSortSettingsCategory.NAME, NoSortSettingsContainer::new);
		addFactory(builder, MemorySettingsCategory.NAME, MemorySettingsContainer::new);
		addFactory(builder, ItemDisplaySettingsCategory.NAME, ItemDisplaySettingsContainer::new);
		SETTINGS_CONTAINER_FACTORIES = builder.build();
	}

	protected final Player player;

	protected final S storageWrapper;

	private final StorageBackgroundProperties storageBackgroundProperties;
	private final List<Slot> storageInventorySlots = new ArrayList<>();
	public final NonNullList<ItemStack> lastGhostSlots = NonNullList.create();
	public final NonNullList<ItemStack> remoteGhostSlots = NonNullList.create();
	private final Map<String, SettingsContainerBase<?>> settingsContainers = new LinkedHashMap<>();
	public final List<Slot> ghostSlots = new ArrayList<>();

	protected SettingsContainer(MenuType<?> menuType, int windowId, Player player, S storageWrapper) {
		super(menuType, windowId);
		this.player = player;
		this.storageWrapper = storageWrapper;
		storageBackgroundProperties = getNumberOfSlots() + storageWrapper.getColumnsTaken() * storageWrapper.getNumberOfSlotRows() <= 81 ? StorageBackgroundProperties.REGULAR : StorageBackgroundProperties.WIDE;

		addStorageInventorySlots();
		addSettingsContainers();
	}

	public S getStorageWrapper() {
		return storageWrapper;
	}

	private void addSettingsContainers() {
		SettingsHandler settingsHandler = storageWrapper.getSettingsHandler();
		settingsHandler.getSettingsCategories().forEach((name, category) -> settingsContainers.put(name, instantiateContainer(this, name, category)));
	}

	private void addStorageInventorySlots() {
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();

		int slotIndex = 0;
		int yPosition = 18;

		while (slotIndex < inventoryHandler.getSlots()) {
			int lineIndex = slotIndex % getSlotsOnLine();
			int finalSlotIndex = slotIndex;
			storageInventorySlots.add(addSlot(new ViewOnlyStorageInventorySlot(inventoryHandler, finalSlotIndex, lineIndex, yPosition)));

			slotIndex++;
			if (slotIndex % getSlotsOnLine() == 0) {
				yPosition += 18;
			}
		}
	}

	@Override
	protected Slot addSlot(Slot slot) {
		slot.index = ghostSlots.size();
		ghostSlots.add(slot);
		lastGhostSlots.add(ItemStack.EMPTY);
		remoteGhostSlots.add(ItemStack.EMPTY);
		return slot;
	}

	@Override
	public void broadcastChanges() {
		for (int slot = 0; slot < ghostSlots.size(); ++slot) {
			ItemStack itemstack = ghostSlots.get(slot).getItem();
			Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
			triggerSlotListeners(slot, itemstack, supplier);
			synchronizeSlotToRemote(slot, itemstack, supplier);
		}
	}

	@Override
	public void broadcastFullState() {
		for (int slotIndex = 0; slotIndex < ghostSlots.size(); ++slotIndex) {
			ItemStack itemstack = ghostSlots.get(slotIndex).getItem();
			triggerSlotListeners(slotIndex, itemstack, itemstack::copy);
		}

		sendAllDataToRemote();
	}

	@SuppressWarnings("java:S2177")
	private void triggerSlotListeners(int slotIndex, ItemStack slotStack, Supplier<ItemStack> slotStackCopy) {
		ItemStack itemstack = lastGhostSlots.get(slotIndex);
		if (!ItemStack.matches(itemstack, slotStack)) {
			boolean clientStackChanged = !slotStack.equals(itemstack, true);
			ItemStack itemstack1 = slotStackCopy.get();
			lastGhostSlots.set(slotIndex, itemstack1);

			if (clientStackChanged) {
				for (ContainerListener containerlistener : containerListeners) {
					containerlistener.slotChanged(this, slotIndex, itemstack1);
				}
			}
		}

	}

	@SuppressWarnings("java:S2177")
	private void synchronizeSlotToRemote(int slotIndex, ItemStack slotStack, Supplier<ItemStack> slotStackCopy) {
		if (!suppressRemoteUpdates) {
			ItemStack itemstack = remoteGhostSlots.get(slotIndex);
			if (!ItemStack.matches(itemstack, slotStack)) {
				ItemStack stackCopy = slotStackCopy.get();
				remoteGhostSlots.set(slotIndex, stackCopy);
				if (synchronizer != null) {
					synchronizer.sendSlotChange(this, slotIndex, stackCopy);
				}
			}
		}
	}

	@Override
	public void sendAllDataToRemote() {
		for (int slotIndex = 0; slotIndex < ghostSlots.size(); slotIndex++) {
			remoteGhostSlots.set(slotIndex, ghostSlots.get(slotIndex).getItem().copy());
		}

		if (synchronizer != null) {
			synchronizer.sendInitialData(this, remoteSlots, remoteCarried, new int[0]);
		}
	}

	public abstract void detectSettingsChangeAndReload();

	@Override
	public void setSynchronizer(ContainerSynchronizer synchronizer) {
		if (player instanceof ServerPlayer serverPlayer && storageWrapper.getInventoryHandler().getStackSizeMultiplier() > 1) {
			super.setSynchronizer(new HighStackCountSynchronizer(serverPlayer));
			return;
		}
		super.setSynchronizer(synchronizer);
	}

	@Override
	public Slot getSlot(int slotId) {
		return ghostSlots.get(slotId);
	}

	public Optional<ItemStack> getMemorizedStackInSlot(int slotId) {
		return storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotFilterItem(slotId).map(ItemStack::new);
	}

	public int getSlotsOnLine() {
		return storageBackgroundProperties.getSlotsOnLine() - storageWrapper.getColumnsTaken();
	}

	public int getNumberOfSlots() {
		return storageWrapper.getInventoryHandler().getSlots();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	public List<Slot> getStorageInventorySlots() {
		return storageInventorySlots;
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains("categoryName")) {
			String categoryName = data.getString("categoryName");
			if (settingsContainers.containsKey(categoryName)) {
				settingsContainers.get(categoryName).handleMessage(data);
			}
		}
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		//noop
	}

	public StorageBackgroundProperties getStorageBackgroundProperties() {
		return storageBackgroundProperties;
	}

	public void forEachSettingsContainer(BiConsumer<String, ? super SettingsContainerBase<?>> consumer) {
		settingsContainers.forEach(consumer);
	}

	public Player getPlayer() {
		return player;
	}

	public BlockPos getBlockPosition() {
		return BlockPos.ZERO;
	}

	private static class ViewOnlyStorageInventorySlot extends SlotItemHandler {
		public ViewOnlyStorageInventorySlot(IItemHandler inventoryHandler, int slotIndex, int lineIndex, int yPosition) {
			super(inventoryHandler, slotIndex, 8 + lineIndex * 18, yPosition);
		}

		@Override
		public boolean mayPickup(Player playerIn) {
			return false;
		}
	}

	public int getNumberOfRows() {
		return storageWrapper.getNumberOfSlotRows();
	}

	private static <C extends ISettingsCategory, T extends SettingsContainerBase<C>> void addFactory(
			ImmutableMap.Builder<String, ISettingsContainerFactory<?, ?>> builder, String categoryName, ISettingsContainerFactory<C, T> factory) {
		builder.put(categoryName, factory);
	}

	public interface ISettingsContainerFactory<C extends ISettingsCategory, T extends SettingsContainerBase<C>> {
		T create(SettingsContainer<?> settingsContainer, String categoryName, C category);
	}

	private static <C extends ISettingsCategory> SettingsContainerBase<C> instantiateContainer(SettingsContainer<?> settingsContainer, String name, C category) {
		//noinspection unchecked
		return (SettingsContainerBase<C>) getSettingsContainerFactory(name).create(settingsContainer, name, category);
	}

	private static <C extends ISettingsCategory, T extends SettingsContainerBase<C>> ISettingsContainerFactory<C, T> getSettingsContainerFactory(String name) {
		//noinspection unchecked
		return (ISettingsContainerFactory<C, T>) SETTINGS_CONTAINER_FACTORIES.get(name);
	}
}
