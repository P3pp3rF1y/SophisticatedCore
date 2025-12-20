package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
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
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedTankUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.MutableStackItemAccess;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class TankUpgradeWrapper extends UpgradeWrapperBase<TankUpgradeWrapper, TankUpgradeItem>
		implements IRenderedTankUpgrade, ITickableUpgrade, IStackableContentsUpgrade {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	public static final int INPUT_RESULT_SLOT = 2;
	public static final int OUTPUT_RESULT_SLOT = 3;
	private Consumer<RenderData.TankRenderData> updateTankRenderDataCallback;
	private final TankComponentItemHandler inventory;
	private FluidStack contents;
	private long cooldownTime = 0;
	private final Journal journal = new Journal();

	private static final Map<ItemStack, Function<ItemAccess, ResourceHandler<FluidResource>>> CUSTOM_FLUIDHANDLER_FACTORIES = Map.of(
			new ItemStack(Items.EXPERIENCE_BOTTLE), itemAccess -> new SwapEmptyFluidContainerHandler.Full(itemAccess, Items.GLASS_BOTTLE, new ItemStack(Items.EXPERIENCE_BOTTLE), XpHelper.experienceToLiquid(8), ModFluids.XP_STILL.get()),
			PotionContents.createItemStack(Items.POTION, Potions.WATER), itemAccess -> new SwapEmptyFluidContainerHandler.Full(itemAccess, Items.GLASS_BOTTLE, PotionContents.createItemStack(Items.POTION, Potions.WATER), 250, Fluids.WATER),
			new ItemStack(Items.GLASS_BOTTLE), itemAccess -> new SwapEmptyFluidContainerHandler.Empty(itemAccess, Items.GLASS_BOTTLE,
					new SwapEmptyFluidContainerHandler.FullContainerDefinition(new ItemStack(Items.EXPERIENCE_BOTTLE), XpHelper.experienceToLiquid(8), ModFluids.XP_STILL.get()),
					new SwapEmptyFluidContainerHandler.FullContainerDefinition(PotionContents.createItemStack(Items.POTION, Potions.WATER), 250, Fluids.WATER))
	);

	protected TankUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		contents = getContents(upgrade).copy();
		if (upgrade.has(DataComponents.CONTAINER)) {
			upgrade.set(ModCoreDataComponents.LENIENT_CONTAINER, upgrade.get(DataComponents.CONTAINER));
		}
		inventory = new TankComponentItemHandler(upgrade);
	}

	public static SimpleFluidContent getContents(ItemStack upgrade) {
		return upgrade.getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY);
	}

	private boolean isValidFluidHandler(ResourceHandler<FluidResource> fluidHandler, boolean isOutput) {
		boolean tankEmpty = contents.isEmpty();
		for (int tank = 0; tank < fluidHandler.size(); tank++) {
			FluidResource fluidInTank = fluidHandler.getResource(tank);
			if (isOutput && fluidHandler.getAmountAsInt(tank) < fluidHandler.getCapacityAsInt(tank, fluidInTank) &&
					(fluidInTank.isEmpty() || tankEmpty || fluidInTank.matches(contents))) {
				return true;
			}
			if (!isOutput && !fluidInTank.isEmpty() && (tankEmpty || fluidInTank.matches(contents))) {
				return true;
			}
		}
		return false;
	}

	private boolean hasNoMatchingFluid(ResourceHandler<FluidResource> fluidHandler) {
		boolean tankEmpty = contents.isEmpty();
		for (int tank = 0; tank < fluidHandler.size(); tank++) {
			FluidResource fluidInTank = fluidHandler.getResource(tank);
			if (!tankEmpty && fluidInTank.matches(contents)) {
				return false;
			} else if (!fluidInTank.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private boolean matchingTankIsFull(ResourceHandler<FluidResource> fluidHandler) {
		boolean tankEmpty = contents.isEmpty();
		for (int tank = 0; tank < fluidHandler.size(); tank++) {
			FluidResource fluidInTank = fluidHandler.getResource(tank);
			int tankCapacity = fluidHandler.getCapacityAsInt(tank, fluidInTank);
			int amount = fluidHandler.getAmountAsInt(tank);
			if (tankEmpty && amount < tankCapacity) {
				return false;
			} else if (fluidInTank.matches(contents) && amount < tankCapacity) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setTankRenderDataUpdateCallback(Consumer<RenderData.TankRenderData> updateTankRenderDataCallback) {
		this.updateTankRenderDataCallback = updateTankRenderDataCallback;
	}

	@Override
	public void forceUpdateTankRenderData() {
		updateTankRenderDataCallback.accept(
				new RenderData.TankRenderData(
						contents.copy(),
						contents.isEmpty() ? 0 : (float) Math.round((float) contents.getAmount() / getCapacity() * 10) / 10
				)
		);
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

	private int getMaxInOut() {
		return (int) Math.max(FluidType.BUCKET_VOLUME, upgradeItem.getTankUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows() * upgradeItem.getAdjustedStackMultiplier(storageWrapper));
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
		serializeContents();

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
		serializeContents();

		return toDrain;
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (level.getGameTime() < cooldownTime) {
			return;
		}

		boolean didSomething = drainStack(inventory.getStackInSlot(INPUT_SLOT));
		didSomething |= fillStack(inventory.getStackInSlot(OUTPUT_SLOT));

		if (didSomething) {
			cooldownTime = level.getGameTime() + upgradeItem.getTankUpgradeConfig().autoFillDrainContainerCooldown.get();
		}
	}

	private boolean drainStack(ItemStack stackToDrain) {
		if (stackToDrain.isEmpty()) {
			return false;
		}

		try (Transaction tx = Transaction.openRoot()) {
			MutableStackItemAccess itemAccess = new MutableStackItemAccess(stackToDrain.copyWithCount(1));
			return getFluidHandler(stackToDrain, itemAccess).map(fluidHandler -> {
				if (drainHandler(fluidHandler, tx) > 0) {
					if (hasNoMatchingFluid(fluidHandler)) {
						if (inventory.insert(INPUT_RESULT_SLOT, itemAccess.getResource(), itemAccess.getAmount(), tx) == itemAccess.getAmount()) {
							tx.commit();
							inventory.setStackInSlot(INPUT_SLOT, stackToDrain.getCount() == 1 ? ItemStack.EMPTY : stackToDrain.copyWithCount(stackToDrain.getCount() - 1));
							return true;
						} else if (stackToDrain.getCount() > 1) {
							return false;
						}
					}
					tx.commit();
					inventory.setStackInSlot(INPUT_SLOT, itemAccess.getStack());
					return true;
				}
				return false;
			}).orElse(false);
		}
	}

	private Optional<ResourceHandler<FluidResource>> getFluidHandler(ItemStack stack, ItemAccess itemAccess) {
		ResourceHandler<FluidResource> result = stack.getCapability(Capabilities.Fluid.ITEM, itemAccess);
		if (result != null) {
			return Optional.of(result);
		}
		return getCustomFluidHandler(stack, itemAccess);
	}

	private boolean fillStack(ItemStack stackToFill) {
		if (stackToFill.isEmpty()) {
			return false;
		}
		try (Transaction tx = Transaction.openRoot()) {
			MutableStackItemAccess itemAccess = new MutableStackItemAccess(stackToFill.copyWithCount(1));
			return getFluidHandler(stackToFill, itemAccess).map(fluidHandler -> {
				if (fillHandler(fluidHandler, tx) > 0) {
					if (matchingTankIsFull(fluidHandler)) {
						if (inventory.insert(OUTPUT_RESULT_SLOT, itemAccess.getResource(), itemAccess.getAmount(), tx) == itemAccess.getAmount()) {
							tx.commit();
							inventory.setStackInSlot(OUTPUT_SLOT, stackToFill.getCount() == 1 ? ItemStack.EMPTY : stackToFill.copyWithCount(stackToFill.getCount() - 1));
							return true;
						} else if (stackToFill.getCount() > 1) {
							return false;
						}
					}
					tx.commit();
					inventory.setStackInSlot(OUTPUT_SLOT, itemAccess.getStack());
					return true;
				}
				return false;
			}).orElse(false);
		}
	}

	public void interactWithCursorStack(ItemStack cursorStack, Consumer<ItemStack> updateContainerStack) {
		MutableStackItemAccess itemAccess = new MutableStackItemAccess(cursorStack);
		getFluidHandler(cursorStack, itemAccess).ifPresent(fluidHandler -> {
			try (Transaction tx = Transaction.openRoot()) {
				if (fillHandler(fluidHandler, tx) > 0) {
					updateContainerStack.accept(itemAccess.getStack());
					tx.commit();
				} else if (drainHandler(fluidHandler, tx) > 0) {
					updateContainerStack.accept(itemAccess.getStack());
					tx.commit();
				}
			}
		});
	}

	private static Optional<ResourceHandler<FluidResource>> getCustomFluidHandler(ItemStack stack, ItemAccess itemAccess) {
		return CUSTOM_FLUIDHANDLER_FACTORIES.entrySet().stream().filter(e -> ItemStack.isSameItemSameComponents(stack, e.getKey())).map(e -> e.getValue().apply(itemAccess)).findFirst();
	}

	public int fillHandler(ResourceHandler<FluidResource> fluidHandler, TransactionContext tx) {
		if (!contents.isEmpty() && isValidFluidHandler(fluidHandler, true)) {
			FluidResource fluidResource = FluidResource.of(contents);
			int filled = fluidHandler.insert(fluidResource, Math.min(FluidType.BUCKET_VOLUME, contents.getAmount()), tx);
			if (filled <= 0) { //checking for less than as well because some mods have incorrect fill logic
				return 0;
			}
			return extract(fluidResource, filled, tx, false);
		}
		return 0;
	}

	public int drainHandler(ResourceHandler<FluidResource> fluidHandler, TransactionContext tx) {
		if (isValidFluidHandler(fluidHandler, false)) {
			FluidResource resource = contents.isEmpty() ? fluidHandler.getResource(0) : FluidResource.of(contents);
			int extracted = contents.isEmpty() ?
					fluidHandler.extract(resource, FluidType.BUCKET_VOLUME, tx) :
					fluidHandler.extract(FluidResource.of(contents), Math.min(FluidType.BUCKET_VOLUME, getCapacity() - contents.getAmount()), tx);

			if (extracted <= 0) {
				return 0;
			}

			return insert(resource, extracted, tx, true);
		}
		return 0;
	}

	@Override
	public int getMinimumMultiplierRequired() {
		return (int) Math.ceil((float) contents.getAmount() / upgradeItem.getBaseCapacity(storageWrapper));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public class Journal extends SnapshotJournal<FluidStack> {
		@Override
		protected FluidStack createSnapshot() {
			return TankUpgradeWrapper.this.getContents().copy();
		}

		@Override
		protected void revertToSnapshot(FluidStack fluidStack) {
			TankUpgradeWrapper.this.contents = fluidStack;
			serializeContents();
		}
	}

	public class TankComponentItemHandler extends ItemAccessItemHandler {
		public TankComponentItemHandler(ItemStack upgrade) {
			super(ItemAccess.forStack(upgrade), ModCoreDataComponents.LENIENT_CONTAINER.get(), 4);
		}

		@Override
		protected ItemResource update(ItemResource accessResource, int index, ItemResource newResource, int newAmount) {
			ItemResource result = super.update(accessResource, index, newResource, newAmount);
			save();
			return result;
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			if (index == INPUT_SLOT) {
				return resource.isEmpty() || hasValidFluidHandler(resource, false);
			} else if (index == OUTPUT_SLOT) {
				return resource.isEmpty() || hasValidFluidHandler(resource, true);
			}
			return index == INPUT_RESULT_SLOT || index == OUTPUT_RESULT_SLOT;
		}

		private boolean hasValidFluidHandler(ItemResource resource, boolean isOutput) {
			return getFluidHandler(resource.toStack(), ItemAccess.forStack(resource.toStack())).map(fluidHandler -> isValidFluidHandler(fluidHandler, isOutput)).orElse(false);
		}

		public ItemStack getStackInSlot(int slot) {
			ItemContainerContents contents = getContents(itemAccess.getResource());
			return getStackFromContents(contents, slot);
		}

		public void setStackInSlot(int slot, ItemStack stack) {
			ItemContainerContents contents = getContents(itemAccess.getResource());
			NonNullList<ItemStack> list = NonNullList.withSize(Math.max(4, this.size), ItemStack.EMPTY);
			contents.copyInto(list);
			list.set(slot, stack);
			upgrade.set(component, ItemContainerContents.fromItems(list));
		}
	}

	private static abstract class SwapEmptyFluidContainerHandler implements ResourceHandler<FluidResource> {
		private final Item empty;
		private final Map<FluidStack, FullContainerDefinition> fullContainers = new HashMap<>();
		private FluidStack contents;
		private final ItemAccess itemAccess;

		public static class Empty extends SwapEmptyFluidContainerHandler {
			public Empty(ItemAccess itemAccess, Item empty, FullContainerDefinition... fullContainers) {
				super(itemAccess, empty, FluidStack.EMPTY, fullContainers);
			}
		}

		public static class Full extends SwapEmptyFluidContainerHandler {
			public Full(ItemAccess itemAccess, Item empty, ItemStack full, int capacity, Fluid validFluid) {
				super(itemAccess, empty, new FluidStack(validFluid, capacity), new FullContainerDefinition(full, capacity, new FluidStack(validFluid, capacity)));
			}
		}

		protected SwapEmptyFluidContainerHandler(ItemAccess itemAccess, Item empty, FluidStack contents, FullContainerDefinition... fullContainers) {
			this.itemAccess = itemAccess;
			this.empty = empty;
			Arrays.stream(fullContainers).forEach(fc -> this.fullContainers.put(fc.validFluid, fc));
			this.contents = contents;
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public FluidResource getResource(int i) {
			return FluidResource.of(contents);
		}

		@Override
		public long getAmountAsLong(int i) {
			return contents.getAmount();
		}

		@Override
		public long getCapacityAsLong(int i, FluidResource resource) {
			return getMatchingDefinition().map(FullContainerDefinition::capacity)
					.orElseGet(() -> fullContainers.values().stream().mapToInt(FullContainerDefinition::capacity).max().orElse(0));
		}

		private Optional<FullContainerDefinition> getMatchingDefinition() {
			return fullContainers.entrySet().stream().filter(e -> FluidStack.isSameFluidSameComponents(e.getKey(), contents)).map(Map.Entry::getValue).findFirst();
		}

		@Override
		public boolean isValid(int i, FluidResource resource) {
			if (!contents.isEmpty()) {
				return resource.matches(contents);
			} else {
				return fullContainers.keySet().stream().anyMatch(resource::matches);
			}
		}

		@Override
		public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
			if (!isValid(0, resource) || itemAccess.getResource().getItem() != empty) {
				return 0;
			}

			return findFirstFullContainer(resource).map(fullContainer -> {
				int result = 0;
				int capacity = fullContainer.capacity();
				if (amount >= capacity) {
					result = capacity;
					itemAccess.exchange(ItemResource.of(fullContainer.full()), 1, tx);
					contents = resource.toStack(capacity);
				}
				return result;
			}).orElse(0);
		}

		private Optional<FullContainerDefinition> findFirstFullContainer(FluidResource resource) {
			return fullContainers.entrySet().stream()
					.filter(e -> resource.matches(e.getKey()))
					.findFirst().map(Map.Entry::getValue);
		}

		@Override
		public int extract(FluidResource resource, int amount, TransactionContext transaction) {
			return ResourceHandler.super.extract(resource, amount, transaction);
		}

		@Override
		public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
			return findFirstFullContainer(resource).map(fullContainer -> {

				int result = 0;
				if (isValid(0, resource) && amount >= fullContainer.capacity()) {
					result = fullContainer.capacity();
					itemAccess.exchange(ItemResource.of(new ItemStack(empty)), 1, tx);
					contents = FluidStack.EMPTY;
				}

				return result;
			}).orElse(0);
		}

		private record FullContainerDefinition(ItemStack full, int capacity, FluidStack validFluid) {
			public FullContainerDefinition(ItemStack full, int capacity, Fluid validFluid) {
				this(full, capacity, new FluidStack(validFluid, capacity));
			}
		}
	}
}