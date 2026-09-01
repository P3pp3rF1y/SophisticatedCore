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
		CLIENT_DATA_TYPES = Map.of(CookingUpgradeClientData.TYPE.getName(), CookingUpgradeClientData.TYPE, JukeboxUpgradeClientData.TYPE.getName(),
				JukeboxUpgradeClientData.TYPE);
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
		update(renderData.withUpgradeItems(upgradeItems), false);
	}

	public <T extends IUpgradeClientData> void setUpgradeClientData(UpgradeClientDataType<T> upgradeClientDataType, T clientData) {
		update(renderData.withUpgradeClientData(upgradeClientDataType, clientData), false);
	}

	public <T extends IUpgradeClientData> Optional<T> getUpgradeClientData(UpgradeClientDataType<T> upgradeClientDataType) {
		if (!renderData.upgradeData().containsKey(upgradeClientDataType)) {
			return Optional.empty();
		}
		return upgradeClientDataType.cast(renderData.upgradeData().get(upgradeClientDataType));
	}

	public void refreshDisplayData(List<RenderData.DisplayItemData> displayItems, List<Integer> inaccessibleSlots, List<Integer> infiniteSlots,
			List<Integer> slotCounts, List<Float> slotFillRatios) {
		update(renderData.withDisplayData(displayItems, inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios), true);
	}

	public void refreshDisplayItemsAndInaccessibleSlots(List<RenderData.DisplayItemData> displayItems, List<Integer> inaccessibleSlots) {
		update(renderData.withDisplayItemsAndInaccessibleSlots(displayItems, inaccessibleSlots), true);
	}

	public void refreshSlotCountsFillRatiosAndInfiniteSlots(List<Integer> slotCounts, List<Float> slotFillRatios, List<Integer> infiniteSlots) {
		update(renderData.withSlotCountsFillRatiosAndInfiniteSlots(slotCounts, slotFillRatios, infiniteSlots), false);
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
		update(renderData.withoutAllUpgradeData(), false);
	}

	public void removeUpgradeClientData(UpgradeClientDataType<?> type) {
		update(renderData.withoutUpgradeData(type), false);
	}

	public RenderData getData() {
		return renderData;
	}

	public void validate(IStorageWrapper storageWrapper, Level level) {
		update(renderData.validated(storageWrapper, level), false);
	}

	public void reloadFrom(RenderData renderData) {
		this.renderData = renderData.copy();
	}

	public void resetUpgradeInfo(boolean triggerChangeListener) {
		update(renderData.withoutUpgradeRenderInfo(), triggerChangeListener);
	}

	public void setTankRenderData(TankPosition tankPosition, RenderData.TankRenderData data) {
		update(renderData.withTank(tankPosition, data), true);
	}

	public Map<TankPosition, RenderData.TankRenderData> getTankRenderData() {
		return renderData.tanks();
	}

	public Optional<RenderData.BatteryRenderData> getBatteryRenderData() {
		return renderData.battery();
	}

	public void setBatteryRenderData(RenderData.BatteryRenderData batteryRenderData) {
		update(renderData.withBattery(batteryRenderData), true);
	}

	public List<ItemStack> getUpgradeItems() {
		return renderData.getUpgradeItemStacks();
	}

	public boolean showsCountsAndFillRatios() {
		return showsCountsAndFillRatios;
	}

	private void update(RenderData updatedRenderData, boolean triggerChangeListener) {
		if (updatedRenderData.equals(renderData)) {
			return;
		}

		renderData = updatedRenderData;
		save(triggerChangeListener);
	}
}
