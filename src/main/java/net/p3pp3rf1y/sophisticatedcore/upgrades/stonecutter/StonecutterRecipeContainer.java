package net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter;

import com.google.common.collect.Lists;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.RecentCraftedResultStorage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingItemHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StonecutterRecipeContainer {
	private static final String DATA_SELECTED_RECIPE_INDEX = "selectedRecipeIndex";
	private final StonecutterUpgradeContainer upgradeContainer;
	private final Slot inputSlot;
	private final IServerUpdater serverUpdater;
	private final Level level;
	private final Slot outputSlot;
	private final ResultContainer resultInventory = new ResultContainer();
	private List<StonecutterRecipe> recipes = Lists.newArrayList();
	private final DataSlot selectedRecipe = DataSlot.standalone();
	private Item inputItem = Items.AIR;
	private final CraftingItemHandler inputInventory;
	private Runnable inventoryUpdateListener = () -> {
	};
	private final Supplier<Optional<ResourceLocation>> getLastSelectedRecipeId;
	private final Consumer<ResourceLocation> setLastSelectedRecipeId;
	private final List<ResourceLocation> recentResultItems = new ArrayList<>();
	private long lastOnTake = -1;

	public StonecutterRecipeContainer(StonecutterUpgradeContainer upgradeContainer, Consumer<Slot> addSlot, IServerUpdater serverUpdater,
			ContainerLevelAccess worldPosCallable, Level level) {
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
		this.serverUpdater = serverUpdater;
		this.level = level;
		addSlot.accept(inputSlot);
		inputInventory = new CraftingItemHandler(upgradeContainer.getUpgradeWrapper()::getInputInventory, this::onCraftMatrixChanged);
		outputSlot = new ResultSlot(worldPosCallable);
		addSlot.accept(outputSlot);

		getLastSelectedRecipeId = upgradeContainer.getUpgradeWrapper()::getRecipeId;
		setLastSelectedRecipeId = upgradeContainer.getUpgradeWrapper()::setRecipeId;

		onCraftMatrixChanged(inputInventory);
	}

	private void onCraftMatrixChanged(Container inventoryIn) {
		ItemStack itemstack = inputSlot.getItem();
		if (itemstack.getItem() != inputItem) {
			inputItem = itemstack.getItem();
			updateAvailableRecipes(inventoryIn, itemstack);
		}
		refreshRecentResultsFromClientCache();
		inventoryUpdateListener.run();
	}

	private void updateAvailableRecipes(Container inventory, ItemStack stack) {
		recipes.clear();
		selectedRecipe.set(-1);
		outputSlot.set(ItemStack.EMPTY);
		if (!stack.isEmpty()) {
			recipes = RecipeHelper.getRecipesOfType(RecipeType.STONECUTTING, inventory);
			getLastSelectedRecipeId.get().ifPresent(id -> {
				for (int i = 0; i < recipes.size(); i++) {
					if (recipes.get(i).getId().equals(id)) {
						selectedRecipe.set(i);
						updateRecipeResultSlot();
					}
				}
			});
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

	public List<StonecutterRecipe> getRecipeList() {
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
			setLastSelectedRecipeId.accept(recipes.get(recipeIndex).getId());
			updateRecipeResultSlot();
			serverUpdater.sendDataToServer(() -> NBTHelper.putInt(new CompoundTag(), DATA_SELECTED_RECIPE_INDEX, recipeIndex));
		}
		return true;
	}

	public boolean isRecentResult(int resultIndex) {
		return isIndexInRecipeBounds(resultIndex)
				&& recentResultItems.contains(getItemRegistryName(recipes.get(resultIndex).getResultItem(level.registryAccess())));
	}

	public int getRecentResultOrder(int resultIndex) {
		if (!isIndexInRecipeBounds(resultIndex)) {
			return Integer.MAX_VALUE;
		}

		int recentIndex = recentResultItems.indexOf(getItemRegistryName(recipes.get(resultIndex).getResultItem(level.registryAccess())));
		return recentIndex < 0 ? Integer.MAX_VALUE : recentIndex;
	}

	private boolean isIndexInRecipeBounds(int index) {
		return index >= 0 && index < recipes.size();
	}

	private void updateRecipeResultSlot() {
		if (!recipes.isEmpty() && isIndexInRecipeBounds(selectedRecipe.get())) {
			StonecutterRecipe stonecuttingrecipe = recipes.get(selectedRecipe.get());
			resultInventory.setRecipeUsed(stonecuttingrecipe);
			outputSlot.set(stonecuttingrecipe.assemble(inputInventory, level.registryAccess()));
		} else {
			outputSlot.set(ItemStack.EMPTY);
		}
	}

	public void handleMessage(CompoundTag data) {
		if (data.contains(DATA_SELECTED_RECIPE_INDEX)) {
			selectRecipe(data.getInt(DATA_SELECTED_RECIPE_INDEX));
		}
	}

	public void refreshRecentResultsFromClientCache() {
		updateClientRecentResults(inputSlot.getItem());
	}

	private void updateClientRecentResults(ItemStack ingredient) {
		if (level.isClientSide) {
			updateRecentResultItems(
					ingredient.isEmpty() ? List.of() : RecentCraftedResultStorage.getClientRecentResults(getRecipeScope(), getItemRegistryName(ingredient)));
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
		return BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.STONECUTTING);
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
			if (level instanceof ServerLevel serverLevel && RecentCraftedResultStorage.get(serverLevel).recordCraftedResult(player, getRecipeScope(),
					getItemRegistryName(inputSlot.getItem()), getItemRegistryName(stack))) {
				if (player instanceof ServerPlayer serverPlayer) {
					RecentCraftedResultStorage.syncToPlayer(serverPlayer);
				}
			}
			ItemStack inputStack = inputSlot.getItem().copy();
			int inputCount = 1;
			stack.onCraftedBy(player.level(), player, stack.getCount());
			resultInventory.awardUsedRecipes(player, List.of(inputSlot.getItem()));
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
			super.onTake(player, stack);
		}
	}

	private boolean tryRefillInput(ItemStack inputStack, int inputCount) {
		if (!inputStack.isEmpty() && inputCount > 0 && inputSlot.getItem().isEmpty() && upgradeContainer.shouldRefillInput()) {
			ItemStack extracted = upgradeContainer.getUpgradeWrapper().extractFromStorage(inputStack.copyWithCount(inputCount), false);
			if (extracted.getCount() == inputCount) {
				inputSlot.safeInsert(extracted, inputCount);
				return true;
			}
		}

		return false;
	}
}
