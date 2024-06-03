package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class AdvancedCraftingUpgradeWrapper extends UpgradeWrapperBase<AdvancedCraftingUpgradeWrapper, AdvancedCraftingUpgradeItem> {
	private final ItemStackHandler inventory;

	public AdvancedCraftingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
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

	public CraftingRefillType shouldReplenish() {
		CraftingRefillType value;
		value = NBTHelper.getEnumConstant(upgrade, "refillCraftingGrid", new Function<String, CraftingRefillType>() {
			@Override
			public CraftingRefillType apply(String s) {
				return CraftingRefillType.fromName(s);
			}
		}).orElse(CraftingRefillType.RefillFromStorageThenPlayer);
		return value;
	}

	public void setReplenish(CraftingRefillType refillCraftingGrid) {
		NBTHelper.setEnumConstant(upgrade, "refillCraftingGrid", refillCraftingGrid);
		save();
	}
}
