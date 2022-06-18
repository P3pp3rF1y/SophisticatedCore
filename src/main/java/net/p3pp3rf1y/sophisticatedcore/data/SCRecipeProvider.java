package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;

import java.util.function.Consumer;

public class SCRecipeProvider extends RecipeProvider {
	public SCRecipeProvider(DataGenerator pGenerator) {
		super(pGenerator);
	}

	@Override
	protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
		SpecialRecipeBuilder.special(ModRecipes.UPGRADE_CLEAR_SERIALIZER.get()).save(consumer, SophisticatedCore.getRegistryName("upgrade_clear"));
	}
}
