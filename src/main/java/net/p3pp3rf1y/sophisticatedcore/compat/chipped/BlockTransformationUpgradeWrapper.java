package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import earth.terrarium.chipped.common.recipes.ChippedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.SimpleItemContent;

import java.util.Optional;
import java.util.function.Consumer;

public class BlockTransformationUpgradeWrapper extends UpgradeWrapperBase<BlockTransformationUpgradeWrapper, BlockTransformationUpgradeItem> {
	private final IItemHandlerModifiable inputInventory;
	private final RecipeType<ChippedRecipe> recipeType;

	protected BlockTransformationUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		inputInventory = new ItemStackHandler(1) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				if (slot == 0) {
					upgrade.set(ModCoreDataComponents.INPUT_ITEM, SimpleItemContent.copyOf(getStackInSlot(0)));
				}
				save();
			}
		};
		inputInventory.setStackInSlot(0, upgrade.getOrDefault(ModCoreDataComponents.INPUT_ITEM, SimpleItemContent.EMPTY).copy());
		recipeType = upgradeItem.getRecipeType();
	}

	public IItemHandlerModifiable getInputInventory() {
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
