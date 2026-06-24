package net.p3pp3rf1y.sophisticatedcore.renderdata;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

class RenderDataHandlerTest {
	@Test
	void setUpgradeItemsDoesNotSaveWhenDataIsUnchanged() {
		AtomicInteger saves = new AtomicInteger();
		RenderDataHandler renderDataHandler = new RenderDataHandler(new RenderData(), renderData -> saves.incrementAndGet());

		renderDataHandler.setUpgradeItems(List.of());

		Assertions.assertEquals(0, saves.get(), "No-op upgrade item update should not trigger save");
	}

	@Test
	void setUpgradeItemsPreservesEmptySlots() {
		RenderDataHandler renderDataHandler = new RenderDataHandler(new RenderData(), renderData -> {
		});

		renderDataHandler.setUpgradeItems(List.of(ItemStack.EMPTY, ItemStack.EMPTY));

		Assertions.assertEquals(List.of(ItemStack.EMPTY, ItemStack.EMPTY), renderDataHandler.getUpgradeItems(),
				"Empty upgrade slots should be preserved in render data");
	}

	@Test
	void resetUpgradeInfoDoesNotTriggerChangeListenerWhenDataIsUnchanged() {
		AtomicInteger saves = new AtomicInteger();
		AtomicInteger changeListenerCalls = new AtomicInteger();
		RenderDataHandler renderDataHandler = new RenderDataHandler(new RenderData(), renderData -> saves.incrementAndGet());
		renderDataHandler.setRenderUpdateChangeListener(handler -> changeListenerCalls.incrementAndGet());

		renderDataHandler.resetUpgradeInfo(true);

		Assertions.assertEquals(0, saves.get(), "No-op reset should not trigger save");
		Assertions.assertEquals(0, changeListenerCalls.get(), "No-op reset should not trigger render update listener");
	}

	@Test
	void setBatteryRenderDataSavesWhenDataChanges() {
		AtomicInteger saves = new AtomicInteger();
		RenderDataHandler renderDataHandler = new RenderDataHandler(new RenderData(), renderData -> saves.incrementAndGet());

		renderDataHandler.setBatteryRenderData(new RenderData.BatteryRenderData(0.5f));

		Assertions.assertEquals(1, saves.get(), "Changed battery render data should trigger save");
		Assertions.assertEquals(Optional.of(new RenderData.BatteryRenderData(0.5f)), renderDataHandler.getBatteryRenderData(),
				"Battery render data should be updated");
	}
}
