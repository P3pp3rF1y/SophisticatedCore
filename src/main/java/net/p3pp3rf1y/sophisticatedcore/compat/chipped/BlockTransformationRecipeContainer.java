package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import com.google.common.base.Suppliers;
import earth.terrarium.chipped.common.recipes.ChippedRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.RecentCraftedResultStorage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingItemHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SimpleItemContent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BlockTransformationRecipeContainer {
	private static final String DATA_SELECTED_RECIPE_INDEX = "selectedRecipeIndex";
	private final BlockTransformationUpgradeContainer upgradeContainer;
	private final RecipeType<ChippedRecipe> recipeType;
	private final Slot inputSlot;
	private final IServerUpdater serverUpdater;
	private final Slot outputSlot;
	private final ResultContainer resultInventory = new ResultContainer();
	@Nullable
	private RecipeHolder<ChippedRecipe> recipe = null;
	private Supplier<List<ItemStack>> results = Collections::emptyList;
	private final DataSlot selectedRecipe = DataSlot.standalone();
	private Item inputItem = Items.AIR;
	private final CraftingItemHandler inputInventory;
	private Runnable inventoryUpdateListener = () -> {
	};
	private final Supplier<Optional<SimpleItemContent>> getLastSelectedResult;
	private final Consumer<ItemStack> setLastSelectedResult;
	private final List<ResourceLocation> recentResultItems = new ArrayList<>();
	private long lastOnTake = -1;

	public BlockTransformationRecipeContainer(BlockTransformationUpgradeContainer upgradeContainer, RecipeType<ChippedRecipe> recipeType, Consumer<Slot> addSlot, IServerUpdater serverUpdater, ContainerLevelAccess worldPosCallable) {
		this.upgradeContainer = upgradeContainer;
		inputSlot = new SlotSuppliedHandler(upgradeContainer.getUpgradeWrapper()::getInputInventory, 0, -1, -1) {
			@Override
			public void setChanged() {
				super.setChanged();
				onCraftMatrixChanged(inputInventory);
			}

			@Override
			public ItemStack remove(int amount) {
				ItemStack ret = super.remove(amount);
				if (getItem().isEmpty()) {
					setChanged();
				}
				return ret;
			}
		};
		this.recipeType = recipeType;
		this.serverUpdater = serverUpdater;
		addSlot.accept(inputSlot);
		inputInventory = new CraftingItemHandler(upgradeContainer.getUpgradeWrapper()::getInputInventory, this::onCraftMatrixChanged);
		outputSlot = new ResultSlot(worldPosCallable);
		addSlot.accept(outputSlot);

		getLastSelectedResult = upgradeContainer.getUpgradeWrapper()::getResult;
		setLastSelectedResult = upgradeContainer.getUpgradeWrapper()::setResult;

		onCraftMatrixChanged(inputInventory);
	}

	private void onCraftMatrixChanged(Container inventoryIn) {
		ItemStack itemstack = inputSlot.getItem();
		if (itemstack.getItem() != inputItem) {
			inputItem = itemstack.getItem();
			updateRecipe(inventoryIn, itemstack);
		}
		refreshRecentResultsFromClientCache();
		inventoryUpdateListener.run();
	}

	private void updateRecipe(Container inventory, ItemStack stack) {
		recipe = null;
		selectedRecipe.set(-1);
		outputSlot.set(ItemStack.EMPTY);
		if (!stack.isEmpty()) {
			ItemStack inputStack = inventory.getItem(0);
			RecipeHelper.getRecipesOfType(recipeType, new SingleRecipeInput(inputStack)).stream().findFirst().ifPresent(r -> {
				recipe = r;
				results = Suppliers.memoize(() -> recipe.value().getResults(inputStack).toList());
				getLastSelectedResult.get().ifPresent(lastSelectedResult -> {
					int i = 0;
					for (ItemStack result : results.get()) {
						if (lastSelectedResult.isSameItemSameComponents(result)) {
							selectedRecipe.set(i);
							updateRecipeResultSlot();
							return;
						}
						i++;
					}
				});
			});
		} else {
			results = Collections::emptyList;
		}
	}

	public Slot getInputSlot() {
		return inputSlot;
	}

	public Slot getOutputSlot() {
		return outputSlot;
	}

	public void setInventoryUpdateListener(Runnable listenerIn) {
		inventoryUpdateListener = listenerIn;
	}

	public List<ItemStack> getResults() {
		return results.get();
	}

	public int getSelectedRecipe() {
		return selectedRecipe.get();
	}

	public boolean hasItemsInInputSlot() {
		return inputSlot.hasItem() && recipe != null;
	}

	public boolean selectRecipeIndex(int recipeIndex) {
		if (recipe != null && isIndexInRecipeBounds(recipeIndex)) {
			selectedRecipe.set(recipeIndex);
			setLastSelectedResult.accept(results.get().get(recipeIndex));
			updateRecipeResultSlot();
			serverUpdater.sendDataToServer(() -> NBTHelper.putInt(new CompoundTag(), DATA_SELECTED_RECIPE_INDEX, recipeIndex));
		}
		return true;
	}

	public boolean isRecentResult(int resultIndex) {
		return isIndexInRecipeBounds(resultIndex) && recentResultItems.contains(getItemRegistryName(results.get().get(resultIndex)));
	}

	public int getRecentResultOrder(int resultIndex) {
		if (!isIndexInRecipeBounds(resultIndex)) {
			return Integer.MAX_VALUE;
		}

		int recentIndex = recentResultItems.indexOf(getItemRegistryName(results.get().get(resultIndex)));
		return recentIndex < 0 ? Integer.MAX_VALUE : recentIndex;
	}

	private boolean isIndexInRecipeBounds(int index) {
		return recipe != null && index >= 0 && index < recipe.value().getResults(inputInventory.getItem(0)).count();
	}

	private void updateRecipeResultSlot() {
		if (recipe != null && isIndexInRecipeBounds(selectedRecipe.get())) {
			recipe.value().getResults(inputInventory.getItem(0)).skip(selectedRecipe.get()).findFirst().ifPresent(stack -> outputSlot.set(stack.copy()));
			resultInventory.setRecipeUsed(recipe);
		} else {
			outputSlot.set(ItemStack.EMPTY);
		}
	}

	public void handlePacket(CompoundTag data) {
		if (data.contains(DATA_SELECTED_RECIPE_INDEX)) {
			selectRecipeIndex(data.getInt(DATA_SELECTED_RECIPE_INDEX));
		}
	}

	public void refreshRecentResultsFromClientCache() {
		updateClientRecentResults(inputSlot.getItem());
	}

	private void updateClientRecentResults(ItemStack ingredient) {
		if (upgradeContainer.getPlayer().level().isClientSide) {
			updateRecentResultItems(ingredient.isEmpty() ? List.of() : getClientRecentResults(ingredient));
		}
	}

	private List<ResourceLocation> getClientRecentResults(ItemStack ingredient) {
		List<ResourceLocation> recentResults = getMatchingIngredientKey(ingredient)
				.map(key -> RecentCraftedResultStorage.getClientRecentResults(getRecipeScope(), key))
				.orElse(List.of());
		if (!recentResults.isEmpty()) {
			return recentResults;
		}

		recentResults = getRecipeNamespaceIngredientKey(ingredient)
				.map(key -> RecentCraftedResultStorage.getClientRecentResults(getRecipeScope(), key))
				.orElse(List.of());
		if (!recentResults.isEmpty()) {
			return recentResults;
		}

		recentResults = RecentCraftedResultStorage.getClientRecentResults(getRecipeScope(), getItemRegistryName(ingredient));
		if (!recentResults.isEmpty()) {
			return recentResults;
		}

		return getResultGroupKey()
				.map(key -> RecentCraftedResultStorage.getClientRecentResults(getRecipeScope(), key))
				.orElse(List.of());
	}

	private void updateRecentResultItems(List<ResourceLocation> recentResults) {
		List<ResourceLocation> previousRecentResultItems = List.copyOf(recentResultItems);
		recentResultItems.clear();
		for (ResourceLocation result : recentResults) {
			if (!recentResultItems.contains(result)) {
				recentResultItems.add(result);
			}
		}
		if (!previousRecentResultItems.equals(recentResultItems)) {
			inventoryUpdateListener.run();
		}
	}

	private ResourceLocation getRecipeScope() {
		return BuiltInRegistries.RECIPE_TYPE.getKey(recipeType);
	}

	private ResourceLocation getItemRegistryName(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
	}

	private ResourceLocation getRecentResultsKey(ItemStack ingredient) {
		return getMatchingIngredientKey(ingredient).orElseGet(() -> getResultGroupKey().orElseGet(() -> getItemRegistryName(ingredient)));
	}

	private Optional<ResourceLocation> getRecipeNamespaceIngredientKey(ItemStack ingredient) {
		if (recipe == null || ingredient.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(ResourceLocation.fromNamespaceAndPath(recipe.id().getNamespace(), getItemRegistryName(ingredient).getPath()));
	}

	private Optional<ResourceLocation> getMatchingIngredientKey(ItemStack inputStack) {
		if (recipe == null || inputStack.isEmpty()) {
			return Optional.empty();
		}

		for (Ingredient ingredient : recipe.value().ingredients()) {
			if (ingredient.test(inputStack)) {
				Optional<ResourceLocation> ingredientKey = getIngredientGroupKey(ingredient);
				if (ingredientKey.isPresent()) {
					return ingredientKey;
				}
			}
		}
		return Optional.empty();
	}

	private Optional<ResourceLocation> getIngredientGroupKey(Ingredient ingredient) {
		ItemStack[] matchingItems = ingredient.getItems();
		if (matchingItems.length > 0) {
			return Optional.of(ResourceLocation.fromNamespaceAndPath(recipe.id().getNamespace(), getItemRegistryName(matchingItems[0]).getPath()));
		}

		return getIngredientKey(ingredient);
	}

	private Optional<ResourceLocation> getIngredientKey(Ingredient ingredient) {
		if (ingredient.isCustom()) {
			return Optional.empty();
		}

		for (Ingredient.Value value : ingredient.getValues()) {
			if (value instanceof Ingredient.TagValue tagValue) {
				return Optional.of(tagValue.tag().location());
			}
		}
		return Optional.empty();
	}

	private Optional<ResourceLocation> getResultGroupKey() {
		List<String> resultIds = results.get().stream().map(result -> getItemRegistryName(result).toString()).sorted().toList();
		if (resultIds.isEmpty()) {
			return Optional.empty();
		}

		UUID resultGroupId = UUID.nameUUIDFromBytes(String.join("|", resultIds).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		return Optional.of(SophisticatedCore.getRL("result_group/" + resultGroupId));
	}

	public boolean isNotResultSlot(Slot slot) {
		return slot != outputSlot;
	}

	private class ResultSlot extends Slot {
		private final ContainerLevelAccess worldPosCallable;

		public ResultSlot(ContainerLevelAccess worldPosCallable) {
			super(resultInventory, 1, -1, -1);
			this.worldPosCallable = worldPosCallable;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public void onTake(Player thePlayer, ItemStack stack) {
			if (upgradeContainer.getPlayer().level() instanceof ServerLevel serverLevel && RecentCraftedResultStorage.get(serverLevel).recordCraftedResult(thePlayer, getRecipeScope(), getRecentResultsKey(inputSlot.getItem()), getItemRegistryName(stack))) {
				if (thePlayer instanceof ServerPlayer serverPlayer) {
					RecentCraftedResultStorage.syncToPlayer(serverPlayer);
				}
			}
			ItemStack inputStack = inputSlot.getItem().copy();
			int inputCount = 1;
			stack.onCraftedBy(thePlayer.level(), thePlayer, stack.getCount());
			resultInventory.awardUsedRecipes(thePlayer, List.of(inputSlot.getItem()));
			ItemStack itemstack = inputSlot.remove(inputCount);
			if (!itemstack.isEmpty()) {
				tryRefillInput(inputStack, inputCount);
				updateRecipeResultSlot();
			}

			worldPosCallable.execute((world, pos) -> {
				long l = world.getGameTime();
				if (lastOnTake != l) {
					world.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
					lastOnTake = l;
				}
			});
			super.onTake(thePlayer, stack);
		}
	}

	private boolean tryRefillInput(ItemStack inputStack, int inputCount) {
		if (!upgradeContainer.shouldRefillInput() || inputStack.isEmpty()) {
			return false;
		}

		ItemStack extracted = upgradeContainer.getUpgradeWrapper().extractFromStorage(inputStack.copyWithCount(inputCount), false);
		if (extracted.getCount() != inputCount) {
			return false;
		}

		inputSlot.safeInsert(extracted, inputCount);
		return true;
	}
}
