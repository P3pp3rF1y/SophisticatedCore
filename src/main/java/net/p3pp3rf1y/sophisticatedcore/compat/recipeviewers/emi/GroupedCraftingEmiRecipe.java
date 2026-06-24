package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingVariant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntPredicate;

public class GroupedCraftingEmiRecipe extends BasicEmiRecipe {
	private final GroupedCraftingRecipe recipe;

	private GroupedCraftingEmiRecipe(ResourceLocation id, GroupedCraftingRecipe recipe, boolean indexInputs, boolean indexOutputs) {
		this(id, recipe, inputIndex -> indexInputs, indexOutputs);
	}

	private GroupedCraftingEmiRecipe(ResourceLocation id, GroupedCraftingRecipe recipe, IntPredicate inputIndexFilter, boolean indexOutputs) {
		super(VanillaEmiRecipeCategories.CRAFTING, id.withPath(path -> path.startsWith("/") ? path : "/" + path), 76, 54);
		this.recipe = recipe;
		List<List<ItemStack>> inputSlots = recipe.getInputSlots();
		for (int inputIndex = 0; inputIndex < inputSlots.size(); inputIndex++) {
			if (inputIndexFilter.test(inputIndex)) {
				inputs.add(EmiIngredient.of(inputSlots.get(inputIndex).stream().map(EmiStack::of).toList()));
			}
		}
		if (indexOutputs) {
			outputs.addAll(recipe.getResultStacks().stream().map(EmiStack::of).toList());
		}
	}

	public static List<GroupedCraftingEmiRecipe> ofGroupedUsageAndFocusedRecipes(GroupedCraftingRecipe recipe) {
		List<GroupedCraftingEmiRecipe> recipes = new ArrayList<>(recipe.getVariants().size() + 1);
		recipes.add(new GroupedCraftingEmiRecipe(recipe.getId(), recipe, inputIndex -> !isBroadFixedInput(recipe, inputIndex), false));
		addFixedInputFocusedUsageRecipes(recipe.getId(), recipe, recipes);
		for (int i = 0; i < recipe.getVariants().size(); i++) {
			int variantIndex = i;
			ResourceLocation id = recipe.getId().withPath(path -> path + "/" + variantIndex);
			GroupedCraftingRecipe narrowedRecipe = new GroupedCraftingRecipe(id, recipe.getDisplayWidth(), recipe.getDisplayHeight(),
					recipe.getFixedInputSlots(), List.of(recipe.getVariants().get(i)), recipe.getResultMatcher());
			recipes.add(new GroupedCraftingEmiRecipe(id, narrowedRecipe, false, true));
		}
		return recipes;
	}

	private static void addFixedInputFocusedUsageRecipes(ResourceLocation baseId, GroupedCraftingRecipe recipe, List<GroupedCraftingEmiRecipe> recipes) {
		List<List<ItemStack>> fixedInputSlots = recipe.getFixedInputSlots();
		for (int slotIndex = 0; slotIndex < fixedInputSlots.size(); slotIndex++) {
			List<ItemStack> fixedInputSlot = fixedInputSlots.get(slotIndex);
			if (fixedInputSlot.size() <= 1) {
				continue;
			}
			for (int stackIndex = 0; stackIndex < fixedInputSlot.size(); stackIndex++) {
				int focusedSlotIndex = slotIndex;
				int focusedStackIndex = stackIndex;
				ResourceLocation id = baseId.withPath(path -> path + "/input/" + focusedSlotIndex + "/" + focusedStackIndex);
				GroupedCraftingRecipe focusedRecipe = new GroupedCraftingRecipe(id, recipe.getDisplayWidth(), recipe.getDisplayHeight(),
						narrowFixedInputSlot(fixedInputSlots, slotIndex, fixedInputSlot.get(stackIndex)), recipe.getVariants(), recipe.getResultMatcher());
				recipes.add(new GroupedCraftingEmiRecipe(id, focusedRecipe, true, false));
			}
		}
	}

	private static boolean isBroadFixedInput(GroupedCraftingRecipe recipe, int inputIndex) {
		List<List<ItemStack>> fixedInputSlots = recipe.getFixedInputSlots();
		return inputIndex < fixedInputSlots.size() && fixedInputSlots.get(inputIndex).size() > 1;
	}

	private static List<List<ItemStack>> narrowFixedInputSlot(List<List<ItemStack>> fixedInputSlots, int focusedSlotIndex, ItemStack focusedStack) {
		List<List<ItemStack>> focusedSlots = new ArrayList<>(fixedInputSlots.size());
		for (int slotIndex = 0; slotIndex < fixedInputSlots.size(); slotIndex++) {
			focusedSlots.add(slotIndex == focusedSlotIndex ? List.of(focusedStack) : fixedInputSlots.get(slotIndex));
		}
		return focusedSlots;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 18, 18);
		widgets.addSlot(EmiIngredient.of(recipe.getFixedInputSlots().get(0).stream().map(EmiStack::of).toList()), 0, 0);
		int unique = getId().hashCode();
		widgets.addGeneratedSlot(random -> EmiStack.of(getVariant(random).displayedInputs().get(0)), unique, 0, 18);
		widgets.addGeneratedSlot(random -> EmiStack.of(getVariant(random).result()), unique, 50, 14).large(true).recipeContext(this);
	}

	@Override
	public Recipe<?> getBackingRecipe() {
		return recipe;
	}

	private GroupedCraftingVariant getVariant(Random random) {
		List<GroupedCraftingVariant> variants = recipe.getVariants();
		return variants.get(random.nextInt(variants.size()));
	}
}
