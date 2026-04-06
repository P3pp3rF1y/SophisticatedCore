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
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapelessRecipeDisplayBuilder;

import java.util.ArrayList;
import java.util.List;

public class EmiShapelessRecipeDisplayBuilder extends ShapelessRecipeDisplayBuilder<EmiCraftingRecipe> {
	private final EmiStack output;
	private final EmiRegistry registry;
	private List<EmiIngredient> inputs = new ArrayList<>();

	public EmiShapelessRecipeDisplayBuilder(EmiRegistry registry, ItemStack result) {
		this.output = EmiStack.of(result);
		this.registry = registry;
	}

	@Override
	public ShapelessRecipeDisplayBuilder<EmiCraftingRecipe> requires(TagKey<Item> tag) {
		inputs.add(EmiIngredient.of(tag));
		return this;
	}

	@Override
	public ShapelessRecipeDisplayBuilder<EmiCraftingRecipe> requires(ItemLike item) {
		inputs.add(EmiStack.of(item));
		return this;
	}

	@Override
	public ShapelessRecipeDisplayBuilder<EmiCraftingRecipe> requires(ItemStack itemStack) {
		inputs.add(EmiStack.of(itemStack));
		return this;
	}

	@Override
	protected ShapelessRecipeDisplayBuilder<EmiCraftingRecipe> requiresItemStacks(List<ItemStack> itemStacks) {
		inputs.add(EmiTags.getIngredient(Item.class, itemStacks.stream().map(EmiStack::of).toList(), 1));
		return this;
	}

	@Override
	public ShapelessRecipeDisplayBuilder<EmiCraftingRecipe> requires(HolderSet<Item> items) {
		inputs.add(EmiTags.getIngredient(Item.class, items.stream().map(h -> EmiStack.of(h.value())).toList(), 1));
		return this;
	}

	@Override
	public void save(ResourceKey<Recipe<?>> id) {
		registry.addRecipe(new EmiCraftingRecipe(inputs, output, id.identifier()));
	}
}
