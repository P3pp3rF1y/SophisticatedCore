package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerCraftingSpecRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;

import java.util.ArrayList;
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
	public List<SlotDisplay> getIngredients(RecipeHolder<R> recipeHolder) {
		CraftingDisplaySpec spec = specFactory.apply(recipeHolder);
		if (recipeHolder.value() instanceof IRecipeViewerCraftingSpecRecipe specRecipe) {
			List<List<ItemStack>> inputStacks = spec.getInputSlots(specRecipe.variants());
			List<SlotDisplay> displays = new ArrayList<>(spec.baseIngredients().size());
			for (int i = 0; i < spec.baseIngredients().size(); i++) {
				List<ItemStack> stacks = i < inputStacks.size() ? inputStacks.get(i) : List.of();
				displays.add(stacks.isEmpty() ? spec.baseIngredients().get(i).display() : slotDisplay(stacks));
			}
			return displays;
		}
		return spec.baseIngredients().stream().map(ingredient -> (SlotDisplay) ingredient.display()).toList();
	}

	private static SlotDisplay slotDisplay(List<ItemStack> stacks) {
		if (stacks.size() == 1) {
			return new SlotDisplay.ItemStackSlotDisplay(stacks.getFirst());
		}
		return new SlotDisplay.Composite(stacks.stream().map(SlotDisplay.ItemStackSlotDisplay::new).map(display -> (SlotDisplay) display).toList());
	}

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
	public void onDisplayedIngredientsUpdate(RecipeHolder<R> recipeHolder, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
		recipeSlots.forEach(IRecipeSlotDrawable::clearDisplayOverrides);
		CraftingDisplaySpec spec = specFactory.apply(recipeHolder);
		List<CraftingDisplayVariant> variants = recipeHolder.value() instanceof IRecipeViewerCraftingSpecRecipe specRecipe
				? specRecipe.variants()
				: narrowToFocus(spec, focuses);
		List<List<ItemStack>> inputStacks = gridInputStacks(spec, variants);
		List<IRecipeSlotDrawable> inputSlots = recipeSlots.stream().filter(slot -> slot.getRole() == RecipeIngredientRole.INPUT).toList();
		for (int i = 0; i < Math.min(inputStacks.size(), inputSlots.size()); i++) {
			if (!inputStacks.get(i).isEmpty()) {
				inputSlots.get(i).createDisplayOverrides().addItemStacks(inputStacks.get(i));
			}
		}
		List<ItemStack> outputs = spec.getOutputStacks(variants);
		if (outputs.isEmpty()) {
			return;
		}
		recipeSlots.stream().filter(slot -> slot.getRole() == RecipeIngredientRole.OUTPUT).findFirst()
				.ifPresent(slot -> slot.createDisplayOverrides().addItemStacks(outputs));
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

	private static List<List<ItemStack>> gridInputStacks(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants) {
		List<List<ItemStack>> inputStacks = spec.getInputSlots(variants);
		if (spec.width() == 3 && spec.height() == 3) {
			return inputStacks;
		}

		List<List<ItemStack>> gridInputStacks = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			gridInputStacks.add(List.of());
		}

		int xOffset = spec.width() == 1 ? 1 : 0;
		int yOffset = spec.height() == 1 ? 1 : 0;
		for (int y = 0; y < spec.height(); y++) {
			for (int x = 0; x < spec.width(); x++) {
				int sourceIndex = x + y * spec.width();
				if (sourceIndex < inputStacks.size()) {
					gridInputStacks.set(x + xOffset + (y + yOffset) * 3, inputStacks.get(sourceIndex));
				}
			}
		}
		return gridInputStacks;
	}
}
