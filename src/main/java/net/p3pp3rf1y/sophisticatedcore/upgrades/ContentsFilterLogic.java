package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ContentsFilterLogic extends FilterLogic {

	private final Supplier<InventoryHandler> getInventoryHandler;
	private final MemorySettingsCategory memorySettings;

	public ContentsFilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int filterSlotCount, Supplier<InventoryHandler> getInventoryHandler,
			MemorySettingsCategory memorySettings, DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> filterAttributesComponent) {
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
	protected boolean matchesFilter(Stream<TagKey<Item>> tags, Item item, int damageValue, boolean empty, DataComponentMap components) {
		if (!shouldFilterByStorage()) {
			return super.matchesFilter(tags, item, damageValue, empty, components);
		}
		for (ItemStackKey filterStack : getInventoryHandler.get().getSlotTracker().getFullStacks()) {
			if (stackMatchesFilter(filterStack.stack(), item, damageValue, empty, components)) {
				return true;
			}
		}
		for (ItemStackKey filterStack : getInventoryHandler.get().getSlotTracker().getPartialStacks()) {
			if (stackMatchesFilter(filterStack.stack(), item, damageValue, empty, components)) {
				return true;
			}
		}
		return memorySettings.matchesFilter(item, components);
	}

	private void setFilterByStorage(boolean filterByStorage) {
		setAttributes(attributes -> attributes.setFilterByStorage(filterByStorage));
		save();
	}

	protected boolean shouldFilterByStorage() {
		return getAttributes().filterByStorage();
	}
}
