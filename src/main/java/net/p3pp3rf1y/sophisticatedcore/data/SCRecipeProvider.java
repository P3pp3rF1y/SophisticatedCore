package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
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

public class SCRecipeProvider extends RecipeProvider {
	public SCRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
		super(packOutput, registries);
	}

	@Override
	protected void buildRecipes(RecipeOutput recipeOutput) {
		SpecialRecipeBuilder.special(UpgradeClearRecipe::new).save(recipeOutput, SophisticatedCore.getRegistryName("upgrade_clear"));
		RecipeOutput enderLinkRecipeOutput = recipeOutput
				.withConditions(new OrCondition(List.of(new ModLoadedCondition("sophisticatedbackpacks"), new ModLoadedCondition("sophisticatedstorage"))));
		SpecialRecipeBuilder.special(EnderLinkerEndpointRecipe::new).save(enderLinkRecipeOutput, SophisticatedCore.getRegistryName("ender_linker_endpoint"));
		SpecialRecipeBuilder.special(EnderLinkerClearRecipe::new).save(enderLinkRecipeOutput, SophisticatedCore.getRegistryName("ender_linker_clear"));
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ENDER_LINKER.get()).pattern("OEO").pattern("BOB").pattern("OEO").define('O', Items.OBSIDIAN)
				.define('E', Items.ENDER_PEARL).define('B', Items.BLAZE_ROD).unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
				.save(recipeOutput.withConditions(new ItemEnabledCondition(ModItems.ENDER_LINKER.get()),
						new OrCondition(List.of(new ModLoadedCondition("sophisticatedbackpacks"), new ModLoadedCondition("sophisticatedstorage")))));
	}
}
