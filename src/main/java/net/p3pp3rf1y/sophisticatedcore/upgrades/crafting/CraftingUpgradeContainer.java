package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.*;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CraftingUpgradeContainer extends UpgradeContainerBase<CraftingUpgradeWrapper, CraftingUpgradeContainer> implements ICraftingContainer {
	private static final String DATA_SHIFT_CLICK_INTO_STORAGE = "shiftClickIntoStorage";
	private static final String DATA_SELECT_RESULT = "selectResult";
	private final ResultContainer craftResult = new ResultContainer();
	private final CraftingItemHandler craftMatrix;
	private final ResultSlot craftingResultSlot;
	@Nullable
	private CraftingRecipe lastRecipe = null;
	private List<CraftingRecipe> matchedCraftingRecipes = new ArrayList<>();
	private List<ItemStack> matchedCraftingResults = new ArrayList<>();
	private int selectedCraftingResultIndex = 0;

	public CraftingUpgradeContainer(Player player, int upgradeContainerId, CraftingUpgradeWrapper upgradeWrapper, UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);

		int slot;
		for (slot = 0; slot < upgradeWrapper.getInventory().getSlots(); slot++) {
			slots.add(new SlotSuppliedHandler(upgradeWrapper::getInventory, slot, -100, -100) {
				@Override
				public void setChanged() {
					super.setChanged();
					updateCraftingResult(player.level(), player, craftMatrix, craftResult, craftingResultSlot);
					craftMatrix.setChanged();
				}
			});
		}
		craftMatrix = new CraftingItemHandler(upgradeWrapper::getInventory, this::onCraftMatrixChanged);
		craftingResultSlot = new ResultSlot(player, craftMatrix, craftResult, slot, -100, -100) {
			@Override
			public void onTake(Player thePlayer, ItemStack stack) {
				ItemStack remainingStack = getItem();
				checkTakeAchievements(stack);
				net.minecraftforge.common.ForgeHooks.setCraftingPlayer(thePlayer);
				List<ItemStack> items;
				if (lastRecipe != null && lastRecipe.matches(craftMatrix, player.level())) {
					items = lastRecipe.getRemainingItems(craftMatrix);
				} else {
					items = craftMatrix.getItems();
				}
				net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
				for (int i = 0; i < items.size(); ++i) {
					ItemStack itemstack = craftMatrix.getItem(i);
					ItemStack itemstack1 = items.get(i);
					if (!itemstack.isEmpty()) {
						craftMatrix.removeItem(i, 1);
						itemstack = craftMatrix.getItem(i);
					}

					if (!itemstack1.isEmpty()) {
						if (itemstack.isEmpty()) {
							craftMatrix.setItem(i, itemstack1);
						} else if (ItemStack.isSameItemSameTags(itemstack, itemstack1)) {
							itemstack1.grow(itemstack.getCount());
							craftMatrix.setItem(i, itemstack1);
						} else if (!player.getInventory().add(itemstack1)) {
							player.drop(itemstack1, false);
						}
					}
					if (thePlayer.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu) {
						Slot slot = slots.get(i);
						storageContainerMenu.setSlotStackToUpdate(slot.index, slot.getItem());
					}
				}

				if (!remainingStack.isEmpty()) {
					player.drop(remainingStack, false);
				}
			}

			@Override
			public void setChanged() {
				super.setChanged();
				if (player.level().isClientSide()) {
					matchedCraftingRecipes.clear();
					matchedCraftingResults.clear();
					if (!getItem().isEmpty()) {
						matchedCraftingRecipes = RecipeHelper.safeGetRecipesFor(RecipeType.CRAFTING, craftMatrix, player.level());
						int resultIndex = 0;
						for (CraftingRecipe craftingRecipe : matchedCraftingRecipes) {
							ItemStack result = craftingRecipe.assemble(craftMatrix, player.level().registryAccess());
							matchedCraftingResults.add(result);
							if (ItemHandlerHelper.canItemStacksStack(getItem(), result)) {
								selectedCraftingResultIndex = resultIndex;
							}
							resultIndex++;
						}
					}
				}
			}
		};
		slots.add(craftingResultSlot);
	}

	@Override
	public void onInit() {
		super.onInit();
		onCraftMatrixChanged(craftMatrix);
	}

	private void onCraftMatrixChanged(Container iInventory) {
		updateCraftingResult(player.level(), player, craftMatrix, craftResult, craftingResultSlot);
	}

	private void updateCraftingResult(Level level, Player player, CraftingContainer inventory, ResultContainer inventoryResult, ResultSlot craftingResultSlot) {
		if (!level.isClientSide) {
			ServerPlayer serverplayerentity = (ServerPlayer) player;
			ItemStack itemstack = ItemStack.EMPTY;
			if (lastRecipe != null && lastRecipe.matches(inventory, level)) {
				itemstack = lastRecipe.assemble(inventory, level.registryAccess());
			} else {
				//noinspection ConstantConditions - we're on server and for sure in the world so getServer can't return null here
				List<CraftingRecipe> recipes = RecipeHelper.safeGetRecipesFor(RecipeType.CRAFTING, inventory, level);
				if (!recipes.isEmpty()) {
					matchedCraftingRecipes = recipes;
					matchedCraftingResults.clear();
					selectedCraftingResultIndex = 0;
					CraftingRecipe craftingRecipe = matchedCraftingRecipes.get(0);
					if (inventoryResult.setRecipeUsed(level, serverplayerentity, craftingRecipe)) {
						lastRecipe = craftingRecipe;
						itemstack = lastRecipe.assemble(inventory, level.registryAccess());
						matchedCraftingResults.add(itemstack.copy());
					} else {
						lastRecipe = null;
					}
					for (int i = 1; i < matchedCraftingRecipes.size(); i++) {
						matchedCraftingResults.add(matchedCraftingRecipes.get(i).assemble(inventory, level.registryAccess()));
					}
				}
			}

			craftingResultSlot.set(itemstack);
			if (serverplayerentity.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu) {
				storageContainerMenu.setSlotStackToUpdate(craftingResultSlot.index, itemstack);
			}
		}
	}

	public List<ItemStack> getMatchedCraftingResults() {
		return matchedCraftingResults;
	}

	public void selectNextCraftingResult() {
		if (matchedCraftingResults.size() > 1) {
			selectCraftingResult((selectedCraftingResultIndex + 1) % matchedCraftingResults.size());
		}
	}

	public void selectPreviousCraftingResult() {
		if (matchedCraftingResults.size() > 1) {
			selectCraftingResult((selectedCraftingResultIndex + matchedCraftingResults.size() - 1) % matchedCraftingResults.size());
		}
	}

	public void selectCraftingResult(int resultIndex) {
		if (resultIndex < 0 || resultIndex >= matchedCraftingResults.size()) {
			return;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			selectedCraftingResultIndex = resultIndex;
			lastRecipe = matchedCraftingRecipes.get(resultIndex);
			ItemStack result = matchedCraftingResults.get(resultIndex).copy();
			craftingResultSlot.set(result);
			//noinspection DataFlowIssue - lastRecipe can't be null here as there's always a recipe in list for the result
			if (craftResult.setRecipeUsed(player.level(), serverPlayer, lastRecipe)
					&& serverPlayer.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu) {
				storageContainerMenu.setSlotStackToUpdate(craftingResultSlot.index, result);
			}
		} else {
			sendDataToServer(() -> NBTHelper.putInt(new CompoundTag(), DATA_SELECT_RESULT, resultIndex));
		}
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains(DATA_SHIFT_CLICK_INTO_STORAGE)) {
			setShiftClickIntoStorage(data.getBoolean(DATA_SHIFT_CLICK_INTO_STORAGE));
		} else if (data.contains(DATA_SELECT_RESULT)) {
			selectCraftingResult(data.getInt(DATA_SELECT_RESULT));
		}
	}

	@Override
	public ItemStack getSlotStackToTransfer(Slot slot) {
		if (slot == craftingResultSlot) {
			ItemStack slotStack = slot.getItem();
			slotStack.getItem().onCraftedBy(slotStack, player.level(), player);
			return slotStack;
		}
		return super.getSlotStackToTransfer(slot);
	}

	@Override
	public List<Slot> getRecipeSlots() {
		return slots.subList(0, 9);
	}

	@Override
	public Container getCraftMatrix() {
		return craftMatrix;
	}

	@Override
	public void setRecipeUsed(ResourceLocation recipeId) {
		if (lastRecipe != null && lastRecipe.getId().equals(recipeId)) {
			return;
		}
		player.level().getRecipeManager().byKey(recipeId).filter(r -> r.getType() == RecipeType.CRAFTING).map(r -> (CraftingRecipe) r)
				.ifPresent(recipe -> {
					lastRecipe = recipe;
					for (int i = 0; i < matchedCraftingRecipes.size(); i++) {
						if (matchedCraftingRecipes.get(i).getId().equals(recipeId)) {
							selectCraftingResult(i);
							return;
						}
					}
				});
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgradeWrapper.shouldShiftClickIntoStorage();
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgradeWrapper.setShiftClickIntoStorage(shiftClickIntoStorage);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage));
	}

	@Override
	public boolean mergeIntoStorageFirst(Slot slot) {
		return !(slot instanceof ResultSlot) || shouldShiftClickIntoStorage();
	}

	@Override
	public boolean allowsPickupAll(Slot slot) {
		return slot != craftingResultSlot;
	}
}
