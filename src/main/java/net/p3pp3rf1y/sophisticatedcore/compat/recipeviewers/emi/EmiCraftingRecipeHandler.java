package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import com.google.common.base.Suppliers;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerCraftingSpecRecipe;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EmiCraftingRecipeHandler implements StandardRecipeHandler<CraftingMenu> {
	private final Map<EmiRecipe, Supplier<EmiRecipe>> transferRecipes = new IdentityHashMap<>();

	public void addRecipe(CraftingSpecEmiRecipe recipe) {
		IRecipeViewerCraftingSpecRecipe backingRecipe = (IRecipeViewerCraftingSpecRecipe) recipe.getBackingRecipe();
		addRecipe(recipe, backingRecipe.spec().shapeless(), backingRecipe.spec().width(), recipe.getDisplayInputSlots(),
				recipe.getDisplayOutputSlots().get(0).get(0));
	}

	public void addRecipe(GroupedCraftingEmiRecipe recipe) {
		GroupedCraftingRecipe backingRecipe = (GroupedCraftingRecipe) recipe.getBackingRecipe();
		addRecipe(recipe, false, backingRecipe.getDisplayWidth(), backingRecipe.getInputSlots(), backingRecipe.getResultStacks().get(0));
	}

	public void addRecipe(EmiRecipe recipe, boolean shapeless, int width, List<List<ItemStack>> inputSlots, ItemStack output) {
		// EMI's subtype comparisons are configured after recipe registration. Build this only when filling/rendering starts.
		transferRecipes.put(recipe, Suppliers.memoize(() -> createTransferRecipe(recipe, shapeless, width, inputSlots, output)));
	}

	private static EmiRecipe createTransferRecipe(EmiRecipe recipe, boolean shapeless, int width, List<List<ItemStack>> inputSlots, ItemStack output) {
		List<EmiIngredient> inputs = new ArrayList<>();
		for (int i = 0; i < inputSlots.size(); i++) {
			List<ItemStack> alternatives = inputSlots.get(i);
			EmiIngredient ingredient = alternatives.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(alternatives.stream().map(EmiStack::of).toList());
			if (!shapeless || !ingredient.isEmpty()) {
				inputs.add(ingredient);
			}
			if (!shapeless && (i + 1) % width == 0) {
				for (int column = width; column < 3; column++) {
					inputs.add(EmiStack.EMPTY);
				}
			}
		}
		// Lookup recipes keep their partial indexes; only filling sees this complete, unregistered grid.
		return new EmiCraftingRecipe(inputs, EmiStack.of(output), recipe.getId(), shapeless);
	}

	@Override
	public List<Slot> getInputSources(CraftingMenu menu) {
		return menu.slots.subList(1, 46);
	}

	@Override
	public List<Slot> getCraftingSlots(CraftingMenu menu) {
		return menu.slots.subList(1, 10);
	}

	@Override
	public Slot getOutputSlot(CraftingMenu menu) {
		return menu.getSlot(0);
	}

	@Override
	public boolean supportsRecipe(EmiRecipe recipe) {
		return transferRecipes.containsKey(recipe);
	}

	@Override
	public boolean canCraft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
		Supplier<EmiRecipe> transferRecipe = transferRecipes.get(recipe);
		return transferRecipe != null && StandardRecipeHandler.super.canCraft(transferRecipe.get(), context);
	}

	@Override
	public boolean craft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
		Supplier<EmiRecipe> transferRecipe = transferRecipes.get(recipe);
		return transferRecipe != null && StandardRecipeHandler.super.craft(transferRecipe.get(), context);
	}

	@Override
	public void render(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context, List<Widget> widgets, GuiGraphics draw) {
		Supplier<EmiRecipe> transferRecipe = transferRecipes.get(recipe);
		if (transferRecipe != null) {
			StandardRecipeHandler.super.render(transferRecipe.get(), context, widgets, draw);
		}
	}
}
