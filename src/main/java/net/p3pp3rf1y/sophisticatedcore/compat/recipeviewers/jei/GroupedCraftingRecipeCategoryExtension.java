package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;

import java.util.List;
import java.util.Optional;

public class GroupedCraftingRecipeCategoryExtension implements ICraftingCategoryExtension<GroupedCraftingRecipe> {
	private static boolean registered = false;

	public static synchronized void registerOnce(IVanillaCategoryExtensionRegistration registration) {
		if (registered) {
			return;
		}
		registration.getCraftingCategory().addExtension(GroupedCraftingRecipe.class, new GroupedCraftingRecipeCategoryExtension());
		registered = true;
	}

	@Override
	public List<SlotDisplay> getIngredients(RecipeHolder<GroupedCraftingRecipe> recipeHolder) {
		return recipeHolder.value().getInputSlots().stream()
				.map(slot -> new SlotDisplay.Composite(slot.stream().map(SlotDisplay.ItemStackSlotDisplay::new).map(display -> (SlotDisplay) display).toList()))
				.map(slot -> (SlotDisplay) slot)
				.toList();
	}

	public void setRecipe(RecipeHolder<GroupedCraftingRecipe> recipeHolder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
		GroupedCraftingRecipe recipe = narrowToFocus(recipeHolder.value(), focuses);
		craftingGridHelper.createAndSetInputs(builder, recipe.getInputSlots(), recipe.getDisplayWidth(), recipe.getDisplayHeight());
		craftingGridHelper.createAndSetOutputs(builder, recipe.getResultStacks());
	}

	private static GroupedCraftingRecipe narrowToFocus(GroupedCraftingRecipe recipe, IFocusGroup focuses) {
		Optional<ItemStack> outputFocus = focuses.getItemStackFocuses(RecipeIngredientRole.OUTPUT)
				.map(focus -> focus.getTypedValue().getIngredient())
				.findFirst();
		return outputFocus.map(itemStack -> recipe.narrowForResult(itemStack).orElse(recipe)).orElse(recipe);
	}

	@Override
	public void onDisplayedIngredientsUpdate(RecipeHolder<GroupedCraftingRecipe> recipeHolder, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
		recipeSlots.forEach(IRecipeSlotDrawable::clearDisplayOverrides);

		GroupedCraftingRecipe recipe = recipeHolder.value();
		recipeSlots.stream()
				.flatMap(slot -> slot.getDisplayedItemStack().stream())
				.flatMap(displayedInput -> recipe.findResultForDisplayedInput(displayedInput).stream())
				.findFirst()
				.ifPresent(result -> recipeSlots.getLast().createDisplayOverrides().addItemStack(result));
	}

	@Override
	public int getWidth(RecipeHolder<GroupedCraftingRecipe> recipeHolder) {
		return recipeHolder.value().getDisplayWidth();
	}

	@Override
	public int getHeight(RecipeHolder<GroupedCraftingRecipe> recipeHolder) {
		return recipeHolder.value().getDisplayHeight();
	}
}
