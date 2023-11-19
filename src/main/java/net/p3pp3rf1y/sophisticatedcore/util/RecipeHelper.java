package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.NonNullList;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper.CompactingShape.*;

public class RecipeHelper {
	private static final LoadingCache<Item, Set<CompactingShape>> ITEM_COMPACTING_SHAPES = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build(new CacheLoader<>() {
		@Override
		public Set<CompactingShape> load(Item item) {
			SophisticatedCore.LOGGER.debug("Compacting shapes not found in cache for \"{}\" - querying recipes to get these", ForgeRegistries.ITEMS.getKey(item));
			return getCompactingShapes(item);
		}
	});
	private static final int MAX_FOLLOW_UP_COMPACTING_RECIPES = 30;
	private static WeakReference<Level> world;
	private static final Map<CompactedItem, CompactingResult> COMPACTING_RESULTS = new HashMap<>();
	private static final Map<Item, UncompactingResult> UNCOMPACTING_RESULTS = new HashMap<>();

	private RecipeHelper() {}

	public static void setWorld(Level w) {
		world = new WeakReference<>(w);
	}

	public static void clearCache() {
		COMPACTING_RESULTS.clear();
		UNCOMPACTING_RESULTS.clear();
		ITEM_COMPACTING_SHAPES.invalidateAll();
	}

