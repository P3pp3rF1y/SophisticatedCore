package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper.CompactingShape.*;

public class RecipeHelper {
	private static final int MAX_FOLLOW_UP_COMPACTING_RECIPES = 30;
	@Nullable
	private static RecipeCache clientCache = null;
	@Nullable
	private static RecipeCache serverCache = null;

	private RecipeHelper() {
	}

	public static void setLevel(Level l) {
		getCache().setLevel(l);
	}

	public static void clearListeners() {
		runOnCache(cache -> cache.recipeChangeListeners.list.clear());
	}

	private static void runOnCache(Consumer<RecipeCache> consumer) {
		consumer.accept(getCache());
	}

	private static RecipeCache getCache() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			if (serverCache == null) {
				serverCache = new RecipeCache();
			}
			return serverCache;
		} else {
			if (clientCache == null) {
				clientCache = new RecipeCache();
			}
			return clientCache;
		}
	}

	private static <T> T getFromCache(Function<RecipeCache, T> getter, T defaultValue) {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			return serverCache == null ? defaultValue : getter.apply(serverCache);
		} else {
			return clientCache == null ? defaultValue : getter.apply(clientCache);
		}
	}

	public static void addRecipeChangeListener(Runnable runnable) {
		runOnCache(cache -> cache.addRecipeChangeListener(runnable));
	}

	@SuppressWarnings("unused") //event parameter used to identify which event this listener is for
	public static void onRecipesUpdated(RecipesUpdatedEvent event) {
		runOnCache(cache -> {
			cache.clearCache();
			cache.recipeChangeListeners.notifyAllListeners();
		});
	}

	@SuppressWarnings("unused") //event parameter used to identify which event this listener is for
	public static void onDataPackSync(OnDatapackSyncEvent event) {
		runOnCache(cache -> {
			cache.clearCache();
			cache.recipeChangeListeners.notifyAllListeners();
		});
	}

	private static Optional<Level> getLevel() {
		return getFromCache(cache -> Optional.ofNullable(cache.level == null ? null : cache.level.get()), Optional.empty());
	}

	private static Set<CompactingShape> getCompactingShapes(ItemStack stack) {
		return getLevel().map(w -> {
			Set<CompactingShape> compactingShapes = new HashSet<>();
			getCompactingShape(stack, w, 2, 2, TWO_BY_TWO_UNCRAFTABLE, TWO_BY_TWO).ifPresent(compactingShapes::add);
			getCompactingShape(stack, w, 3, 3, THREE_BY_THREE_UNCRAFTABLE, THREE_BY_THREE).ifPresent(compactingShapes::add);
			if (compactingShapes.isEmpty()) {
				compactingShapes.add(NONE);
			}
			return compactingShapes;
		}).orElse(Collections.emptySet());
	}

	private static Optional<CompactingShape> getCompactingShape(ItemStack stack, Level w, int width, int height, CompactingShape uncraftableShape, CompactingShape shape) {
		CompactingResult compactingResult = getCompactingResult(stack, w, width, height);
		if (!compactingResult.getResult().isEmpty()) {
			if (ItemStack.isSameItemSameComponents(stack, compactingResult.getResult())) {
				return Optional.empty();
			}

			if (isPartOfCompactingLoop(stack, compactingResult.getResult(), w)) {
				return Optional.empty();
			}

			if (uncompactMatchesItem(compactingResult.getResult(), w, stack, width * height)) {
				return Optional.of(uncraftableShape);
			} else {
				return Optional.of(shape);
			}
		}
		return Optional.empty();
	}

	private static boolean isPartOfCompactingLoop(ItemStack firstCompacted, ItemStack firstCompactResult, Level w) {
		ItemStack compactingResultStack;
		int iterations = 0;
		Set<Integer> compactedItemHashes = new HashSet<>();
		Queue<ItemStack> itemsToCompact = new LinkedList<>();
		itemsToCompact.add(firstCompactResult);
		while (!itemsToCompact.isEmpty()) {
			ItemStack itemToCompact = itemsToCompact.poll();
			compactingResultStack = getCompactingResult(itemToCompact, w, 2, 2).getResult();
			if (!compactingResultStack.isEmpty()) {
				if (ItemStack.isSameItemSameComponents(compactingResultStack, firstCompacted)) {
					return true;
				} else if (compactedItemHashes.contains(ItemStack.hashItemAndComponents(compactingResultStack))) {
					return false; //loop exists but the first compacted item isn't part of it so we will let it be compacted, but no follow up compacting will happen
				}
				itemsToCompact.add(compactingResultStack);
			}

			compactingResultStack = getCompactingResult(itemToCompact, w, 3, 3).getResult();
			if (!compactingResultStack.isEmpty()) {
				if (ItemStack.isSameItemSameComponents(compactingResultStack, firstCompacted)) {
					return true;
				} else if (compactedItemHashes.contains(ItemStack.hashItemAndComponents(compactingResultStack))) {
					return false; //loop exists but the first compacted item isn't part of it so we will let it be compacted, but no follow up compacting will happen
				}
				itemsToCompact.add(compactingResultStack);
			}
			compactedItemHashes.add(ItemStack.hashItemAndComponents(itemToCompact));
			iterations++;
			if (iterations > MAX_FOLLOW_UP_COMPACTING_RECIPES) {
				return true; //we were unable to figure out if the loop exists because of way too many follow up compacting recipe thus not allowing to compact anyway
			}
		}
		return false;
	}

	private static boolean uncompactMatchesItem(ItemStack itemToUncompact, Level w, ItemStack itemToMatch, int count) {
		for (ItemStack uncompactResult : getUncompactResultItems(w, itemToUncompact)) {
			if (ItemStack.isSameItemSameComponents(uncompactResult, itemToMatch) && uncompactResult.getCount() == count) {
				return true;
			}
		}
		return false;
	}

	public static UncompactingResult getUncompactingResult(ItemStack uncompactedItem) {
		return getFromCache(cache -> cache.getUncompactingResults().computeIfAbsent(ItemStack.hashItemAndComponents(uncompactedItem), k -> getLevel().map(w -> {
			for (ItemStack uncompactResultItem : getUncompactResultItems(w, uncompactedItem)) {
				if (uncompactResultItem.getCount() == 9) {
					if (ItemStack.isSameItemSameComponents(getCompactingResult(uncompactResultItem, 3, 3).getResult(), uncompactedItem)) {
						return new UncompactingResult(uncompactResultItem, THREE_BY_THREE_UNCRAFTABLE);
					}
				} else if (uncompactResultItem.getCount() == 4 && ItemStack.isSameItemSameComponents(getCompactingResult(uncompactResultItem, 2, 2).getResult(), uncompactedItem)) {
					return new UncompactingResult(uncompactResultItem, TWO_BY_TWO_UNCRAFTABLE);
				}
			}
			return UncompactingResult.EMPTY;
		}).orElse(UncompactingResult.EMPTY)), UncompactingResult.EMPTY);
	}

	private static List<ItemStack> getUncompactResultItems(Level w, ItemStack itemToUncompact) {
		CraftingContainer craftingInventory = getFilledCraftingInventory(itemToUncompact, 1, 1);
		return safeGetRecipesFor(RecipeType.CRAFTING, craftingInventory.asCraftInput(), w).stream().map(r -> r.value().assemble(craftingInventory.asCraftInput(), w.registryAccess())).toList();
	}

	public static CompactingResult getCompactingResult(ItemStack stack, CompactingShape shape) {
		if (shape == TWO_BY_TWO_UNCRAFTABLE || shape == TWO_BY_TWO) {
			return RecipeHelper.getCompactingResult(stack, 2, 2);
		} else if (shape == THREE_BY_THREE_UNCRAFTABLE || shape == THREE_BY_THREE) {
			return RecipeHelper.getCompactingResult(stack, 3, 3);
		}
		return CompactingResult.EMPTY;
	}

	public static CompactingResult getCompactingResult(ItemStack stack, int width, int height) {
		return getLevel().map(w -> getCompactingResult(stack, w, width, height)).orElse(CompactingResult.EMPTY);
	}

	private static CompactingResult getCompactingResult(ItemStack stack, Level level, int width, int height) {
		return getFromCache(cache -> getCompactingResult(stack, level, width, height, cache.getCompactingResults()), CompactingResult.EMPTY);
	}

	private static CompactingResult getCompactingResult(ItemStack stack, Level level, int width, int height, Map<CompactedItem, CompactingResult> cachedCompactingResults) {
		CompactedItem compactedItem = new CompactedItem(stack, width, height);
		if (cachedCompactingResults.containsKey(compactedItem)) {
			return cachedCompactingResults.get(compactedItem);
		}

		CraftingContainer craftingInventory = getFilledCraftingInventory(stack, width, height);
		List<RecipeHolder<CraftingRecipe>> compactingRecipes = safeGetRecipesFor(RecipeType.CRAFTING, craftingInventory.asCraftInput(), level);

		if (compactingRecipes.isEmpty()) {
			cachedCompactingResults.put(compactedItem, CompactingResult.EMPTY);
			return CompactingResult.EMPTY;
		}

		if (compactingRecipes.size() == 1) {
			return cacheAndGetCompactingResult(compactedItem, compactingRecipes.getFirst().value(), craftingInventory);
		}

		for (RecipeHolder<CraftingRecipe> recipeHolder : compactingRecipes) {
			CraftingRecipe recipe = recipeHolder.value();
			ItemStack result = recipe.assemble(craftingInventory.asCraftInput(), level.registryAccess());
			if (uncompactMatchesItem(result, level, stack, width * height)) {
				return cacheAndGetCompactingResult(compactedItem, recipe, craftingInventory, result);
			}
		}

		return cacheAndGetCompactingResult(compactedItem, compactingRecipes.getFirst().value(), craftingInventory);
	}

	private static CompactingResult cacheAndGetCompactingResult(CompactedItem compactedItem, CraftingRecipe recipe, CraftingContainer craftingInventory) {
		return getLevel().map(level ->
				cacheAndGetCompactingResult(compactedItem, recipe, craftingInventory, recipe.assemble(craftingInventory.asCraftInput(), level.registryAccess()))
		).orElse(CompactingResult.EMPTY);
	}

	private static CompactingResult cacheAndGetCompactingResult(CompactedItem compactedItem, CraftingRecipe recipe, CraftingContainer craftingInventory, ItemStack result) {
		List<ItemStack> remainingItems = new ArrayList<>();
		recipe.getRemainingItems(craftingInventory.asCraftInput()).forEach(stack -> {
			if (!stack.isEmpty()) {
				remainingItems.add(stack);
			}
		});

		CompactingResult compactingResult = new CompactingResult(result, remainingItems);
		return getFromCache(cache -> {
			if (!result.isEmpty()) {
				cache.getCompactingResults().put(compactedItem, compactingResult);
			}
			return compactingResult;
		}, compactingResult);
	}

	private static CraftingContainer getFilledCraftingInventory(ItemStack stack, int width, int height) {
		CraftingContainer craftinginventory = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			public boolean stillValid(Player playerIn) {
				return false;
			}
		}, width, height);

		for (int i = 0; i < craftinginventory.getContainerSize(); i++) {
			craftinginventory.setItem(i, stack.copyWithCount(1));
		}
		return craftinginventory;
	}

	public static <T extends AbstractCookingRecipe> Optional<RecipeHolder<T>> getCookingRecipe(ItemStack stack, RecipeType<T> recipeType) {
		return getLevel().flatMap(w -> safeGetRecipeFor(recipeType, new SingleRecipeInput(stack), w, null));
	}

	public static Set<CompactingShape> getItemCompactingShapes(ItemStack stack) {
		return getFromCache(cache -> cache.getItemCompactingShapes(stack), Collections.emptySet());
	}

	public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getRecipesOfType(RecipeType<T> recipeType, I inventory) {
		return getLevel().map(w -> w.getRecipeManager().getRecipesFor(recipeType, inventory, w)).orElse(Collections.emptyList());
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> safeGetRecipeFor(RecipeType<T> recipeType, I inventory, @Nullable ResourceLocation recipeId) {
		return getLevel().flatMap(w -> safeGetRecipeFor(recipeType, inventory, w, recipeId));
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> safeGetRecipeFor(RecipeType<T> recipeType, I inventory, Level level, @Nullable ResourceLocation recipeId) {
		try {
			return level.getRecipeManager().getRecipeFor(recipeType, inventory, level, recipeId);
		} catch (Exception e) {
			SophisticatedCore.LOGGER.error("Error while getting recipe ", e);
			return Optional.empty();
		}
	}

	public static <I extends CraftingInput, T extends Recipe<I>> List<RecipeHolder<T>> safeGetRecipesFor(RecipeType<T> recipeType, I inventory, Level level) {
		try {
			return level.getRecipeManager().getRecipesFor(recipeType, inventory, level);
		} catch (Exception e) {
			SophisticatedCore.LOGGER.error("Error while getting recipe ", e);
			return Collections.emptyList();
		}
	}

	public enum CompactingShape {
		NONE(false, 0),
		THREE_BY_THREE(false, 9),
		TWO_BY_TWO(false, 4),
		THREE_BY_THREE_UNCRAFTABLE(true, 9),
		TWO_BY_TWO_UNCRAFTABLE(true, 4);

		private final int numberOfIngredients;

		private final boolean uncraftable;

		CompactingShape(boolean uncraftable, int numberOfIngredients) {
			this.uncraftable = uncraftable;
			this.numberOfIngredients = numberOfIngredients;
		}

		public boolean isUncraftable() {
			return uncraftable;
		}

		public int getNumberOfIngredients() {
			return numberOfIngredients;
		}
	}

	public static class CompactingResult {
		public static final CompactingResult EMPTY = new CompactingResult(ItemStack.EMPTY, Collections.emptyList());

		private final ItemStack result;
		private final List<ItemStack> remainingItems;

		public CompactingResult(ItemStack result, List<ItemStack> remainingItems) {
			this.result = result;
			this.remainingItems = remainingItems;
		}

		public ItemStack getResult() {
			return result;
		}

		public List<ItemStack> getRemainingItems() {
			return remainingItems;
		}
	}

	public static class UncompactingResult {
		public static final UncompactingResult EMPTY = new UncompactingResult(ItemStack.EMPTY, NONE);

		private final ItemStack result;

		private final CompactingShape compactUsingShape;

		public UncompactingResult(ItemStack result, CompactingShape compactUsingShape) {
			this.result = result.copyWithCount(1);
			this.compactUsingShape = compactUsingShape;
		}

		public ItemStack getResult() {
			return result;
		}

		public CompactingShape getCompactUsingShape() {
			return compactUsingShape;
		}
	}

	private static class CompactedItem {
		private final ItemStack item;
		private final int itemHash;
		private final int width;
		private final int height;

		private CompactedItem(ItemStack item, int width, int height) {
			this.item = item.copyWithCount(1);
			this.width = width;
			this.height = height;
			this.itemHash = ItemStack.hashItemAndComponents(item);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CompactedItem that = (CompactedItem) o;
			return width == that.width &&
					height == that.height &&
					ItemStack.isSameItemSameComponents(item, that.item);
		}

		@Override
		public int hashCode() {
			return Objects.hash(itemHash, width, height);
		}
	}

	private static class RecipeChangeListenerList {
		private final List<WeakReference<Runnable>> list = new CopyOnWriteArrayList<>();

		public void add(Runnable runnable) {
			list.add(new WeakReference<>(runnable));
		}

		public void notifyAllListeners() {
			list.removeIf(ref -> {
				Runnable runnable = ref.get();
				if (runnable != null) {
					runnable.run();
					return false;
				}
				return true;
			});
		}
	}

	private static class RecipeCache {
		private final Cache<Integer, Set<CompactingShape>> itemCompactingShapes = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
		private final Map<CompactedItem, CompactingResult> compactingResults = new HashMap<>();
		private final Map<Integer, UncompactingResult> uncompactingResults = new HashMap<>();
		private final RecipeChangeListenerList recipeChangeListeners = new RecipeChangeListenerList();
		private WeakReference<Level> level;

		public void addRecipeChangeListener(Runnable runnable) {
			recipeChangeListeners.add(runnable);
		}

		public Map<Integer, UncompactingResult> getUncompactingResults() {
			return uncompactingResults;
		}

		public Map<CompactedItem, CompactingResult> getCompactingResults() {
			return compactingResults;
		}

		public Set<CompactingShape> getItemCompactingShapes(ItemStack stack) {
			int hash = ItemStack.hashItemAndComponents(stack);
			Set<CompactingShape> compactingShapes = itemCompactingShapes.getIfPresent(hash);
			if (compactingShapes == null) {
				SophisticatedCore.LOGGER.debug("Compacting shapes not found in cache for \"{}\" - querying recipes to get these", BuiltInRegistries.ITEM.getKey(stack.getItem()));
				compactingShapes = getCompactingShapes(stack);
				itemCompactingShapes.put(hash, compactingShapes);
			}

			return compactingShapes;
		}

		private void clearCache() {
			compactingResults.clear();
			uncompactingResults.clear();
			itemCompactingShapes.invalidateAll();
		}

		public void setLevel(Level l) {
			level = new WeakReference<>(l);
		}
	}
}
