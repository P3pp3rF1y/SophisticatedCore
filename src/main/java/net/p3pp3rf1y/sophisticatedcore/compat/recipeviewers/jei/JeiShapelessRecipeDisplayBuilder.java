package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.CustomDisplayIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapelessRecipeDisplayBuilder;

import java.util.ArrayList;
import java.util.List;

public class JeiShapelessRecipeDisplayBuilder extends ShapelessRecipeDisplayBuilder<CraftingRecipe> {
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
		return requires(CustomDisplayIngredient.of(DataComponentIngredient.of(true, itemStack), new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(itemStack))));
	}

	@Override
	protected ShapelessRecipeDisplayBuilder<CraftingRecipe> requiresItemStacks(List<ItemStack> itemStacks) {
		return requires(CustomDisplayIngredient.of(getDisplayIngredient(itemStacks),
				new SlotDisplay.Composite(itemStacks.stream().map(ItemStackTemplate::fromNonEmptyStack).map(SlotDisplay.ItemStackSlotDisplay::new).map(SlotDisplay.class::cast).toList())));
	}

	private static Ingredient getDisplayIngredient(List<ItemStack> itemStacks) {
		if (itemStacks.isEmpty()) {
			return Ingredient.of(Items.AIR);
		}

		if (itemStacks.size() == 1) {
			return DataComponentIngredient.of(true, itemStacks.getFirst());
		}

		return CompoundIngredient.of(itemStacks.stream().map(stack -> DataComponentIngredient.of(true, stack)).toArray(Ingredient[]::new));
	}

	@Override
	public ShapelessRecipeDisplayBuilder<CraftingRecipe> requires(HolderSet<Item> items) {
		return requires(Ingredient.of(items));
	}

	@Override
	public ShapelessRecipeDisplayBuilder<CraftingRecipe> requires(Ingredient ingredient) {
		ingredients.add(ingredient);
		return this;
	}

	@Override
	public void save(ResourceKey<Recipe<?>> id) {
		generator.acceptCrafting(new RecipeHolder<>(id, new ShapelessRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""), ItemStackTemplate.fromNonEmptyStack(result), ingredients)));
	}
}
