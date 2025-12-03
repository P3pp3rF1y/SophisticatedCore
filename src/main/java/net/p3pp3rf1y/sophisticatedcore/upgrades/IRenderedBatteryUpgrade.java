package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;

import java.util.function.Consumer;

public interface IRenderedBatteryUpgrade {
	void setBatteryRenderDataUpdateCallback(Consumer<RenderData.BatteryRenderData> updateTankRenderDataCallback);

	void forceUpdateBatteryRenderData();
}
