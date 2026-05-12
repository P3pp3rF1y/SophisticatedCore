package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

public final class RecipeViewerIngredients {
	private static final Ingredient EMPTY = new Ingredient(EmptyIngredient.INSTANCE);

	private RecipeViewerIngredients() {
	}

	public static Ingredient empty() {
		return EMPTY;
	}

	private enum EmptyIngredient implements ICustomIngredient {
		INSTANCE;

		@Override
		public boolean test(ItemStack stack) {
			return stack.isEmpty();
		}

		@Override
		public Stream<Holder<Item>> items() {
			return Stream.empty();
		}

		@Override
		public boolean isSimple() {
			return true;
		}

		@Override
		public IngredientType<?> getType() {
			throw new UnsupportedOperationException("Recipe viewer empty slot ingredients are display-only");
		}

		@Override
		public SlotDisplay display() {
			return SlotDisplay.Empty.INSTANCE;
		}
	}
}
