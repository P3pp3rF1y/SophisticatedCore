package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapedDisplay;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapedRecipeDisplayBuilder;

import java.util.*;

public class ReiShapedRecipeDisplayBuilder extends ShapedRecipeDisplayBuilder<Display> {
	private final List<EntryIngredient> output;
	private final Map<Character, EntryIngredient> inputEntryIngredients = new LinkedHashMap<>();
	private final DisplayRegistry registry;
	private List<EntryIngredient> inputs = new ArrayList<>();

	public ReiShapedRecipeDisplayBuilder(DisplayRegistry registry, ItemStack result) {
		this.registry = registry;
		output = List.of(EntryIngredients.of(result));
	}

	@Override
	public ShapedRecipeDisplayBuilder<Display> define(Character symbol, TagKey<Item> tag) {
		inputEntryIngredients.put(symbol, EntryIngredients.ofTag(BasicDisplay.registryAccess(), tag, EntryStacks::ofItemHolder));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<Display> define(Character symbol, ItemLike item) {
		inputEntryIngredients.put(symbol, EntryIngredients.of(item));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<Display> define(Character symbol, ItemStack itemStack) {
		inputEntryIngredients.put(symbol, EntryIngredients.of(itemStack));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<Display> define(Character symbol, List<ItemStack> itemStacks) {
		inputEntryIngredients.put(symbol, EntryIngredients.ofItemStacks(itemStacks));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<Display> define(HolderSet<Item> items) {
		inputs.add(EntryIngredients.ofItemsHolderSet(items));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<Display> define(ItemStack itemStack) {
		inputs.add(EntryIngredients.of(itemStack));
		return this;
	}

	@Override
	protected ShapedRecipeDisplayBuilder<Display> defineDisplayStacks(List<ItemStack> itemStacks) {
		inputs.add(EntryIngredients.ofItemStacks(itemStacks));
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<Display> setDimensions(int width, int height) {
		this.width = width;
		this.height = height;
		return this;
	}

	@Override
	public void save(ResourceKey<Recipe<?>> id) {
		if (!inputEntryIngredients.isEmpty()) {
			inputs = unpack(inputEntryIngredients, EntryIngredient.empty()).getOrThrow();
		}
		registry.add(new DefaultCustomShapedDisplay(inputs, output, Optional.of(id.identifier()), width, height));
	}
}
