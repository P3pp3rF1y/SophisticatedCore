package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ContentsFilterLogic extends FilterLogic {

	private final Supplier<InventoryHandler> getInventoryHandler;
	private final MemorySettingsCategory memorySettings;

	public ContentsFilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int filterSlotCount, Supplier<InventoryHandler> getInventoryHandler, MemorySettingsCategory memorySettings, DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> filterAttributesComponent) {
		super(upgrade, saveHandler, filterSlotCount, filterAttributesComponent);
		this.getInventoryHandler = getInventoryHandler;
		this.memorySettings = memorySettings;
	}

	public ContentsFilterType getFilterType() {
		if (shouldFilterByStorage()) {
			return ContentsFilterType.STORAGE;
		}
		return isAllowList() ? ContentsFilterType.ALLOW : ContentsFilterType.BLOCK;
	}

	public void setDepositFilterType(ContentsFilterType contentsFilterType) {
		switch (contentsFilterType) {
			case ALLOW -> {
				setFilterByStorage(false);
				setAllowList(true);
			}
			case BLOCK -> {
				setFilterByStorage(false);
				setAllowList(false);
			}
			case STORAGE -> {
				setFilterByStorage(true);
				save();
			}
		}
	}

	@Override
	public boolean matchesFilter(ItemStack stack) {
		if (!shouldFilterByStorage()) {
			return super.matchesFilter(stack);
		}

		for (ItemStackKey filterStack : getInventoryHandler.get().getSlotTracker().getFullStacks()) {
			if (stackMatchesFilter(stack, filterStack.getStack())) {
				return true;
			}
		}
		for (ItemStackKey filterStack : getInventoryHandler.get().getSlotTracker().getPartialStacks()) {
			if (stackMatchesFilter(stack, filterStack.getStack())) {
				return true;
			}
		}
		return memorySettings.matchesFilter(stack);
	}

	private void setFilterByStorage(boolean filterByStorage) {
		setAttributes(attributes -> attributes.setFilterByStorage(filterByStorage));
		save();
	}

	protected boolean shouldFilterByStorage() {
		return getAttributes().filterByStorage();
	}
}
