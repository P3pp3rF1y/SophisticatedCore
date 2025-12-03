package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import earth.terrarium.chipped.common.recipes.ChippedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SimpleItemContent;

import java.util.Optional;
import java.util.function.Consumer;

public class BlockTransformationUpgradeWrapper extends UpgradeWrapperBase<BlockTransformationUpgradeWrapper, BlockTransformationUpgradeItem> {
	private final ResourceHandler<ItemResource> inputInventory;
	private final RecipeType<ChippedRecipe> recipeType;

	protected BlockTransformationUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		inputInventory = new ItemStacksResourceHandler(1) {
			@Override
			protected void onContentsChanged(int slot, ItemStack previousContents) {
				super.onContentsChanged(slot, previousContents);
				if (slot == 0) {
					upgrade.set(ModCoreDataComponents.INPUT_ITEM, SimpleItemContent.copyOf(getResource(0).toStack(getAmountAsInt(0))));
				}
				save();
			}
		};
		ItemStack inputItem = upgrade.getOrDefault(ModCoreDataComponents.INPUT_ITEM, SimpleItemContent.EMPTY).copy();
		InventoryHelper.set(inputInventory, 0, ItemResource.of(inputItem), inputItem.getCount());
		recipeType = upgradeItem.getRecipeType();
	}

	public ResourceHandler<ItemResource> getInputInventory() {
		return inputInventory;
	}

	public void setResult(ItemStack result) {
		if (result.isEmpty()) {
			upgrade.remove(ModCoreDataComponents.RESULT_ITEM);
			return;
		}

		upgrade.set(ModCoreDataComponents.RESULT_ITEM, SimpleItemContent.copyOf(result));
		save();
	}

	public Optional<SimpleItemContent> getResult() {
		return Optional.ofNullable(upgrade.get(ModCoreDataComponents.RESULT_ITEM));
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

	public RecipeType<ChippedRecipe> getRecipeType() {
		return recipeType;
	}
}
