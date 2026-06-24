package net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SimpleItemContent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class BlockConverterUpgradeWrapper<U extends BlockConverterUpgradeItem<U, W>, W extends BlockConverterUpgradeWrapper<U, W>>
		extends
			UpgradeWrapperBase<W, U> {
	private final ItemStacksResourceHandler inputInventory;

	public BlockConverterUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		inputInventory = new ItemStacksResourceHandler(1) {
			@Override
			protected void onContentsChanged(int slot, ItemStack previousContents) {
				super.onContentsChanged(slot, previousContents);
				if (slot == 0) {
					ItemStack inputStack = stacks.getFirst();
					if (inputStack.isEmpty()) {
						upgrade.remove(ModCoreDataComponents.INPUT_ITEM);
					} else {
						upgrade.set(ModCoreDataComponents.INPUT_ITEM, SimpleItemContent.copyOf(inputStack));
					}
				}
				save();
			}
		};
		ItemStack inputItem = upgrade.getOrDefault(ModCoreDataComponents.INPUT_ITEM, SimpleItemContent.EMPTY).copy();
		inputInventory.set(0, ItemResource.of(inputItem), inputItem.getCount());
	}

	public ResourceHandler<ItemResource> getInputInventory() {
		return inputInventory;
	}

	public void setRecipeId(@Nullable ResourceKey<Recipe<?>> recipeId) {
		if (recipeId == null) {
			upgrade.remove(ModCoreDataComponents.RECIPE_ID);
			return;
		}
		upgrade.set(ModCoreDataComponents.RECIPE_ID, recipeId);
		save();
	}

	public Optional<ResourceKey<Recipe<?>>> getRecipeId() {
		return Optional.ofNullable(upgrade.get(ModCoreDataComponents.RECIPE_ID));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgrade.getOrDefault(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, true);
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgrade.set(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage);
		save();
	}

	public boolean shouldRefillInput() {
		return upgrade.getOrDefault(ModCoreDataComponents.REFILL_INPUT, false);
	}

	public void setRefillInput(boolean refillInput) {
		upgrade.set(ModCoreDataComponents.REFILL_INPUT, refillInput);
		save();
	}

	public ItemStack extractFromStorage(ItemStack stack, boolean simulate) {
		int extracted = simulate
				? InventoryHelper.simulateExtractExact(storageWrapper.getInventoryHandler(), ItemResource.of(stack), stack.getCount())
				: InventoryHelper.extract(storageWrapper.getInventoryHandler(), ItemResource.of(stack), stack.getCount());
		return extracted == 0 ? ItemStack.EMPTY : stack.copyWithCount(extracted);
	}
}
