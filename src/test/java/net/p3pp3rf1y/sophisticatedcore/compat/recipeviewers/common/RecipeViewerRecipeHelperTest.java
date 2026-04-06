package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.server.Bootstrap;
import net.p3pp3rf1y.sophisticatedcore.crafting.CustomShapelessRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeViewerRecipeHelperTest {
	static {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		bindTestComponents(Items.STONE, Items.DIORITE, Items.GRANITE);
	}

	private static void bindTestComponents(Item... items) {
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : items) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	@Test
	void getIngredientsIncludesWrappedShapedRecipeIngredients() {
		WrappedShapedRecipe wrappedRecipe = new WrappedShapedRecipe(new ShapedRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
				new ShapedRecipePattern(2, 1, NonNullList.of(Optional.empty(), Optional.of(Ingredient.of(Items.STONE)), Optional.of(Ingredient.of(Items.DIORITE))), Optional.empty()),
				ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.GRANITE))));

		Collection<Optional<Ingredient>> ingredients = RecipeViewerRecipeHelper.getIngredients(wrappedRecipe);

		assertEquals(2, ingredients.stream().filter(Optional::isPresent).count());
		assertTrue(ingredients.stream().flatMap(Optional::stream).anyMatch(ingredient -> ingredient.test(new ItemStack(Items.STONE))));
		assertTrue(ingredients.stream().flatMap(Optional::stream).anyMatch(ingredient -> ingredient.test(new ItemStack(Items.DIORITE))));
		assertTrue(RecipeViewerRecipeHelper.getShapedRecipe(wrappedRecipe).isPresent());
	}

	@Test
	void getIngredientsIncludesWrappedCustomShapelessRecipeIngredients() {
		WrappedCustomShapelessRecipe wrappedRecipe = new WrappedCustomShapelessRecipe(new ShapelessRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
				ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.GRANITE)), List.of(Ingredient.of(Items.STONE), Ingredient.of(Items.DIORITE))));

		Collection<Optional<Ingredient>> ingredients = RecipeViewerRecipeHelper.getIngredients(wrappedRecipe);

		assertEquals(2, ingredients.size());
		assertTrue(ingredients.stream().allMatch(Optional::isPresent));
		assertTrue(RecipeViewerRecipeHelper.getShapelessIngredients(wrappedRecipe).isPresent());
		assertTrue(RecipeViewerRecipeHelper.getShapelessResult(wrappedRecipe).filter(stack -> stack.is(Items.GRANITE)).isPresent());
	}

	private static class WrappedShapedRecipe implements CraftingRecipe, IWrapperRecipe<ShapedRecipe> {
		private final ShapedRecipe compose;

		private WrappedShapedRecipe(ShapedRecipe compose) {
			this.compose = compose;
		}

		@Override
		public ShapedRecipe getCompose() {
			return compose;
		}

		@Override
		public boolean matches(CraftingInput input, Level level) {
			return compose.matches(input, level);
		}

		@Override
		public ItemStack assemble(CraftingInput input) {
			return compose.assemble(input);
		}

		@Override
		public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
			return ShapedRecipe.SERIALIZER;
		}

		@Override
		public boolean showNotification() {
			return compose.showNotification();
		}

		@Override
		public String group() {
			return compose.group();
		}

		@Override
		public CraftingBookCategory category() {
			return compose.category();
		}

		@Override
		public PlacementInfo placementInfo() {
			return compose.placementInfo();
		}

		@Override
		public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
			return compose.display();
		}
	}

	private static class WrappedCustomShapelessRecipe extends CustomShapelessRecipe implements IWrapperRecipe<ShapelessRecipe> {
		private final ShapelessRecipe compose;

		private WrappedCustomShapelessRecipe(ShapelessRecipe compose) {
			super(compose.group(), compose.category(), compose.result, compose.ingredients);
			this.compose = compose;
		}

		@Override
		public ShapelessRecipe getCompose() {
			return compose;
		}

		@Override
		public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
			return ShapelessRecipe.SERIALIZER;
		}

		@Override
		public boolean showNotification() {
			return compose.showNotification();
		}
	}
}
