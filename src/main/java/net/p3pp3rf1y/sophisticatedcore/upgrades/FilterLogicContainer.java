package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.FilterSlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

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
		ItemStackHandler filterHandler = filterLogic.get().getFilterHandler();
		InventoryHelper.iterate(filterHandler, (slot, stack) -> {
			FilterLogicSlot filterSlot = new FilterLogicSlot(() -> filterLogic.get().getFilterHandler(), slot, onFilterSlotSet);
			addSlot.accept(filterSlot);
			filterSlots.add(filterSlot);
		});
	}

	public static class FilterLogicSlot extends FilterSlotItemHandler {
		private boolean enabled = true;
		private final BiConsumer<Integer, Integer> onSet;

		public FilterLogicSlot(Supplier<IItemHandler> filterHandler, Integer slot) {
			this(filterHandler, slot, (ignored, button) -> {
			});
		}

		public FilterLogicSlot(Supplier<IItemHandler> filterHandler, Integer slot, BiConsumer<Integer, Integer> onSet) {
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
			onSet.accept(getContainerSlot(), button);
		}
	}
}