	public static void onReload(final AddReloadListenerEvent evt) {
		evt.addListener(new SimplePreparableReloadListener<Void>() {
			@Nonnull
			@Override
			protected Void prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
				return null;
			}

			@Override
			protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
				clearCache();
			}
		});
	}

	private static Optional<Level> getWorld() {
		return world != null ? Optional.ofNullable(world.get()) : Optional.empty();
	}

	private static Set<CompactingShape> getCompactingShapes(Item item) {
		return getWorld().map(w -> {
			Set<CompactingShape> compactingShapes = new HashSet<>();
			getCompactingShape(item, w, 2, 2, TWO_BY_TWO_UNCRAFTABLE, TWO_BY_TWO).ifPresent(compactingShapes::add);
			getCompactingShape(item, w, 3, 3, THREE_BY_THREE_UNCRAFTABLE, THREE_BY_THREE).ifPresent(compactingShapes::add);
			if (compactingShapes.isEmpty()) {
				compactingShapes.add(NONE);
			}
			return compactingShapes;
		}).orElse(Collections.emptySet());
	}

	private static Optional<CompactingShape> getCompactingShape(Item item, Level w, int width, int height, CompactingShape uncraftableShape, CompactingShape shape) {
		CompactingResult compactingResult = getCompactingResult(item, w, width, height);
		if (!compactingResult.getResult().isEmpty()) {
			if (item == compactingResult.getResult().getItem()) {
				return Optional.empty();
			}

			if (isPartOfCompactingLoop(item, compactingResult.getResult().getItem(), w)) {
				return Optional.empty();
			}

			if (uncompactMatchesItem(compactingResult.getResult(), w, item, width * height)) {
				return Optional.of(uncraftableShape);
			} else {
				return Optional.of(shape);
			}
		}
		return Optional.empty();
	}

	private static boolean isPartOfCompactingLoop(Item firstCompacted, Item firstCompactResult, Level w) {
		ItemStack compactingResultStack;
		int iterations = 0;
		Set<Item> compactedItems = new HashSet<>();
		Queue<Item> itemsToCompact = new LinkedList<>();
		itemsToCompact.add(firstCompactResult);
		while (!itemsToCompact.isEmpty()) {
			Item itemToCompact = itemsToCompact.poll();
			compactingResultStack = getCompactingResult(itemToCompact, w, 2, 2).getResult();
			if (!compactingResultStack.isEmpty()) {
				if (compactingResultStack.getItem() == firstCompacted) {
					return true;
				} else if (compactedItems.contains(compactingResultStack.getItem())) {
					return false; //loop exists but the first compacted item isn't part of it so we will let it be compacted, but no follow up compacting will happen
				}
				itemsToCompact.add(compactingResultStack.getItem());
			}

			compactingResultStack = getCompactingResult(itemToCompact, w, 3, 3).getResult();
			if (!compactingResultStack.isEmpty()) {
				if (compactingResultStack.getItem() == firstCompacted) {
					return true;
				} else if (compactedItems.contains(compactingResultStack.getItem())) {
					return false; //loop exists but the first compacted item isn't part of it so we will let it be compacted, but no follow up compacting will happen
				}
				itemsToCompact.add(compactingResultStack.getItem());
			}
			compactedItems.add(itemToCompact);
			iterations++;
			if (iterations > MAX_FOLLOW_UP_COMPACTING_RECIPES) {
				return true; //we were unable to figure out if the loop exists because of way too many follow up compacting recipe thus not allowing to compact anyway
			}
		}
		return false;
	}

	private static boolean uncompactMatchesItem(ItemStack result, Level w, Item item, int count) {
		Item itemToUncompact = result.getItem();
		for(ItemStack uncompactResult : getUncompactResultItems(w, itemToUncompact)) {
			if (uncompactResult.getItem() == item && uncompactResult.getCount() == count) {
				return true;
			}
		}
		return false;
	}

	public static UncompactingResult getUncompactingResult(Item resultItem) {
		return UNCOMPACTING_RESULTS.computeIfAbsent(resultItem, k -> getWorld().map(w -> {
			for (ItemStack uncompactResultItem : getUncompactResultItems(w, resultItem)) {
				if (uncompactResultItem.getCount() == 9) {
					if (getCompactingResult(uncompactResultItem.getItem(), 3, 3).getResult().getItem() == resultItem) {
						return new UncompactingResult(uncompactResultItem.getItem(), THREE_BY_THREE_UNCRAFTABLE);
					}
				} else if (uncompactResultItem.getCount() == 4 && getCompactingResult(uncompactResultItem.getItem(), 2, 2).getResult().getItem() == resultItem) {
					return new UncompactingResult(uncompactResultItem.getItem(), TWO_BY_TWO_UNCRAFTABLE);
				}
			}
			return UncompactingResult.EMPTY;
		}).orElse(UncompactingResult.EMPTY));
	}

	private static List<ItemStack> getUncompactResultItems(Level w, Item itemToUncompact) {
		CraftingContainer craftingInventory = getFilledCraftingInventory(itemToUncompact, 1, 1);
		return safeGetRecipesFor(RecipeType.CRAFTING, craftingInventory, w).stream().map(r -> r.assemble(craftingInventory, w.registryAccess())).toList();
	}

	public static CompactingResult getCompactingResult(Item item, CompactingShape shape) {
		if (shape == TWO_BY_TWO_UNCRAFTABLE || shape == TWO_BY_TWO) {
			return RecipeHelper.getCompactingResult(item, 2, 2);
		} else if (shape == THREE_BY_THREE_UNCRAFTABLE || shape == THREE_BY_THREE) {
			return RecipeHelper.getCompactingResult(item, 3, 3);
		}
		return CompactingResult.EMPTY;
	}

	public static CompactingResult getCompactingResult(Item item, int width, int height) {
		return getWorld().map(w -> getCompactingResult(item, w, width, height)).orElse(CompactingResult.EMPTY);
	}

	private static CompactingResult getCompactingResult(Item item, Level level, int width, int height) {
		CompactedItem compactedItem = new CompactedItem(item, width, height);
		if (COMPACTING_RESULTS.containsKey(compactedItem)) {
			return COMPACTING_RESULTS.get(compactedItem);
		}

		CraftingContainer craftingInventory = getFilledCraftingInventory(item, width, height);
		List<CraftingRecipe> compactingRecipes = safeGetRecipesFor(RecipeType.CRAFTING, craftingInventory, level);

		if (compactingRecipes.isEmpty()) {
			COMPACTING_RESULTS.put(compactedItem, CompactingResult.EMPTY);
			return CompactingResult.EMPTY;
		}

		if (compactingRecipes.size() == 1) {
			return cacheAndGetCompactingResult(compactedItem, compactingRecipes.get(0), craftingInventory);
		}

		for (CraftingRecipe recipe : compactingRecipes) {
			ItemStack result = recipe.assemble(craftingInventory, level.registryAccess());
			if (uncompactMatchesItem(result, level, item, width * height)) {
				return cacheAndGetCompactingResult(compactedItem, recipe, craftingInventory, result);
			}
		}

		return cacheAndGetCompactingResult(compactedItem, compactingRecipes.get(0), craftingInventory);
	}

	private static CompactingResult cacheAndGetCompactingResult(CompactedItem compactedItem, CraftingRecipe recipe, CraftingContainer craftingInventory) {
		Level level = world.get();
		if (level == null) {
			return CompactingResult.EMPTY;
		}

		return cacheAndGetCompactingResult(compactedItem, recipe, craftingInventory, recipe.assemble(craftingInventory, level.registryAccess()));
	}

	private static CompactingResult cacheAndGetCompactingResult(CompactedItem compactedItem, CraftingRecipe recipe, CraftingContainer craftingInventory, ItemStack result) {
		List<ItemStack> remainingItems = new ArrayList<>();
		recipe.getRemainingItems(craftingInventory).forEach(stack -> {
			if (!stack.isEmpty()) {
				remainingItems.add(stack);
			}
		});

		CompactingResult compactingResult = new CompactingResult(result, remainingItems);
		if (!result.isEmpty()) {
			COMPACTING_RESULTS.put(compactedItem, compactingResult);
		}
		return compactingResult;
	}

	private static CraftingContainer getFilledCraftingInventory(Item item, int width, int height) {
		CraftingContainer craftinginventory = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
				return ItemStack.EMPTY;
			}

			public boolean stillValid(Player playerIn) {
				return false;
			}
		}, width, height);

		for (int i = 0; i < craftinginventory.getContainerSize(); i++) {
			craftinginventory.setItem(i, new ItemStack(item));
		}
		return craftinginventory;
	}

	public static <T extends AbstractCookingRecipe> Optional<T> getCookingRecipe(ItemStack stack, RecipeType<T> recipeType) {
		return getWorld().flatMap(w -> safeGetRecipeFor(recipeType, new RecipeWrapper(new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, stack))), w));
	}

	public static Set<CompactingShape> getItemCompactingShapes(Item item) {
		return ITEM_COMPACTING_SHAPES.getUnchecked(item);
	}

	public static List<StonecutterRecipe> getStonecuttingRecipes(Container inventory) {
		return getRecipesOfType(RecipeType.STONECUTTING, inventory);
	}

	public static <T extends Recipe<Container>> List<T> getRecipesOfType(RecipeType<T> recipeType, Container inventory) {
		return getWorld().map(w -> w.getRecipeManager().getRecipesFor(recipeType, inventory, w)).orElse(Collections.emptyList());
	}

	public static <C extends Container, T extends Recipe<C>> Optional<T> safeGetRecipeFor(RecipeType<T> recipeType, C inventory, Level level) {
		try {
			return level.getRecipeManager().getRecipeFor(recipeType, inventory, level);
		}
		catch (Exception e) {
			SophisticatedCore.LOGGER.error("Error while getting recipe ", e);
			return Optional.empty();
		}
	}

	private static <C extends Container, T extends Recipe<C>> List<T> safeGetRecipesFor(RecipeType<T> recipeType, C inventory, Level level) {
		try {
			return level.getRecipeManager().getRecipesFor(recipeType, inventory, level);
		}
		catch (Exception e) {
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
		public static final UncompactingResult EMPTY = new UncompactingResult(Items.AIR, NONE);

		private final Item result;

		private final CompactingShape compactUsingShape;

		public UncompactingResult(Item result, CompactingShape compactUsingShape) {
			this.result = result;
			this.compactUsingShape = compactUsingShape;
		}

		public Item getResult() {
			return result;
		}

		public CompactingShape getCompactUsingShape() {
			return compactUsingShape;
		}
	}

	private static class CompactedItem {
		private final Item item;
		private final int width;
		private final int height;

		private CompactedItem(Item item, int width, int height) {
			this.item = item;
			this.width = width;
			this.height = height;
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
					item.equals(that.item);
		}

		@Override
		public int hashCode() {
			return Objects.hash(item, width, height);
		}
	}
}
