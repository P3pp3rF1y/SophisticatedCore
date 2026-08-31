package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerClearRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerEndpointRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.ItemEnabledCondition;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeClearRecipe;
import net.p3pp3rf1y.sophisticatedcore.init.ModItems;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CoreRecipeProvider extends RecipeProvider {
	public CoreRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
		super(provider, recipeOutput);
	}

	@Override
	protected void buildRecipes() {
		HolderLookup.RegistryLookup<Item> items = registries.lookupOrThrow(Registries.ITEM);
		SpecialRecipeBuilder.special(UpgradeClearRecipe::new).save(output, ResourceKey.create(Registries.RECIPE, SophisticatedCore.getRL("upgrade_clear")));
		RecipeOutput enderLinkOutput = output
				.withConditions(new OrCondition(List.of(new ModLoadedCondition("sophisticatedbackpacks"), new ModLoadedCondition("sophisticatedstorage"))));
		SpecialRecipeBuilder.special(EnderLinkerEndpointRecipe::new).save(enderLinkOutput,
				ResourceKey.create(Registries.RECIPE, SophisticatedCore.getRL("ender_linker_endpoint")));
		SpecialRecipeBuilder.special(EnderLinkerClearRecipe::new).save(enderLinkOutput,
				ResourceKey.create(Registries.RECIPE, SophisticatedCore.getRL("ender_linker_clear")));
		ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.ENDER_LINKER.get()).pattern("OEO").pattern("BOB").pattern("OEO")
				.define('O', Items.OBSIDIAN).define('E', Items.ENDER_PEARL).define('B', Items.BLAZE_ROD).unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
				.save(output.withConditions(new ItemEnabledCondition(ModItems.ENDER_LINKER.get()),
						new OrCondition(List.of(new ModLoadedCondition("sophisticatedbackpacks"), new ModLoadedCondition("sophisticatedstorage")))),
						ResourceKey.create(Registries.RECIPE, SophisticatedCore.getRL("ender_linker")));
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
