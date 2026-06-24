package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import com.google.common.collect.Maps;
import mezz.jei.library.plugins.vanilla.crafting.JeiShapedRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapedRecipeDisplayBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JeiShapedRecipeDisplayBuilder extends ShapedRecipeDisplayBuilder<CraftingRecipe> {
	private final JeiRecipeDisplayGenerator generator;
	private final SlotDisplay result;
	private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
	private final Map<Character, SlotDisplay> displayKey = Maps.newLinkedHashMap();
	private List<Optional<Ingredient>> ingredients = new ArrayList<>();
	private List<SlotDisplay> displays = new ArrayList<>();
	private final HolderGetter<Item> items;

	public JeiShapedRecipeDisplayBuilder(HolderGetter<Item> items, JeiRecipeDisplayGenerator generator, ItemStack result) {
		this.items = items;
		this.generator = generator;
		this.result = new SlotDisplay.ItemStackSlotDisplay(result);
	}

	@Override
	public JeiShapedRecipeDisplayBuilder define(Character symbol, TagKey<Item> tag) {
		Ingredient ingredient = Ingredient.of(items.getOrThrow(tag));
		return define(symbol, ingredient, ingredient.display());
	}

	@Override
	public JeiShapedRecipeDisplayBuilder define(Character symbol, ItemLike item) {
		Ingredient ingredient = Ingredient.of(item);
		return define(symbol, ingredient, ingredient.display());
	}

	@Override
	public JeiShapedRecipeDisplayBuilder define(Character symbol, ItemStack itemStack) {
		return define(symbol, Ingredient.of(itemStack.getItem()), new SlotDisplay.ItemStackSlotDisplay(itemStack));
	}

	@Override
	public ShapedRecipeDisplayBuilder<CraftingRecipe> define(Character symbol, List<ItemStack> itemStacks) {
		return define(symbol, Ingredient.of(itemStacks.isEmpty() ? Items.AIR : itemStacks.getFirst().getItem()),
				new SlotDisplay.Composite(itemStacks.stream().map(SlotDisplay.ItemStackSlotDisplay::new).map(SlotDisplay.class::cast).toList()));
	}

	@Override
	public ShapedRecipeDisplayBuilder<CraftingRecipe> setDimensions(int width, int height) {
		this.width = width;
		this.height = height;
		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<CraftingRecipe> define(HolderSet<Item> items) {
		if (items.size() > 0) {
			Ingredient ingredient = Ingredient.of(items);
			ingredients.add(Optional.of(ingredient));
			displays.add(ingredient.display());
		} else {
			ingredients.add(Optional.empty());
			displays.add(SlotDisplay.Empty.INSTANCE);
		}

		return this;
	}

	@Override
	public ShapedRecipeDisplayBuilder<CraftingRecipe> define(ItemStack itemStack) {
		ingredients.add(Optional.of(Ingredient.of(itemStack.getItem())));
		displays.add(new SlotDisplay.ItemStackSlotDisplay(itemStack));
		return this;
	}

	private JeiShapedRecipeDisplayBuilder define(Character symbol, Ingredient ingredient, SlotDisplay slotDisplay) {
		if (key.containsKey(symbol)) {
			throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
		} else if (symbol == ' ') {
			throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
		} else {
			key.put(symbol, ingredient);
			displayKey.put(symbol, slotDisplay);
			return this;
		}
	}

	@Override
	public void save(ResourceKey<Recipe<?>> id) {
		ShapedRecipePattern pattern;
		List<SlotDisplay> displays;
		if (ingredients.isEmpty()) {
			pattern = ShapedRecipePattern.of(key, rows);
			displays = unpack(displayKey, SlotDisplay.Empty.INSTANCE).getOrThrow();
		} else {
			pattern = new ShapedRecipePattern(width, height, ingredients, Optional.empty());
			displays = this.displays;
		}

		generator.acceptCrafting(new RecipeHolder<>(id, new JeiShapedRecipe("", CraftingBookCategory.MISC, pattern, displays, result)));
	}
}
