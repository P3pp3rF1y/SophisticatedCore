package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;

public class HoldingRecipeOutput implements RecipeOutput {
	private final Advancement.Builder advancement;
	private Recipe<?> recipe;
	@Nullable
	private AdvancementHolder advancementHolder;
	private ICondition[] conditions;

	public HoldingRecipeOutput(Advancement.Builder advancement) {
		this.advancement = advancement;
	}

	@Override
	public Advancement.Builder advancement() {
		return advancement;
	}

	@Override
	public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
		this.recipe = recipe;
		this.advancementHolder = advancement;
		this.conditions = conditions;
	}

	public Recipe<?> getRecipe() {
		return recipe;
	}

	@Nullable
	public AdvancementHolder getAdvancementHolder() {
		return advancementHolder;
	}

	public ICondition[] getConditions() {
		return conditions;
	}
}
