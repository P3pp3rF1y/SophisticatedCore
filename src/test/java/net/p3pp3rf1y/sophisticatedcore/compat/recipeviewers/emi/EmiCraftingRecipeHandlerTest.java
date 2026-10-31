package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiComparisonDefaults;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmiCraftingRecipeHandlerTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void addRecipeUsesCompleteGridForRecipeAndUsageViews() {
		List<CraftingSpecEmiRecipe> recipes = craftingSpecRecipes();
		EmiCraftingRecipeHandler handler = new EmiCraftingRecipeHandler();
		for (CraftingSpecEmiRecipe recipe : recipes) {
			handler.addRecipe(recipe);
		}
		List<EmiCraftContext<CraftingMenu>> contexts = recipes.stream().map(recipe -> mockCraftContext()).toList();
		List<Boolean> canCraftResults = new ArrayList<>();

		for (int i = 0; i < recipes.size(); i++) {
			canCraftResults.add(handler.canCraft(recipes.get(i), contexts.get(i)));
		}

		for (int i = 0; i < recipes.size(); i++) {
			assertTrue(canCraftResults.get(i));
			assertTransferInputs(capturedTransferRecipe(contexts.get(i)), List.of(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
					Optional.of(Items.STRING), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
		}
	}

	@Test
	void addRecipeRetainsBaseMaterialsWhenVariantIsSparse() {
		NonNullList<Ingredient> baseIngredients = NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.STRING), Ingredient.of(Items.STICK),
				Ingredient.of(Items.DIRT), Ingredient.of(Items.COBBLESTONE));
		CraftingDisplaySpec spec = new CraftingDisplaySpec(new ResourceLocation("test:base_materials"), false, 2, 2, baseIngredients,
				List.of(new CraftingDisplayVariant(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.DIAMOND)),
						List.of(new ItemStack(Items.EMERALD)))),
				new SourceResultFocusBehavior(3, (variant, stack) -> Optional.empty(), (variant, stack) -> Optional.empty()));
		CraftingSpecEmiRecipe recipe = CraftingSpecEmiRecipe.ofGroupedUsageAndFocusedRecipes(spec).get(0);
		EmiCraftingRecipeHandler handler = new EmiCraftingRecipeHandler();
		handler.addRecipe(recipe);
		EmiCraftContext<CraftingMenu> context = mockCraftContext();

		boolean canCraft = handler.canCraft(recipe, context);

		assertTrue(canCraft);
		assertTransferInputs(capturedTransferRecipe(context), List.of(Optional.of(Items.STRING), Optional.of(Items.STICK), Optional.empty(),
				Optional.of(Items.DIRT), Optional.of(Items.DIAMOND), Optional.empty()));
	}

	@Test
	void addRecipePadsOneByTwoAndTwoByTwoShapesToThreeColumns() {
		EmiCraftingRecipeHandler handler = new EmiCraftingRecipeHandler();
		EmiRecipe oneByTwo = new EmiCraftingRecipe(List.of(), EmiStack.of(Items.DIAMOND), new ResourceLocation("test:one_by_two"));
		EmiRecipe twoByTwo = new EmiCraftingRecipe(List.of(), EmiStack.of(Items.EMERALD), new ResourceLocation("test:two_by_two"));
		handler.addRecipe(oneByTwo, false, 1, List.of(List.of(new ItemStack(Items.STRING)), List.of(new ItemStack(Items.STICK))), new ItemStack(Items.DIAMOND));
		handler.addRecipe(twoByTwo, false, 2, List.of(List.of(new ItemStack(Items.STRING)), List.of(new ItemStack(Items.STICK)),
				List.of(new ItemStack(Items.DIRT)), List.of(new ItemStack(Items.COBBLESTONE))), new ItemStack(Items.EMERALD));
		EmiCraftContext<CraftingMenu> oneByTwoContext = mockCraftContext();
		EmiCraftContext<CraftingMenu> twoByTwoContext = mockCraftContext();

		boolean canCraftOneByTwo = handler.canCraft(oneByTwo, oneByTwoContext);
		boolean canCraftTwoByTwo = handler.canCraft(twoByTwo, twoByTwoContext);

		assertTrue(canCraftOneByTwo);
		assertTrue(canCraftTwoByTwo);
		assertTransferInputs(capturedTransferRecipe(oneByTwoContext),
				List.of(Optional.of(Items.STRING), Optional.empty(), Optional.empty(), Optional.of(Items.STICK), Optional.empty(), Optional.empty()));
		assertTransferInputs(capturedTransferRecipe(twoByTwoContext), List.of(Optional.of(Items.STRING), Optional.of(Items.STICK), Optional.empty(),
				Optional.of(Items.DIRT), Optional.of(Items.COBBLESTONE), Optional.empty()));
	}

	@Test
	void addRecipeCompactsShapelessInputs() {
		EmiCraftingRecipeHandler handler = new EmiCraftingRecipeHandler();
		EmiRecipe recipe = new EmiCraftingRecipe(List.of(), EmiStack.of(Items.DIAMOND), new ResourceLocation("test:shapeless"));
		handler.addRecipe(recipe, true, 3, List.of(List.of(new ItemStack(Items.STRING)), List.of(), List.of(new ItemStack(Items.STICK))),
				new ItemStack(Items.DIAMOND));
		EmiCraftContext<CraftingMenu> context = mockCraftContext();

		boolean canCraft = handler.canCraft(recipe, context);

		assertTrue(canCraft);
		assertTransferInputs(capturedTransferRecipe(context), List.of(Optional.of(Items.STRING), Optional.of(Items.STICK)));
	}

	@Test
	void addRecipeUsesNarrowFocusedAlternativesWithoutChangingIndexedRecipe() {
		GroupedCraftingRecipe groupedRecipe = new GroupedCraftingRecipe(new ResourceLocation("test:focused"), 1, 2,
				List.of(List.of(new ItemStack(Items.STRING), new ItemStack(Items.STICK))),
				List.of(new GroupedCraftingVariant(List.of(new ItemStack(Items.DIRT)), new ItemStack(Items.DIAMOND))));
		List<GroupedCraftingEmiRecipe> recipes = GroupedCraftingEmiRecipe.ofGroupedUsageAndFocusedRecipes(groupedRecipe);
		GroupedCraftingEmiRecipe focusedRecipe = recipes.get(1);
		List<EmiIngredient> indexedInputs = List.copyOf(focusedRecipe.getInputs());
		EmiCraftingRecipeHandler handler = new EmiCraftingRecipeHandler();
		for (GroupedCraftingEmiRecipe recipe : recipes) {
			handler.addRecipe(recipe);
		}
		EmiCraftContext<CraftingMenu> context = mockCraftContext();

		boolean canCraft = handler.canCraft(focusedRecipe, context);

		assertTrue(canCraft);
		assertEquals(indexedInputs, focusedRecipe.getInputs());
		assertTransferInputs(capturedTransferRecipe(context),
				List.of(Optional.of(Items.STRING), Optional.empty(), Optional.empty(), Optional.of(Items.DIRT), Optional.empty(), Optional.empty()));
	}

	@Test
	void supportsRecipeRejectsUnregisteredCraftingRecipeWithSameId() {
		CraftingSpecEmiRecipe registeredRecipe = craftingSpecRecipes().get(0);
		EmiCraftingRecipeHandler handler = new EmiCraftingRecipeHandler();
		handler.addRecipe(registeredRecipe);
		EmiRecipe unregisteredRecipe = new EmiCraftingRecipe(List.of(EmiStack.of(Items.STRING)), EmiStack.of(Items.DIAMOND), registeredRecipe.getId());
		EmiCraftContext<CraftingMenu> context = mockCraftContext();

		boolean supportsRecipe = handler.supportsRecipe(unregisteredRecipe);
		boolean canCraft = handler.canCraft(unregisteredRecipe, context);

		assertFalse(supportsRecipe);
		assertFalse(canCraft);
		verifyNoInteractions(context);
	}

	@Test
	void addRecipeBuildsAlternativesAfterSubtypeComparisonConfiguration() {
		ItemStack firstAlternative = new ItemStack(Items.OAK_PLANKS);
		firstAlternative.setHoverName(Component.literal("first"));
		ItemStack secondAlternative = new ItemStack(Items.OAK_PLANKS);
		secondAlternative.setHoverName(Component.literal("second"));
		EmiRecipe recipe = new EmiCraftingRecipe(List.of(), EmiStack.of(Items.DIAMOND), new ResourceLocation("test:subtype_timing"));
		EmiCraftingRecipeHandler handler = new EmiCraftingRecipeHandler();
		handler.addRecipe(recipe, false, 1, List.of(List.of(firstAlternative, secondAlternative)), new ItemStack(Items.DIAMOND));
		EmiCraftContext<CraftingMenu> context = mockCraftContext();
		Map<Object, Comparison> originalComparisons = EmiComparisonDefaults.comparisons;
		try {
			EmiComparisonDefaults.comparisons = Map.of(Items.OAK_PLANKS, Comparison.compareNbt());

			boolean canCraft = handler.canCraft(recipe, context);

			assertTrue(canCraft);
			EmiIngredient transferIngredient = capturedTransferRecipe(context).getInputs().get(0);
			assertEquals(2, transferIngredient.getEmiStacks().size());
			assertEquals(EmiStack.of(firstAlternative), transferIngredient.getEmiStacks().get(0));
			assertEquals(EmiStack.of(secondAlternative), transferIngredient.getEmiStacks().get(1));
		} finally {
			EmiComparisonDefaults.comparisons = originalComparisons;
		}
	}

	private static List<CraftingSpecEmiRecipe> craftingSpecRecipes() {
		NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
		ingredients.set(4, Ingredient.of(Items.STRING));
		CraftingDisplaySpec spec = new CraftingDisplaySpec(new ResourceLocation("test:crafting_spec"), false, 3, 3, ingredients,
				List.of(new CraftingDisplayVariant(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STRING),
						ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY), List.of(new ItemStack(Items.DIAMOND)))),
				new SourceResultFocusBehavior(4, (variant, stack) -> Optional.empty(), (variant, stack) -> Optional.empty()));
		return CraftingSpecEmiRecipe.ofGroupedUsageAndFocusedRecipes(spec);
	}

	private static EmiRecipe capturedTransferRecipe(EmiCraftContext<CraftingMenu> context) {
		ArgumentCaptor<EmiRecipe> recipeCaptor = ArgumentCaptor.forClass(EmiRecipe.class);
		verify(context.getInventory()).canCraft(recipeCaptor.capture());
		return recipeCaptor.getValue();
	}

	@SuppressWarnings("unchecked")
	private static EmiCraftContext<CraftingMenu> mockCraftContext() {
		EmiPlayerInventory inventory = mock(EmiPlayerInventory.class);
		EmiCraftContext<CraftingMenu> context = mock(EmiCraftContext.class);
		when(context.getInventory()).thenReturn(inventory);
		when(inventory.canCraft(any(EmiRecipe.class))).thenReturn(true);
		return context;
	}

	private static void assertTransferInputs(EmiRecipe recipe, List<Optional<Item>> expectedItems) {
		List<EmiIngredient> inputs = recipe.getInputs();
		assertEquals(expectedItems.size(), inputs.size());
		for (int inputIndex = 0; inputIndex < expectedItems.size(); inputIndex++) {
			Optional<Item> expectedItem = expectedItems.get(inputIndex);
			EmiIngredient input = inputs.get(inputIndex);
			if (expectedItem.isEmpty()) {
				assertTrue(input.isEmpty(), "Expected an empty transfer input at index " + inputIndex);
			} else {
				assertEquals(1, input.getEmiStacks().size(), "Expected one transfer input alternative at index " + inputIndex);
				assertTrue(input.getEmiStacks().get(0).getItemStack().is(expectedItem.get()),
						"Expected " + expectedItem.get() + " at transfer input index " + inputIndex);
			}
		}
	}
}
