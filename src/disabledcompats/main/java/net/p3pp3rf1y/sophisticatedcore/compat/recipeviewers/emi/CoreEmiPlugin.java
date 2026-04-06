package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;

@SuppressWarnings("unused")
@EmiEntrypoint
public class CoreEmiPlugin implements EmiPlugin {
	@Override
	public void register(EmiRegistry registry) {
		ClientRecipeHelper.addAllRecipesOfType(new EmiRecipeDisplayGenerator(registry), RecipeType.CRAFTING, UpgradeNextTierRecipe.class);
	}
}
