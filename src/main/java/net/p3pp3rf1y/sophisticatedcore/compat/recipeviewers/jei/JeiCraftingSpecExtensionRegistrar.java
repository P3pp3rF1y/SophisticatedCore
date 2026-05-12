package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.*;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class JeiCraftingSpecExtensionRegistrar {
	private static final IFocusBehavior<CraftingDisplayVariant> ALL_VARIANTS_FOCUS_BEHAVIOR = new IFocusBehavior<>() {
		@Override
		public List<CraftingDisplayVariant> allDisplays(List<CraftingDisplayVariant> variants) {
			return variants;
		}

		@Override
		public List<CraftingDisplayVariant> recipesFor(List<CraftingDisplayVariant> variants, ItemStack focusedOutput) {
			return variants.stream().filter(variant -> variant.outputs().stream().anyMatch(output -> ItemStack.isSameItemSameComponents(output, focusedOutput))).toList();
		}

		@Override
		public List<CraftingDisplayVariant> usagesFor(List<CraftingDisplayVariant> variants, ItemStack focusedInput) {
			return variants.stream().filter(variant -> variant.inputs().stream().anyMatch(input -> ItemStack.isSameItemSameComponents(input, focusedInput))).toList();
		}
	};

	private static boolean specRecipeExtensionsRegistered = false;

	private JeiCraftingSpecExtensionRegistrar() {
	}

	public static void registerCraftingSpecExtensions(IVanillaCategoryExtensionRegistration registration, Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		registerCraftingSpecExtensions(registration, catalogSupplier, focusedStackPredicate, catalogSupplier.get().getCraftingSpecExtensionRecipeClasses());
	}

	public static void registerCraftingSpecExtensions(IVanillaCategoryExtensionRegistration registration, Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate,
			List<Class<? extends CraftingRecipe>> recipeClasses) {
		registerSpecRecipeExtensions(registration, catalogSupplier);
		recipeClasses.forEach(recipeClass -> registerCraftingSpecExtension(registration, recipeClass, catalogSupplier, focusedStackPredicate));
	}

	private static synchronized void registerSpecRecipeExtensions(IVanillaCategoryExtensionRegistration registration, Supplier<IRecipeViewerDisplayCatalog> catalogSupplier) {
		if (specRecipeExtensionsRegistered) {
			return;
		}

		registerCraftingSpecExtension(registration, CraftingDisplaySpec.SpecShapedRecipe.class, catalogSupplier, stack -> true);
		registerCraftingSpecExtension(registration, CraftingDisplaySpec.SpecShapelessRecipe.class, catalogSupplier, stack -> true);
		specRecipeExtensionsRegistered = true;
	}

	private static <R extends CraftingRecipe> void registerCraftingSpecExtension(IVanillaCategoryExtensionRegistration registration, Class<R> recipeClass, Supplier<IRecipeViewerDisplayCatalog> catalogSupplier,
			Predicate<ItemStack> focusedStackPredicate) {
		registration.getCraftingCategory().addExtension(recipeClass, new CraftingSpecCategoryExtension<>(recipeHolder -> getCraftingSpec(catalogSupplier.get(), recipeHolder), focusedStackPredicate));
	}

	private static CraftingDisplaySpec getCraftingSpec(IRecipeViewerDisplayCatalog catalog, RecipeHolder<? extends CraftingRecipe> recipeHolder) {
		if (recipeHolder.value() instanceof IRecipeViewerCraftingSpecRecipe specRecipe) {
			return specRecipe.spec();
		}
		return catalog.getCraftingDisplaySpecReplacing(recipeHolder).orElseGet(() -> basicCraftingSpec(recipeHolder));
	}

	private static CraftingDisplaySpec basicCraftingSpec(RecipeHolder<? extends CraftingRecipe> recipeHolder) {
		CraftingRecipe recipe = recipeHolder.value();
		NonNullList<Ingredient> ingredients = NonNullList.createWithCapacity(recipe.placementInfo().ingredients().size());
		for (Optional<Ingredient> ingredient : RecipeHelper.getIngredients(recipe)) {
			ingredients.add(ingredient.orElseGet(ClientRecipeHelper::emptyDisplayIngredient));
		}
		List<ItemStack> inputs = ingredients.stream()
				.map(ingredient -> ingredient.items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY))
				.toList();
		List<CraftingDisplayVariant> variants = List.of(new CraftingDisplayVariant(inputs, List.of(ClientRecipeHelper.getResultItem(recipe))));
		int width = recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getWidth() : 0;
		int height = recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getHeight() : 0;
		return new CraftingDisplaySpec(recipeHolder.id().identifier(), !(recipe instanceof ShapedRecipe), width, height, ingredients, variants, ALL_VARIANTS_FOCUS_BEHAVIOR);
	}
}
