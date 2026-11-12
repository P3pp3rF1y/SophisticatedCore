package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemAccessItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedTankUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.jspecify.annotations.Nullable;

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
	private Consumer<RenderData.TankRenderData> updateTankRenderDataCallback;
	private final TankComponentItemHandler inventory;
	private final TankContainerHandler containers;
	private FluidStack contents;
	private long cooldownTime = 0;
	private final Journal journal = new Journal();

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
				return TankUpgradeWrapper.this.getCapacity();
			}

			@Override
			public int insert(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
				return TankUpgradeWrapper.this.insert(resource, amount, tx, ignoreInOutLimit);
			}

			@Override
			public int extract(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
				return TankUpgradeWrapper.this.extract(resource, amount, tx, ignoreInOutLimit);
			}

			@Override
			protected ItemStack getStackInSlot(int slot) {
				return inventory.getStackInSlot(slot);
			}

			@Override
			protected void setStackInSlot(int slot, ItemStack stack) {
				inventory.setStackInSlot(slot, stack);
			}

			@Override
			protected void setStackInSlot(int slot, ItemStack stack, TransactionContext tx) {
				inventory.setStackInSlot(slot, stack, tx);
			}
		};
		inventory.migrateLegacyContents();
	}

	public static SimpleFluidContent getContents(ItemStack upgrade) {
		return upgrade.getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY);
	}

	@Override
	public void setTankRenderDataUpdateCallback(Consumer<RenderData.TankRenderData> updateTankRenderDataCallback) {
		this.updateTankRenderDataCallback = updateTankRenderDataCallback;
	}

	@Override
	public void forceUpdateTankRenderData() {
		updateTankRenderDataCallback.accept(new RenderData.TankRenderData(contents.copy(),
				contents.isEmpty() ? 0 : (float) Math.round((float) contents.getAmount() / getCapacity() * 10) / 10));
	}

	public FluidStack getContents() {
		return contents;
	}

	public FluidResource getResource() {
		return FluidResource.of(contents);
	}

	public int getAmount() {
		return contents.getAmount();
	}

	public int getCapacity() {
		return upgradeItem.getTankCapacity(storageWrapper);
	}

	public TankComponentItemHandler getInventory() {
		return inventory;
	}

	public TankContainerHandler getContainerHandler() {
		return containers;
	}

	private int getMaxInOut() {
		return (int) Math.max(FluidType.BUCKET_VOLUME, upgradeItem.getTankUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows()
				* upgradeItem.getAdjustedStackMultiplier(storageWrapper));
	}

	public int insert(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		int capacity = getCapacity();

		if (contents.getAmount() >= capacity || (!contents.isEmpty() && !resource.matches(contents))) {
			return 0;
		}

		int toFill = Math.min(capacity - contents.getAmount(), amount);
		if (!ignoreInOutLimit) {
			toFill = Math.min(getMaxInOut(), toFill);
		}

		journal.updateSnapshots(tx);
		if (contents.isEmpty()) {
			contents = resource.toStack(toFill);
		} else {
			contents.setAmount(contents.getAmount() + toFill);
		}

		return toFill;
	}

	private void serializeContents() {
		upgrade.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(contents));
		save();
		forceUpdateTankRenderData();
	}

	public int extract(FluidResource resource, int maxDrain, TransactionContext tx, boolean ignoreInOutLimit) {
		if (contents.isEmpty() || !resource.matches(contents)) {
			return 0;
		}

		int toDrain = Math.min(maxDrain, contents.getAmount());
		if (!ignoreInOutLimit) {
			toDrain = Math.min(getMaxInOut(), toDrain);
		}

		journal.updateSnapshots(tx);
		if (toDrain == contents.getAmount()) {
			contents = FluidStack.EMPTY;
		} else {
			contents.setAmount(contents.getAmount() - toDrain);
		}

		return toDrain;
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (level.getGameTime() >= cooldownTime && containers.processContainers()) {
			cooldownTime = level.getGameTime() + upgradeItem.getTankUpgradeConfig().autoFillDrainContainerCooldown.get();
		}
	}

	public void interactWithCursorStack(ItemStack cursorStack, Consumer<ItemStack> updateContainerStack) {
		containers.interactWithCursorStack(cursorStack, updateContainerStack);
	}

	public int fillHandler(ResourceHandler<FluidResource> fluidHandler, TransactionContext tx) {
		return containers.fillHandler(fluidHandler, tx);
	}

	public int drainHandler(ResourceHandler<FluidResource> fluidHandler, TransactionContext tx) {
		return containers.drainHandler(fluidHandler, tx);
	}

	@Override
	public int getMinimumMultiplierRequired() {
		return (int) Math.ceil((float) contents.getAmount() / upgradeItem.getBaseCapacity(storageWrapper));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	private record TankSnapshot(FluidStack fluid, ItemContainerContents items) {
	}

	public class Journal extends SnapshotJournal<TankSnapshot> {
		@Override
		protected TankSnapshot createSnapshot() {
			return new TankSnapshot(contents.copy(), inventory.getComponentContents());
		}

		@Override
		protected void revertToSnapshot(TankSnapshot snapshot) {
			contents = snapshot.fluid();
			upgrade.set(ModCoreDataComponents.LENIENT_CONTAINER, snapshot.items());
		}

		@Override
		protected void onRootCommit(TankSnapshot originalState) {
			boolean fluidChanged = !FluidStack.matches(originalState.fluid(), contents);
			if (fluidChanged) {
				serializeContents();
			} else if (!originalState.items().equals(inventory.getComponentContents())) {
				save();
			}
		}
	}

	public class TankComponentItemHandler extends ItemAccessItemHandler {
		public TankComponentItemHandler(ItemStack upgrade) {
			super(ItemAccess.forStack(upgrade), ModCoreDataComponents.LENIENT_CONTAINER.get(), 4);
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
			journal.updateSnapshots(tx);
			return super.insert(index, resource, amount, tx);
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
			journal.updateSnapshots(tx);
			return super.extract(index, resource, amount, tx);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			return containers.isItemValid(index, resource.toStack());
		}

		public ItemStack getStackInSlot(int slot) {
			return getStackFromContents(getComponentContents(), slot);
		}

		private ItemContainerContents getComponentContents() {
			return getContents(itemAccess.getResource());
		}

		private void migrateLegacyContents() {
			for (int slot = INPUT_SLOT; slot <= OUTPUT_SLOT; slot++) {
				ItemStack stack = getStackInSlot(slot);
				if (!stack.isEmpty() && !containers.isItemValid(slot, stack)) {
					try (Transaction tx = Transaction.openRoot()) {
						int inserted = insert(slot + 2, ItemResource.of(stack), stack.getCount(), tx);
						if (inserted > 0 && extract(slot, ItemResource.of(stack), inserted, tx) == inserted) {
							tx.commit();
						}
					}
				}
			}
		}

		public void setStackInSlot(int slot, ItemStack stack) {
			setStackInSlotInternal(slot, stack);
			save();
		}

		public void setStackInSlot(int slot, ItemStack stack, TransactionContext tx) {
			journal.updateSnapshots(tx);
			setStackInSlotInternal(slot, stack);
		}

		private void setStackInSlotInternal(int slot, ItemStack stack) {
			ItemContainerContents contents = getComponentContents();
			NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), this.size), ItemStack.EMPTY);
			contents.copyInto(list);
			list.set(slot, stack);
			upgrade.set(component, ItemContainerContents.fromItems(list));
		}
	}

}
