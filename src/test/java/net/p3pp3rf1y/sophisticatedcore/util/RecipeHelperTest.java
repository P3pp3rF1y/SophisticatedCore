package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RecipeHelperTest {

	private static Level regularOrderRecipesLevel;
	private static Level reverseOrderRecipesLevel;

	private static List<RecipeHolder<CraftingRecipe>> getCraftingRecipes() {
		List<RecipeHolder<CraftingRecipe>> craftingRecipes = new ArrayList<>();
		//stones
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("granite_to_diorite"), new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(3, 3, ingredients(Items.GRANITE), Optional.empty()), new ItemStack(Items.DIORITE))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("granite_from_diorite"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.GRANITE, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.DIORITE)))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("stone_to_granite"), new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(3, 3, ingredients(Items.STONE), Optional.empty()), new ItemStack(Items.GRANITE))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("stone_from_granite"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.STONE, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GRANITE)))));

		//gold
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("gold_ingot_to_gold_block"), new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(3, 3, ingredients(Items.GOLD_INGOT), Optional.empty()), new ItemStack(Items.GOLD_BLOCK))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("gold_ingot_from_gold_block"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.GOLD_INGOT, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GOLD_BLOCK)))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("gold_nugget_to_gold_ingot"), new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(3, 3, ingredients(Items.GOLD_NUGGET), Optional.empty()), new ItemStack(Items.GOLD_INGOT))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("gold_nugget_from_gold_ingot"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.GOLD_NUGGET, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GOLD_INGOT)))));

		//hollow 3x3 reversible compression
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("charcoal_to_coal_hollow"), new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(3, 3, hollowIngredients(Items.CHARCOAL), Optional.empty()), new ItemStack(Items.COAL))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("charcoal_from_coal"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.CHARCOAL, 8), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.COAL)))));


		//confusion recipes
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("gold_nugget_to_diorite"), new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(3, 3, ingredients(Items.GOLD_NUGGET), Optional.empty()), new ItemStack(Items.DIORITE))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("granite_to_gold_block"), new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(3, 3, ingredients(Items.GRANITE), Optional.empty()), new ItemStack(Items.GOLD_BLOCK))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("gold_nugget_from_granite"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.GOLD_NUGGET, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GRANITE)))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("granite_from_diamond"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.GRANITE, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.DIAMOND)))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("iron_nugget_from_granite"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.IRON_NUGGET, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GRANITE)))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("stone_from_gold_ingot"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.STONE, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GOLD_INGOT)))));
		craftingRecipes.add(new RecipeHolder<>(ResourceLocation.parse("torches_from_gold_block"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.TORCH, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GOLD_BLOCK)))));

		return craftingRecipes;
	}

	static Stream<Level> classParams() {
		return Stream.of(regularOrderRecipesLevel, reverseOrderRecipesLevel);
	}

	static Stream<Arguments> withClassParams(List<Arguments> methodParams) {
		return classParams().flatMap(classParam -> methodParams.stream().map(arguments -> new CombinedArguments(classParam, arguments)));
	}

	private static class CombinedArguments implements Arguments {
		private final Object[] arguments;

		public CombinedArguments(Level level, Arguments methodArguments) {
			arguments = new Object[methodArguments.get().length + 1];
			arguments[0] = level;
			System.arraycopy(methodArguments.get(), 0, arguments, 1, methodArguments.get().length);
		}
		@Override
		public Object[] get() {
			return arguments;
		}
	}

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		regularOrderRecipesLevel = getLevelWithRecipeManagerFor(getCraftingRecipes());

		List<RecipeHolder<CraftingRecipe>> reverseOrderRecipes = getCraftingRecipes();
		Collections.reverse(reverseOrderRecipes);
		reverseOrderRecipesLevel = getLevelWithRecipeManagerFor(reverseOrderRecipes);
	}

	private static Level getLevelWithRecipeManagerFor(List<RecipeHolder<CraftingRecipe>> craftingRecipes) {
		RecipeManager mockRecipeManager = mock(RecipeManager.class);
		when(mockRecipeManager.getRecipesFor(eq(RecipeType.CRAFTING), any(CraftingInput.class), any())).thenAnswer(i -> {
			List<RecipeHolder<CraftingRecipe>> matchingRecipes = new ArrayList<>();
			CraftingInput craftingInput = i.getArgument(1);
			Level level = i.getArgument(2);
			for (RecipeHolder<CraftingRecipe> craftingRecipe : craftingRecipes) {
				if (craftingRecipe.value().matches(craftingInput, level)) {
					matchingRecipes.add(craftingRecipe);
				}
			}
			return matchingRecipes;
		});

		Level level = mock(Level.class);
		when(level.getRecipeManager()).thenReturn(mockRecipeManager);
		return level;
	}

	private static NonNullList<Ingredient> ingredients(Item item) {
		return NonNullList.of(Ingredient.EMPTY,
				Ingredient.of(item), Ingredient.of(item), Ingredient.of(item),
				Ingredient.of(item), Ingredient.of(item), Ingredient.of(item),
				Ingredient.of(item), Ingredient.of(item), Ingredient.of(item)
		);
	}

	private static NonNullList<Ingredient> hollowIngredients(Item item) {
		return NonNullList.of(Ingredient.EMPTY,
				Ingredient.of(item), Ingredient.of(item), Ingredient.of(item),
				Ingredient.of(item), Ingredient.EMPTY, Ingredient.of(item),
				Ingredient.of(item), Ingredient.of(item), Ingredient.of(item)
		);
	}

	@AfterEach
	void clearCache() {
		RecipeHelper.onRecipesUpdated(null);
	}

	@ParameterizedTest
	@MethodSource
	void getCompactingResultReturnsResultForFullThreeByThreeShape(Level level, Item item, RecipeHelper.CompactingResult expectedResult) {
		RecipeHelper.setLevel(level);

		RecipeHelper.CompactingResult actualResult = RecipeHelper.getCompactingResult(new ItemStack(item), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE);

		assertCompactingResultEquals(expectedResult, actualResult, "getCompactingResult returned wrong result");
	}

	static Stream<Arguments> getCompactingResultReturnsResultForFullThreeByThreeShape() {
		return withClassParams(
				List.of(
						Arguments.of(Items.GOLD_INGOT, new RecipeHelper.CompactingResult(new ItemStack(Items.GOLD_BLOCK), Collections.emptyList())),
						Arguments.of(Items.GOLD_NUGGET, new RecipeHelper.CompactingResult(new ItemStack(Items.GOLD_INGOT), Collections.emptyList())),
						Arguments.of(Items.GRANITE, new RecipeHelper.CompactingResult(new ItemStack(Items.DIORITE), Collections.emptyList())),
						Arguments.of(Items.STONE, new RecipeHelper.CompactingResult(new ItemStack(Items.GRANITE), Collections.emptyList()))
				)
		);
	}

	@ParameterizedTest
	@MethodSource
	void getCompactingResultReturnsResultForDonutShape(Level level) {
		RecipeHelper.setLevel(level);

		RecipeHelper.CompactingResult actualResult = RecipeHelper.getCompactingResult(new ItemStack(Items.CHARCOAL), getHollowThreeByThreeShape());

		assertCompactingResultEquals(new RecipeHelper.CompactingResult(new ItemStack(Items.COAL), Collections.emptyList()), actualResult, "getCompactingResult returned wrong result for masked shape");
	}

	static Stream<Arguments> getCompactingResultReturnsResultForDonutShape() {
		return classParams().map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource
	void doesUncompactMatchReturnsTrueForReversibleDonutShape(Level level) {
		RecipeHelper.setLevel(level);

		if (!RecipeHelper.doesUncompactMatch(new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL), 8)) {
			assertionFailure().message("doesUncompactMatch returned false for reversible hollow 3x3 recipe").buildAndThrow();
		}
	}

	static Stream<Arguments> doesUncompactMatchReturnsTrueForReversibleDonutShape() {
		return classParams().map(Arguments::of);
	}

	@Test
	void compactingRecipeShapeParseReturnsDonutShapeWithExpectedFilledSlots() {
		String shapeDefinition = "111/101/111";

		RecipeHelper.CompactingRecipeShape shape = RecipeHelper.CompactingRecipeShape.parse(shapeDefinition).orElseThrow();

		assertEquals(3, shape.width());
		assertEquals(3, shape.height());
		assertEquals(8, shape.ingredientCount());
		assertTrue(shape.isSlotFilled(0));
		assertTrue(shape.isSlotFilled(1));
		assertTrue(shape.isSlotFilled(2));
		assertTrue(shape.isSlotFilled(3));
		assertFalse(shape.isSlotFilled(4));
		assertTrue(shape.isSlotFilled(5));
		assertTrue(shape.isSlotFilled(6));
		assertTrue(shape.isSlotFilled(7));
		assertTrue(shape.isSlotFilled(8));
	}

	private static RecipeHelper.CompactingRecipeShape getHollowThreeByThreeShape() {
		return RecipeHelper.CompactingRecipeShape.parse("111/101/111").orElseThrow();
	}


	@ParameterizedTest
	@MethodSource
	void getUncompactingResultReturnsExpectedResult(Level level, Item item, RecipeHelper.UncompactingResult expectedResult) {
		RecipeHelper.setLevel(level);

		RecipeHelper.UncompactingResult actualResult = RecipeHelper.getUncompactingResult(new ItemStack(item));

		assertUncompactingResultEquals(expectedResult, actualResult, "getUncompactingResult returned wrong result");
	}

	static Stream<Arguments> getUncompactingResultReturnsExpectedResult() {
		return withClassParams(
				List.of(
						Arguments.of(Items.GOLD_BLOCK, new RecipeHelper.UncompactingResult(new ItemStack(Items.GOLD_INGOT), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
						Arguments.of(Items.GOLD_INGOT, new RecipeHelper.UncompactingResult(new ItemStack(Items.GOLD_NUGGET), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
						Arguments.of(Items.DIORITE, new RecipeHelper.UncompactingResult(new ItemStack(Items.GRANITE), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
						Arguments.of(Items.GRANITE, new RecipeHelper.UncompactingResult(new ItemStack(Items.STONE), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE))
				)
		);
	}

	@ParameterizedTest
	@MethodSource
	void getItemCompactingShapesReturnsExpectedShapes(Level level, Item item, Set<RecipeHelper.CompactingShape> shapes) {
		RecipeHelper.setLevel(level);

		Set<RecipeHelper.CompactingShape> actualShapes = RecipeHelper.getItemCompactingShapes(new ItemStack(item));

		if (!Objects.equals(shapes, actualShapes)) {
			assertionFailure().message("getItemCompactingShapes returned wrong result")
					.expected(shapes)
					.actual(actualShapes)
					.buildAndThrow();
		}
	}

	static Stream<Arguments> getItemCompactingShapesReturnsExpectedShapes() {
		return withClassParams(
				List.of(
						Arguments.of(Items.GOLD_INGOT, Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
						Arguments.of(Items.GOLD_NUGGET, Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
						Arguments.of(Items.GRANITE, Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
						Arguments.of(Items.STONE, Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE))
				)
		);
	}

	private static void assertCompactingResultEquals(RecipeHelper.CompactingResult expected, RecipeHelper.CompactingResult actual, Object message) {
		if (ItemStack.matches(expected.getResult(), actual.getResult())
				&& areItemListsEqual(expected.getRemainingItems(), actual.getRemainingItems())) {
			return;
		}

		assertionFailure().message(message)
				.expected(expected.getResult() + ":" + expected.getRemainingItems())
				.actual(actual.getResult() + ":" + actual.getRemainingItems())
				.buildAndThrow();
	}


	private static boolean areItemListsEqual(List<ItemStack> expected, List<ItemStack> actual) {
		if (expected.size() != actual.size()) {
			return false;
		}
		for (int i = 0; i < expected.size(); i++) {
			if (!ItemStack.matches(expected.get(i), actual.get(i))) {
				return false;
			}
		}
		return true;
	}
	private static void assertUncompactingResultEquals(RecipeHelper.UncompactingResult expected, RecipeHelper.UncompactingResult actual, Object message) {
		if (ItemStack.isSameItemSameComponents(expected.getResult(), actual.getResult()) && expected.getCompactUsingShape() == actual.getCompactUsingShape()) {
			return;
		}
		assertionFailure().message(message)
				.expected(expected.getResult() + ":" + expected.getCompactUsingShape())
				.actual(actual.getResult() + ":" + actual.getCompactUsingShape())
				.buildAndThrow();
	}
}
