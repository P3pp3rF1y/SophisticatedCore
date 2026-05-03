package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CraftingUpgradeContainer extends UpgradeContainerBase<CraftingUpgradeWrapper, CraftingUpgradeContainer> implements ICraftingContainer {
	private static final String DATA_SHIFT_CLICK_INTO_STORAGE = "shiftClickIntoStorage";
	private static final String DATA_SELECT_RESULT = "selectResult";
	private static final String DATA_REFILL_CRAFTING_GRID = "refill_crafting_grid";
	private final ResultContainer craftResult = new ResultContainer();
	private final CraftingItemHandler craftMatrix;
	private final ResultSlot craftingResultSlot;
	@Nullable
	private RecipeHolder<CraftingRecipe> lastRecipe = null;
	private List<RecipeHolder<CraftingRecipe>> matchedCraftingRecipes = new ArrayList<>();
	private final List<ItemStack> matchedCraftingResults = new ArrayList<>();
	private int selectedCraftingResultIndex = 0;

	public CraftingUpgradeContainer(Player player, int upgradeContainerId, CraftingUpgradeWrapper upgradeWrapper, UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);

		int slot;
		for (slot = 0; slot < upgradeWrapper.getInventory().size(); slot++) {
			slots.add(new SlotSuppliedHandler(upgradeWrapper::getInventory, slot, -100, -100) {
				@Override
				public void set(ItemStack stack) {
					super.set(stack);
					craftMatrix.setChanged();
					updateCraftingResult(player.level(), player, craftMatrix, craftResult, craftingResultSlot);
				}

				@Override
				public boolean mayPickup(Player player) {
					return getItem().isEmpty() || super.mayPickup(player); // allow taking empty slots so that JEI slot validation would be cool with these slots
				}
			});
		}
		craftMatrix = new CraftingItemHandler(upgradeWrapper::getInventory, this::onCraftMatrixChanged);
		craftingResultSlot = new ResultSlot(player, craftMatrix, craftResult, slot, -100, -100) {
			@Override
			public void onTake(Player thePlayer, ItemStack stack) {
				if (thePlayer.level().isClientSide()) {
					return;
				}

				ItemStack remainingStack = getItem();
				checkTakeAchievements(stack);
				net.neoforged.neoforge.common.CommonHooks.setCraftingPlayer(thePlayer);
				List<ItemStack> remainingItems;
				if (lastRecipe != null && lastRecipe.value().matches(craftMatrix.asCraftInput(), player.level())) {
					remainingItems = lastRecipe.value().getRemainingItems(craftMatrix.asCraftInput());
				} else {
					remainingItems = NonNullList.withSize(craftMatrix.getContainerSize(), ItemStack.EMPTY);
				}

				net.neoforged.neoforge.common.CommonHooks.setCraftingPlayer(null);
				CraftingInput.Positioned craftingInput = craftMatrix.asPositionedCraftInput();
				int remaininItemsIndex = 0;
				for (int row = craftingInput.top(); row < craftingInput.top() + craftingInput.input().height(); row++) {
					for (int col = craftingInput.left(); col < craftingInput.left() + craftingInput.input().width(); col++) {
						int i = row * craftMatrix.getWidth() + col;
						if (remaininItemsIndex >= 9) {
							logErrorAndDropRemainingItems(remaininItemsIndex, remainingItems);
							break;
						}

						ItemStack recipeInputStack = craftMatrix.getItem(i);
						ItemStack remainingItemStack = remainingItems.get(remaininItemsIndex);
						if (!recipeInputStack.isEmpty()) {
							if (remainingItemStack.isEmpty() && shouldRefillCraftingGrid() && upgradeWrapper.extractFromStorageOrPlayer(player, recipeInputStack)) {
								onCraftMatrixChanged(craftMatrix);
							} else {
								craftMatrix.removeItem(i, 1);
							}
							recipeInputStack = craftMatrix.getItem(i);
						}

						if (!remainingItemStack.isEmpty()) {
							if (recipeInputStack.isEmpty()) {
								craftMatrix.setItem(i, remainingItemStack);
							} else if (ItemStack.isSameItemSameComponents(recipeInputStack, remainingItemStack)) {
								remainingItemStack.grow(recipeInputStack.getCount());
								craftMatrix.setItem(i, remainingItemStack);
							} else if (!player.getInventory().add(remainingItemStack)) {
								player.drop(remainingItemStack, false);
							}
						}
						remaininItemsIndex++;
					}
				}

				if (!remainingStack.isEmpty()) {
					player.drop(remainingStack, false);
				}

				if (matchedCraftingRecipes.isEmpty() && player.level().isClientSide()) {
					lastRecipe = null;
				}
			}

			private void logErrorAndDropRemainingItems(int remaininItemsIndex, List<ItemStack> remainingItems) {
				for (int j = remaininItemsIndex; j < remainingItems.size(); j++) {
					ItemStack remaining = remainingItems.get(j);
					if (!remaining.isEmpty()) {
						player.drop(remaining, false);
					}
				}
				SophisticatedCore.LOGGER.error("Recipe " + (lastRecipe != null ? lastRecipe.id() : "[unknown]") + " returned more than 9 remaining items, ignoring the rest!");
			}

			@Override
			public void setChanged() {
				super.setChanged();
				if (player.level().isClientSide()) {
					matchedCraftingResults.clear();
					if (!getItem().isEmpty()) {
						matchedCraftingRecipes = RecipeHelper.safeGetRecipesFor(RecipeType.CRAFTING, craftMatrix.asCraftInput(), player.level());
						int resultIndex = 0;
						for (RecipeHolder<CraftingRecipe> craftingRecipe : matchedCraftingRecipes) {
							ItemStack result = craftingRecipe.value().assemble(craftMatrix.asCraftInput());
							matchedCraftingResults.add(result);
							if (ItemStack.isSameItemSameComponents(getItem(), result)) {
								selectedCraftingResultIndex = resultIndex;
								lastRecipe = craftingRecipe;
							}
							resultIndex++;
						}
					} else {
						matchedCraftingRecipes = new ArrayList<>();
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
		if (!level.isClientSide()) {
			ServerPlayer serverplayerentity = (ServerPlayer) player;
			ItemStack itemstack = ItemStack.EMPTY;
			CraftingInput craftInput = inventory.asCraftInput();

			if (!craftInput.isEmpty()) {
				if (lastRecipe != null && lastRecipe.value().matches(craftInput, level)) {
					itemstack = lastRecipe.value().assemble(craftInput);
				} else {
					List<RecipeHolder<CraftingRecipe>> recipes = RecipeHelper.safeGetRecipesFor(RecipeType.CRAFTING, craftInput, level);
					if (!recipes.isEmpty()) {
						matchedCraftingRecipes = recipes;
						matchedCraftingResults.clear();
						selectedCraftingResultIndex = 0;
						RecipeHolder<CraftingRecipe> craftingRecipe = matchedCraftingRecipes.getFirst();
						if (inventoryResult.setRecipeUsed(serverplayerentity, craftingRecipe)) {
							lastRecipe = craftingRecipe;
							itemstack = lastRecipe.value().assemble(craftInput);
							matchedCraftingResults.add(itemstack.copy());
						} else {
							lastRecipe = null;
						}
						for (int i = 1; i < matchedCraftingRecipes.size(); i++) {
						matchedCraftingResults.add(matchedCraftingRecipes.get(i).value().assemble(craftInput));
						}
					}
				}
			}

			craftingResultSlot.set(itemstack);
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
		selectedCraftingResultIndex = resultIndex;
		lastRecipe = matchedCraftingRecipes.get(resultIndex);
		if (player instanceof ServerPlayer serverPlayer) {
			ItemStack result = matchedCraftingResults.get(resultIndex).copy();
			craftingResultSlot.set(result);
			//noinspection DataFlowIssue - lastRecipe can't be null here as there's always a recipe in list for the result
			craftResult.setRecipeUsed(serverPlayer, lastRecipe);
		} else {
			sendDataToServer(() -> NBTHelper.putInt(new CompoundTag(), DATA_SELECT_RESULT, resultIndex));
		}
	}

	@Override
	public void handlePacket(CompoundTag data) {
		data.getBoolean(DATA_SHIFT_CLICK_INTO_STORAGE).ifPresent(this::setShiftClickIntoStorage);
		data.getBoolean(DATA_REFILL_CRAFTING_GRID).ifPresent(this::setRefillCraftingGrid);
		data.getInt(DATA_SELECT_RESULT).ifPresent(this::selectCraftingResult);
	}

	@Override
	public ItemStack getSlotStackToTransfer(Slot slot) {
		if (slot == craftingResultSlot) {
			ItemStack slotStack = slot.getItem();
			slotStack.getItem().onCraftedBy(slotStack, player);
			return slotStack;
		}
		return super.getSlotStackToTransfer(slot);
	}

	@Override
	public List<Slot> getRecipeSlots() {
		return slots.subList(0, 9);
	}

	@Override
	public int getCraftingGridWidth() {
		return craftMatrix.getWidth();
	}

	@Override
	public int getCraftingGridHeight() {
		return craftMatrix.getHeight();
	}

	@Override
	public Container getCraftMatrix() {
		return craftMatrix;
	}

	@Override
	public void setRecipeUsed(ResourceKey<Recipe<?>> recipeId) {
		if (lastRecipe != null && lastRecipe.id().equals(recipeId) || !(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		serverLevel.recipeAccess().byKey(recipeId).filter(r -> r.value().getType() == RecipeType.CRAFTING).map(r -> (RecipeHolder<CraftingRecipe>) r)
				.ifPresent(recipe -> {
					lastRecipe = recipe;
					for (int i = 0; i < matchedCraftingRecipes.size(); i++) {
						if (matchedCraftingRecipes.get(i).id().equals(recipeId)) {
							selectCraftingResult(i);
							return;
						}
					}
				});
	}

	@Override
	public RecipeType<?> getRecipeType() {
		return RecipeType.CRAFTING;
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgradeWrapper.shouldShiftClickIntoStorage();
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgradeWrapper.setShiftClickIntoStorage(shiftClickIntoStorage);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage));
	}

	public boolean shouldRefillCraftingGrid() {
		return upgradeWrapper.shouldRefillCraftingGridNBT();
	}

	public void setRefillCraftingGrid(boolean replenish) {
		upgradeWrapper.setRefillCraftingGridNBT(replenish);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_REFILL_CRAFTING_GRID, replenish));
	}

	@Override
	public boolean mergeIntoStorageFirst(Slot slot) {
		return !(slot instanceof ResultSlot) || shouldShiftClickIntoStorage();
	}

	@Override
	public boolean allowsPickupAll(Slot slot) {
		return slot != craftingResultSlot;
	}

	@Override
	public int getRepeatedQuickMoveLimit(Slot slot, ItemStack transferredStack) {
		return slot == craftingResultSlot && shouldRefillCraftingGrid() ? transferredStack.getMaxStackSize() : 0;
	}
}
