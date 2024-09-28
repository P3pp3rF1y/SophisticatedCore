package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedBatteryUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class BatteryUpgradeWrapper extends UpgradeWrapperBase<BatteryUpgradeWrapper, BatteryUpgradeItem>
		implements IRenderedBatteryUpgrade, IEnergyStorage, ITickableUpgrade, IStackableContentsUpgrade {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	public static final String ENERGY_STORED_TAG = "energyStored";
	private Consumer<BatteryRenderInfo> updateTankRenderInfoCallback;
	private final ItemStackHandler inventory;
	private int energyStored;

	protected BatteryUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inventory = new ItemStackHandler(2) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				upgrade.addTagElement("inventory", serializeNBT());
				save();
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				if (slot == INPUT_SLOT) {
					return isValidInputItem(stack);
				} else if (slot == OUTPUT_SLOT) {
					return isValidOutputItem(stack);
				}
				return false;
			}

			private boolean isValidInputItem(ItemStack stack) {
				return isValidEnergyItem(stack, false);
			}

			private boolean isValidOutputItem(ItemStack stack) {
				return isValidEnergyItem(stack, true);
			}

			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}
		};
		NBTHelper.getCompound(upgrade, "inventory").ifPresent(inventory::deserializeNBT);
		energyStored = getEnergyStored(upgrade);
	}

	public static int getEnergyStored(ItemStack upgrade) {
		return NBTHelper.getInt(upgrade, ENERGY_STORED_TAG).orElse(0);
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		return innerReceiveEnergy(maxReceive, simulate);
	}

	private int innerReceiveEnergy(int maxReceive, boolean simulate) {
		int ret = Math.min(getMaxEnergyStored() - energyStored, Math.min(getMaxInOut(), maxReceive));
		if (!simulate) {
			energyStored += ret;
			serializeEnergyStored();
		}
		return ret;
	}

	private void serializeEnergyStored() {
		NBTHelper.setInteger(upgrade, ENERGY_STORED_TAG, energyStored);
		save();
		forceUpdateBatteryRenderInfo();
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		return innerExtractEnergy(maxExtract, simulate);
	}

	private int innerExtractEnergy(int maxExtract, boolean simulate) {
		int ret = Math.min(energyStored, Math.min(getMaxInOut(), maxExtract));

		if (!simulate) {
			energyStored -= ret;
			serializeEnergyStored();
		}
		return ret;
	}

	@Override
	public int getEnergyStored() {
		return energyStored;
	}

	@Override
	public int getMaxEnergyStored() {
		return upgradeItem.getMaxEnergyStored(storageWrapper);
	}

	@Override
	public boolean canExtract() {
		return true;
	}

	@Override
	public boolean canReceive() {
		return true;
	}

	private int getMaxInOut() {
		double stackMultiplier = upgradeItem.getAdjustedStackMultiplier(storageWrapper);
		int baseInOut = upgradeItem.getBatteryUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows();
		return stackMultiplier > Integer.MAX_VALUE / baseInOut ? Integer.MAX_VALUE : (int) (baseInOut * stackMultiplier);
	}

	private boolean isValidEnergyItem(ItemStack stack, boolean isOutput) {
		return stack.getCapability(ForgeCapabilities.ENERGY).map(energyStorage -> isOutput || energyStorage.getEnergyStored() > 0).orElse(false);
	}

	@Override
	public void setBatteryRenderInfoUpdateCallback(Consumer<BatteryRenderInfo> updateTankRenderInfoCallback) {
		this.updateTankRenderInfoCallback = updateTankRenderInfoCallback;
	}

	@Override
	public void forceUpdateBatteryRenderInfo() {
		BatteryRenderInfo batteryRenderInfo = new BatteryRenderInfo(1f);
		batteryRenderInfo.setChargeRatio((float) Math.round((float) energyStored / getMaxEnergyStored() * 4) / 4);
		updateTankRenderInfoCallback.accept(batteryRenderInfo);
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level world, BlockPos pos) {
		if (energyStored < getMaxEnergyStored()) {
			inventory.getStackInSlot(INPUT_SLOT).getCapability(ForgeCapabilities.ENERGY).ifPresent(this::receiveFromStorage);
		}

		if (energyStored > 0) {
			inventory.getStackInSlot(OUTPUT_SLOT).getCapability(ForgeCapabilities.ENERGY).ifPresent(this::extractToStorage);
		}
	}

	private void extractToStorage(IEnergyStorage energyStorage) {
		int toExtract = innerExtractEnergy(getMaxInOut(), true);
		if (toExtract > 0) {
			toExtract = energyStorage.receiveEnergy(toExtract, true);
			if (toExtract > 0) {
				energyStorage.receiveEnergy(toExtract, false);
				innerExtractEnergy(toExtract, false);
			}
		}
	}

	private void receiveFromStorage(IEnergyStorage energyStorage) {
		int toReceive = innerReceiveEnergy(getMaxInOut(), true);
		if (toReceive > 0) {
			toReceive = energyStorage.extractEnergy(toReceive, true);
			if (toReceive > 0) {
				energyStorage.extractEnergy(toReceive, false);
				innerReceiveEnergy(toReceive, false);
			}
		}
	}

	public IItemHandler getInventory() {
		return inventory;
	}

	@Override
	public int getMinimumMultiplierRequired() {
		return (int) Math.ceil((float) energyStored / upgradeItem.getMaxEnergyBase(storageWrapper));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}
}
