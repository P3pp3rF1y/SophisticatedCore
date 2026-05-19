package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecipeViewerDisplayCatalog implements IRecipeViewerDisplayCatalog {
	private final List<IRecipeViewerDisplaySpec<RecipeHolder<GroupedCraftingRecipe>>> groupedCraftingSpecs = new ArrayList<>();
	private final List<CraftingDisplaySpec> craftingSpecs = new ArrayList<>();
	private final List<Class<? extends CraftingRecipe>> craftingSpecExtensionRecipeClasses = new ArrayList<>();
	private final List<SmithingDisplaySpec> smithingSpecs = new ArrayList<>();
	private final List<RecipeHolder<CraftingRecipe>> craftingRecipes = new ArrayList<>();

	@Override
	public void addGroupedCraftingSpec(SingleColorDyeRecipeSpec spec) {
		groupedCraftingSpecs.add(spec);
	}

	@Override
	public List<IRecipeViewerDisplaySpec<RecipeHolder<GroupedCraftingRecipe>>> getGroupedCraftingSpecs() {
		return List.copyOf(groupedCraftingSpecs);
	}

	@Override
	public void addCraftingSpec(CraftingDisplaySpec spec) {
		craftingSpecs.add(spec);
	}

	@Override
	public List<CraftingDisplaySpec> getCraftingSpecs() {
		return List.copyOf(craftingSpecs);
	}

	@Override
	public void addCraftingSpecExtensionRecipeClass(Class<? extends CraftingRecipe> recipeClass) {
		craftingSpecExtensionRecipeClasses.add(recipeClass);
	}

	@Override
	public List<Class<? extends CraftingRecipe>> getCraftingSpecExtensionRecipeClasses() {
		return List.copyOf(craftingSpecExtensionRecipeClasses);
	}

	@Override
	public void addSmithingSpec(SmithingDisplaySpec spec) {
		smithingSpecs.add(spec);
	}

	@Override
	public List<SmithingDisplaySpec> getSmithingSpecs() {
		return List.copyOf(smithingSpecs);
	}

	@Override
	public Optional<SmithingDisplaySpec> getSmithingDisplaySpecReplacing(SmithingRecipe recipe) {
		return smithingSpecs.stream().filter(spec -> spec.replacesSmithingRecipe(recipe)).findFirst();
	}

	@Override
	public List<SmithingDisplayView> getGlobalSmithingDisplays() {
		return smithingSpecs.stream()
				.map(spec -> new SmithingDisplayView(spec, spec.getGlobalDisplays()))
				.filter(view -> !view.variants().isEmpty())
				.toList();
	}

	@Override
	public List<SmithingDisplayView> getSmithingRecipesFor(ItemStack focusedOutput) {
		return smithingSpecs.stream()
				.map(spec -> new SmithingDisplayView(spec, spec.getRecipesFor(focusedOutput)))
				.filter(view -> !view.variants().isEmpty())
				.toList();
	}

	@Override
	public List<SmithingDisplayView> getSmithingUsagesFor(ItemStack focusedInput) {
		return smithingSpecs.stream()
				.map(spec -> new SmithingDisplayView(spec, spec.getUsagesFor(focusedInput)))
				.filter(view -> !view.variants().isEmpty())
				.toList();
	}

	@Override
	public void addCraftingRecipe(RecipeHolder<CraftingRecipe> recipe) {
		craftingRecipes.add(recipe);
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getCraftingRecipes() {
		return List.copyOf(craftingRecipes);
	}

	@Override
	public List<CraftingDisplayView> getGlobalCraftingDisplays() {
		return craftingSpecs.stream()
				.map(spec -> new CraftingDisplayView(spec, spec.getGlobalDisplays()))
				.filter(view -> !view.variants().isEmpty())
				.toList();
	}

	@Override
	public List<CraftingDisplayView> getCraftingRecipesFor(ItemStack focusedOutput) {
		return craftingSpecs.stream()
				.map(spec -> new CraftingDisplayView(spec, spec.getRecipesFor(focusedOutput)))
				.filter(view -> !view.variants().isEmpty())
				.toList();
	}

	@Override
	public List<CraftingDisplayView> getCraftingUsagesFor(ItemStack focusedInput) {
		return craftingSpecs.stream()
				.map(spec -> new CraftingDisplayView(spec, getUsagesFor(spec, focusedInput)))
				.filter(view -> !view.variants().isEmpty())
				.toList();
	}

	@Override
	public boolean replacesCraftingRecipe(RecipeHolder<?> recipeHolder) {
		return getCraftingDisplaySpecReplacing(recipeHolder).isPresent();
	}

	@Override
	public Optional<CraftingDisplaySpec> getCraftingDisplaySpecReplacing(RecipeHolder<?> recipeHolder) {
		return craftingSpecs.stream().filter(spec -> spec.replacesCraftingRecipe(recipeHolder)).findFirst();
	}

	private static List<CraftingDisplayVariant> getUsagesFor(CraftingDisplaySpec spec, ItemStack focusedInput) {
		List<CraftingDisplayVariant> usages = spec.getUsagesFor(focusedInput);
		if (!usages.isEmpty()) {
			return usages;
		}
		if (spec.focusBehavior() instanceof SourceResultFocusBehavior sourceResultFocusBehavior
				&& sourceResultFocusBehavior.sourceInputIndex() < spec.baseIngredients().size()
				&& spec.baseIngredients().get(sourceResultFocusBehavior.sourceInputIndex()).test(focusedInput)) {
			return List.of();
		}
		return spec.baseIngredients().stream().anyMatch(ingredient -> ingredient.test(focusedInput)) ? spec.getGlobalDisplays() : List.of();
	}
}
