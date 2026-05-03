package net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter;

import com.google.common.collect.Lists;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingItemHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BlockConverterRecipeContainer<R extends SingleItemRecipe, W extends BlockConverterUpgradeWrapper<?, ?>, RC extends BlockConverterRecipeContainer<R, W, RC, C>,  C extends BlockConverterUpgradeContainer<R, W, C, RC>> {
	private static final String DATA_SELECTED_RECIPE_INDEX = "selectedRecipeIndex";
	private final C upgradeContainer;
	private final Slot inputSlot;
	private final IServerUpdater serverUpdater;
	protected final Level level;
	private final Slot outputSlot;
	private final ResultContainer resultInventory = new ResultContainer();
	private List<RecipeHolder<R>> recipes = Lists.newArrayList();
	private final DataSlot selectedRecipe = DataSlot.standalone();
	private Item inputItem = Items.AIR;
	private final CraftingItemHandler inputInventory;
	private Runnable inventoryUpdateListener = () -> {};
	private final Supplier<Optional<ResourceKey<Recipe<?>>>> getLastSelectedRecipeId;
	private final Consumer<ResourceKey<Recipe<?>>> setLastSelectedRecipeId;
	private final List<ResourceLocation> recentResultItems = new ArrayList<>();
	private long lastOnTake = -1;
	private final SoundEvent craftSound;

	public BlockConverterRecipeContainer(C upgradeContainer, Consumer<Slot> addSlot, IServerUpdater serverUpdater, ContainerLevelAccess worldPosCallable, Level level, SoundEvent craftSound) {
		this.upgradeContainer = upgradeContainer;
		this.level = level;
		inputSlot = new SlotSuppliedHandler(upgradeContainer.getUpgradeWrapper()::getInputInventory, 0, -1, -1) {
			private boolean countIncreased = false;

			@Override
			public void setChanged() {
				super.setChanged();
				onCraftMatrixChanged(inputInventory, countIncreased);
			}

			@Override
			public ItemStack safeInsert(ItemStack stack, int increment) {
				countIncreased = increment > 0;
				return super.safeInsert(stack, increment);
			}

			@Override
			public ItemStack remove(int amount) {
				ItemStack ret = super.remove(amount);
				setChanged();
				countIncreased = false;
				return ret;
			}
		};
		this.craftSound = craftSound;
		this.serverUpdater = serverUpdater;
		addSlot.accept(inputSlot);
		inputInventory = new CraftingItemHandler(upgradeContainer.getUpgradeWrapper()::getInputInventory, inventory -> onCraftMatrixChanged(inventory, false));
		outputSlot = new ResultSlot(worldPosCallable);
		addSlot.accept(outputSlot);

		getLastSelectedRecipeId = upgradeContainer.getUpgradeWrapper()::getRecipeId;
		setLastSelectedRecipeId = upgradeContainer.getUpgradeWrapper()::setRecipeId;

		onCraftMatrixChanged(inputInventory, false);
	}

	protected abstract RecipeType<R> getRecipeType();

	protected abstract List<RecipeHolder<R>> filterAndSortRecipes(List<RecipeHolder<R>> recipes);

	private void onCraftMatrixChanged(Container inventory, boolean countIncreased) {
		ItemStack stack = inputSlot.getItem();
		if (shouldRefreshRecipes(stack, countIncreased)) {
			inputItem = stack.getItem();
			updateAvailableRecipes(inventory, stack);
		}
		updateClientRecentResults(stack);
		inventoryUpdateListener.run();
	}

	protected boolean shouldRefreshRecipes(ItemStack itemstack, boolean countIncreased) {
		return itemstack.getItem() != inputItem;
	}

	private void updateAvailableRecipes(Container inventory, ItemStack stack) {
		selectedRecipe.set(-1);
		outputSlot.set(ItemStack.EMPTY);
		if (!stack.isEmpty()) {
			recipes = RecipeHelper.getRecipesOfType(getRecipeType(), new SingleRecipeInput(inventory.getItem(0)));
			recipes = filterAndSortRecipes(recipes);
			getLastSelectedRecipeId.get().ifPresent(id -> {
				for (int i = 0; i < recipes.size(); i++) {
					if (recipes.get(i).id().equals(id)) {
						selectedRecipe.set(i);
						updateRecipeResultSlot();
					}
				}
			});
		} else {
			recipes = Collections.emptyList();
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

	public List<RecipeHolder<R>> getRecipeList() {
		return recipes;
	}

	public int getSelectedRecipe() {
		return selectedRecipe.get();
	}

	public boolean hasItemsInInputSlot() {
		return inputSlot.hasItem() && !recipes.isEmpty();
	}

	public boolean selectRecipe(int recipeIndex) {
		if (isIndexInRecipeBounds(recipeIndex)) {
			selectedRecipe.set(recipeIndex);
			setLastSelectedRecipeId.accept(recipes.get(recipeIndex).id());
			updateRecipeResultSlot();
			serverUpdater.sendDataToServer(() -> NBTHelper.putInt(new CompoundTag(), DATA_SELECTED_RECIPE_INDEX, recipeIndex));
		}
		return true;
	}

	public boolean isRecentResult(int resultIndex) {
		return isIndexInRecipeBounds(resultIndex) && recentResultItems.contains(getItemRegistryName(recipes.get(resultIndex).value().result));
	}

	public int getRecentResultOrder(int resultIndex) {
		if (!isIndexInRecipeBounds(resultIndex)) {
			return Integer.MAX_VALUE;
		}

		int recentIndex = recentResultItems.indexOf(getItemRegistryName(recipes.get(resultIndex).value().result));
		return recentIndex < 0 ? Integer.MAX_VALUE : recentIndex;
	}

	private boolean isIndexInRecipeBounds(int index) {
		return index >= 0 && index < recipes.size();
	}

	private void updateRecipeResultSlot() {
		if (!recipes.isEmpty() && isIndexInRecipeBounds(selectedRecipe.get())) {
			RecipeHolder<R> recipe = recipes.get(selectedRecipe.get());
			resultInventory.setRecipeUsed(recipe);
			outputSlot.set(recipe.value().assemble(new SingleRecipeInput(inputInventory.getItem(0)), level.registryAccess()));
		} else {
			outputSlot.set(ItemStack.EMPTY);
		}
	}

	public void handlePacket(CompoundTag data) {
		data.getInt(DATA_SELECTED_RECIPE_INDEX).ifPresent(this::selectRecipe);
	}

	public void refreshRecentResultsFromClientCache() {
		updateClientRecentResults(inputSlot.getItem());
	}

	private void updateClientRecentResults(ItemStack ingredient) {
		if (level.isClientSide) {
			updateRecentResultItems(ingredient.isEmpty() ? List.of() : RecentCraftedResultStorage.getClientRecentResults(getRecipeScope(), getItemRegistryName(ingredient)));
		}
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
		return BuiltInRegistries.RECIPE_TYPE.getKey(getRecipeType());
	}

	private ResourceLocation getItemRegistryName(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
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
		public void onTake(Player player, ItemStack stack) {
			if (level instanceof ServerLevel serverLevel && RecentCraftedResultStorage.get(serverLevel).recordCraftedResult(player, getRecipeScope(), getItemRegistryName(inputSlot.getItem()), getItemRegistryName(stack))) {
				if (player instanceof ServerPlayer serverPlayer) {
					RecentCraftedResultStorage.syncToPlayer(serverPlayer);
				}
			}
			ItemStack inputStack = inputSlot.getItem().copy();
			int inputCount = getInputCount();
			stack.onCraftedBy(player, stack.getCount());
			resultInventory.awardUsedRecipes(player, List.of(inputSlot.getItem()));
			ItemStack itemstack = inputSlot.remove(inputCount);
			if (!itemstack.isEmpty()) {
				tryRefillInput(inputStack, inputCount);
				updateRecipeResultSlot();
			}

			worldPosCallable.execute((world, pos) -> {
				long l = world.getGameTime();
				if (lastOnTake != l) {
					world.playSound(null, pos, craftSound, SoundSource.BLOCKS, 1.0F, 1.0F);
					lastOnTake = l;
				}
			});
			super.onTake(player, stack);
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

		inputSlot.safeInsert(extracted, extracted.getCount());
		return true;
	}

	protected abstract int getInputCount();
}
