package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import earth.terrarium.chipped.recipe.ChippedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.Optional;
import java.util.function.Consumer;

public class BlockTransformationUpgradeWrapper extends UpgradeWrapperBase<BlockTransformationUpgradeWrapper, BlockTransformationUpgradeItem> {
	private static final String RESULT_TAG = "result";
	private final IItemHandlerModifiable inputInventory;
	private final RecipeType<ChippedRecipe> recipeType;

	protected BlockTransformationUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		inputInventory = new ItemStackHandler(1) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				if (slot == 0) {
					upgrade.addTagElement("input", getStackInSlot(0).serializeNBT());
				}
				save();
			}
		};
		NBTHelper.getCompound(upgrade, "input").ifPresent(tag -> inputInventory.setStackInSlot(0, ItemStack.of(tag)));
		recipeType = upgradeItem.getRecipeType();
	}

	public IItemHandlerModifiable getInputInventory() {
		return inputInventory;
	}

	public void setResult(ItemStack result) {
		if (result.isEmpty()) {
			NBTHelper.removeTag(upgrade, RESULT_TAG);
			return;
		}

		upgrade.getOrCreateTag().put(RESULT_TAG, result.serializeNBT());
		save();
	}

	public Optional<ItemStack> getResult() {
		return NBTHelper.getCompound(upgrade, RESULT_TAG).map(ItemStack::of);
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public boolean shouldShiftClickIntoStorage() {
		return NBTHelper.getBoolean(upgrade, "shiftClickIntoStorage").orElse(true);
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		NBTHelper.setBoolean(upgrade, "shiftClickIntoStorage", shiftClickIntoStorage);
		save();
	}

	public RecipeType<ChippedRecipe> getRecipeType() {
		return recipeType;
	}
}
