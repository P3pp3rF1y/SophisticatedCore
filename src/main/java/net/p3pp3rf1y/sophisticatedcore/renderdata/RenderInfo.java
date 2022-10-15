package net.p3pp3rf1y.sophisticatedcore.renderdata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedBatteryUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedTankUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeRenderData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeRenderData;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class RenderInfo {
	private static final String TANKS_TAG = "tanks";
	private static final String BATTERY_TAG = "battery";
	private static final String TANK_POSITION_TAG = "position";
	private static final String TANK_INFO_TAG = "info";
	private static final String ITEM_DISPLAY_TAG = "itemDisplay";
	private static final String UPGRADES_TAG = "upgrades";

	private static final Map<String, UpgradeRenderDataType<?>> RENDER_DATA_TYPES;

	static {
		RENDER_DATA_TYPES = Map.of(
				CookingUpgradeRenderData.TYPE.getName(), CookingUpgradeRenderData.TYPE,
				JukeboxUpgradeRenderData.TYPE.getName(), JukeboxUpgradeRenderData.TYPE
		);
	}

	private ItemDisplayRenderInfo itemDisplayRenderInfo;
	private final Supplier<Runnable> getSaveHandler;
	private final Map<UpgradeRenderDataType<?>, IUpgradeRenderData> upgradeData = new HashMap<>();

	private final Map<TankPosition, IRenderedTankUpgrade.TankRenderInfo> tankRenderInfos = new LinkedHashMap<>();
	@Nullable
	private IRenderedBatteryUpgrade.BatteryRenderInfo batteryRenderInfo = null;

	private Consumer<RenderInfo> changeListener = ri -> {};

	protected RenderInfo(Supplier<Runnable> getSaveHandler) {
		this.getSaveHandler = getSaveHandler;
		itemDisplayRenderInfo = new ItemDisplayRenderInfo();
	}

	public ItemDisplayRenderInfo getItemDisplayRenderInfo() {
		return itemDisplayRenderInfo;
	}

	public <T extends IUpgradeRenderData> void setUpgradeRenderData(UpgradeRenderDataType<T> upgradeRenderDataType, T renderData) {
		upgradeData.put(upgradeRenderDataType, renderData);
		serializeUpgradeData(upgrades -> upgrades.put(upgradeRenderDataType.getName(), renderData.serializeNBT()));
		save();
	}

	public <T extends IUpgradeRenderData> Optional<T> getUpgradeRenderData(UpgradeRenderDataType<T> upgradeRenderDataType) {
		if (!upgradeData.containsKey(upgradeRenderDataType)) {
			return Optional.empty();
		}
		return upgradeRenderDataType.cast(upgradeData.get(upgradeRenderDataType));
	}

	private void serializeUpgradeData(Consumer<CompoundTag> modifyUpgradesTag) {
		CompoundTag renderInfo = getRenderInfoTag().orElse(new CompoundTag());
		CompoundTag upgrades = renderInfo.getCompound(UPGRADES_TAG);
		modifyUpgradesTag.accept(upgrades);
		renderInfo.put(UPGRADES_TAG, upgrades);
		serializeRenderInfo(renderInfo);
	}

	public void refreshItemDisplayRenderInfo(List<DisplayItem> displayItems) {
		itemDisplayRenderInfo = new ItemDisplayRenderInfo(displayItems);
		CompoundTag renderInfo = getRenderInfoTag().orElse(new CompoundTag());
		renderInfo.put(ITEM_DISPLAY_TAG, itemDisplayRenderInfo.serialize());
		serializeRenderInfo(renderInfo);
		save();
	}

	public void updateItemDisplayRenderInfo(int index, ItemStack item, int rotation) {
		if (item.isEmpty()) {
			itemDisplayRenderInfo.removeDisplayItem(index);
		} else {
			itemDisplayRenderInfo.setItem(index, item);
			itemDisplayRenderInfo.setRotation(index, rotation);
		}
		CompoundTag renderInfo = getRenderInfoTag().orElse(new CompoundTag());
		renderInfo.put(ITEM_DISPLAY_TAG, itemDisplayRenderInfo.serialize());
		serializeRenderInfo(renderInfo);
		save();
	}

	public void setChangeListener(Consumer<RenderInfo> changeListener) {
		this.changeListener = changeListener;
	}

	protected void save(boolean triggerChangeListener) {
		getSaveHandler.get().run();

		if (triggerChangeListener) {
			changeListener.accept(this);
		}
	}

	protected void save() {
		save(false);
	}

	protected abstract void serializeRenderInfo(CompoundTag renderInfo);

	protected void deserialize() {
		getRenderInfoTag().ifPresent(renderInfoTag -> {
			deserializeItemDisplay(renderInfoTag);
			deserializeUpgrades(renderInfoTag);
			deserializeTanks(renderInfoTag);
			deserializeBattery(renderInfoTag);
		});
		changeListener.accept(this);
	}

	private void deserializeItemDisplay(CompoundTag renderInfoTag) {
		itemDisplayRenderInfo = ItemDisplayRenderInfo.deserialize(renderInfoTag.getCompound(ITEM_DISPLAY_TAG));
	}

	protected abstract Optional<CompoundTag> getRenderInfoTag();

	public Map<UpgradeRenderDataType<?>, IUpgradeRenderData> getUpgradeRenderData() {
		return upgradeData;
	}

	public void removeUpgradeRenderData(UpgradeRenderDataType<?> type) {
		upgradeData.remove(type);
		serializeUpgradeData(upgrades -> upgrades.remove(type.getName()));
		save();
	}

	private void deserializeUpgrades(CompoundTag renderInfoTag) {
		CompoundTag upgrades = renderInfoTag.getCompound(UPGRADES_TAG);
		upgrades.getAllKeys().forEach(key -> {
			if (RENDER_DATA_TYPES.containsKey(key)) {
				UpgradeRenderDataType<?> upgradeRenderDataType = RENDER_DATA_TYPES.get(key);
				upgradeData.put(upgradeRenderDataType, upgradeRenderDataType.deserialize(upgrades.getCompound(key)));
			}
		});
	}

	public CompoundTag getNbt() {
		return getRenderInfoTag().orElse(new CompoundTag());
	}

	public void deserializeFrom(CompoundTag renderInfoNbt) {
		resetUpgradeInfo(false);
		upgradeData.clear();
		serializeRenderInfo(renderInfoNbt);
		deserialize();
	}

	public void resetUpgradeInfo(boolean triggerChangeListener) {
		tankRenderInfos.clear();
		batteryRenderInfo = null;
		getRenderInfoTag().ifPresent(renderInfoTag -> {
			renderInfoTag.remove(TANKS_TAG);
			renderInfoTag.remove(BATTERY_TAG);
		});
		save(triggerChangeListener);
	}

	public void setTankRenderInfo(TankPosition tankPosition, IRenderedTankUpgrade.TankRenderInfo tankRenderInfo) {
		tankRenderInfos.put(tankPosition, tankRenderInfo);
		serializeTank(tankPosition, tankRenderInfo);
		save();
	}

	private void deserializeTanks(CompoundTag renderInfoTag) {
		ListTag tanks = renderInfoTag.getList(TANKS_TAG, Tag.TAG_COMPOUND);
		for (int i = 0; i < tanks.size(); i++) {
			CompoundTag tank = tanks.getCompound(i);
			tankRenderInfos.put(TankPosition.valueOf(tank.getString(TANK_POSITION_TAG).toUpperCase(Locale.ENGLISH)), IRenderedTankUpgrade.TankRenderInfo.deserialize(tank.getCompound(TANK_INFO_TAG)));
		}
	}

	private void deserializeBattery(CompoundTag renderInfoTag) {
		batteryRenderInfo = NBTHelper.getCompound(renderInfoTag, BATTERY_TAG).map(IRenderedBatteryUpgrade.BatteryRenderInfo::deserialize).orElse(null);
	}

	private void serializeTank(TankPosition tankPosition, IRenderedTankUpgrade.TankRenderInfo tankRenderInfo) {
		CompoundTag tankInfo = tankRenderInfo.serialize();

		CompoundTag renderInfo = getRenderInfoTag().orElse(new CompoundTag());
		ListTag tanks = renderInfo.getList(TANKS_TAG, Tag.TAG_COMPOUND);

		boolean infoSet = false;
		for (int i = 0; i < tanks.size(); i++) {
			CompoundTag tank = tanks.getCompound(i);
			if (tank.getString(TANK_POSITION_TAG).equals(tankPosition.getSerializedName())) {
				tank.put(TANK_INFO_TAG, tankInfo);
				infoSet = true;
			}
		}
		if (!infoSet) {
			CompoundTag tankPositionInfo = new CompoundTag();
			tankPositionInfo.putString(TANK_POSITION_TAG, tankPosition.getSerializedName());
			tankPositionInfo.put(TANK_INFO_TAG, tankInfo);
			tanks.add(tankPositionInfo);
			renderInfo.put(TANKS_TAG, tanks);
		}

		serializeRenderInfo(renderInfo);
	}

	public Map<TankPosition, IRenderedTankUpgrade.TankRenderInfo> getTankRenderInfos() {
		return tankRenderInfos;
	}

	public Optional<IRenderedBatteryUpgrade.BatteryRenderInfo> getBatteryRenderInfo() {
		return Optional.ofNullable(batteryRenderInfo);
	}

	public void setBatteryRenderInfo(IRenderedBatteryUpgrade.BatteryRenderInfo batteryRenderInfo) {
		this.batteryRenderInfo = batteryRenderInfo;
		CompoundTag batteryInfo = batteryRenderInfo.serialize();
		CompoundTag renderInfo = getRenderInfoTag().orElse(new CompoundTag());
		renderInfo.put(BATTERY_TAG, batteryInfo);
		serializeRenderInfo(renderInfo);
		save();
	}

	public static class ItemDisplayRenderInfo {
		private static final String ITEMS_TAG = "items";
		private final List<DisplayItem> displayItems;

		private ItemDisplayRenderInfo(DisplayItem displayItem) {
			displayItems = new ArrayList<>();
			displayItems.add(displayItem);
		}

		private ItemDisplayRenderInfo(List<DisplayItem> displayItems) {
			this.displayItems = displayItems;
		}

		public ItemDisplayRenderInfo() {
			this(new ArrayList<>());
		}

		public CompoundTag serialize() {
			CompoundTag ret = new CompoundTag();
			if (displayItems.size() == 1) {
				displayItems.get(0).serialize(ret);
			} else if (displayItems.size() > 1) {
				NBTHelper.putList(ret, ITEMS_TAG, displayItems, displayItem -> displayItem.serialize(new CompoundTag()));
			}
			return ret;
		}

		private void setItem(int index, ItemStack item) {
			if (index > displayItems.size()) {
				return;
			}

			if (item.isEmpty()) {
				displayItems.remove(index);
			} else {
				if (index == displayItems.size()) {
					displayItems.add(new DisplayItem(item, 0, index));
				} else {
					displayItems.get(index).item = item;
				}
			}
		}

		private void removeDisplayItem(int index) {
			displayItems.remove(index);
		}

		public static ItemDisplayRenderInfo deserialize(CompoundTag tag) {
			if (tag.contains(DisplayItem.ITEM_TAG)) {
				return new ItemDisplayRenderInfo(DisplayItem.deserialize(tag));
			} else if (tag.contains(ITEMS_TAG)) {
				List<DisplayItem> items = NBTHelper.getCollection(tag, ITEMS_TAG, Tag.TAG_COMPOUND, stackTag -> Optional.of(DisplayItem.deserialize((CompoundTag) stackTag)), ArrayList::new).orElseGet(ArrayList::new);
				return new ItemDisplayRenderInfo(items);
			}
			return new ItemDisplayRenderInfo();
		}

		private void setRotation(int index, int rot) {
			if (!isValidIndex(displayItems, index)) {
				return;
			}

			displayItems.get(index).rotation = rot;
		}

		public Optional<DisplayItem> getDisplayItem() {
			return isValidIndex(displayItems, 0) ? Optional.of(displayItems.get(0)) : Optional.empty();
		}
		private boolean isValidIndex(List<?> list, int index) {
			return index >= 0 && index < list.size();
		}

		public List<DisplayItem> getDisplayItems() {
			return displayItems;
		}
	}
	public static class DisplayItem {
		private static final String ITEM_TAG = "item";
		private static final String ROTATION_TAG = "rotation";
		private static final String SLOT_INDEX_TAG = "slotIndex";
		private ItemStack item;
		private int rotation;
		private int slotIndex;

		public DisplayItem(ItemStack item, int rotation, int slotIndex) {
			this.item = item;
			this.rotation = rotation;
			this.slotIndex = slotIndex;
		}

		private CompoundTag serialize(CompoundTag tag) {
			tag.put(ITEM_TAG, item.serializeNBT());
			tag.putInt(ROTATION_TAG, rotation);
			tag.putInt(SLOT_INDEX_TAG, slotIndex);
			return tag;
		}

		private static DisplayItem deserialize(CompoundTag tag) {
			return new DisplayItem(ItemStack.of(tag.getCompound(ITEM_TAG)), tag.getInt(ROTATION_TAG), tag.getInt(SLOT_INDEX_TAG));
		}

		public ItemStack getItem() {
			return item;
		}

		public int getRotation() {
			return rotation;
		}

		public int getSlotIndex() {
			return slotIndex;
		}
	}
}
