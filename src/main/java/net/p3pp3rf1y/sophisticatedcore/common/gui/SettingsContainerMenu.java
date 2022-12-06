package net.p3pp3rf1y.sophisticatedcore.common.gui;

import com.google.common.base.Suppliers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SyncContainerClientDataMessage;
import net.p3pp3rf1y.sophisticatedcore.network.SyncTemplateSettingsMessage;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsContainerBase;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTemplateStorage;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class SettingsContainerMenu<S extends IStorageWrapper> extends AbstractContainerMenu implements ISyncedContainer {
	private static final Map<String, ISettingsContainerFactory<?, ?>> SETTINGS_CONTAINER_FACTORIES = new HashMap<>();
	private static final String ACTION_TAG = "action";

	static {
		addFactory(MainSettingsCategory.NAME, MainSettingsContainer::new);
		addFactory(NoSortSettingsCategory.NAME, NoSortSettingsContainer::new);
		addFactory(MemorySettingsCategory.NAME, MemorySettingsContainer::new);
		addFactory(ItemDisplaySettingsCategory.NAME, ItemDisplaySettingsContainer::new);
	}

	protected final Player player;

	protected final S storageWrapper;

	private final List<Slot> storageInventorySlots = new ArrayList<>();
	public final NonNullList<ItemStack> lastGhostSlots = NonNullList.create();
	public final NonNullList<ItemStack> remoteGhostSlots = NonNullList.create();
	private final Map<String, SettingsContainerBase<?>> settingsContainers = new LinkedHashMap<>();
	public final List<Slot> ghostSlots = new ArrayList<>();

	@Nullable
	private TemplateSettingsHandler selectedTemplate;

	protected SettingsContainerMenu(MenuType<?> menuType, int windowId, Player player, S storageWrapper) {
		super(menuType, windowId);
		this.player = player;
		this.storageWrapper = storageWrapper;

		addStorageInventorySlots();
		addSettingsContainers();
	}

	public int getNumberOfStorageInventorySlots() {
		return storageWrapper.getInventoryHandler().getSlots();
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

		while (slotIndex < inventoryHandler.getSlots()) {
			int finalSlotIndex = slotIndex;
			storageInventorySlots.add(addSlot(new ViewOnlyStorageInventorySlot(inventoryHandler, finalSlotIndex)));

			slotIndex++;
		}
	}

	public int getColumnsTaken() {
		return storageWrapper.getColumnsTaken();
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

	public int getNumberOfSlots() {
		return storageWrapper.getInventoryHandler().getSlots();
	}

	@Override
	public void sendAllDataToRemote() {
		for (int slotIndex = 0; slotIndex < ghostSlots.size(); slotIndex++) {
			remoteGhostSlots.set(slotIndex, ghostSlots.get(slotIndex).getItem().copy());
		}

		if (synchronizer != null) {
			synchronizer.sendInitialData(this, remoteGhostSlots, remoteCarried, new int[0]);
		}

		if (player instanceof ServerPlayer serverPlayer) {
			SophisticatedCore.PACKET_HANDLER.sendToClient(serverPlayer, new SyncTemplateSettingsMessage(SettingsTemplateStorage.get().getPlayerTemplates(serverPlayer)));
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

	public void onMemorizedStackRemoved(int slotId) {
		if (getSlot(slotId).getItem().isEmpty()) {
			storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemChanged(slotId);
		}
	}

	public void onMemorizedItemsChanged() {
		storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
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
		} else if (data.contains(ACTION_TAG, Tag.TAG_STRING)) {
			String action = data.getString(ACTION_TAG);
			if (action.equals("saveTemplate")) {
				saveTemplate(data.getInt("slot"));
			} else if (action.equals("loadTemplate")) {
				loadTemplate();
			}
		}
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		//noop
	}

	@Override
	public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
		return ItemStack.EMPTY;
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

	public void saveTemplate(int saveSlot) {
		SettingsTemplateStorage.get().putPlayerTemplate(player, saveSlot, storageWrapper.getSettingsHandler().getNbt().copy());
		sendDataToServer(() -> {
			CompoundTag tag = NBTHelper.putString(new CompoundTag(), ACTION_TAG, "saveTemplate");
			tag.putInt("slot", saveSlot);
			return tag;
		});
	}

	public void loadTemplate() {
		if (selectedTemplate == null) {
			return;
		}
		storageWrapper.getSettingsHandler().getSettingsCategories().values().forEach(category -> overwriteCategory(category, selectedTemplate.getTypeCategory(category.getClass())));

		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), ACTION_TAG, "loadTemplate"));
	}

	private <T extends ISettingsCategory<T>> void overwriteCategory(ISettingsCategory<?> currentCategory, ISettingsCategory<?> otherCategory) {
		//noinspection unchecked
		((T) currentCategory).overwriteWith(((T) otherCategory));
	}

	public <T extends ISettingsCategory<?>> Optional<T> getSelectedTemplatesCategory(Class<T> categoryClass) {
		return selectedTemplate != null ? Optional.of(selectedTemplate.getTypeCategory(categoryClass)) : Optional.empty();
	}

	private static class ViewOnlyStorageInventorySlot extends SlotItemHandler {
		public ViewOnlyStorageInventorySlot(IItemHandler inventoryHandler, int slotIndex) {
			super(inventoryHandler, slotIndex, 0, 0);
		}

		@Override
		public boolean mayPickup(Player playerIn) {
			return false;
		}
	}

	public int getNumberOfRows() {
		return storageWrapper.getNumberOfSlotRows();
	}

	protected static <C extends ISettingsCategory, T extends SettingsContainerBase<C>> void addFactory(String categoryName, ISettingsContainerFactory<C, T> factory) {
		SETTINGS_CONTAINER_FACTORIES.put(categoryName, factory);
	}

	public interface ISettingsContainerFactory<C extends ISettingsCategory, T extends SettingsContainerBase<C>> {
		T create(SettingsContainerMenu<?> settingsContainer, String categoryName, C category);
	}

	private static <C extends ISettingsCategory> SettingsContainerBase<C> instantiateContainer(SettingsContainerMenu<?> settingsContainer, String name, C category) {
		//noinspection unchecked
		return (SettingsContainerBase<C>) getSettingsContainerFactory(name).create(settingsContainer, name, category);
	}

	private static <C extends ISettingsCategory, T extends SettingsContainerBase<C>> ISettingsContainerFactory<C, T> getSettingsContainerFactory(String name) {
		//noinspection unchecked
		return (ISettingsContainerFactory<C, T>) SETTINGS_CONTAINER_FACTORIES.get(name);
	}

	public void updateSelectedTemplate(@Nullable CompoundTag settingsTag) {
		if (settingsTag == null) {
			selectedTemplate = null;
		} else {
			selectedTemplate = new TemplateSettingsHandler(settingsTag) {
				@Override
				protected SettingsHandler getCurrentSettingsHandler() {
					return storageWrapper.getSettingsHandler();
				}
			};
		}
	}

	public void sendDataToServer(Supplier<CompoundTag> supplyData) {
		if (isServer()) {
			return;
		}
		CompoundTag data = supplyData.get();
		SophisticatedCore.PACKET_HANDLER.sendToServer(new SyncContainerClientDataMessage(data));
	}

	protected boolean isServer() {
		return !player.level.isClientSide;
	}

	private abstract static class TemplateSettingsHandler extends SettingsHandler {

		protected TemplateSettingsHandler(CompoundTag contentsNbt) {
			super(contentsNbt, () -> {}, NoopStorageWrapper.INSTANCE::getInventoryHandler, NoopStorageWrapper.INSTANCE::getRenderInfo);
		}

		protected abstract SettingsHandler getCurrentSettingsHandler();

		@Override
		protected CompoundTag getSettingsNbtFromContentsNbt(CompoundTag contentsNbt) {
			return contentsNbt;
		}

		@Override
		protected void addItemDisplayCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderInfo> renderInfoSupplier, CompoundTag settingsNbt) {
			int itemNumberLimit = getCurrentSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).getItemNumberLimit();
			addSettingsCategory(settingsNbt, ItemDisplaySettingsCategory.NAME, markContentsDirty, (categoryNbt, saveNbt) ->
					new ItemDisplaySettingsCategory(inventoryHandlerSupplier, renderInfoSupplier, categoryNbt, saveNbt, itemNumberLimit, () -> getTypeCategory(MemorySettingsCategory.class)));
		}

		@Override
		public String getGlobalSettingsCategoryName() {
			return getCurrentSettingsHandler().getGlobalSettingsCategoryName();
		}

		@Override
		public ISettingsCategory<?> instantiateGlobalSettingsCategory(CompoundTag categoryNbt, Consumer<CompoundTag> saveNbt) {
			return getCurrentSettingsHandler().instantiateGlobalSettingsCategory(categoryNbt, saveNbt);
		}

		@Override
		protected void saveCategoryNbt(CompoundTag settingsNbt, String categoryName, CompoundTag tag) {
			//noop
		}
	}
}
