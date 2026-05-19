package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerCraftingSpecRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class JeiCraftingSpecExtensionRegistrar {
	private static boolean specRecipeExtensionsRegistered = false;

	private JeiCraftingSpecExtensionRegistrar() {
	}

	public static void registerCraftingSpecExtensions(IVanillaCategoryExtensionRegistration registration, Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		registerSpecRecipeExtensions(registration, catalogSupplier);
		catalogSupplier.get().getCraftingSpecExtensionRecipeClasses().forEach(recipeClass -> registerCraftingSpecExtension(registration, recipeClass, catalogSupplier, focusedStackPredicate));
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
		registration.getCraftingCategory().addCategoryExtension(recipeClass,
				recipe -> new CraftingSpecCategoryExtension<>(recipe, rawRecipe -> getCraftingSpec(catalogSupplier.get(), rawRecipe), focusedStackPredicate));
	}

	private static CraftingDisplaySpec getCraftingSpec(IRecipeViewerDisplayCatalog catalog, CraftingRecipe recipe) {
		if (recipe instanceof IRecipeViewerCraftingSpecRecipe specRecipe) {
			return specRecipe.spec();
		}
		return catalog.getCraftingDisplaySpecReplacing(recipe).orElseThrow();
	}
}
