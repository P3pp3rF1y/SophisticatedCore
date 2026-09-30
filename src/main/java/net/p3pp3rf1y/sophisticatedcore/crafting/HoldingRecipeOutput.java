package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

public class HoldingRecipeOutput implements RecipeOutput {
	private final Advancement.Builder advancement;
	private final RecipeOutput delegate;
	private Recipe<?> recipe;
	@Nullable
	private AdvancementHolder advancementHolder;
	private ICondition[] conditions;

	public HoldingRecipeOutput(RecipeOutput delegate) {
		this.delegate = delegate;
		this.advancement = delegate.advancement();
	}

	@Override
	public Advancement.Builder advancement() {
		return advancement;
	}

	@Override
	public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registry) {
		return delegate.lookup(registry);
	}

	@Override
	public <S> Stream<Holder.Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> registry) {
		return delegate.listContextElements(registry);
	}

	@Override
	public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
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
