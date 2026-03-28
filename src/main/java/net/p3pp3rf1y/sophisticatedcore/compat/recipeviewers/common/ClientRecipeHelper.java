package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
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
import java.util.stream.Stream;

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
		return recipe.assemble(container);
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
			int fSlot = slot;
			ingredient.ifPresentOrElse(
					i -> {
						if (ingredientMatchesVariantItem(variantItem, i)) {
							craftingInventory.setItem(fSlot, variantItem.copy());
						} else {
							craftingInventory.setItem(fSlot, getStackFromIngredient(i));
						}
					}, () -> craftingInventory.setItem(fSlot, ItemStack.EMPTY)
			);
			slot++;
		}
		return assemble(recipe, craftingInventory.asCraftInput());
	}

	private static ItemStack getStackFromIngredient(Ingredient i) {
		if (i.getCustomIngredient() != null) {
			return i.getCustomIngredient().items().findFirst().map(holder -> new ItemStack(holder.value())).orElse(ItemStack.EMPTY);
		}

		return i.getValues().size() > 0 ? new ItemStack(i.getValues().get(0).value()) : ItemStack.EMPTY;
	}

	private static boolean ingredientMatchesVariantItem(ItemStack variantItem, Ingredient ingredient) {
		if (ingredient.getCustomIngredient() != null) {
			return ingredient.getCustomIngredient().items().anyMatch(holder -> holder.value() == variantItem.getItem());
		}

		return ingredient.getValues().stream().anyMatch(holder -> holder.value() == variantItem.getItem());
	}

	public static <U extends CraftingRecipe> void addVariantRecipes(IRecipeDisplayGenerator<?> generator,
																	Class<U> originalRecipeClass,
																	Function<CraftingRecipe, List<ItemStack>> getVariantItems,
																	Function<ItemStack, Optional<PropertyBasedSubtypeInterpreter>> getSubtypeInterpreter,
																	String modId, String idPrefix) {
		runOnAllRecipesOfType(RecipeType.CRAFTING, originalRecipeClass, recipe -> getVariantItems.apply(recipe.value()).forEach(variantItem -> {
					ItemStack result = getVariantResult(recipe.value(), variantItem.copy());
					Identifier id = Identifier.fromNamespaceAndPath(modId,
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
			generator.shaped(shapedRecipe.result.create())
					.setDimensions(shapedRecipe.pattern.width(), shapedRecipe.pattern.height())
					.defineIngredients(shapedRecipe.getIngredients())
					.save(recipe.id());
		} else if (recipe.value() instanceof CustomShapelessRecipe shapelessRecipe) {
			generator.shapeless(shapelessRecipe.result().create())
					.requires(shapelessRecipe.placementInfo().ingredients())
					.save(recipe.id());
		} else if (recipe.value() instanceof ICustomSmithingRecipe smithingRecipe) {
			generator.smithing(smithingRecipe.templateIngredient(), smithingRecipe.baseIngredient(), smithingRecipe.additionIngredient(), smithingRecipe.result())
					.save(recipe.id());
		}
	}

	private static void addVariantIngredientRecipe(IRecipeDisplayGenerator<?> generator, Recipe<?> recipe, ItemStack variantItem, ItemStack result, Identifier id) {
		if (recipe instanceof ShapedRecipe shapedRecipe) {
			ShapedRecipeDisplayBuilder<?> shaped = generator.shaped(result)
					.setDimensions(shapedRecipe.pattern.width(), shapedRecipe.pattern.height());

			for (Optional<Ingredient> ingredient : shapedRecipe.getIngredients()) {
				ingredient.ifPresentOrElse(
						i -> {
							if (ingredientMatchesVariantItem(variantItem, i)) {
								shaped.define(variantItem);
							} else {
								shaped.define(i);
							}
						}, () -> shaped.define(HolderSet.empty())
				);
			}

			shaped.save(ResourceKey.create(Registries.RECIPE, id));
		} else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
			addShapelessRecipe(generator, variantItem, result, id, shapelessRecipe.ingredients);
		} else if (recipe instanceof CustomShapelessRecipe customShapelessRecipe) {
			addShapelessRecipe(generator, variantItem, result, id, customShapelessRecipe.placementInfo().ingredients());
		}
	}

	private static void addShapelessRecipe(IRecipeDisplayGenerator<?> generator, ItemStack variantItem, ItemStack result, Identifier id, List<Ingredient> ingredients) {
		ShapelessRecipeDisplayBuilder<?> shapeless = generator.shapeless(result);

		for (Ingredient ingredient : ingredients) {
			if (ingredientMatchesVariantItem(variantItem, ingredient)) {
				shapeless.requires(variantItem);
			} else {
				shapeless.requires(ingredient);
			}
		}

		shapeless.save(ResourceKey.create(Registries.RECIPE, id));
	}

	public static List<ItemStack> getIngredientCreativeTabVariants(Recipe<?> recipe, Class<? extends ICreativeTabSupplier> itemClass) {
		return getIngredientCreativeTabVariants(recipe, itemClass, stack -> {
		});
	}

	public static List<ItemStack> getIngredientCreativeTabVariants(Recipe<?> recipe, Class<? extends ICreativeTabSupplier> itemClass, Consumer<ItemStack> updateStack) {
		List<ItemStack> ingredientItems = new ArrayList<>();
		for (Optional<Ingredient> ingredient : RecipeHelper.getIngredients(recipe)) {
			ingredient.ifPresent(i -> {
				getIngredientValues(i).map(Holder::value).filter(itemClass::isInstance).map(itemClass::cast).forEach(item -> {
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

	private static Stream<Holder<Item>> getIngredientValues(Ingredient i) {
		if (i.getCustomIngredient() != null) {
			return i.getCustomIngredient().items();
		}
		return i.getValues().stream();
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
