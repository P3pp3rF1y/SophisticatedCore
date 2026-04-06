package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;

@SuppressWarnings("unused")
@REIPluginClient
public class CoreReiClientPlugin implements REIClientPlugin {
	@Override
	public void registerDisplays(DisplayRegistry registry) {
		ClientRecipeHelper.addAllRecipesOfType(new ReiRecipeDisplayGenerator(registry), RecipeType.CRAFTING, UpgradeNextTierRecipe.class);
	}
}
