package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import com.google.common.collect.Lists;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class ShapedRecipeDisplayBuilder<R> implements IRecipeDisplayBuilder {
	protected final List<String> rows = Lists.newArrayList();
	protected int width;
	protected int height;

	public ShapedRecipeDisplayBuilder<R> pattern(String patternRow) {
		if (!rows.isEmpty() && patternRow.length() != rows.getFirst().length()) {
			throw new IllegalArgumentException("Pattern must be the same width on every line!");
		} else {
			rows.add(patternRow);
			return this;
		}
	}

	public abstract ShapedRecipeDisplayBuilder<R> define(Character symbol, TagKey<Item> tag);

	public abstract ShapedRecipeDisplayBuilder<R> define(Character symbol, ItemLike item);

	public abstract ShapedRecipeDisplayBuilder<R> define(Character symbol, ItemStack itemStack);

	public abstract ShapedRecipeDisplayBuilder<R> define(Character symbol, List<ItemStack> itemStacks);

	public abstract ShapedRecipeDisplayBuilder<R> define(HolderSet<Item> items);

	public abstract ShapedRecipeDisplayBuilder<R> define(ItemStack itemStack);

	public ShapedRecipeDisplayBuilder<R> defineIngredients(List<Optional<Ingredient>> ingredients) {
		ingredients.forEach(i -> define(i.map(Ingredient::getValues).orElse(HolderSet.empty())));
		return this;
	}

	public abstract ShapedRecipeDisplayBuilder<R> setDimensions(int width, int height);

	protected <D> DataResult<List<D>> unpack(Map<Character, D> key, D empty) {
		String[] astring = ShapedRecipePattern.shrink(rows);
		width = astring[0].length();
		height = astring.length;
		List<D> list = new ArrayList<>(width * height);
		CharSet charset = new CharArraySet(key.keySet());

		for (String s : astring) {
			for (int k = 0; k < s.length(); k++) {
				char c = s.charAt(k);
				if (c == ' ') {
					list.add(empty);
				} else {
					D displayIngredient = key.get(c);
					if (displayIngredient == null) {
						return DataResult.error(() -> "Pattern references symbol '" + c + "' but it's not defined in the key");
					}

					list.add(displayIngredient);
				}

				charset.remove(c);
			}
		}

		return !charset.isEmpty()
				? DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charset)
				: DataResult.success(list);
	}
}
