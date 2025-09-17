package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.function.Consumer;

public class CraftingUpgradeWrapper extends UpgradeWrapperBase<CraftingUpgradeWrapper, CraftingUpgradeItem> {
	private final ItemStackHandler inventory;

	public CraftingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		inventory = new ItemStackHandler(9) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				upgrade.addTagElement("craftingInventory", serializeNBT());
				save();
			}
		};
		NBTHelper.getCompound(upgrade, "craftingInventory").ifPresent(inventory::deserializeNBT);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public boolean shouldShiftClickIntoStorage() {
		return NBTHelper.getBoolean(upgrade, "shiftClickIntoStorage").orElse(true);
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		NBTHelper.setBoolean(upgrade, "shiftClickIntoStorage", shiftClickIntoStorage);
		save();
	}

	public boolean shouldRefillCraftingGridNBT() {
		return NBTHelper.getBoolean(upgrade, "refill_crafting_grid").orElse(false);
	}

	public void setRefillCraftingGridNBT(boolean replenish) {
		NBTHelper.setBoolean(upgrade, "refill_crafting_grid", replenish);
		save();
	}

	public boolean extractFromStorageOrPlayer(Player player, ItemStack stack) {
		return extractFromStorage(stack) || extractFromPlayer(player, stack);
	}

	private boolean extractFromPlayer(Player player, ItemStack stack) {
		int playerInvMatchingIndex = player.getInventory().findSlotMatchingItem(stack);
		if (playerInvMatchingIndex >= 0) {
			player.getInventory().removeItem(playerInvMatchingIndex, 1);
			return true;
		}
		return false;
	}

	private boolean extractFromStorage(ItemStack stack) {
		return !InventoryHelper.extractFromInventory(s -> ItemHandlerHelper.canItemStacksStack(s, stack), 1, storageWrapper.getInventoryHandler(), false).isEmpty();
	}

	public boolean insertIntoStorageOrPlayer(Player player, ItemStack stack) {
		if (shouldShiftClickIntoStorage() && InventoryHelper.insertIntoInventory(stack, storageWrapper.getInventoryHandler(), false).isEmpty()) {
			return true;
		}

		return player.getInventory().add(stack);
	}
}
