package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedBatteryUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class BatteryUpgradeWrapper extends UpgradeWrapperBase<BatteryUpgradeWrapper, BatteryUpgradeItem>
		implements IRenderedBatteryUpgrade, IEnergyStorage, ITickableUpgrade, IStackableContentsUpgrade {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	private Consumer<BatteryRenderInfo> updateTankRenderInfoCallback;
	private final BatteryComponentItemHandler inventory;
	private int energyStored;

	protected BatteryUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inventory = new BatteryComponentItemHandler(upgrade);
		energyStored = getEnergyStored(upgrade);
	}

	public static int getEnergyStored(ItemStack upgrade) {
		return upgrade.getOrDefault(ModCoreDataComponents.ENERGY_STORED, 0);
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
		upgrade.set(ModCoreDataComponents.ENERGY_STORED, energyStored);
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
		IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);

		if (energyStorage == null) {
			return false;
		}

		if (isOutput) {
			return energyStorage.canReceive();
		} else {
			return energyStorage.canExtract() && energyStorage.getEnergyStored() > 0;
		}
	}

	@Override
	public void setBatteryRenderInfoUpdateCallback(Consumer<BatteryRenderInfo> updateTankRenderInfoCallback) {
		this.updateTankRenderInfoCallback = updateTankRenderInfoCallback;
	}

	@Override
	public void forceUpdateBatteryRenderInfo() {
		BatteryRenderInfo batteryRenderInfo = new BatteryRenderInfo(1f);
		batteryRenderInfo.setChargeRatio((float) energyStored / getMaxEnergyStored());
		updateTankRenderInfoCallback.accept(batteryRenderInfo);
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		if (energyStored < getMaxEnergyStored()) {
			ItemStack energyContainer = inventory.getStackInSlot(INPUT_SLOT);
			IEnergyStorage energyStorage = energyContainer.getCapability(Capabilities.EnergyStorage.ITEM);
			if (energyStorage != null) {
				receiveFromStorage(energyContainer, energyStorage);

			}
		}

		if (energyStored > 0) {
			ItemStack energyContainer = inventory.getStackInSlot(OUTPUT_SLOT);
			IEnergyStorage energyStorage = energyContainer.getCapability(Capabilities.EnergyStorage.ITEM);
			if (energyStorage != null) {
				extractToStorage(energyContainer, energyStorage);
			}
		}
	}

	private void extractToStorage(ItemStack energyContainer, IEnergyStorage energyStorage) {
		int toExtract = innerExtractEnergy(getMaxInOut(), true);
		if (toExtract > 0) {
			toExtract = energyStorage.receiveEnergy(toExtract, true);
			if (toExtract > 0) {
				energyStorage.receiveEnergy(toExtract, false);
				innerExtractEnergy(toExtract, false);
				inventory.setStackInSlotWithoutValidation(OUTPUT_SLOT, energyContainer);
			}
		}
	}

	private void receiveFromStorage(ItemStack energyContainer, IEnergyStorage energyStorage) {
		int toReceive = innerReceiveEnergy(getMaxInOut(), true);
		if (toReceive > 0) {
			toReceive = energyStorage.extractEnergy(toReceive, true);
			if (toReceive > 0) {
				energyStorage.extractEnergy(toReceive, false);
				innerReceiveEnergy(toReceive, false);
				inventory.setStackInSlotWithoutValidation(INPUT_SLOT, energyContainer);
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

	private class BatteryComponentItemHandler extends ComponentItemHandler {
		public BatteryComponentItemHandler(ItemStack upgrade) {
			super(upgrade, DataComponents.CONTAINER, 2);
		}

		@Override
		protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
			super.onContentsChanged(slot, oldStack, newStack);
			save();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (slot == INPUT_SLOT) {
				return stack.isEmpty() || isValidInputItem(stack);
			} else if (slot == OUTPUT_SLOT) {
				return stack.isEmpty() || isValidOutputItem(stack);
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

		public void setStackInSlotWithoutValidation(int slot, ItemStack stack) {
			super.updateContents(getContents(), stack, slot);
		}
	}
}
