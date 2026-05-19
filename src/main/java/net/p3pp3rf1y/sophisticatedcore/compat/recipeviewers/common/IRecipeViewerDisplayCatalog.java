package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.util.List;
import java.util.Optional;

public interface IRecipeViewerDisplayCatalog {
	void addGroupedCraftingSpec(SingleColorDyeRecipeSpec spec);

	List<IRecipeViewerDisplaySpec<GroupedCraftingRecipe>> getGroupedCraftingSpecs();

	void addCraftingSpec(CraftingDisplaySpec spec);

	List<CraftingDisplaySpec> getCraftingSpecs();

	void addCraftingSpecExtensionRecipeClass(Class<? extends CraftingRecipe> recipeClass);

	List<Class<? extends CraftingRecipe>> getCraftingSpecExtensionRecipeClasses();

	void addSmithingSpec(SmithingDisplaySpec spec);

	List<SmithingDisplaySpec> getSmithingSpecs();

	Optional<SmithingDisplaySpec> getSmithingDisplaySpecReplacing(SmithingRecipe recipe);

	List<SmithingDisplayView> getGlobalSmithingDisplays();

	List<SmithingDisplayView> getSmithingRecipesFor(ItemStack focusedOutput);

	List<SmithingDisplayView> getSmithingUsagesFor(ItemStack focusedInput);

	void addCraftingRecipe(CraftingRecipe recipe);

	List<CraftingRecipe> getCraftingRecipes();

	List<CraftingDisplayView> getGlobalCraftingDisplays();

	List<CraftingDisplayView> getCraftingRecipesFor(ItemStack focusedOutput);

	List<CraftingDisplayView> getCraftingUsagesFor(ItemStack focusedInput);

	boolean replacesCraftingRecipe(Recipe<?> recipe);

	Optional<CraftingDisplaySpec> getCraftingDisplaySpecReplacing(Recipe<?> recipe);
}
