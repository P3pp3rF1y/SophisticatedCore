package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerCraftingSpecRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class JeiCraftingSpecExtensionRegistrar {
	private JeiCraftingSpecExtensionRegistrar() {
	}

	public static void registerCraftingSpecExtensions(IVanillaCategoryExtensionRegistration registration, Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		catalogSupplier.get().getCraftingSpecExtensionRecipeClasses().forEach(recipeClass -> registerCraftingSpecExtension(registration, recipeClass, catalogSupplier, focusedStackPredicate));
	}

	private static <R extends CraftingRecipe> void registerCraftingSpecExtension(IVanillaCategoryExtensionRegistration registration, Class<R> recipeClass, Supplier<IRecipeViewerDisplayCatalog> catalogSupplier,
			Predicate<ItemStack> focusedStackPredicate) {
		registration.getCraftingCategory().addExtension(recipeClass, new CraftingSpecCategoryExtension<>(recipeHolder -> getCraftingSpec(catalogSupplier.get(), recipeHolder), focusedStackPredicate));
	}

	private static CraftingDisplaySpec getCraftingSpec(IRecipeViewerDisplayCatalog catalog, RecipeHolder<? extends CraftingRecipe> recipeHolder) {
		if (recipeHolder.value() instanceof IRecipeViewerCraftingSpecRecipe specRecipe) {
			return specRecipe.spec();
		}
		return catalog.getCraftingDisplaySpecReplacing(recipeHolder).orElseThrow();
	}
}
