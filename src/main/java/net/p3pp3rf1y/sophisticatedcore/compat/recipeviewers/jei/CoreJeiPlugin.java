package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;

@SuppressWarnings("unused")
@JeiPlugin
public class CoreJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "default");
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		IModPlugin.super.registerGuiHandlers(registration);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		JeiRecipeDisplayGenerator generator = new JeiRecipeDisplayGenerator();
		ClientRecipeHelper.addAllRecipesOfType(generator, RecipeType.CRAFTING, UpgradeNextTierRecipe.class);
		registration.addRecipes(RecipeTypes.CRAFTING, generator.getCraftingRecipes());
	}
}
