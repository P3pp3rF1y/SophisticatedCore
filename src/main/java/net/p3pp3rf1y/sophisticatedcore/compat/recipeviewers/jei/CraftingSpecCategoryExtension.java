package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerCraftingSpecRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class CraftingSpecCategoryExtension<R extends CraftingRecipe> implements ICraftingCategoryExtension {
	private final R recipe;
	private final Function<R, CraftingDisplaySpec> specFactory;
	private final Predicate<ItemStack> focusedStackPredicate;

	public CraftingSpecCategoryExtension(R recipe, Function<R, CraftingDisplaySpec> specFactory, Predicate<ItemStack> focusedStackPredicate) {
		this.recipe = recipe;
		this.specFactory = specFactory;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
		CraftingDisplaySpec spec = specFactory.apply(recipe);
		if (!(recipe instanceof IRecipeViewerCraftingSpecRecipe) && !spec.replacedRecipeIds().isEmpty()) {
			craftingGridHelper.createAndSetInputs(builder, baseInputSlots(spec), spec.width(), spec.height());
			craftingGridHelper.createAndSetOutputs(builder, List.of(recipe.getResultItem(null)));
			return;
		}
		List<CraftingDisplayVariant> variants = recipe instanceof IRecipeViewerCraftingSpecRecipe specRecipe ? specRecipe.variants() : vanillaRecipeVariants(spec, focuses);
		List<IRecipeSlotBuilder> inputSlots = craftingGridHelper.createAndSetInputs(builder, spec.getInputSlots(variants), spec.width(), spec.height());
		IRecipeSlotBuilder outputSlot = craftingGridHelper.createAndSetOutputs(builder, spec.getOutputStacks(variants));
		if (spec.focusBehavior() instanceof SourceResultFocusBehavior sourceResultFocusBehavior && sourceResultFocusBehavior.sourceInputIndex() < inputSlots.size()) {
			try {
				builder.createFocusLink(inputSlots.get(sourceResultFocusBehavior.sourceInputIndex()), outputSlot);
			} catch (IllegalArgumentException e) {
				// JEI requires linked slots to have the same number of ingredients; grouped displays can legitimately differ.
			}
		}
	}

	@Override
	public int getWidth() {
		return specFactory.apply(recipe).width();
	}

	@Override
	public int getHeight() {
		return specFactory.apply(recipe).height();
	}

	private List<CraftingDisplayVariant> vanillaRecipeVariants(CraftingDisplaySpec spec, IFocusGroup focuses) {
		return spec.replacedRecipeIds().isEmpty() ? narrowToFocus(spec, focuses) : spec.getGlobalDisplays();
	}

	private static List<List<ItemStack>> baseInputSlots(CraftingDisplaySpec spec) {
		List<List<ItemStack>> inputSlots = new ArrayList<>(spec.baseIngredients().size());
		spec.baseIngredients().forEach(ingredient -> inputSlots.add(List.of(ingredient.getItems())));
		return inputSlots;
	}

	private List<CraftingDisplayVariant> narrowToFocus(CraftingDisplaySpec spec, IFocusGroup focuses) {
		Optional<ItemStack> outputFocus = focuses.getItemStackFocuses(RecipeIngredientRole.OUTPUT)
				.map(focus -> focus.getTypedValue().getIngredient())
				.filter(focusedStackPredicate)
				.findFirst();
		if (outputFocus.isPresent()) {
			List<CraftingDisplayVariant> variants = spec.getRecipesFor(outputFocus.get());
			return variants.isEmpty() ? spec.getGlobalDisplays() : variants;
		}

		Optional<ItemStack> inputFocus = focuses.getItemStackFocuses(RecipeIngredientRole.INPUT)
				.map(focus -> focus.getTypedValue().getIngredient())
				.filter(focusedStackPredicate)
				.findFirst();
		if (inputFocus.isPresent()) {
			List<CraftingDisplayVariant> variants = spec.getUsagesFor(inputFocus.get());
			return variants.isEmpty() ? spec.getGlobalDisplays() : variants;
		}
		return spec.getGlobalDisplays();
	}
}
