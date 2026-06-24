package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class RecipeViewerDisplayCatalog implements IRecipeViewerDisplayCatalog {
	private static final long FOCUSED_CRAFTING_CACHE_TTL_NANOS = 30_000_000_000L;

	private final List<IRecipeViewerDisplaySpec<GroupedCraftingRecipe>> groupedCraftingSpecs = new ArrayList<>();
	private final List<CraftingDisplaySpec> craftingSpecs = new ArrayList<>();
	private final List<Class<? extends CraftingRecipe>> craftingSpecExtensionRecipeClasses = new ArrayList<>();
	private final List<SmithingDisplaySpec> smithingSpecs = new ArrayList<>();
	private final List<CraftingRecipe> craftingRecipes = new ArrayList<>();
	private final Map<StackDisplayKey, TimedCraftingDisplayViews> craftingRecipesForCache = new HashMap<>();
	private final Map<StackDisplayKey, TimedCraftingDisplayViews> craftingUsagesForCache = new HashMap<>();

	@Override
	public void addGroupedCraftingSpec(SingleColorDyeRecipeSpec spec) {
		groupedCraftingSpecs.add(spec);
	}

	@Override
	public List<IRecipeViewerDisplaySpec<GroupedCraftingRecipe>> getGroupedCraftingSpecs() {
		return List.copyOf(groupedCraftingSpecs);
	}

	@Override
	public void addCraftingSpec(CraftingDisplaySpec spec) {
		craftingSpecs.add(spec);
		craftingRecipesForCache.clear();
		craftingUsagesForCache.clear();
	}

	@Override
	public List<CraftingDisplaySpec> getCraftingSpecs() {
		return List.copyOf(craftingSpecs);
	}

	@Override
	public void addCraftingSpecExtensionRecipeClass(Class<? extends CraftingRecipe> recipeClass) {
		craftingSpecExtensionRecipeClasses.add(recipeClass);
	}

	@Override
	public List<Class<? extends CraftingRecipe>> getCraftingSpecExtensionRecipeClasses() {
		return List.copyOf(craftingSpecExtensionRecipeClasses);
	}

	@Override
	public void addSmithingSpec(SmithingDisplaySpec spec) {
		smithingSpecs.add(spec);
	}

	@Override
	public List<SmithingDisplaySpec> getSmithingSpecs() {
		return List.copyOf(smithingSpecs);
	}

	@Override
	public Optional<SmithingDisplaySpec> getSmithingDisplaySpecReplacing(SmithingRecipe recipe) {
		return smithingSpecs.stream().filter(spec -> spec.replacesSmithingRecipe(recipe)).findFirst();
	}

	@Override
	public List<SmithingDisplayView> getGlobalSmithingDisplays() {
		return smithingSpecs.stream().map(spec -> new SmithingDisplayView(spec, spec.getGlobalDisplays())).filter(view -> !view.variants().isEmpty()).toList();
	}

	@Override
	public List<SmithingDisplayView> getSmithingRecipesFor(ItemStack focusedOutput) {
		return smithingSpecs.stream().map(spec -> new SmithingDisplayView(spec, spec.getRecipesFor(focusedOutput))).filter(view -> !view.variants().isEmpty())
				.toList();
	}

	@Override
	public List<SmithingDisplayView> getSmithingUsagesFor(ItemStack focusedInput) {
		return smithingSpecs.stream().map(spec -> new SmithingDisplayView(spec, spec.getUsagesFor(focusedInput))).filter(view -> !view.variants().isEmpty())
				.toList();
	}

	@Override
	public void addCraftingRecipe(CraftingRecipe recipe) {
		craftingRecipes.add(recipe);
	}

	@Override
	public List<CraftingRecipe> getCraftingRecipes() {
		return List.copyOf(craftingRecipes);
	}

	@Override
	public List<CraftingDisplayView> getGlobalCraftingDisplays() {
		return craftingSpecs.stream().map(spec -> new CraftingDisplayView(spec, spec.getGlobalDisplays())).filter(view -> !view.variants().isEmpty()).toList();
	}

	@Override
	public List<CraftingDisplayView> getCraftingRecipesFor(ItemStack focusedOutput) {
		return getCachedCraftingViews(craftingRecipesForCache, focusedOutput, () -> craftingSpecs.stream()
				.map(spec -> new CraftingDisplayView(spec, spec.getRecipesFor(focusedOutput))).filter(view -> !view.variants().isEmpty()).toList());
	}

	@Override
	public List<CraftingDisplayView> getCraftingUsagesFor(ItemStack focusedInput) {
		return getCachedCraftingViews(craftingUsagesForCache, focusedInput, () -> craftingSpecs.stream()
				.map(spec -> new CraftingDisplayView(spec, getUsagesFor(spec, focusedInput))).filter(view -> !view.variants().isEmpty()).toList());
	}

	private static List<CraftingDisplayView> getCachedCraftingViews(Map<StackDisplayKey, TimedCraftingDisplayViews> cache, ItemStack stack,
			Supplier<List<CraftingDisplayView>> viewSupplier) {
		long now = System.nanoTime();
		cache.entrySet().removeIf(entry -> now - entry.getValue().createdAtNanos() >= FOCUSED_CRAFTING_CACHE_TTL_NANOS);
		StackDisplayKey key = StackDisplayKey.of(stack);
		TimedCraftingDisplayViews cachedViews = cache.get(key);
		if (cachedViews != null) {
			return cachedViews.views();
		}

		List<CraftingDisplayView> views = viewSupplier.get();
		cache.put(key, new TimedCraftingDisplayViews(views, now));
		return views;
	}

	@Override
	public boolean replacesCraftingRecipe(Recipe<?> recipe) {
		return getCraftingDisplaySpecReplacing(recipe).isPresent();
	}

	@Override
	public Optional<CraftingDisplaySpec> getCraftingDisplaySpecReplacing(Recipe<?> recipe) {
		return craftingSpecs.stream().filter(spec -> spec.replacesCraftingRecipe(recipe)).findFirst();
	}

	private static List<CraftingDisplayVariant> getUsagesFor(CraftingDisplaySpec spec, ItemStack focusedInput) {
		List<CraftingDisplayVariant> usages = spec.getUsagesFor(focusedInput);
		if (!usages.isEmpty()) {
			return usages;
		}
		if (spec.focusBehavior() instanceof SourceResultFocusBehavior sourceResultFocusBehavior
				&& sourceResultFocusBehavior.sourceInputIndex() < spec.baseIngredients().size()
				&& spec.baseIngredients().get(sourceResultFocusBehavior.sourceInputIndex()).test(focusedInput)) {
			return List.of();
		}
		return spec.baseIngredients().stream().anyMatch(ingredient -> ingredient.test(focusedInput)) ? spec.getGlobalDisplays() : List.of();
	}

	private record StackDisplayKey(Item item, CompoundTag tag) {
		private static StackDisplayKey of(ItemStack stack) {
			return new StackDisplayKey(stack.getItem(), stack.getTag() == null ? null : stack.getTag().copy());
		}
	}

	private record TimedCraftingDisplayViews(List<CraftingDisplayView> views, long createdAtNanos) {
	}
}
