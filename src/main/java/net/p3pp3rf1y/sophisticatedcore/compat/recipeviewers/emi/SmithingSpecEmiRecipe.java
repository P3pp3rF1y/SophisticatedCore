package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplayView;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplayVariant;

import java.util.ArrayList;
import java.util.List;

public class SmithingSpecEmiRecipe implements EmiRecipe {
	private final SmithingDisplaySpec spec;
	private final List<SmithingDisplayVariant> variants;
	private final EmiIngredient template;
	private final EmiIngredient base;
	private final EmiIngredient addition;
	private final List<EmiStack> outputs;
	private final ResourceLocation id;
	private final boolean indexTemplate;
	private final boolean indexBase;
	private final boolean indexAddition;
	private final boolean indexOutputs;

	public static List<SmithingSpecEmiRecipe> of(SmithingDisplayView view) {
		List<SmithingSpecEmiRecipe> recipes = new ArrayList<>(view.variants().size());
		for (int i = 0; i < view.variants().size(); i++) {
			recipes.add(new SmithingSpecEmiRecipe(view.spec(), List.of(view.variants().get(i)), i));
		}
		return recipes;
	}

	public static List<SmithingSpecEmiRecipe> of(SmithingDisplaySpec spec) {
		List<SmithingDisplayVariant> variants = spec.getAllDisplays();
		List<SmithingSpecEmiRecipe> recipes = new ArrayList<>(variants.size());
		for (int i = 0; i < variants.size(); i++) {
			recipes.add(new SmithingSpecEmiRecipe(spec, List.of(variants.get(i)), i));
		}
		return recipes;
	}

	public static List<SmithingSpecEmiRecipe> ofGroupedUsageAndFocusedRecipes(SmithingDisplaySpec spec) {
		List<SmithingDisplayVariant> variants = spec.getAllDisplays();
		List<SmithingSpecEmiRecipe> recipes = new ArrayList<>(variants.size() * 2 + 1);
		recipes.add(new SmithingSpecEmiRecipe(spec, variants, "", true, false, true, false));
		for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
			recipes.add(new SmithingSpecEmiRecipe(spec, List.of(variants.get(variantIndex)), "/source/" + variantIndex, false, true, false, false));
			recipes.add(new SmithingSpecEmiRecipe(spec, List.of(variants.get(variantIndex)), "/" + variantIndex, false, false, false, true));
		}
		return recipes;
	}

	private SmithingSpecEmiRecipe(SmithingDisplaySpec spec, List<SmithingDisplayVariant> variants, int variantIndex) {
		this(spec, variants, variantIndex >= 0 ? "/" + variantIndex : "", true, true, true, true);
	}

	private SmithingSpecEmiRecipe(SmithingDisplaySpec spec, List<SmithingDisplayVariant> variants, String idSuffix, boolean indexTemplate, boolean indexBase, boolean indexAddition, boolean indexOutputs) {
		this.spec = spec;
		this.variants = variants;
		id = spec.id().withPath(path -> (path.startsWith("/") ? path : "/" + path) + idSuffix);
		template = EmiIngredient.of(spec.template().orElseThrow());
		base = EmiIngredient.of(spec.getBaseStacks(variants).stream().map(EmiStack::of).toList());
		addition = EmiIngredient.of(spec.addition().orElseThrow());
		outputs = spec.getResultStacks(variants).stream().map(EmiStack::of).toList();
		this.indexTemplate = indexTemplate;
		this.indexBase = indexBase;
		this.indexAddition = indexAddition;
		this.indexOutputs = indexOutputs;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return VanillaEmiRecipeCategories.SMITHING;
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		List<EmiIngredient> inputs = new ArrayList<>(3);
		if (indexTemplate) {
			inputs.add(template);
		}
		if (indexBase) {
			inputs.add(base);
		}
		if (indexAddition) {
			inputs.add(addition);
		}
		return inputs;
	}

	@Override
	public List<EmiStack> getOutputs() {
		return indexOutputs ? outputs : List.of();
	}

	public List<List<ItemStack>> getDisplayInputSlots() {
		return List.of(itemStacks(template), itemStacks(base), itemStacks(addition));
	}

	public List<List<ItemStack>> getDisplayOutputSlots() {
		return outputs.stream().map(EmiStack::getItemStack).map(List::of).toList();
	}

	private static List<ItemStack> itemStacks(EmiIngredient ingredient) {
		return ingredient.getEmiStacks().stream().map(EmiStack::getItemStack).toList();
	}

	@Override
	public int getDisplayWidth() {
		return 112;
	}

	@Override
	public int getDisplayHeight() {
		return 18;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 62, 1);
		widgets.addSlot(template, 0, 0);
		int unique = spec.id().hashCode();
		widgets.addGeneratedSlot(random -> EmiStack.of(getVariant(random).base()), unique, 18, 0);
		widgets.addSlot(addition, 36, 0);
		widgets.addGeneratedSlot(random -> EmiStack.of(getVariant(random).result()), unique, 94, 0).recipeContext(this);
	}

	private SmithingDisplayVariant getVariant(java.util.Random random) {
		return variants.get(random.nextInt(variants.size()));
	}
}
