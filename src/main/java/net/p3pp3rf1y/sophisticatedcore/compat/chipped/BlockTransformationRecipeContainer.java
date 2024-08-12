package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import com.google.common.base.Suppliers;
import earth.terrarium.chipped.common.recipes.ChippedRecipe;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingItemHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SimpleItemContent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BlockTransformationRecipeContainer {
	private static final String DATA_SELECTED_RECIPE_INDEX = "selectedRecipeIndex";
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
	private long lastOnTake = -1;

	public BlockTransformationRecipeContainer(BlockTransformationUpgradeContainer upgradeContainer, RecipeType<ChippedRecipe> recipeType, Consumer<Slot> addSlot, IServerUpdater serverUpdater, ContainerLevelAccess worldPosCallable) {
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
			stack.onCraftedBy(thePlayer.level(), thePlayer, stack.getCount());
			resultInventory.awardUsedRecipes(thePlayer, List.of(inputSlot.getItem()));
			ItemStack itemstack = inputSlot.remove(1);
			if (!itemstack.isEmpty()) {
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
}
