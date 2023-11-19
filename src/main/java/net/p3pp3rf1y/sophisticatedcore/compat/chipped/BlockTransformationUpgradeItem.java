package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import earth.terrarium.chipped.recipe.ChippedRecipe;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.function.Supplier;

public class BlockTransformationUpgradeItem extends UpgradeItemBase<BlockTransformationUpgradeWrapper> {
	private static final UpgradeType<BlockTransformationUpgradeWrapper> TYPE = new UpgradeType<>(BlockTransformationUpgradeWrapper::new);
	private final Supplier<RecipeType<ChippedRecipe>> getRecipeType;
	public BlockTransformationUpgradeItem(CreativeModeTab itemGroup, Supplier<RecipeType<ChippedRecipe>> getRecipeType) {super(itemGroup);
		this.getRecipeType = getRecipeType;
	}

	@Override
	public UpgradeType<BlockTransformationUpgradeWrapper> getType() {
		return TYPE;
	}

	public RecipeType<ChippedRecipe> getRecipeType() {
		return getRecipeType.get();
	}

}
