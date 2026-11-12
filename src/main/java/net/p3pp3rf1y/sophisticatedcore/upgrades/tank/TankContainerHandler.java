package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.util.MutableStackItemAccess;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class TankContainerHandler {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	public static final int INPUT_RESULT_SLOT = 2;
	public static final int OUTPUT_RESULT_SLOT = 3;
	private final ResourceHandler<ItemResource> inventory;

	private static final Map<ItemStack, Function<ItemAccess, ResourceHandler<FluidResource>>> CUSTOM_FLUIDHANDLER_FACTORIES = Map.of(
			new ItemStack(Items.EXPERIENCE_BOTTLE),
			itemAccess -> new SwapEmptyFluidContainerHandler.Full(
					itemAccess, Items.GLASS_BOTTLE, new ItemStack(Items.EXPERIENCE_BOTTLE), XpHelper.experienceToLiquid(8), ModFluids.XP_STILL.get()),
			PotionContents.createItemStack(Items.POTION, Potions.WATER),
			itemAccess -> new SwapEmptyFluidContainerHandler.Full(
					itemAccess, Items.GLASS_BOTTLE, PotionContents.createItemStack(Items.POTION, Potions.WATER), 250, Fluids.WATER),
			new ItemStack(Items.GLASS_BOTTLE),
			itemAccess -> new SwapEmptyFluidContainerHandler.Empty(itemAccess, Items.GLASS_BOTTLE,
					new SwapEmptyFluidContainerHandler.FullContainerDefinition(new ItemStack(Items.EXPERIENCE_BOTTLE), XpHelper.experienceToLiquid(8),
							ModFluids.XP_STILL.get()),
					new SwapEmptyFluidContainerHandler.FullContainerDefinition(PotionContents.createItemStack(Items.POTION, Potions.WATER), 250,
							Fluids.WATER)));

	protected TankContainerHandler(ResourceHandler<ItemResource> inventory) {
		this.inventory = inventory;
	}

	public abstract FluidStack getContents();
	public abstract int getTankCapacity();
	public abstract int insert(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit);
	public abstract int extract(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit);
	protected abstract ItemStack getStackInSlot(int slot);
	protected abstract void setStackInSlot(int slot, ItemStack stack);

	protected void setStackInSlot(int slot, ItemStack stack, TransactionContext tx) {
		setStackInSlot(slot, stack);
	}

	private boolean isValidFluidHandler(ResourceHandler<FluidResource> fluidHandler, boolean isOutput) {
		FluidStack contents = getContents();
		boolean tankEmpty = contents.isEmpty();
		for (int tank = 0; tank < fluidHandler.size(); tank++) {
			FluidResource fluidInTank = fluidHandler.getResource(tank);
			if (isOutput && fluidHandler.getAmountAsInt(tank) < fluidHandler.getCapacityAsInt(tank, fluidInTank)
					&& (fluidInTank.isEmpty() || tankEmpty || fluidInTank.matches(contents))) {
				return true;
			}
			if (!isOutput && !fluidInTank.isEmpty() && (tankEmpty || fluidInTank.matches(contents))) {
				return true;
			}
		}
		return false;
	}

	private boolean hasNoMatchingFluid(ResourceHandler<FluidResource> fluidHandler) {
		FluidStack contents = getContents();
		if (contents.isEmpty()) {
			for (int tank = 0; tank < fluidHandler.size(); tank++) {
				if (!fluidHandler.getResource(tank).isEmpty()) {
					return false;
				}
			}
			return true;
		}
		for (int tank = 0; tank < fluidHandler.size(); tank++) {
			if (fluidHandler.getResource(tank).matches(contents)) {
				return false;
			}
		}
		return true;
	}

	private boolean matchingTankIsFull(ResourceHandler<FluidResource> fluidHandler) {
		FluidStack contents = getContents();
		boolean tankEmpty = contents.isEmpty();
		for (int tank = 0; tank < fluidHandler.size(); tank++) {
			FluidResource fluidInTank = fluidHandler.getResource(tank);
			int capacity = fluidHandler.getCapacityAsInt(tank, fluidInTank);
			int amount = fluidHandler.getAmountAsInt(tank);
			if (tankEmpty && amount < capacity || fluidInTank.matches(contents) && amount < capacity) {
				return false;
			}
		}
		return true;
	}

	public boolean processContainers() {
		boolean didSomething = drainStack(getStackInSlot(INPUT_SLOT));
		didSomething |= fillStack(getStackInSlot(OUTPUT_SLOT));
		return didSomething;
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
						if (itemAccess.getResource().isEmpty()
								|| inventory.insert(INPUT_RESULT_SLOT, itemAccess.getResource(), itemAccess.getAmount(), tx) == itemAccess.getAmount()) {
							setStackInSlot(INPUT_SLOT, stackToDrain.getCount() == 1 ? ItemStack.EMPTY : stackToDrain.copyWithCount(stackToDrain.getCount() - 1),
									tx);
							tx.commit();
							return true;
						} else if (stackToDrain.getCount() > 1) {
							return false;
						}
					}
					setStackInSlot(INPUT_SLOT, itemAccess.getStack(), tx);
					tx.commit();
					return true;
				}
				return false;
			}).orElse(false);
		}
	}

	private Optional<ResourceHandler<FluidResource>> getFluidHandler(ItemStack stack, ItemAccess itemAccess) {
		ResourceHandler<FluidResource> result = stack.getCapability(Capabilities.Fluid.ITEM, itemAccess);
		return result != null ? Optional.of(result) : getCustomFluidHandler(stack, itemAccess);
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
						if (itemAccess.getResource().isEmpty()
								|| inventory.insert(OUTPUT_RESULT_SLOT, itemAccess.getResource(), itemAccess.getAmount(), tx) == itemAccess.getAmount()) {
							setStackInSlot(OUTPUT_SLOT, stackToFill.getCount() == 1 ? ItemStack.EMPTY : stackToFill.copyWithCount(stackToFill.getCount() - 1),
									tx);
							tx.commit();
							return true;
						} else if (stackToFill.getCount() > 1) {
							return false;
						}
					}
					setStackInSlot(OUTPUT_SLOT, itemAccess.getStack(), tx);
					tx.commit();
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
				if (fillHandler(fluidHandler, tx) > 0 || drainHandler(fluidHandler, tx) > 0) {
					updateContainerStack.accept(itemAccess.getStack());
					tx.commit();
				}
			}
		});
	}

	private static Optional<ResourceHandler<FluidResource>> getCustomFluidHandler(ItemStack stack, ItemAccess itemAccess) {
		return CUSTOM_FLUIDHANDLER_FACTORIES.entrySet().stream().filter(e -> ItemStack.isSameItemSameComponents(stack, e.getKey()))
				.map(e -> e.getValue().apply(itemAccess)).findFirst();
	}

	public int fillHandler(ResourceHandler<FluidResource> fluidHandler, TransactionContext tx) {
		FluidStack contents = getContents();
		if (!contents.isEmpty() && isValidFluidHandler(fluidHandler, true)) {
			FluidResource resource = FluidResource.of(contents);
			int filled = fluidHandler.insert(resource, Math.min(FluidType.BUCKET_VOLUME, contents.getAmount()), tx);
			if (filled <= 0) {
				return 0;
			}
			return extract(resource, filled, tx, false);
		}
		return 0;
	}

	public int drainHandler(ResourceHandler<FluidResource> fluidHandler, TransactionContext tx) {
		FluidStack contents = getContents();
		if (isValidFluidHandler(fluidHandler, false)) {
			FluidResource resource = contents.isEmpty() ? fluidHandler.getResource(0) : FluidResource.of(contents);
			int extracted = contents.isEmpty()
					? fluidHandler.extract(resource, FluidType.BUCKET_VOLUME, tx)
					: fluidHandler.extract(resource, Math.min(FluidType.BUCKET_VOLUME, getTankCapacity() - contents.getAmount()), tx);
			return extracted <= 0 ? 0 : insert(resource, extracted, tx, true);
		}
		return 0;
	}

	public boolean isItemValid(int slot, ItemStack stack) {
		if (slot == INPUT_SLOT || slot == OUTPUT_SLOT) {
			return stack.isEmpty()
					|| getFluidHandler(stack, ItemAccess.forStack(stack)).map(handler -> isValidFluidHandler(handler, slot == OUTPUT_SLOT)).orElse(false);
		}
		return slot == INPUT_RESULT_SLOT || slot == OUTPUT_RESULT_SLOT;
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
				super(itemAccess, empty, new FluidStack(validFluid, capacity),
						new FullContainerDefinition(full, capacity, new FluidStack(validFluid, capacity)));
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
			return fullContainers.entrySet().stream().filter(e -> FluidStack.isSameFluidSameComponents(e.getKey(), contents)).map(Map.Entry::getValue)
					.findFirst();
		}

		@Override
		public boolean isValid(int i, FluidResource resource) {
			return !contents.isEmpty() ? resource.matches(contents) : fullContainers.keySet().stream().anyMatch(resource::matches);
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
			return fullContainers.entrySet().stream().filter(e -> resource.matches(e.getKey())).findFirst().map(Map.Entry::getValue);
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
