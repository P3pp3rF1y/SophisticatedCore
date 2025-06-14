package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.crafting.CustomShapelessRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.ICustomSmithingRecipe;
import net.p3pp3rf1y.sophisticatedcore.util.ICreativeTabSupplier;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClientRecipeHelper {
	private ClientRecipeHelper() {
	}

	public static <I extends RecipeInput, R extends Recipe<I>, U extends Recipe<?>> void runOnAllRecipesOfType(RecipeType<R> recipeType, Class<U> filterRecipeClass, Consumer<RecipeHolder<R>> run) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}
		RecipeHelper.getRecipesOfType(recipeType)
				.stream()
				.filter(r -> filterRecipeClass.isInstance(r.value()))
				.forEach(run);
	}

	public static <I extends RecipeInput> ItemStack assemble(Recipe<I> recipe, I container) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			throw new NullPointerException("level must not be null.");
		}
		RegistryAccess registryAccess = level.registryAccess();
		return recipe.assemble(container, registryAccess);
	}

	private static ItemStack getVariantResult(CraftingRecipe recipe, ItemStack variantItem) {
		Collection<Optional<Ingredient>> ingredients = RecipeHelper.getIngredients(recipe);
		CraftingContainer craftingInventory = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			public boolean stillValid(Player playerIn) {
				return false;
			}
		}, 3, 3);
		int slot = 0;
		for (Optional<Ingredient> ingredient : ingredients) {
			ingredient.ifPresentOrElse(
					i -> {
						if (i.getValues().size() > 0 && i.getValues().get(0) == variantItem.getItem()) {
							craftingInventory.setItem(slot, variantItem.copy());
						} else {
							craftingInventory.setItem(slot, i.getValues().size() == 0 ? ItemStack.EMPTY : new ItemStack(i.getValues().get(0).value()));
						}
					}, () -> craftingInventory.setItem(slot, ItemStack.EMPTY)
			);
		}
		return assemble(recipe, craftingInventory.asCraftInput());
	}

	public static <U extends CraftingRecipe> void addVariantRecipes(IRecipeDisplayGenerator<?> generator,
																					  Class<U> originalRecipeClass,
																					  Function<CraftingRecipe, List<ItemStack>> getVariantItems,
																					  Function<ItemStack, Optional<PropertyBasedSubtypeInterpreter>> getSubtypeInterpreter,
																					  String modId, String idPrefix) {
		runOnAllRecipesOfType(RecipeType.CRAFTING, originalRecipeClass, recipe -> getVariantItems.apply(recipe.value()).forEach(variantItem -> {
					ItemStack result = getVariantResult(recipe.value(), variantItem);
					ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modId,
							idPrefix
									+ getItemString(getSubtypeInterpreter, variantItem)
									+ "_to_"
									+ getItemString(getSubtypeInterpreter, result)
					);
					addVariantIngredientRecipe(generator, recipe.value(), variantItem, result, id);
				}

		));
	}

	private static <U extends PropertyBasedSubtypeInterpreter> String getItemString(Function<ItemStack, Optional<U>> getSubtypeInterpreter, ItemStack storageItem) {
		return getSubtypeInterpreter.apply(storageItem).map(interpreter -> interpreter.getRegistrySanitizedItemString(storageItem)).orElse("");
	}

	public static <I extends RecipeInput, R extends Recipe<I>> void addAllRecipesOfType(IRecipeDisplayGenerator<?> generator, RecipeType<R> recipeType, Class<? extends Recipe<?>> filterRecipeClass) {
		runOnAllRecipesOfType(recipeType, filterRecipeClass, recipe -> addRecipe(generator, recipe));
	}

	private static <I extends RecipeInput, R extends Recipe<I>> void addRecipe(IRecipeDisplayGenerator<?> generator, RecipeHolder<R> recipe) {
		if (recipe.value() instanceof ShapedRecipe shapedRecipe) {
			generator.shaped(shapedRecipe.result)
					.setDimensions(shapedRecipe.pattern.width(), shapedRecipe.pattern.height())
					.defineIngredients(shapedRecipe.getIngredients())
					.save(recipe.id());
		} else if (recipe.value() instanceof CustomShapelessRecipe shapelessRecipe) {
			generator.shapeless(shapelessRecipe.result())
					.requires(shapelessRecipe.placementInfo().ingredients())
					.save(recipe.id());
		} else if (recipe.value() instanceof ICustomSmithingRecipe smithingRecipe) {
			generator.smithing(smithingRecipe.templateIngredient(), smithingRecipe.baseIngredient(), smithingRecipe.additionIngredient(), smithingRecipe.result())
					.save(recipe.id());
		}
	}

	private static void addVariantIngredientRecipe(IRecipeDisplayGenerator<?> generator, Recipe<?> recipe, ItemStack variantItem, ItemStack result, ResourceLocation id) {
		if (recipe instanceof ShapedRecipe shapedRecipe) {
			ShapedRecipeDisplayBuilder<?> shaped = generator.shaped(result)
					.setDimensions(shapedRecipe.pattern.width(), shapedRecipe.pattern.height());

			for (Optional<Ingredient> ingredient : shapedRecipe.getIngredients()) {
				ingredient.ifPresentOrElse(
						i -> {
							if (i.getValues().size() > 0 && i.getValues().get(0) == variantItem.getItem()) {
								shaped.define(variantItem);
							} else {
								shaped.define(i.getValues());
							}
						}, () -> shaped.define(HolderSet.empty())
				);
			}

			shaped.save(ResourceKey.create(Registries.RECIPE, id));
		} else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
			ShapelessRecipeDisplayBuilder<?> shapeless = generator.shapeless(result);

			for (Ingredient ingredient : shapelessRecipe.ingredients) {
				if (ingredient.getValues().size() > 0 && ingredient.getValues().get(0) == variantItem.getItem()) {
					shapeless.requires(variantItem);
				} else {
					shapeless.requires(ingredient.getValues());
				}
			}

			shapeless.save(ResourceKey.create(Registries.RECIPE, id));
		}
	}

	public static List<ItemStack> getIngredientCreativeTabVariants(Recipe<?> recipe, Class<? extends ICreativeTabSupplier> itemClass) {
		return getIngredientCreativeTabVariants(recipe, itemClass, stack -> {});
	}

	public static List<ItemStack> getIngredientCreativeTabVariants(Recipe<?> recipe, Class<? extends ICreativeTabSupplier> itemClass, Consumer<ItemStack> updateStack) {
		List<ItemStack> ingredientItems = new ArrayList<>();
		for (Optional<Ingredient> ingredient : RecipeHelper.getIngredients(recipe)) {
			ingredient.ifPresent(i -> {
				i.getValues().stream().map(Holder::value).filter(itemClass::isInstance).map(itemClass::cast).forEach(item -> {
					item.addCreativeTabItems(stack -> {
						updateStack.accept(stack);
						ingredientItems.add(stack);
					});
				});
			});
			if (!ingredientItems.isEmpty()) {
				break;
			}
		}
		return ingredientItems;
	}

	public static List<ItemStack> getCustomIngredientVariants(Recipe<?> recipe, Class<? extends ICustomIngredient> customIngredientClass) {
		for (Optional<Ingredient> ingredient : RecipeHelper.getIngredients(recipe)) {
			if (ingredient.isPresent()) {
				Ingredient i = ingredient.get();
				if (customIngredientClass.isInstance(i.getCustomIngredient())) {
					ICustomIngredient customIngredient = i.getCustomIngredient();
					return customIngredient.display().resolveForStacks(RecipeHelper.getContextMap());
				}
			}
		}
		return Collections.emptyList();
	}
}
