package net.p3pp3rf1y.sophisticatedcore.renderdata;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeClientData;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class RenderDataHandler {
	private static final Map<String, UpgradeClientDataType<?>> CLIENT_DATA_TYPES;

	static {
		CLIENT_DATA_TYPES = Map.of(
				CookingUpgradeClientData.TYPE.getName(), CookingUpgradeClientData.TYPE,
				JukeboxUpgradeClientData.TYPE.getName(), JukeboxUpgradeClientData.TYPE
		);
	}

	private final Consumer<RenderData> saveHandler;
	private final boolean showsCountsAndFillRatios;

	private Consumer<RenderDataHandler> displayItemsChangeListener = ri -> {
	};

	private RenderData renderData;

	public RenderDataHandler(RenderData renderData, Consumer<RenderData> saveHandler) {
		this(renderData, saveHandler, false);
	}

	public RenderDataHandler(RenderData renderData, Consumer<RenderData> saveHandler, boolean showsCountsAndFillRatios) {
		this.renderData = renderData;
		this.saveHandler = saveHandler;
		this.showsCountsAndFillRatios = showsCountsAndFillRatios;
	}

	public RenderData.DisplayData getDisplayData() {
		return renderData.display();
	}

	public void setUpgradeItems(List<ItemStack> upgradeItems) {
		renderData.setUpgradeItems(upgradeItems);
		save();
	}

	public <T extends IUpgradeClientData> void setUpgradeClientData(UpgradeClientDataType<T> upgradeClientDataType, T clientData) {
		renderData.putUpgradeData(upgradeClientDataType, clientData);
		save();
	}

	public <T extends IUpgradeClientData> Optional<T> getUpgradeClientData(UpgradeClientDataType<T> upgradeClientDataType) {
		if (!renderData.upgradeData().containsKey(upgradeClientDataType)) {
			return Optional.empty();
		}
		return upgradeClientDataType.cast(renderData.upgradeData().get(upgradeClientDataType));
	}

	public void refreshDisplayData(List<RenderData.DisplayItemData> displayItems, List<Integer> inaccessibleSlots, List<Integer> infiniteSlots, List<Integer> slotCounts, List<Float> slotFillRatios) {
		renderData.display().refreshData(displayItems, inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
		save();
		displayItemsChangeListener.accept(this);
	}

	public void refreshDisplayItemsAndInaccessibleSlots(List<RenderData.DisplayItemData> displayItems, List<Integer> inaccessibleSlots) {
		renderData.display().refreshDisplayItemsAndInaccessibleSlots(displayItems, inaccessibleSlots);
		save();
		displayItemsChangeListener.accept(this);
	}

	public void refreshSlotCountsFillRatiosAndInfiniteSlots(List<Integer> slotCounts, List<Float> slotFillRatios, List<Integer> infiniteSlots) {
		renderData.display().refreshSlotCountsFillRatiosAndInfiniteSlots(infiniteSlots, slotCounts, slotFillRatios);
		save();
	}

	public void setDisplayItemsChangeListener(Consumer<RenderDataHandler> displayItemsChangeListener) {
		this.displayItemsChangeListener = displayItemsChangeListener;
	}

	protected void save(boolean triggerChangeListener) {
		saveHandler.accept(renderData);

		if (triggerChangeListener) {
			displayItemsChangeListener.accept(this);
		}
	}

	protected void save() {
		save(false);
	}

	public Map<UpgradeClientDataType<?>, IUpgradeClientData> getUpgradeClientData() {
		return renderData.upgradeData();
	}

	public void removeAllUpgradeClientData() {
		renderData.removeAllUpgradeData();
		save();
	}

	public void removeUpgradeClientData(UpgradeClientDataType<?> type) {
		renderData.removeUpgradeData(type);
		save();
	}

	public RenderData getData() {
		return renderData;
	}

	public void reloadFrom(RenderData renderData) {
		this.renderData = renderData;
	}

	public void resetUpgradeInfo(boolean triggerChangeListener) {
		renderData.clearTanks();
		renderData.clearBattery();
		save(triggerChangeListener);
	}

	public void setTankRenderData(TankPosition tankPosition, RenderData.TankRenderData data) {
		renderData.setTank(tankPosition, data);
		save();
	}

	public Map<TankPosition, RenderData.TankRenderData> getTankRenderData() {
		return renderData.tanks();
	}

	public Optional<RenderData.BatteryRenderData> getBatteryRenderData() {
		return renderData.battery();
	}

	public void setBatteryRenderData(RenderData.BatteryRenderData batteryRenderData) {
		renderData.setBattery(batteryRenderData);
		save();
	}

	public List<ItemStack> getUpgradeItems() {
		return renderData.upgradeItems();
	}

	public boolean showsCountsAndFillRatios() {
		return showsCountsAndFillRatios;
	}
}
