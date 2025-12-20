package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemAccessItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedBatteryUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class BatteryUpgradeWrapper extends UpgradeWrapperBase<BatteryUpgradeWrapper, BatteryUpgradeItem>
		implements IRenderedBatteryUpgrade, EnergyHandler, ITickableUpgrade, IStackableContentsUpgrade {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	private Consumer<RenderData.BatteryRenderData> updateBatteryRenderDataCallback;
	private final BatteryComponentItemHandler inventory;
	private int energyStored;
	private final EnergyJournal energyJournal;

	protected BatteryUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		energyJournal = new EnergyJournal();
		if (upgrade.has(DataComponents.CONTAINER)) {
			upgrade.set(ModCoreDataComponents.LENIENT_CONTAINER, upgrade.get(DataComponents.CONTAINER));
		}
		inventory = new BatteryComponentItemHandler(upgrade);
		energyStored = getEnergyStored(upgrade);
	}

	public static int getEnergyStored(ItemStack upgrade) {
		return upgrade.getOrDefault(ModCoreDataComponents.ENERGY_STORED, 0);
	}

	@Override
	public int insert(int amount, TransactionContext tx) {
		return innerReceiveEnergy(amount, tx);
	}

	private int innerReceiveEnergy(int maxReceive, TransactionContext tx) {
		int ret = (int) Math.min(getCapacityAsLong() - energyStored, Math.min(getMaxInOut(), maxReceive));
		energyJournal.updateSnapshots(tx);
		energyStored += ret;
		return ret;
	}

	private void serializeEnergyStored() {
		upgrade.set(ModCoreDataComponents.ENERGY_STORED, energyStored);
		save();
		forceUpdateBatteryRenderData();
	}

	@Override
	public int extract(int amount, TransactionContext tx) {
		return innerExtractEnergy(amount, tx);
	}

	private int innerExtractEnergy(int maxExtract, TransactionContext tx) {
		int ret = Math.min(energyStored, Math.min(getMaxInOut(), maxExtract));
		energyJournal.updateSnapshots(tx);
		energyStored -= ret;
		return ret;
	}

	@Override
	public long getAmountAsLong() {
		return energyStored;
	}

	@Override
	public long getCapacityAsLong() {
		return upgradeItem.getMaxEnergyStored(storageWrapper);
	}

	private int getMaxInOut() {
		double stackMultiplier = upgradeItem.getAdjustedStackMultiplier(storageWrapper);
		int baseInOut = upgradeItem.getBatteryUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows();
		return stackMultiplier > (double) Integer.MAX_VALUE / baseInOut ? Integer.MAX_VALUE : (int) (baseInOut * stackMultiplier);
	}

	@Override
	public void setBatteryRenderDataUpdateCallback(Consumer<RenderData.BatteryRenderData> updateBatteryRenderDataCallback) {
		this.updateBatteryRenderDataCallback = updateBatteryRenderDataCallback;
	}

	@Override
	public void forceUpdateBatteryRenderData() {
		RenderData.BatteryRenderData batteryRenderData = new RenderData.BatteryRenderData(Mth.clamp((float) energyStored / getCapacityAsLong(), 0, 1));
		updateBatteryRenderDataCallback.accept(batteryRenderData);
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (energyStored < getCapacityAsLong()) {
			ItemStack energyContainer = inventory.getStackInSlot(INPUT_SLOT);
			if (!energyContainer.isEmpty()) {
				ItemAccess itemAccess = ItemAccess.forStack(energyContainer);
				EnergyHandler energyHandler = itemAccess.getCapability(Capabilities.Energy.ITEM);
				if (energyHandler != null) {
					receiveFromStorage(energyContainer, energyHandler);
				}
			}
		}

		if (energyStored > 0) {
			ItemStack energyContainer = inventory.getStackInSlot(OUTPUT_SLOT);
			if (!energyContainer.isEmpty()) {
				ItemAccess itemAccess = ItemAccess.forStack(energyContainer);
				EnergyHandler energyStorage = itemAccess.getCapability(Capabilities.Energy.ITEM);
				if (energyStorage != null) {
					extractToStorage(energyContainer, energyStorage);
				}
			}
		}
	}

	private void extractToStorage(ItemStack energyContainer, EnergyHandler energyStorage) {
		try (Transaction tx = Transaction.openRoot()) {
			int maxToInsert = Math.min(energyStored, getMaxInOut());
			int inserted = energyStorage.insert(maxToInsert, tx);
			if (inserted > 0) {
				extract(inserted, tx);
				inventory.setStackInSlot(OUTPUT_SLOT, energyContainer);
				tx.commit();
			}
		}
	}

	private void receiveFromStorage(ItemStack energyContainer, EnergyHandler energyStorage) {
		try (Transaction tx = Transaction.openRoot()) {
			int toReceive = (int) Math.min(getCapacityAsLong() - energyStored, getMaxInOut());
			int received = energyStorage.extract(toReceive, tx);
			if (received > 0) {
				innerReceiveEnergy(toReceive, tx);
				inventory.setStackInSlot(INPUT_SLOT, energyContainer);
				tx.commit();
			}
		}
	}

	public ResourceHandler<ItemResource> getInventory() {
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

	private class BatteryComponentItemHandler extends ItemAccessItemHandler {
		private final ItemStack upgrade;

		public BatteryComponentItemHandler(ItemStack upgrade) {
			super(ItemAccess.forStack(upgrade), ModCoreDataComponents.LENIENT_CONTAINER.get(), 2);
			this.upgrade = upgrade;
		}

		@Override
		protected ItemResource update(ItemResource accessResource, int index, ItemResource newResource, int newAmount) {
			ItemResource result = super.update(accessResource, index, newResource, newAmount);
			save();
			return result;
		}

		@Override
		public boolean isValid(int slot, ItemResource resource) {
			return isEnergyHandler(ItemAccess.forHandlerIndex(this, slot));
		}

		private boolean isEnergyHandler(ItemAccess itemAccess) {
			return itemAccess.getCapability(Capabilities.Energy.ITEM) != null;
		}

		public ItemStack getStackInSlot(int slot) {
			ItemContainerContents contents = getContents(itemAccess.getResource());
			return getStackFromContents(contents, slot);
		}

		public void setStackInSlot(int slot, ItemStack stack) {
			ItemContainerContents contents = getContents(itemAccess.getResource());
			NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), this.size), ItemStack.EMPTY);
			contents.copyInto(list);
			list.set(slot, stack);
			upgrade.set(component, ItemContainerContents.fromItems(list));
		}

		@Override
		protected int getCapacity(int index, ItemResource resource) {
			return 1;
		}
	}

	private class EnergyJournal extends SnapshotJournal<Integer> {
		protected Integer createSnapshot() {
			return BatteryUpgradeWrapper.this.energyStored;
		}

		protected void revertToSnapshot(Integer snapshot) {
			BatteryUpgradeWrapper.this.energyStored = snapshot;
		}

		protected void onRootCommit(Integer originalState) {
			int previousAmount = originalState;
			if (BatteryUpgradeWrapper.this.energyStored != previousAmount) {
				BatteryUpgradeWrapper.this.serializeEnergyStored();
			}
		}
	}
}