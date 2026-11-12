package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedTankUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

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
	private Consumer<TankRenderInfo> updateTankRenderInfoCallback;
	private final TankComponentItemHandler inventory;
	private final TankContainerHandler containers;
	private FluidStack contents;
	private long cooldownTime = 0;

	protected TankUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		contents = getContents(upgrade).copy();
		if (upgrade.has(DataComponents.CONTAINER)) {
			upgrade.set(ModCoreDataComponents.LENIENT_CONTAINER, upgrade.get(DataComponents.CONTAINER));
		}
		upgrade.remove(DataComponents.CONTAINER);
		inventory = new TankComponentItemHandler(upgrade);
		containers = new TankContainerHandler(inventory) {
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
		inventory.migrateLegacyContents();
	}

	public static SimpleFluidContent getContents(ItemStack upgrade) {
		return upgrade.getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY);
	}
	@Override
	public void setTankRenderInfoUpdateCallback(Consumer<TankRenderInfo> callback) {
		updateTankRenderInfoCallback = callback;
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
	public TankComponentItemHandler getInventory() {
		return inventory;
	}
	private int getMaxInOut() {
		return (int) Math.max(FluidType.BUCKET_VOLUME, upgradeItem.getTankUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows()
				* upgradeItem.getAdjustedStackMultiplier(storageWrapper));
	}
	public int fill(FluidStack resource, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
		int capacity = getTankCapacity();
		if (contents.getAmount() >= capacity || (!contents.isEmpty() && !FluidStack.isSameFluidSameComponents(resource, contents))) {
			return 0;
		}
		int toFill = Math.min(capacity - contents.getAmount(), resource.getAmount());
		if (!ignoreInOutLimit) {
			toFill = Math.min(getMaxInOut(), toFill);
		}
		if (action == IFluidHandler.FluidAction.EXECUTE) {
			if (contents.isEmpty()) {
				contents = resource.copyWithAmount(toFill);
			} else {
				contents.setAmount(contents.getAmount() + toFill);
			}
			serializeContents();
		}
		return toFill;
	}
	private void serializeContents() {
		upgrade.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(contents));
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
		FluidStack ret = contents.copyWithAmount(toDrain);
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
		if (level.getGameTime() >= cooldownTime && containers.processContainers()) {
			cooldownTime = level.getGameTime() + upgradeItem.getTankUpgradeConfig().autoFillDrainContainerCooldown.get();
		}
	}
	public void interactWithCursorStack(ItemStack stack, Consumer<ItemStack> updateContainerStack) {
		containers.interactWithCursorStack(stack, updateContainerStack);
	}
	@Override
	public int getMinimumMultiplierRequired() {
		return (int) Math.ceil((float) contents.getAmount() / upgradeItem.getBaseCapacity(storageWrapper));
	}
	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public class TankComponentItemHandler extends ComponentItemHandler {
		public TankComponentItemHandler(ItemStack upgrade) {
			super(upgrade, ModCoreDataComponents.LENIENT_CONTAINER.get(), 4);
		}
		private void migrateLegacyContents() {
			ItemContainerContents current = getContents();
			for (int slot = INPUT_SLOT; slot <= OUTPUT_SLOT; slot++) {
				ItemStack stack = getStackFromContents(current, slot);
				if (!stack.isEmpty() && !containers.isItemValid(slot, stack)) {
					ItemStack remainder = insertItem(slot + 2, stack, false);
					if (remainder.getCount() != stack.getCount()) {
						updateContents(getContents(), remainder, slot);
					}
				}
			}
		}
		@Override
		protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
			super.onContentsChanged(slot, oldStack, newStack);
			save();
		}
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return containers.isItemValid(slot, stack);
		}
	}
}
