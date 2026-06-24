package net.p3pp3rf1y.sophisticatedcore.compat.sawmill;

import net.mehvahdjukaar.sawmill.RecipeSorter;
import net.mehvahdjukaar.sawmill.SawmillMod;
import net.mehvahdjukaar.sawmill.WoodcuttingRecipe;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterRecipeContainer;

import java.util.List;
import java.util.function.Consumer;

public class SawmillRecipeContainer
		extends
			BlockConverterRecipeContainer<WoodcuttingRecipe, SawmillUpgradeItem.Wrapper, SawmillRecipeContainer, SawmillUpgradeContainer> {
	private int maxInputCount;

	public SawmillRecipeContainer(SawmillUpgradeContainer upgradeContainer, Consumer<Slot> addSlot, IServerUpdater serverUpdater,
			ContainerLevelAccess worldPosCallable, Level level) {
		super(upgradeContainer, addSlot, serverUpdater, worldPosCallable, level, SawmillMod.SAWMILL_TAKE.get());
	}

	@Override
	protected RecipeType<WoodcuttingRecipe> getRecipeType() {
		return SawmillMod.WOODCUTTING_RECIPE.get();
	}

	@Override
	protected List<RecipeHolder<WoodcuttingRecipe>> filterAndSortRecipes(List<RecipeHolder<WoodcuttingRecipe>> recipes) {
		recipes.removeIf(r -> r.value().result.is(SawmillMod.BLACKLIST));
		RecipeSorter.sort(recipes, level);
		recipes = recipes.subList(0, Math.min(recipes.size(), 255));
		this.maxInputCount = recipes.stream().mapToInt(r -> r.value().getInputCount()).max().orElse(0);
		return recipes;
	}

	@Override
	protected boolean shouldRefreshRecipes(ItemStack itemstack, boolean countIncreased) {
		return super.shouldRefreshRecipes(itemstack, countIncreased) || itemstack.getCount() < maxInputCount || countIncreased;
	}

	@Override
	protected int getInputCount() {
		if (getSelectedRecipe() < 0 || getSelectedRecipe() >= getRecipeList().size()) {
			return 0;
		}

		return getRecipeList().get(getSelectedRecipe()).value().getInputCount();
	}
}
