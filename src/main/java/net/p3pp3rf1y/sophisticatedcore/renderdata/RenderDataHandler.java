package net.p3pp3rf1y.sophisticatedcore.renderdata;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
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

	private Consumer<RenderDataHandler> renderUpdateChangeListener = ri -> {
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
		renderData = renderData.withUpgradeItems(upgradeItems);
		save();
	}

	public <T extends IUpgradeClientData> void setUpgradeClientData(UpgradeClientDataType<T> upgradeClientDataType, T clientData) {
		renderData = renderData.withUpgradeClientData(upgradeClientDataType, clientData);
		save();
	}

	public <T extends IUpgradeClientData> Optional<T> getUpgradeClientData(UpgradeClientDataType<T> upgradeClientDataType) {
		if (!renderData.upgradeData().containsKey(upgradeClientDataType)) {
			return Optional.empty();
		}
		return upgradeClientDataType.cast(renderData.upgradeData().get(upgradeClientDataType));
	}

	public void refreshDisplayData(List<RenderData.DisplayItemData> displayItems, List<Integer> inaccessibleSlots, List<Integer> infiniteSlots, List<Integer> slotCounts, List<Float> slotFillRatios) {
		renderData = renderData.withDisplayData(displayItems, inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
		save();
		renderUpdateChangeListener.accept(this);
	}

	public void refreshDisplayItemsAndInaccessibleSlots(List<RenderData.DisplayItemData> displayItems, List<Integer> inaccessibleSlots) {
		renderData = renderData.withDisplayItemsAndInaccessibleSlots(displayItems, inaccessibleSlots);
		save();
		renderUpdateChangeListener.accept(this);
	}

	public void refreshSlotCountsFillRatiosAndInfiniteSlots(List<Integer> slotCounts, List<Float> slotFillRatios, List<Integer> infiniteSlots) {
		renderData = renderData.withSlotCountsFillRatiosAndInfiniteSlots(slotCounts, slotFillRatios, infiniteSlots);
		save();
	}

	public void setRenderUpdateChangeListener(Consumer<RenderDataHandler> renderUpdateChangeListener) {
		this.renderUpdateChangeListener = renderUpdateChangeListener;
	}

	@Deprecated
	public void setDisplayItemsChangeListener(Consumer<RenderDataHandler> renderUpdateChangeListener) {
		setRenderUpdateChangeListener(renderUpdateChangeListener);
	}

	protected void save(boolean triggerChangeListener) {
		saveHandler.accept(renderData);

		if (triggerChangeListener) {
			renderUpdateChangeListener.accept(this);
		}
	}

	protected void save() {
		save(false);
	}

	public Map<UpgradeClientDataType<?>, IUpgradeClientData> getUpgradeClientData() {
		return renderData.upgradeData();
	}

	public void removeAllUpgradeClientData() {
		renderData = renderData.withoutAllUpgradeData();
		save();
	}

	public void removeUpgradeClientData(UpgradeClientDataType<?> type) {
		renderData = renderData.withoutUpgradeData(type);
		save();
	}

	public RenderData getData() {
		return renderData;
	}

	public void validate(IStorageWrapper storageWrapper, Level level) {
		RenderData validated = renderData.validated(storageWrapper, level);
		if (validated != renderData) {
			renderData = validated;
			save();
		}
	}

	public void reloadFrom(RenderData renderData) {
		this.renderData = renderData.copy();
	}

	public void resetUpgradeInfo(boolean triggerChangeListener) {
		renderData = renderData.withoutUpgradeRenderInfo();
		save(triggerChangeListener);
	}

	public void setTankRenderData(TankPosition tankPosition, RenderData.TankRenderData data) {
		renderData = renderData.withTank(tankPosition, data);
		save();
	}

	public Map<TankPosition, RenderData.TankRenderData> getTankRenderData() {
		return renderData.tanks();
	}

	public Optional<RenderData.BatteryRenderData> getBatteryRenderData() {
		return renderData.battery();
	}

	public void setBatteryRenderData(RenderData.BatteryRenderData batteryRenderData) {
		renderData = renderData.withBattery(batteryRenderData);
		save();
	}

	public List<ItemStack> getUpgradeItems() {
		return renderData.getUpgradeItemStacks();
	}

	public boolean showsCountsAndFillRatios() {
		return showsCountsAndFillRatios;
	}
}
