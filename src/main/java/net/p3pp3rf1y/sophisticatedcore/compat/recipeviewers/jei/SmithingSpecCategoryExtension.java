package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplayVariant;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SmithingSpecCategoryExtension<R extends SmithingRecipe> implements ISmithingCategoryExtension<R> {
	private final Function<R, SmithingDisplaySpec> specFactory;
	private final Predicate<ItemStack> focusedStackPredicate;

	public SmithingSpecCategoryExtension(Function<R, SmithingDisplaySpec> specFactory, Predicate<ItemStack> focusedStackPredicate) {
		this.specFactory = specFactory;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public <T extends IIngredientAcceptor<T>> void setTemplate(R recipe, T ingredientAcceptor) {
		specFactory.apply(recipe).template().ifPresent(ingredientAcceptor::addIngredients);
	}

	@Override
	public <T extends IIngredientAcceptor<T>> void setBase(R recipe, T ingredientAcceptor) {
		SmithingDisplaySpec spec = specFactory.apply(recipe);
		spec.getBaseStacks(spec.getGlobalDisplays()).forEach(ingredientAcceptor::addItemStack);
	}

	@Override
	public <T extends IIngredientAcceptor<T>> void setAddition(R recipe, T ingredientAcceptor) {
		specFactory.apply(recipe).addition().ifPresent(ingredientAcceptor::addIngredients);
	}

	@Override
	public <T extends IIngredientAcceptor<T>> void setOutput(R recipe, T ingredientAcceptor) {
		SmithingDisplaySpec spec = specFactory.apply(recipe);
		spec.getResultStacks(spec.getGlobalDisplays()).forEach(ingredientAcceptor::addItemStack);
	}

	@Override
	public void onDisplayedIngredientsUpdate(R recipe, IRecipeSlotDrawable templateSlot, IRecipeSlotDrawable baseSlot, IRecipeSlotDrawable additionSlot,
			IRecipeSlotDrawable outputSlot, IFocusGroup focuses) {
		SmithingDisplaySpec spec = specFactory.apply(recipe);
		List<SmithingDisplayVariant> focusedVariants = getFocusedVariants(spec, focuses);
		if (!focusedVariants.isEmpty()) {
			SmithingDisplayVariant variant = focusedVariants.get(0);
			baseSlot.createDisplayOverrides().addItemStack(variant.base());
			outputSlot.createDisplayOverrides().addItemStack(variant.result());
			return;
		}

		baseSlot.getDisplayedItemStack().flatMap(stack -> spec.getUsagesFor(stack).stream().findFirst())
				.ifPresent(variant -> outputSlot.createDisplayOverrides().addItemStack(variant.result()));
	}

	private List<SmithingDisplayVariant> getFocusedVariants(SmithingDisplaySpec spec, IFocusGroup focuses) {
		Optional<ItemStack> outputFocus = focuses.getItemStackFocuses(RecipeIngredientRole.OUTPUT).map(focus -> focus.getTypedValue().getIngredient())
				.filter(focusedStackPredicate).findFirst();
		return outputFocus.map(spec::getRecipesFor).orElseGet(() -> focuses.getItemStackFocuses(RecipeIngredientRole.INPUT)
				.map(focus -> focus.getTypedValue().getIngredient()).filter(focusedStackPredicate).findFirst().map(spec::getUsagesFor).orElse(List.of()));

	}
}
