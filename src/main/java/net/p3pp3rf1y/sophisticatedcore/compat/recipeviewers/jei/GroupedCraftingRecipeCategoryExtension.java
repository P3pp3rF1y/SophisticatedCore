package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;

import java.util.List;
import java.util.Optional;

public class GroupedCraftingRecipeCategoryExtension implements ICraftingCategoryExtension {
	private static boolean registered = false;
	private final GroupedCraftingRecipe recipe;

	private GroupedCraftingRecipeCategoryExtension(GroupedCraftingRecipe recipe) {
		this.recipe = recipe;
	}

	public static synchronized void registerOnce(IVanillaCategoryExtensionRegistration registration) {
		if (registered) {
			return;
		}
		registration.getCraftingCategory().addCategoryExtension(GroupedCraftingRecipe.class, GroupedCraftingRecipeCategoryExtension::new);
		registered = true;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
		GroupedCraftingRecipe focusedRecipe = narrowToFocus(recipe, focuses);
		craftingGridHelper.createAndSetInputs(builder, focusedRecipe.getInputSlots(), focusedRecipe.getDisplayWidth(), focusedRecipe.getDisplayHeight());
		craftingGridHelper.createAndSetOutputs(builder, focusedRecipe.getResultStacks());
	}

	private static GroupedCraftingRecipe narrowToFocus(GroupedCraftingRecipe recipe, IFocusGroup focuses) {
		Optional<ItemStack> outputFocus = focuses.getItemStackFocuses(RecipeIngredientRole.OUTPUT)
				.map(focus -> focus.getTypedValue().getIngredient())
				.findFirst();
		return outputFocus.map(itemStack -> recipe.narrowForResult(itemStack).orElse(recipe)).orElse(recipe);
	}

	@Override
	public void onDisplayedIngredientsUpdate(List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
		recipeSlots.forEach(IRecipeSlotDrawable::clearDisplayOverrides);

		recipeSlots.stream()
				.flatMap(slot -> slot.getDisplayedItemStack().stream())
				.flatMap(displayedInput -> recipe.findResultForDisplayedInput(displayedInput).stream())
				.findFirst()
				.ifPresent(result -> recipeSlots.get(recipeSlots.size() - 1).createDisplayOverrides().addItemStack(result));
	}

	@Override
	public int getWidth() {
		return recipe.getDisplayWidth();
	}

	@Override
	public int getHeight() {
		return recipe.getDisplayHeight();
	}
}
