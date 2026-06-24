package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerCraftingSpecRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class CraftingSpecCategoryExtension<R extends CraftingRecipe> implements ICraftingCategoryExtension<R> {
	private final Function<RecipeHolder<R>, CraftingDisplaySpec> specFactory;
	private final Predicate<ItemStack> focusedStackPredicate;

	public CraftingSpecCategoryExtension(Function<RecipeHolder<R>, CraftingDisplaySpec> specFactory, Predicate<ItemStack> focusedStackPredicate) {
		this.specFactory = specFactory;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public void setRecipe(RecipeHolder<R> recipeHolder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
		CraftingDisplaySpec spec = specFactory.apply(recipeHolder);
		List<CraftingDisplayVariant> variants = recipeHolder.value() instanceof IRecipeViewerCraftingSpecRecipe specRecipe
				? specRecipe.variants()
				: narrowToFocus(spec, focuses);
		List<List<ItemStack>> inputStacks = spec.getInputSlots(variants);
		List<ItemStack> outputStacks = spec.getOutputStacks(variants);
		List<IRecipeSlotBuilder> inputSlots = craftingGridHelper.createAndSetInputs(builder, inputStacks, spec.width(), spec.height());
		IRecipeSlotBuilder outputSlot = craftingGridHelper.createAndSetOutputs(builder, outputStacks);
		if (spec.focusBehavior() instanceof SourceResultFocusBehavior sourceResultFocusBehavior
				&& sourceResultFocusBehavior.sourceInputIndex() < inputSlots.size()) {
			List<ItemStack> sourceStacks = inputStacks.get(sourceResultFocusBehavior.sourceInputIndex());
			if (sourceStacks.size() == 1 && outputStacks.size() == 1) {
				builder.createFocusLink(inputSlots.get(sourceResultFocusBehavior.sourceInputIndex()), outputSlot);
			}
		}
	}

	@Override
	public int getWidth(RecipeHolder<R> recipeHolder) {
		return specFactory.apply(recipeHolder).width();
	}

	@Override
	public int getHeight(RecipeHolder<R> recipeHolder) {
		return specFactory.apply(recipeHolder).height();
	}

	private List<CraftingDisplayVariant> narrowToFocus(CraftingDisplaySpec spec, IFocusGroup focuses) {
		Optional<ItemStack> outputFocus = focuses.getItemStackFocuses(RecipeIngredientRole.OUTPUT).map(focus -> focus.getTypedValue().getIngredient())
				.filter(focusedStackPredicate).findFirst();
		if (outputFocus.isPresent()) {
			List<CraftingDisplayVariant> variants = spec.getRecipesFor(outputFocus.get());
			return variants.isEmpty() ? spec.getGlobalDisplays() : variants;
		}

		Optional<ItemStack> inputFocus = focuses.getItemStackFocuses(RecipeIngredientRole.INPUT).map(focus -> focus.getTypedValue().getIngredient())
				.filter(focusedStackPredicate).findFirst();
		if (inputFocus.isPresent()) {
			List<CraftingDisplayVariant> variants = spec.getUsagesFor(inputFocus.get());
			return variants.isEmpty() ? spec.getGlobalDisplays() : variants;
		}
		return spec.getGlobalDisplays();
	}
}
