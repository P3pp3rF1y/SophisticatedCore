package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingVariant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GroupedCraftingEmiRecipe extends BasicEmiRecipe {
	private final GroupedCraftingRecipe recipe;

	private GroupedCraftingEmiRecipe(ResourceLocation id, GroupedCraftingRecipe recipe, boolean indexInputs, boolean indexOutputs) {
		super(VanillaEmiRecipeCategories.CRAFTING, id.withPath(path -> path.startsWith("/") ? path : "/" + path), 76, 54);
		this.recipe = recipe;
		if (indexInputs) {
			recipe.getInputSlots().forEach(inputSlot -> inputs.add(EmiIngredient.of(inputSlot.stream().map(EmiStack::of).toList())));
		}
		if (indexOutputs) {
			outputs.addAll(recipe.getResultStacks().stream().map(EmiStack::of).toList());
		}
	}

	public static List<GroupedCraftingEmiRecipe> ofGroupedUsageAndFocusedRecipes(GroupedCraftingRecipe recipe) {
		List<GroupedCraftingEmiRecipe> recipes = new ArrayList<>(recipe.getVariants().size() + 1);
		recipes.add(new GroupedCraftingEmiRecipe(recipe.getId(), recipe, true, false));
		for (int i = 0; i < recipe.getVariants().size(); i++) {
			int variantIndex = i;
			ResourceLocation id = recipe.getId().withPath(path -> path + "/" + variantIndex);
			GroupedCraftingRecipe narrowedRecipe = new GroupedCraftingRecipe(id, recipe.getDisplayWidth(), recipe.getDisplayHeight(), recipe.getFixedInputSlots(), List.of(recipe.getVariants().get(i)));
			recipes.add(new GroupedCraftingEmiRecipe(id, narrowedRecipe, false, true));
		}
		return recipes;
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
