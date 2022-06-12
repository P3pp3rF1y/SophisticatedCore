package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;

@SuppressWarnings("unused")
@JeiPlugin
public class CorePlugin implements IModPlugin {
	@Override
	public ResourceLocation getPluginUid() {
		return new ResourceLocation(SophisticatedCore.MOD_ID, "default");
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(RecipeTypes.CRAFTING, UpgradeNextTierRecipe.REGISTERED_RECIPES.values().stream().map(CorePlugin::copyShapedRecipe).toList());
	}

	public static CraftingRecipe copyShapedRecipe(ShapedRecipe recipe) {
		return new ShapedRecipe(recipe.getId(), "", recipe.getRecipeWidth(), recipe.getRecipeHeight(), recipe.getIngredients(), recipe.getResultItem());
	}
}
