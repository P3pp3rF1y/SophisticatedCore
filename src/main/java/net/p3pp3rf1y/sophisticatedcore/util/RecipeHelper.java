package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import javax.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper.CompactingShape.*;

@SuppressWarnings("PMD.UnnecessaryImport")
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

	@SuppressWarnings("unused") // event parameter used to identify which event this listener is for
	public static void onRecipesUpdated(RecipesUpdatedEvent event) {
		runOnCache(cache -> {
			cache.clearCache();
			cache.recipeChangeListeners.notifyAllListeners();
		});
	}

	@SuppressWarnings("unused") // event parameter used to identify which event this listener is for
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

	private static Optional<CompactingShape> getCompactingShape(ItemStack stack, Level w, int width, int height, CompactingShape uncraftableShape,
			CompactingShape shape) {
		CompactingResult compactingResult = getCompactingResult(stack, w, width, height);
		if (!compactingResult.getResult().isEmpty()) {
			if (ItemHandlerHelper.canItemStacksStack(stack, compactingResult.getResult())) {
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
				if (ItemHandlerHelper.canItemStacksStack(compactingResultStack, firstCompacted)) {
					return true;
				} else if (compactedItemHashes.contains(ItemStackKey.getHashCode(compactingResultStack))) {
					return false; // loop exists but the first compacted item isn't part of it so we will let it be compacted, but no follow up compacting will
									// happen
				}
				itemsToCompact.add(compactingResultStack);
			}

			compactingResultStack = getCompactingResult(itemToCompact, w, 3, 3).getResult();
			if (!compactingResultStack.isEmpty()) {
				if (ItemHandlerHelper.canItemStacksStack(compactingResultStack, firstCompacted)) {
					return true;
				} else if (compactedItemHashes.contains(ItemStackKey.getHashCode(compactingResultStack))) {
					return false; // loop exists but the first compacted item isn't part of it so we will let it be compacted, but no follow up compacting will
									// happen
				}
				itemsToCompact.add(compactingResultStack);
			}
			compactedItemHashes.add(ItemStackKey.getHashCode(itemToCompact));
			iterations++;
			if (iterations > MAX_FOLLOW_UP_COMPACTING_RECIPES) {
				return true; // we were unable to figure out if the loop exists because of way too many follow up compacting recipe thus not allowing to compact
								// anyway
			}
		}
		return false;
	}

	private static boolean uncompactMatchesItem(ItemStack itemToUncompact, Level w, ItemStack itemToMatch, int count) {
		for (ItemStack uncompactResult : getUncompactResultItems(w, itemToUncompact)) {
			if (ItemHandlerHelper.canItemStacksStack(uncompactResult, itemToMatch) && uncompactResult.getCount() == count) {
				return true;
			}
		}
		return false;
	}

	public static UncompactingResult getUncompactingResult(ItemStack uncompactedItem) {
		return getFromCache(cache -> cache.getUncompactingResults().computeIfAbsent(ItemStackKey.getHashCode(uncompactedItem), k -> getLevel().map(w -> {
			for (ItemStack uncompactResultItem : getUncompactResultItems(w, uncompactedItem)) {
				if (uncompactResultItem.getCount() == 9) {
					if (ItemHandlerHelper.canItemStacksStack(getCompactingResult(uncompactResultItem, 3, 3).getResult(), uncompactedItem)) {
						return new UncompactingResult(uncompactResultItem, THREE_BY_THREE_UNCRAFTABLE);
					}
				} else if (uncompactResultItem.getCount() == 4
						&& ItemHandlerHelper.canItemStacksStack(getCompactingResult(uncompactResultItem, 2, 2).getResult(), uncompactedItem)) {
					return new UncompactingResult(uncompactResultItem, TWO_BY_TWO_UNCRAFTABLE);
				}
			}
			return UncompactingResult.EMPTY;
		}).orElse(UncompactingResult.EMPTY)), UncompactingResult.EMPTY);
	}

	private static List<ItemStack> getUncompactResultItems(Level w, ItemStack itemToUncompact) {
		CraftingContainer craftingInventory = getFilledCraftingInventory(itemToUncompact, 1, 1);
		return safeGetRecipesFor(RecipeType.CRAFTING, craftingInventory, w).stream().map(r -> r.assemble(craftingInventory, w.registryAccess())).toList();
	}

	public static List<ItemStack> getUncompactResultItems(ItemStack itemToUncompact) {
		return getLevel().map(w -> getUncompactResultItems(w, itemToUncompact)).orElse(Collections.emptyList());
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

	public static CompactingResult getCompactingResult(ItemStack stack, CompactingRecipeShape shape) {
		return getLevel().map(w -> getCompactingResult(stack, w, shape)).orElse(CompactingResult.EMPTY);
	}

	public static boolean doesUncompactMatch(ItemStack itemToUncompact, ItemStack itemToMatch, int count) {
		return getLevel().map(w -> uncompactMatchesItem(itemToUncompact, w, itemToMatch, count)).orElse(false);
	}

	private static CompactingResult getCompactingResult(ItemStack stack, Level level, int width, int height) {
		return getCompactingResult(stack, level, CompactingRecipeShape.full(width, height));
	}

	private static CompactingResult getCompactingResult(ItemStack stack, Level level, CompactingRecipeShape shape) {
		return getFromCache(cache -> getCompactingResult(stack, level, shape, cache.getCompactingResults()), CompactingResult.EMPTY);
	}

	private static CompactingResult getCompactingResult(ItemStack stack, Level level, CompactingRecipeShape shape,
			Map<CompactedItem, CompactingResult> cachedCompactingResults) {
		CompactedItem compactedItem = new CompactedItem(stack, shape);
		if (cachedCompactingResults.containsKey(compactedItem)) {
			return cachedCompactingResults.get(compactedItem);
		}

		CraftingContainer craftingInventory = getFilledCraftingInventory(stack, shape);
		List<CraftingRecipe> compactingRecipes = safeGetRecipesFor(RecipeType.CRAFTING, craftingInventory, level);

		if (compactingRecipes.isEmpty()) {
			cachedCompactingResults.put(compactedItem, CompactingResult.EMPTY);
			return CompactingResult.EMPTY;
		}

		if (compactingRecipes.size() == 1) {
			return cacheAndGetCompactingResult(compactedItem, compactingRecipes.get(0), craftingInventory);
		}

		for (CraftingRecipe recipe : compactingRecipes) {
			ItemStack result = recipe.assemble(craftingInventory, level.registryAccess());
			if (uncompactMatchesItem(result, level, stack, shape.ingredientCount())) {
				return cacheAndGetCompactingResult(compactedItem, recipe, craftingInventory, result);
			}
		}

		return cacheAndGetCompactingResult(compactedItem, compactingRecipes.get(0), craftingInventory);
	}

	private static CompactingResult cacheAndGetCompactingResult(CompactedItem compactedItem, CraftingRecipe recipe, CraftingContainer craftingInventory) {
		return getLevel()
				.map(level -> cacheAndGetCompactingResult(compactedItem, recipe, craftingInventory, recipe.assemble(craftingInventory, level.registryAccess())))
				.orElse(CompactingResult.EMPTY);
	}

	private static CompactingResult cacheAndGetCompactingResult(CompactedItem compactedItem, CraftingRecipe recipe, CraftingContainer craftingInventory,
			ItemStack result) {
		List<ItemStack> remainingItems = new ArrayList<>();
		recipe.getRemainingItems(craftingInventory).forEach(stack -> {
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
		return getFilledCraftingInventory(stack, CompactingRecipeShape.full(width, height));
	}

	private static CraftingContainer getFilledCraftingInventory(ItemStack stack, CompactingRecipeShape shape) {
		CraftingContainer craftinginventory = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
				return ItemStack.EMPTY;
			}

			public boolean stillValid(Player playerIn) {
				return false;
			}
		}, shape.width(), shape.height());

		for (int i = 0; i < craftinginventory.getContainerSize(); i++) {
			if (shape.isSlotFilled(i)) {
				craftinginventory.setItem(i, stack.copyWithCount(1));
			}
		}
		return craftinginventory;
	}

	public static <T extends AbstractCookingRecipe> Optional<T> getCookingRecipe(ItemStack stack, RecipeType<T> recipeType) {
		return getLevel().flatMap(w -> safeGetRecipeFor(recipeType, new RecipeWrapper(new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, stack))), w, null));
	}

	public static Optional<Recipe<?>> getRecipe(ResourceLocation recipeId) {
		return getLevel().flatMap(level -> level.getRecipeManager().byKey(recipeId).map(recipe -> (Recipe<?>) recipe));
	}

	public static Set<CompactingShape> getItemCompactingShapes(ItemStack stack) {
		return getFromCache(cache -> cache.getItemCompactingShapes(stack), Collections.emptySet());
	}

	public static <T extends Recipe<Container>> List<T> getRecipesOfType(RecipeType<T> recipeType, Container inventory) {
		return getLevel().map(w -> w.getRecipeManager().getRecipesFor(recipeType, inventory, w)).orElse(Collections.emptyList());
	}

	public static <C extends Container, T extends Recipe<C>> Optional<T> safeGetRecipeFor(RecipeType<T> recipeType, C inventory,
			@Nullable ResourceLocation recipeId) {
		return getLevel().flatMap(w -> safeGetRecipeFor(recipeType, inventory, w, recipeId));
	}

	public static <C extends Container, T extends Recipe<C>> Optional<T> safeGetRecipeFor(RecipeType<T> recipeType, C inventory, Level level,
			@Nullable ResourceLocation recipeId) {
		try {
			return level.getRecipeManager().getRecipeFor(recipeType, inventory, level, recipeId).map(Pair::getSecond);
		} catch (Exception e) {
			SophisticatedCore.LOGGER.error("Error while getting recipe ", e);
			return Optional.empty();
		}
	}

	public static <C extends Container, T extends Recipe<C>> List<T> safeGetRecipesFor(RecipeType<T> recipeType, C inventory, Level level) {
		try {
			return level.getRecipeManager().getRecipesFor(recipeType, inventory, level);
		} catch (Exception e) {
			SophisticatedCore.LOGGER.error("Error while getting recipe ", e);
			return Collections.emptyList();
		}
	}

	public enum CompactingShape {
		NONE(false, 0), THREE_BY_THREE(false, 9), TWO_BY_TWO(false, 4), THREE_BY_THREE_UNCRAFTABLE(true, 9), TWO_BY_TWO_UNCRAFTABLE(true, 4);

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

	public static class CompactingRecipeShape {
		private final int width;
		private final int height;
		private final long filledSlots;
		private final int ingredientCount;

		private CompactingRecipeShape(int width, int height, long filledSlots, int ingredientCount) {
			this.width = width;
			this.height = height;
			this.filledSlots = filledSlots;
			this.ingredientCount = ingredientCount;
		}

		public static CompactingRecipeShape full(int width, int height) {
			return new CompactingRecipeShape(width, height, (1L << (width * height)) - 1, width * height);
		}

		public static Optional<CompactingRecipeShape> parse(String shape) {
			String[] rows = shape.split("/");
			if (rows.length == 0 || rows.length > 3) {
				return Optional.empty();
			}

			int width = rows[0].length();
			if (width == 0 || width > 3) {
				return Optional.empty();
			}

			long filledSlots = 0;
			int ingredientCount = 0;
			for (int row = 0; row < rows.length; row++) {
				if (rows[row].length() != width) {
					return Optional.empty();
				}
				for (int column = 0; column < width; column++) {
					char slot = rows[row].charAt(column);
					if (slot == '1') {
						filledSlots |= 1L << (row * width + column);
						ingredientCount++;
					} else if (slot != '0') {
						return Optional.empty();
					}
				}
			}

			return ingredientCount > 1 ? Optional.of(new CompactingRecipeShape(width, rows.length, filledSlots, ingredientCount)) : Optional.empty();
		}

		public int width() {
			return width;
		}

		public int height() {
			return height;
		}

		public int ingredientCount() {
			return ingredientCount;
		}

		public boolean isSlotFilled(int slot) {
			return slot >= 0 && slot < width * height && (filledSlots & (1L << slot)) != 0;
		}

		public boolean fitsWithin(int maxWidth, int maxHeight) {
			return width <= maxWidth && height <= maxHeight;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CompactingRecipeShape that = (CompactingRecipeShape) o;
			return width == that.width && height == that.height && filledSlots == that.filledSlots && ingredientCount == that.ingredientCount;
		}

		@Override
		public int hashCode() {
			return Objects.hash(width, height, filledSlots, ingredientCount);
		}
	}

	private static class CompactedItem {
		private final ItemStack item;
		private final int itemHash;
		private final CompactingRecipeShape shape;

		private CompactedItem(ItemStack item, int width, int height) {
			this(item, CompactingRecipeShape.full(width, height));
		}

		private CompactedItem(ItemStack item, CompactingRecipeShape shape) {
			this.item = item.copyWithCount(1);
			this.itemHash = ItemStackKey.getHashCode(item);
			this.shape = shape;
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
			return Objects.equals(shape, that.shape) && ItemHandlerHelper.canItemStacksStack(item, that.item);
		}

		@Override
		public int hashCode() {
			return Objects.hash(itemHash, shape);
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
			int hash = ItemStackKey.getHashCode(stack);
			Set<CompactingShape> compactingShapes = itemCompactingShapes.getIfPresent(hash);
			if (compactingShapes == null) {
				SophisticatedCore.LOGGER.debug("Compacting shapes not found in cache for \"{}\" - querying recipes to get these",
						ForgeRegistries.ITEMS.getKey(stack.getItem()));
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
