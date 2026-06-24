package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeClearRecipe;

import java.util.concurrent.CompletableFuture;

public class CoreRecipeProvider extends RecipeProvider {
	public CoreRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
		super(provider, recipeOutput);
	}

	@Override
	protected void buildRecipes() {
		HolderLookup.RegistryLookup<Item> items = registries.lookupOrThrow(Registries.ITEM);
		SpecialRecipeBuilder.special(UpgradeClearRecipe::new).save(output,
				ResourceKey.create(Registries.RECIPE, SophisticatedCore.getIdentifier("upgrade_clear")));
	}

	public static class Runner extends RecipeProvider.Runner {
		protected Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
			super(packOutput, registries);
		}

		@Override
		protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
			return new CoreRecipeProvider(provider, recipeOutput);
		}

		@Override
		public String getName() {
			return "Sophisticated Core Recipes";
		}
	}
}
