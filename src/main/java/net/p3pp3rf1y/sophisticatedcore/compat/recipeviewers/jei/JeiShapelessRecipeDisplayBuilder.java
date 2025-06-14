package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.scores.DisplaySlot;
import net.neoforged.neoforge.common.crafting.CustomDisplayIngredient;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapelessRecipeDisplayBuilder;

import java.util.ArrayList;
import java.util.List;

public class JeiShapelessRecipeDisplayBuilder extends ShapelessRecipeDisplayBuilder<CraftingRecipe> {
	private final List<DisplaySlot> displays = new ArrayList();
	private final HolderGetter<Item> items;
	private final JeiRecipeDisplayGenerator generator;
	private final List<Ingredient> ingredients = new ArrayList<>();
	private final ItemStack result;


	public JeiShapelessRecipeDisplayBuilder(HolderGetter<Item> items, JeiRecipeDisplayGenerator generator, ItemStack result) {
		this.items = items;
		this.generator = generator;
		this.result = result;
	}

	@Override
	public ShapelessRecipeDisplayBuilder<CraftingRecipe> requires(TagKey<Item> tag) {
		return requires(Ingredient.of(items.getOrThrow(tag)));
	}

	@Override
	public ShapelessRecipeDisplayBuilder<CraftingRecipe> requires(ItemLike item) {
		return requires(Ingredient.of(item));
	}

	@Override
	public ShapelessRecipeDisplayBuilder<CraftingRecipe> requires(ItemStack itemStack) {
		return requires(CustomDisplayIngredient.of(Ingredient.of(itemStack.getItem()), new SlotDisplay.ItemStackSlotDisplay(itemStack)));
	}

	@Override
	public ShapelessRecipeDisplayBuilder<CraftingRecipe> requires(HolderSet<Item> items) {
		return requires(Ingredient.of(items));
	}

	private ShapelessRecipeDisplayBuilder<CraftingRecipe> requires(Ingredient ingredient) {
		ingredients.add(ingredient);
		return this;
	}

	@Override
	public void save(ResourceKey<Recipe<?>> id) {
		generator.acceptCrafting(new RecipeHolder<>(id, new ShapelessRecipe("", CraftingBookCategory.MISC, result, ingredients)));
	}
}
