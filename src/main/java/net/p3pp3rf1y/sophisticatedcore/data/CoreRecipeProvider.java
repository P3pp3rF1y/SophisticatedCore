package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerClearRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerEndpointRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.ItemEnabledCondition;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeClearRecipe;
import net.p3pp3rf1y.sophisticatedcore.init.ModItems;

import java.util.List;

public class CoreRecipeProvider extends RecipeProvider {
	public CoreRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
		super(recipes, advancements);
	}

	@Override
	protected void buildRecipes() {
		var items = output.lookup(Registries.ITEM);
		SpecialRecipeBuilder.special(() -> UpgradeClearRecipe.INSTANCE).save(output,
				ResourceKey.create(Registries.RECIPE, SophisticatedCore.getIdentifier("upgrade_clear")));
		RecipeOutput enderLinkRecipeOutput = output
				.withConditions(new OrCondition(List.of(new ModLoadedCondition("sophisticatedbackpacks"), new ModLoadedCondition("sophisticatedstorage"))));
		SpecialRecipeBuilder.special(() -> EnderLinkerEndpointRecipe.INSTANCE).save(enderLinkRecipeOutput,
				ResourceKey.create(Registries.RECIPE, SophisticatedCore.getIdentifier("ender_linker_endpoint")));
		SpecialRecipeBuilder.special(() -> EnderLinkerClearRecipe.INSTANCE).save(enderLinkRecipeOutput,
				ResourceKey.create(Registries.RECIPE, SophisticatedCore.getIdentifier("ender_linker_clear")));
		ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.ENDER_LINKER.get()).pattern("OEO").pattern("BOB").pattern("OEO")
				.define('O', Items.OBSIDIAN).define('E', Items.ENDER_PEARL).define('B', Items.BLAZE_ROD).unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
				.save(output.withConditions(new ItemEnabledCondition(ModItems.ENDER_LINKER.get()),
						new OrCondition(List.of(new ModLoadedCondition("sophisticatedbackpacks"), new ModLoadedCondition("sophisticatedstorage")))),
						ResourceKey.create(Registries.RECIPE, SophisticatedCore.getIdentifier("ender_linker")));
	}

}
