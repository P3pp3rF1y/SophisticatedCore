package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapelessDisplay;
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
import java.util.Optional;

public class ReiShapelessRecipeDisplayBuilder extends ShapelessRecipeDisplayBuilder<Display> {
	private final List<EntryIngredient> inputs = new ArrayList<>();
	private final List<EntryIngredient> output;
	private final DisplayRegistry registry;

	public ReiShapelessRecipeDisplayBuilder(DisplayRegistry registry, ItemStack result) {
		this.registry = registry;
		output = List.of(EntryIngredients.of(result));
	}

	@Override
	public ShapelessRecipeDisplayBuilder<Display> requires(TagKey<Item> tag) {
		inputs.add(EntryIngredients.ofItemTag(tag));
		return this;
	}

	@Override
	public ShapelessRecipeDisplayBuilder<Display> requires(ItemLike item) {
		inputs.add(EntryIngredients.of(item));
		return this;
	}

	@Override
	public ShapelessRecipeDisplayBuilder<Display> requires(ItemStack itemStack) {
		inputs.add(EntryIngredients.of(itemStack));
		return this;
	}

	@Override
	protected ShapelessRecipeDisplayBuilder<Display> requiresItemStacks(List<ItemStack> itemStacks) {
		inputs.add(EntryIngredients.ofItemStacks(itemStacks));
		return this;
	}

	@Override
	public ShapelessRecipeDisplayBuilder<Display> requires(HolderSet<Item> items) {
		inputs.add(EntryIngredients.ofItemsHolderSet(items));
		return this;
	}

	@Override
	public void save(ResourceKey<Recipe<?>> id) {
		registry.add(new DefaultCustomShapelessDisplay(inputs, output, Optional.of(id.identifier())));
	}
}
