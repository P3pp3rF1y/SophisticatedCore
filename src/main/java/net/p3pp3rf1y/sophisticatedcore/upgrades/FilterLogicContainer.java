package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.FilterSlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FilterLogicContainer<T extends FilterLogic> extends FilterLogicContainerBase<T, FilterLogicContainer.FilterLogicSlot> {
	public FilterLogicContainer(Supplier<T> filterLogic, IServerUpdater serverUpdater, Consumer<Slot> addSlot) {
		this(filterLogic, serverUpdater, addSlot, (slot, button) -> {
		});
	}

	public FilterLogicContainer(Supplier<T> filterLogic, IServerUpdater serverUpdater, Consumer<Slot> addSlot, BiConsumer<Integer, Integer> onFilterSlotSet) {
		super(serverUpdater, filterLogic, addSlot);
		ItemStacksResourceHandler filterHandler = filterLogic.get().getFilterHandler();
		for (int slot = 0; slot < filterHandler.size(); slot++) {
			FilterLogicSlot filterSlot = new FilterLogicSlot(() -> filterLogic.get().getFilterHandler(), slot, onFilterSlotSet);
			addSlot.accept(filterSlot);
			filterSlots.add(filterSlot);
		}
	}

	public static class FilterLogicSlot extends FilterSlotItemHandler {
		private boolean enabled = true;
		private final BiConsumer<Integer, Integer> onSet;

		public FilterLogicSlot(Supplier<ResourceHandler<ItemResource>> filterHandler, int slot, BiConsumer<Integer, Integer> onSet) {
			super(filterHandler, slot, -100, -100);
			this.onSet = onSet;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public boolean isActive() {
			return enabled;
		}

		@Override
		public void onItemSet(int button) {
			onSet.accept(this.slot, button);
		}
	}
}
