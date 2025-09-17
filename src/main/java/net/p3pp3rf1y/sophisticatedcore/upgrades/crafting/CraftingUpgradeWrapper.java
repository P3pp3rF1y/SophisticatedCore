package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.StatefulComponentItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.function.Consumer;

public class CraftingUpgradeWrapper extends UpgradeWrapperBase<CraftingUpgradeWrapper, CraftingUpgradeItem> {
	private final StatefulComponentItemHandler inventory;

	public CraftingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		inventory = new StatefulComponentItemHandler(upgrade, DataComponents.CONTAINER, 9) {
			@Override
			protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
				super.onContentsChanged(slot, oldStack, newStack);
				save();
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return true;
			}
		};
	}

	public StatefulComponentItemHandler getInventory() {
		return inventory;
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgrade.getOrDefault(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, true);
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgrade.set(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage);
		save();
	}

	public boolean shouldRefillCraftingGridNBT() {
		return upgrade.getOrDefault(ModCoreDataComponents.REFILL_CRAFTING_GRID, false);
	}

	public void setRefillCraftingGridNBT(boolean replenish) {
		upgrade.set(ModCoreDataComponents.REFILL_CRAFTING_GRID, replenish);
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
		return !InventoryHelper.extractFromInventory(s -> ItemStack.isSameItemSameComponents(s, stack), 1, storageWrapper.getInventoryHandler(), false).isEmpty();
	}

	public boolean insertIntoStorageOrPlayer(Player player, ItemStack stack) {
		if (shouldShiftClickIntoStorage() && InventoryHelper.insertIntoInventory(stack, storageWrapper.getInventoryHandler(), false).isEmpty()) {
			return true;
		}

		return player.getInventory().add(stack);
	}
}
