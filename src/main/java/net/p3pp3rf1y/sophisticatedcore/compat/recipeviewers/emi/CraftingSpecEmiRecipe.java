package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayView;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IGroupedOutputFocusBehavior;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntPredicate;

public class CraftingSpecEmiRecipe extends BasicEmiRecipe {
	private final CraftingDisplaySpec spec;
	private final List<CraftingDisplayVariant> variants;
	private final List<EmiIngredient> gridInputs;
	private final int sourceInputIndex;

	// EMI lacks the dynamic focused lookup hooks used by JEI/REI, so focus-only
	// variants are registered statically and constrained through input/output indexing.
	public static List<CraftingSpecEmiRecipe> ofGroupedUsageAndFocusedRecipes(CraftingDisplaySpec spec) {
		List<CraftingDisplayVariant> variants = spec.getAllDisplays();
		if (variants.isEmpty()) {
			return List.of();
		}
		List<CraftingDisplayVariant> globalVariants = spec.getGlobalDisplays();
		boolean groupGlobalOutputs = spec.focusBehavior() instanceof IGroupedOutputFocusBehavior;
		int sourceInputIndex = getSourceInputIndex(spec);
		List<CraftingSpecEmiRecipe> recipes = new ArrayList<>(sourceInputIndex >= 0 ? variants.size() * 2 + 1 : variants.size() + 1);
		recipes.add(new CraftingSpecEmiRecipe(spec, variants, "", inputIndex -> inputIndex != sourceInputIndex, false));
		if (groupGlobalOutputs && !globalVariants.isEmpty()) {
			recipes.add(new CraftingSpecEmiRecipe(spec, globalVariants, "/outputs", inputIndex -> false, true));
		}
		for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
			if (sourceInputIndex >= 0) {
				recipes.add(new CraftingSpecEmiRecipe(spec, List.of(variants.get(variantIndex)), "/source/" + variantIndex, inputIndex -> inputIndex == sourceInputIndex, false));
			}
			if (!groupGlobalOutputs || !globalVariants.contains(variants.get(variantIndex))) {
				recipes.add(new CraftingSpecEmiRecipe(spec, List.of(variants.get(variantIndex)), "/" + variantIndex, inputIndex -> false, true));
			}
		}
		return recipes;
	}

	public static List<CraftingSpecEmiRecipe> of(CraftingDisplayView view) {
		List<CraftingSpecEmiRecipe> recipes = new ArrayList<>(view.variants().size());
		for (int i = 0; i < view.variants().size(); i++) {
			recipes.add(new CraftingSpecEmiRecipe(view.spec(), List.of(view.variants().get(i)), i));
		}
		return recipes;
	}

	public static List<CraftingSpecEmiRecipe> of(CraftingDisplaySpec spec) {
		List<CraftingDisplayVariant> variants = spec.getAllDisplays();
		List<CraftingSpecEmiRecipe> recipes = new ArrayList<>(variants.size());
		for (int i = 0; i < variants.size(); i++) {
			recipes.add(new CraftingSpecEmiRecipe(spec, List.of(variants.get(i)), i));
		}
		return recipes;
	}

	private CraftingSpecEmiRecipe(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants) {
		this(spec, variants, -1);
	}

	private CraftingSpecEmiRecipe(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants, int variantIndex) {
		this(spec, variants, variantIndex, inputIndex -> true, true);
	}

	private CraftingSpecEmiRecipe(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants, int variantIndex, IntPredicate inputIndexFilter, boolean indexOutputs) {
		this(spec, variants, variantIndex >= 0 ? "/" + variantIndex : "", inputIndexFilter, indexOutputs);
	}

	private CraftingSpecEmiRecipe(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants, String idSuffix, IntPredicate inputIndexFilter, boolean indexOutputs) {
		super(VanillaEmiRecipeCategories.CRAFTING, spec.id().withPath(path -> (path.startsWith("/") ? path : "/" + path) + idSuffix), 118, 54);
		this.spec = spec;
		this.variants = variants;
		this.gridInputs = getGridInputs(spec, variants);
		this.sourceInputIndex = getSourceInputIndex(spec);
		for (int i = 0; i < gridInputs.size(); i++) {
			EmiIngredient ingredient = gridInputs.get(i);
			if (inputIndexFilter.test(i) && !ingredient.isEmpty()) {
				inputs.add(ingredient);
			}
		}
		if (indexOutputs) {
			outputs.addAll(spec.getOutputStacks(variants).stream().map(EmiStack::of).toList());
		}
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 18);
		if (spec.shapeless()) {
			widgets.addTexture(EmiTexture.SHAPELESS, 97, 0);
		}
		int sOff = 0;
		if (!spec.shapeless()) {
			if (canFit(1, 3)) {
				sOff -= 1;
			}
			if (canFit(3, 1)) {
				sOff -= 3;
			}
		}
		int unique = spec.id().hashCode();
		for (int i = 0; i < 9; i++) {
			int s = i + sOff;
			if (s >= 0 && s < gridInputs.size()) {
				if (s == sourceInputIndex) {
					widgets.addGeneratedSlot(random -> EmiStack.of(getVariant(random).inputs().get(sourceInputIndex)), unique, i % 3 * 18, i / 3 * 18);
				} else {
					widgets.addSlot(gridInputs.get(s), i % 3 * 18, i / 3 * 18);
				}
			} else {
				widgets.addSlot(EmiStack.EMPTY, i % 3 * 18, i / 3 * 18);
			}
		}
		widgets.addGeneratedSlot(random -> EmiStack.of(getVariant(random).firstOutput()), unique, 92, 14).large(true).recipeContext(this);
	}

	@Override
	public Recipe<?> getBackingRecipe() {
		return spec.recipe(variants);
	}

	public List<List<ItemStack>> getDisplayInputSlots() {
		return spec.getInputSlots(variants);
	}

	public List<List<ItemStack>> getDisplayOutputSlots() {
		return spec.getOutputStacks(variants).stream().map(List::of).toList();
	}

	private boolean canFit(int width, int height) {
		if (gridInputs.size() > 9) {
			return false;
		}
		for (int i = 0; i < gridInputs.size(); i++) {
			int x = i % 3;
			int y = i / 3;
			if (!gridInputs.get(i).isEmpty() && (x >= width || y >= height)) {
				return false;
			}
		}
		return true;
	}

	private CraftingDisplayVariant getVariant(Random random) {
		return variants.get(random.nextInt(variants.size()));
	}

	private static List<EmiIngredient> getGridInputs(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants) {
		return spec.getInputSlots(variants).stream()
				.map(inputSlot -> inputSlot.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(inputSlot.stream().map(EmiStack::of).toList()))
				.toList();
	}

	private static int getSourceInputIndex(CraftingDisplaySpec spec) {
		return spec.focusBehavior() instanceof SourceResultFocusBehavior sourceResultFocusBehavior ? sourceResultFocusBehavior.sourceInputIndex() : -1;
	}
}
