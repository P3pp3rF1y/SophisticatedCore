package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.common.gui.*;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class AdvancedCraftingUpgradeContainer extends UpgradeContainerBase<AdvancedCraftingUpgradeWrapper, AdvancedCraftingUpgradeContainer> implements ICraftingContainer {
	private static final String DATA_SHIFT_CLICK_INTO_STORAGE = "shiftClickIntoStorage";
	private static final String DATA_REPLENISH = "refillCraftingGrid";
	private final ResultContainer craftResult = new ResultContainer();
	private final CraftingItemHandler craftMatrix;
	private final ResultSlot craftingResultSlot;
	@Nullable
	private CraftingRecipe lastRecipe = null;

	public AdvancedCraftingUpgradeContainer(Player player, int upgradeContainerId, AdvancedCraftingUpgradeWrapper upgradeWrapper, UpgradeContainerType<AdvancedCraftingUpgradeWrapper, AdvancedCraftingUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);

		int slot;
		for (slot = 0; slot < upgradeWrapper.getInventory().getSlots(); slot++) {
			slots.add(new SlotSuppliedHandler(upgradeWrapper::getInventory, slot, -100, -100) {
				@Override
				public void setChanged() {
					super.setChanged();
					updateCraftingResult(player.level, player, craftMatrix, craftResult, craftingResultSlot);
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
				NonNullList<ItemStack> nonnulllist;
				if (lastRecipe != null && lastRecipe.matches(craftMatrix, player.level)) {
					nonnulllist = lastRecipe.getRemainingItems(craftMatrix);
				} else {
					nonnulllist = craftMatrix.items;
				}
				net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
				for (int i = 0; i < nonnulllist.size(); ++i) {
					ItemStack itemstack = craftMatrix.getItem(i);
					ItemStack itemstack1 = nonnulllist.get(i);
					if (!itemstack.isEmpty()) {
						refillCraftingGrid(itemstack, i);
						itemstack = craftMatrix.getItem(i);
					}

					if (!itemstack1.isEmpty()) {
						if (itemstack.isEmpty()) {
							craftMatrix.setItem(i, itemstack1);
						} else if (ItemStack.isSame(itemstack, itemstack1) && ItemStack.tagMatches(itemstack, itemstack1)) {
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
		};
		slots.add(craftingResultSlot);
	}

	public void refillCraftingGrid(ItemStack itemstack, int i) {
		CraftingRefillType refillType = shouldRefillCraftingGrid();
		switch (refillType) {
			case RefillFromStorage:
				handleRefillFromStorageOnly(itemstack, i);
				break;
			case RefillFromPlayer:
				handleRefillFromPlayerOnly(itemstack, i);
				break;
			case RefillFromPlayerThenStorage:
				handleRefill(itemstack, i, true);
				break;
			case RefillFromStorageThenPlayer:
				handleRefill(itemstack, i, false);
				break;
			default:
				craftMatrix.removeItem(i, 1);
				break;
		}
	}

	private void handleRefillFromStorageOnly(ItemStack itemstack, int i) {
		if (!extractFromStorage(itemstack)) {
			craftMatrix.removeItem(i, 1);
		} else {
			onCraftMatrixChanged(craftMatrix);
		}
	}

	private void handleRefillFromPlayerOnly(ItemStack itemstack, int i) {
		if (!extractFromPlayer(itemstack)) {
			craftMatrix.removeItem(i, 1);
		} else {
			onCraftMatrixChanged(craftMatrix);
		}
	}

	private void handleRefill(ItemStack itemstack, int i, boolean firstFromPlayer) {
		if (extractFromInventory(itemstack, firstFromPlayer) || extractFromInventory(itemstack, !firstFromPlayer)) {
			onCraftMatrixChanged(craftMatrix);
		} else {
			craftMatrix.removeItem(i, 1);
		}
	}

	private boolean extractFromInventory(ItemStack itemstack, boolean fromPlayer) {
		return fromPlayer ? extractFromPlayer(itemstack) : extractFromStorage(itemstack);
	}

	private boolean extractFromPlayer(ItemStack itemstack) {
		int playerInvMatchingIndex = player.getInventory().findSlotMatchingItem(itemstack);
		if (playerInvMatchingIndex >= 0) {
			player.getInventory().removeItem(playerInvMatchingIndex, 1);
			return true;
		}
		return false;
	}

	private boolean extractFromStorage(ItemStack itemstack) {
		return !InventoryHelper.extractFromInventory(itemstack.getItem(), 1, upgradeWrapper.getStorageWrapper().getInventoryHandler(), false).isEmpty();
	}


	@Override
	public void onInit() {
		super.onInit();
		onCraftMatrixChanged(craftMatrix);
	}

	private void onCraftMatrixChanged(Container iInventory) {
		updateCraftingResult(player.level, player, craftMatrix, craftResult, craftingResultSlot);
	}

	private void updateCraftingResult(Level world, Player player, CraftingContainer inventory, ResultContainer inventoryResult, ResultSlot craftingResultSlot) {
		if (!world.isClientSide) {
			ServerPlayer serverplayerentity = (ServerPlayer) player;
			ItemStack itemstack = ItemStack.EMPTY;
			if (lastRecipe != null && lastRecipe.matches(inventory, world)) {
				itemstack = lastRecipe.assemble(inventory);
			} else {
				//noinspection ConstantConditions - we're on server and for sure in the world so getServer can't return null here
				Optional<CraftingRecipe> optional = RecipeHelper.safeGetRecipeFor(RecipeType.CRAFTING, inventory, world);
				if (optional.isPresent()) {
					CraftingRecipe craftingRecipe = optional.get();
					if (inventoryResult.setRecipeUsed(world, serverplayerentity, craftingRecipe)) {
						lastRecipe = craftingRecipe;
						itemstack = lastRecipe.assemble(inventory);
					} else {
						lastRecipe = null;
					}
				}
			}

			craftingResultSlot.set(itemstack);
			if (serverplayerentity.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu) {
				storageContainerMenu.setSlotStackToUpdate(craftingResultSlot.index, itemstack);
			}
		}
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains(DATA_SHIFT_CLICK_INTO_STORAGE)) {
			setShiftClickIntoStorage(data.getBoolean(DATA_SHIFT_CLICK_INTO_STORAGE));
		}
		if (data.contains(DATA_REPLENISH)) {
			setRefillCraftingGrid(CraftingRefillType.fromName(data.getString(DATA_REPLENISH)));
		}
	}

	@Override
	public ItemStack getSlotStackToTransfer(Slot slot) {
		if (slot == craftingResultSlot) {
			ItemStack slotStack = slot.getItem();
			slotStack.getItem().onCraftedBy(slotStack, player.level, player);
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

	public boolean shouldShiftClickIntoStorage() {
		return upgradeWrapper.shouldShiftClickIntoStorage();
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgradeWrapper.setShiftClickIntoStorage(shiftClickIntoStorage);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage));
	}

	public CraftingRefillType shouldRefillCraftingGrid() {
		return upgradeWrapper.shouldReplenish();
	}

	public void setRefillCraftingGrid(CraftingRefillType refillCraftingGrid) {
		upgradeWrapper.setReplenish(refillCraftingGrid);
		sendDataToServer(() -> NBTHelper.putEnumConstant(new CompoundTag(), DATA_REPLENISH, refillCraftingGrid));
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
