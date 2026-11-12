package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedTankUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;

import java.util.function.Consumer;

public class TankUpgradeWrapper extends UpgradeWrapperBase<TankUpgradeWrapper, TankUpgradeItem>
		implements
			IRenderedTankUpgrade,
			ITickableUpgrade,
			IStackableContentsUpgrade {
	public static final int INPUT_SLOT = TankContainerHandler.INPUT_SLOT;
	public static final int OUTPUT_SLOT = TankContainerHandler.OUTPUT_SLOT;
	public static final int INPUT_RESULT_SLOT = TankContainerHandler.INPUT_RESULT_SLOT;
	public static final int OUTPUT_RESULT_SLOT = TankContainerHandler.OUTPUT_RESULT_SLOT;
	private static final String CONTENTS_TAG = "contents";
	private Consumer<TankRenderInfo> updateTankRenderInfoCallback;
	private final TankInventoryHandler inventory;
	private final TankContainerHandler containerHandler;
	private FluidStack contents;
	private long cooldownTime = 0;

	protected TankUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		contents = getContents(upgrade);
		inventory = new TankInventoryHandler(upgrade);
		containerHandler = new TankContainerHandler(inventory) {
			@Override
			public FluidStack getContents() {
				return TankUpgradeWrapper.this.getContents();
			}

			@Override
			public int getTankCapacity() {
				return TankUpgradeWrapper.this.getTankCapacity();
			}

			@Override
			public int fill(FluidStack resource, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
				return TankUpgradeWrapper.this.fill(resource, action, ignoreInOutLimit);
			}

			@Override
			public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
				return TankUpgradeWrapper.this.drain(maxDrain, action, ignoreInOutLimit);
			}
		};
		NBTHelper.getCompound(upgrade, "inventory").ifPresent(inventory::deserializeNBT);
		inventory.migrateLegacyContents();
	}

	public static FluidStack getContents(ItemStack upgrade) {
		return NBTHelper.getCompound(upgrade, CONTENTS_TAG).map(FluidStack::loadFluidStackFromNBT).orElse(FluidStack.EMPTY);
	}

	@Override
	public void setTankRenderInfoUpdateCallback(Consumer<TankRenderInfo> updateTankRenderInfoCallback) {
		this.updateTankRenderInfoCallback = updateTankRenderInfoCallback;
	}

	@Override
	public void forceUpdateTankRenderInfo() {
		TankRenderInfo renderInfo = new TankRenderInfo();
		if (!contents.isEmpty()) {
			renderInfo.setFluid(contents);
			renderInfo.setFillRatio((float) Math.round((float) contents.getAmount() / getTankCapacity() * 10) / 10);
		}
		updateTankRenderInfoCallback.accept(renderInfo);
	}

	public FluidStack getContents() {
		return contents;
	}

	public int getTankCapacity() {
		return upgradeItem.getTankCapacity(storageWrapper);
	}

	public IItemHandler getInventory() {
		return inventory;
	}

	private int getMaxInOut() {
		double scaled = (double) upgradeItem.getTankUpgradeConfig().maxInputOutput.get() * storageWrapper.getFluidTransferScale()
				* upgradeItem.getAdjustedStackMultiplier(storageWrapper);
		return (int) Math.min(Integer.MAX_VALUE, Math.max(FluidType.BUCKET_VOLUME, scaled));
	}

	public int fill(FluidStack resource, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
		int capacity = getTankCapacity();
		if (contents.getAmount() >= capacity || (!contents.isEmpty() && !resource.isFluidEqual(contents))) {
			return 0;
		}
		int toFill = Math.min(capacity - contents.getAmount(), resource.getAmount());
		if (!ignoreInOutLimit) {
			toFill = Math.min(getMaxInOut(), toFill);
		}
		if (action == IFluidHandler.FluidAction.EXECUTE) {
			if (contents.isEmpty()) {
				contents = new FluidStack(resource, toFill);
			} else {
				contents.setAmount(contents.getAmount() + toFill);
			}
			serializeContents();
		}
		return toFill;
	}

	private void serializeContents() {
		upgrade.addTagElement(CONTENTS_TAG, contents.writeToNBT(new CompoundTag()));
		save();
		forceUpdateTankRenderInfo();
	}

	public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
		if (contents.isEmpty()) {
			return FluidStack.EMPTY;
		}
		int toDrain = Math.min(maxDrain, contents.getAmount());
		if (!ignoreInOutLimit) {
			toDrain = Math.min(getMaxInOut(), toDrain);
		}
		FluidStack ret = new FluidStack(contents, toDrain);
		if (action == IFluidHandler.FluidAction.EXECUTE) {
			if (toDrain == contents.getAmount()) {
				contents = FluidStack.EMPTY;
			} else {
				contents.setAmount(contents.getAmount() - toDrain);
			}
			serializeContents();
		}
		return ret;
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (level.getGameTime() >= cooldownTime && containerHandler.processContainers()) {
			cooldownTime = level.getGameTime() + upgradeItem.getTankUpgradeConfig().autoFillDrainContainerCooldown.get();
		}
	}

	public void interactWithCursorStack(ItemStack cursorStack, Consumer<ItemStack> updateContainerStack) {
		containerHandler.interactWithCursorStack(cursorStack, updateContainerStack);
	}

	@Override
	public int getMinimumMultiplierRequired() {
		return (int) Math.ceil((float) contents.getAmount() / upgradeItem.getBaseCapacity(storageWrapper));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	private class TankInventoryHandler extends ItemStackHandler {
		private final ItemStack upgrade;

		private TankInventoryHandler(ItemStack upgrade) {
			super(4);
			this.upgrade = upgrade;
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			if (stack.isEmpty() && getStackInSlot(slot).isEmpty()) {
				return;
			}
			super.setStackInSlot(slot, stack);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			upgrade.addTagElement("inventory", serializeNBT());
			save();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return containerHandler.isItemValid(slot, stack);
		}

		private void migrateLegacyContents() {
			for (int slot = INPUT_SLOT; slot <= OUTPUT_SLOT; slot++) {
				ItemStack stack = getStackInSlot(slot);
				if (!stack.isEmpty() && !containerHandler.isItemValid(slot, stack)) {
					ItemStack remainder = insertItem(slot + 2, stack, false);
					if (remainder.getCount() != stack.getCount()) {
						setStackInSlot(slot, remainder);
					}
				}
			}
		}

		@Override
		public void setSize(int size) {
			super.setSize(4);
		}
	}
}
