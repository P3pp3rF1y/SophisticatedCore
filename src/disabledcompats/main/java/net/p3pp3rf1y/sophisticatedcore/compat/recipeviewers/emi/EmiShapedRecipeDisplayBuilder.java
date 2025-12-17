package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiTags;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapedRecipeDisplayBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmiShapedRecipeDisplayBuilder extends ShapedRecipeDisplayBuilder<EmiCraftingRecipe> {
	private final EmiStack output;
	private final Map<Character, EmiIngredient> inputIngredients = new LinkedHashMap<>();
	private final EmiRegistry registry;
	private List<EmiIngredient> inputs = new ArrayList<>();

	public EmiShapedRecipeDisplayBuilder(EmiRegistry registry, ItemStack result) {
		this.output = EmiStack.of(result);
		this.registry = registry;
	}

	@Override
	public ShapedRecipeDisplayBuilder<EmiCraftingRecipe> define(Character symbol, TagKey<Item> tag) {
		inputIngredients.put(symbol, EmiIngredient.of(tag));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<EmiCraftingRecipe> define(Character symbol, ItemLike item) {
		inputIngredients.put(symbol, EmiStack.of(item));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<EmiCraftingRecipe> define(Character symbol, ItemStack itemStack) {
		inputIngredients.put(symbol, EmiStack.of(itemStack));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<EmiCraftingRecipe> define(Character symbol, List<ItemStack> itemStacks) {
		inputIngredients.put(symbol, EmiTags.getIngredient(Item.class, itemStacks.stream().map(EmiStack::of).toList(), 1));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<EmiCraftingRecipe> define(HolderSet<Item> items) {
		inputs.add(EmiTags.getIngredient(Item.class, items.stream().map(h -> EmiStack.of(h.value())).toList(), 1));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<EmiCraftingRecipe> define(ItemStack itemStack) {
		inputs.add(EmiStack.of(itemStack));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<EmiCraftingRecipe> setDimensions(int width, int height) {
		//noop - not required for EMI
		return this;
	}

	@Override
	public void save(ResourceKey<Recipe<?>> id) {
		if (!inputIngredients.isEmpty()) {
			inputs = unpack(inputIngredients, EmiStack.EMPTY).getOrThrow();
		}
		registry.addRecipe(new EmiCraftingRecipe(inputs, output, id.identifier()));
	}
}
