package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;

import java.util.function.Consumer;

public class SCRecipeProvider extends RecipeProvider {
	public SCRecipeProvider(PackOutput packOutput) {
		super(packOutput);
	}

	@Override
	protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
		SpecialRecipeBuilder.special(ModRecipes.UPGRADE_CLEAR_SERIALIZER.get()).save(consumer, SophisticatedCore.getRegistryName("upgrade_clear"));
		SpecialRecipeBuilder.special(ModRecipes.ENDER_LINKER_ENDPOINT_SERIALIZER.get()).save(consumer,
				SophisticatedCore.getRegistryName("ender_linker_endpoint"));
		SpecialRecipeBuilder.special(ModRecipes.ENDER_LINKER_CLEAR_SERIALIZER.get()).save(consumer, SophisticatedCore.getRegistryName("ender_linker_clear"));
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ENDER_LINKER.get()).pattern("OEO").pattern("BOB").pattern("OEO").define('O', Items.OBSIDIAN)
				.define('E', Items.ENDER_PEARL).define('B', Items.BLAZE_ROD).unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
				.save(consumer, SophisticatedCore.getRegistryName("ender_linker"));
	}
}
