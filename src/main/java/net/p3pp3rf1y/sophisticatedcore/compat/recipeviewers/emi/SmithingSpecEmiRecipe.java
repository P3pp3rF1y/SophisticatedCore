package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplayView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SmithingSpecEmiRecipe implements EmiRecipe {
	private final SmithingDisplaySpec spec;
	private final List<SmithingDisplayVariant> variants;
	private final EmiIngredient template;
	private final EmiIngredient base;
	private final EmiIngredient addition;
	private final List<EmiStack> outputs;
	private final ResourceLocation id;

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

	private SmithingSpecEmiRecipe(SmithingDisplaySpec spec, List<SmithingDisplayVariant> variants, int variantIndex) {
		this.spec = spec;
		this.variants = variants;
		id = spec.id().withPath(path -> (path.startsWith("/") ? path : "/" + path) + (variantIndex >= 0 ? "/" + variantIndex : ""));
		template = EmiIngredient.of(spec.template().orElseThrow());
		base = EmiIngredient.of(spec.getBaseStacks(variants).stream().map(EmiStack::of).toList());
		addition = EmiIngredient.of(spec.addition().orElseThrow());
		outputs = spec.getResultStacks(variants).stream().map(EmiStack::of).toList();
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
		return List.of(template, base, addition);
	}

	@Override
	public List<EmiStack> getOutputs() {
		return outputs;
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

	private SmithingDisplayVariant getVariant(Random random) {
		return variants.get(random.nextInt(variants.size()));
	}
}
