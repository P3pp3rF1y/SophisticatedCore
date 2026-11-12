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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
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
	private final IItemHandlerModifiable inventory;

	private static final Map<ItemStack, Function<ItemStack, IFluidHandlerItem>> CUSTOM_FLUIDHANDLER_FACTORIES = Map.of(new ItemStack(Items.EXPERIENCE_BOTTLE),
			stack -> new SwapEmptyFluidContainerHandler.Full(
					stack, Items.GLASS_BOTTLE, new ItemStack(Items.EXPERIENCE_BOTTLE), XpHelper.experienceToLiquid(8), ModFluids.XP_STILL.get()),
			PotionContents.createItemStack(Items.POTION, Potions.WATER),
			stack -> new SwapEmptyFluidContainerHandler.Full(
					stack, Items.GLASS_BOTTLE, PotionContents.createItemStack(Items.POTION, Potions.WATER), 250, Fluids.WATER),
			new ItemStack(Items.GLASS_BOTTLE),
			stack -> new SwapEmptyFluidContainerHandler.Empty(stack, Items.GLASS_BOTTLE,
					new SwapEmptyFluidContainerHandler.FullContainerDefinition(new ItemStack(Items.EXPERIENCE_BOTTLE), XpHelper.experienceToLiquid(8),
							ModFluids.XP_STILL.get()),
					new SwapEmptyFluidContainerHandler.FullContainerDefinition(PotionContents.createItemStack(Items.POTION, Potions.WATER), 250,
							Fluids.WATER)));

	protected TankContainerHandler(IItemHandlerModifiable inventory) {
		this.inventory = inventory;
	}

	private boolean isValidFluidHandler(IFluidHandlerItem fluidHandler, boolean isOutput) {
		FluidStack contents = getContents();
		boolean tankEmpty = contents.isEmpty();
		for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
			FluidStack fluidInTank = fluidHandler.getFluidInTank(tank);
			if (isOutput && fluidInTank.getAmount() < fluidHandler.getTankCapacity(tank)
					&& (fluidInTank.isEmpty() || tankEmpty || FluidStack.isSameFluidSameComponents(fluidInTank, contents))) {
				return true;
			}
			if (!isOutput && !fluidInTank.isEmpty() && (tankEmpty || FluidStack.isSameFluidSameComponents(contents, fluidInTank))) {
				return true;
			}
		}
		return false;
	}

	private boolean hasNoMatchingFluid(IFluidHandlerItem fluidHandler) {
		FluidStack contents = getContents();
		if (contents.isEmpty()) {
			for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
				if (!fluidHandler.getFluidInTank(tank).isEmpty()) {
					return false;
				}
			}
			return true;
		}

		for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
			if (FluidStack.isSameFluidSameComponents(fluidHandler.getFluidInTank(tank), contents)) {
				return false;
			}
		}
		return true;
	}

	private boolean matchingTankIsFull(IFluidHandlerItem fluidHandler) {
		FluidStack contents = getContents();
		boolean tankEmpty = contents.isEmpty();
		for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
			FluidStack fluidInTank = fluidHandler.getFluidInTank(tank);
			int tankCapacity = fluidHandler.getTankCapacity(tank);
			if (tankEmpty && fluidInTank.getAmount() < tankCapacity) {
				return false;
			} else if (FluidStack.isSameFluidSameComponents(fluidInTank, contents) && fluidInTank.getAmount() < tankCapacity) {
				return false;
			}
		}
		return true;
	}

	public abstract FluidStack getContents();
	public abstract int getTankCapacity();
	public abstract int fill(FluidStack resource, IFluidHandler.FluidAction action, boolean ignoreInOutLimit);
	public abstract FluidStack drain(int maxDrain, IFluidHandler.FluidAction action, boolean ignoreInOutLimit);

	public boolean processContainers() {
		boolean didSomething = tryDraining(inventory.getStackInSlot(INPUT_SLOT));
		didSomething |= tryFilling(inventory.getStackInSlot(OUTPUT_SLOT));
		return didSomething;
	}

	private boolean tryDraining(ItemStack inputSlotStack) {
		ItemStack stackToDrain = inputSlotStack;
		if (inputSlotStack.getCount() > 1) {
			stackToDrain = inputSlotStack.copyWithCount(1);
			if (!drainStack(stackToDrain, true, stack -> {
			})) {
				return false;
			}
			return drainStack(stackToDrain, false, stack -> inventory.setStackInSlot(INPUT_SLOT, inputSlotStack.copyWithCount(inputSlotStack.getCount() - 1)));
		}

		return drainStack(stackToDrain, false, stack -> inventory.setStackInSlot(INPUT_SLOT, stack));
	}

	private boolean drainStack(ItemStack stackToDrain, boolean simulateFullDrain, Consumer<ItemStack> updateContainerStack) {
		return getFluidHandler(stackToDrain).map(fluidHandler -> drainHandler(fluidHandler, updateContainerStack, true, simulateFullDrain)).orElse(false);
	}

	private Optional<IFluidHandlerItem> getFluidHandler(ItemStack stack) {
		IFluidHandlerItem result = stack.getCapability(Capabilities.FluidHandler.ITEM);
		if (result != null) {
			return Optional.of(result);
		}
		return getCustomFluidHandler(stack);
	}

	private boolean tryFilling(ItemStack outputSlotStack) {
		ItemStack stackToFill = outputSlotStack;
		if (outputSlotStack.getCount() > 1) {
			stackToFill = outputSlotStack.copyWithCount(1);
			if (!fillStack(stackToFill, true, stack -> {
			})) {
				return false;
			}
			return fillStack(stackToFill, false, stack -> inventory.setStackInSlot(OUTPUT_SLOT, outputSlotStack.copyWithCount(outputSlotStack.getCount() - 1)));
		}

		return fillStack(stackToFill, false, stack -> inventory.setStackInSlot(OUTPUT_SLOT, stack));
	}

	private boolean fillStack(ItemStack outputSlotStack, boolean simulateFullFill, Consumer<ItemStack> updateContainerStack) {
		return getFluidHandler(outputSlotStack).map(fluidHandler -> fillHandler(fluidHandler, updateContainerStack, true, simulateFullFill)).orElse(false);
	}

	public void interactWithCursorStack(ItemStack cursorStack, Consumer<ItemStack> updateContainerStack) {
		getFluidHandler(cursorStack).ifPresent(fluidHandler -> {
			FluidStack tankContents = getContents();
			if (tankContents.isEmpty()) {
				drainHandler(fluidHandler, updateContainerStack);
			} else {
				if (!fillHandler(fluidHandler, updateContainerStack, false, false)) {
					drainHandler(fluidHandler, updateContainerStack);
				}
			}
		});
	}

	private static Optional<IFluidHandlerItem> getCustomFluidHandler(ItemStack stack) {
		return CUSTOM_FLUIDHANDLER_FACTORIES.entrySet().stream().filter(e -> ItemStack.isSameItemSameComponents(stack, e.getKey()))
				.map(e -> e.getValue().apply(stack)).findFirst();
	}

	private boolean fillHandler(IFluidHandlerItem fluidHandler, Consumer<ItemStack> updateContainerStack, boolean moveFullToResult,
			boolean simulateIncludingFullFill) {
		FluidStack contents = getContents();
		if (contents.isEmpty() || !isValidFluidHandler(fluidHandler, true)) {
			return false;
		}
		int filled = fluidHandler.fill(contents.copyWithAmount(Math.min(FluidType.BUCKET_VOLUME, contents.getAmount())), IFluidHandler.FluidAction.SIMULATE);
		if (filled <= 0 || moveFullToResult && isFullAfterFillButUnableToInsertIntoResult(fluidHandler.getContainer().copy())) {
			return false;
		}
		if (simulateIncludingFullFill) {
			return canCompletelyFillContainer(fluidHandler, filled);
		}
		FluidStack drained = drain(filled, IFluidHandler.FluidAction.EXECUTE, false);
		fluidHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
		boolean moveToResult = moveFullToResult && getFluidHandler(fluidHandler.getContainer()).map(this::matchingTankIsFull).orElse(false);
		updateTransferredContainer(fluidHandler, updateContainerStack, OUTPUT_RESULT_SLOT, moveToResult);
		return true;
	}

	private boolean canCompletelyFillContainer(IFluidHandlerItem fluidHandler, int amount) {
		FluidStack drained = drain(amount, IFluidHandler.FluidAction.SIMULATE, false);
		if (drained.getAmount() != amount) {
			return false;
		}
		return getFluidHandler(fluidHandler.getContainer().copy()).map(copyFluidHandler -> {
			if (copyFluidHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE) != amount) {
				return false;
			}
			return getFluidHandler(copyFluidHandler.getContainer()).map(this::matchingTankIsFull).orElse(false);
		}).orElse(false);
	}

	private void updateTransferredContainer(IFluidHandlerItem fluidHandler, Consumer<ItemStack> updateContainerStack, int resultSlot, boolean moveToResult) {
		if (moveToResult) {
			updateContainerStack.accept(ItemStack.EMPTY);
			inventory.insertItem(resultSlot, fluidHandler.getContainer(), false);
		} else {
			updateContainerStack.accept(fluidHandler.getContainer());
		}
	}

	private boolean isFullAfterFillButUnableToInsertIntoResult(ItemStack containerCopy) {
		FluidStack contents = getContents();
		return getFluidHandler(containerCopy).map(copyFluidHandler -> {
			copyFluidHandler.fill(contents.copyWithAmount(Math.min(FluidType.BUCKET_VOLUME, contents.getAmount())), IFluidHandler.FluidAction.EXECUTE);
			return getFluidHandler(copyFluidHandler.getContainer()).map(fh -> {
				int tank = getMatchingTank(fh, contents);
				if (tank < 0) {
					return true;
				}
				return fh.getFluidInTank(tank).getAmount() == fh.getTankCapacity(tank)
						&& !inventory.insertItem(OUTPUT_RESULT_SLOT, copyFluidHandler.getContainer(), true).isEmpty();
			}).orElse(true);
		}).orElse(true);
	}

	private void drainHandler(IFluidHandlerItem fluidHandler, Consumer<ItemStack> updateContainerStack) {
		drainHandler(fluidHandler, updateContainerStack, false, false);
	}

	private boolean drainHandler(IFluidHandlerItem fluidHandler, Consumer<ItemStack> updateContainerStack, boolean moveEmptyToResult,
			boolean simulateIncludingFullDrain) {
		FluidStack contents = getContents();
		if (!isValidFluidHandler(fluidHandler, false)) {
			return false;
		}
		FluidStack extracted = contents.isEmpty()
				? fluidHandler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE)
				: fluidHandler.drain(contents.copyWithAmount(Math.min(FluidType.BUCKET_VOLUME, getTankCapacity() - contents.getAmount())),
						IFluidHandler.FluidAction.SIMULATE);
		if (extracted.isEmpty() || moveEmptyToResult && isEmptyAfterDrainButUnableToInsertIntoResult(fluidHandler.getContainer().copy(), extracted)) {
			return false;
		}
		if (simulateIncludingFullDrain) {
			return fluidHandler.getTanks() > 0 && fill(extracted, IFluidHandler.FluidAction.SIMULATE, false) == fluidHandler.getTankCapacity(0);
		}
		int filled = fill(extracted, IFluidHandler.FluidAction.EXECUTE, false);
		FluidStack toExtract = filled == extracted.getAmount() ? extracted : extracted.copyWithAmount(filled);
		fluidHandler.drain(toExtract, IFluidHandler.FluidAction.EXECUTE);
		boolean moveToResult = moveEmptyToResult && getFluidHandler(fluidHandler.getContainer()).map(this::hasNoMatchingFluid).orElse(true);
		updateTransferredContainer(fluidHandler, updateContainerStack, INPUT_RESULT_SLOT, moveToResult);
		return true;
	}

	private boolean isEmptyAfterDrainButUnableToInsertIntoResult(ItemStack containerCopy, FluidStack extracted) {
		return getFluidHandler(containerCopy).map(copyFluidHandler -> {
			int tank = getMatchingTank(copyFluidHandler, extracted);
			if (tank < 0) {
				return true;
			}
			copyFluidHandler.drain(extracted, IFluidHandler.FluidAction.EXECUTE);
			return getFluidHandler(copyFluidHandler.getContainer()).map(fh -> fh.getTanks() <= tank || fh.getFluidInTank(tank).isEmpty()).orElse(true)
					&& !inventory.insertItem(INPUT_RESULT_SLOT, copyFluidHandler.getContainer(), true).isEmpty();
		}).orElse(true);
	}

	private int getMatchingTank(IFluidHandlerItem fluidHandler, FluidStack matchTo) {
		for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
			FluidStack fluidInTank = fluidHandler.getFluidInTank(tank);
			if (FluidStack.isSameFluidSameComponents(fluidInTank, matchTo)) {
				return tank;
			}
		}
		return -1;
	}

	public boolean isItemValid(int slot, ItemStack stack) {
		if (slot == INPUT_SLOT || slot == OUTPUT_SLOT) {
			return stack.isEmpty() || getFluidHandler(stack).map(handler -> isValidFluidHandler(handler, slot == OUTPUT_SLOT)).orElse(false);
		}
		return slot == INPUT_RESULT_SLOT || slot == OUTPUT_RESULT_SLOT;
	}

	private static abstract class SwapEmptyFluidContainerHandler implements IFluidHandlerItem {
		private ItemStack container;
		private final Item empty;
		private final Map<FluidStack, FullContainerDefinition> fullContainers = new HashMap<>();
		private FluidStack contents;

		public static class Empty extends SwapEmptyFluidContainerHandler {
			public Empty(ItemStack container, Item empty, FullContainerDefinition... fullContainers) {
				super(container, empty, FluidStack.EMPTY, fullContainers);
			}
		}

		public static class Full extends SwapEmptyFluidContainerHandler {
			public Full(ItemStack container, Item empty, ItemStack full, int capacity, Fluid validFluid) {
				super(container, empty, new FluidStack(validFluid, capacity),
						new FullContainerDefinition(full, capacity, new FluidStack(validFluid, capacity)));
			}
		}

		protected SwapEmptyFluidContainerHandler(ItemStack container, Item empty, FluidStack contents, FullContainerDefinition... fullContainers) {
			this.container = container;
			this.empty = empty;
			Arrays.stream(fullContainers).forEach(fc -> this.fullContainers.put(fc.validFluid, fc));
			this.contents = contents;
		}

		@Override
		public ItemStack getContainer() {
			return container;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return contents;
		}

		@Override
		public int getTankCapacity(int tank) {
			return getMatchingDefinition().map(FullContainerDefinition::capacity)
					.orElseGet(() -> fullContainers.values().stream().mapToInt(FullContainerDefinition::capacity).max().orElse(0));
		}

		private Optional<FullContainerDefinition> getMatchingDefinition() {
			return fullContainers.entrySet().stream().filter(e -> FluidStack.isSameFluidSameComponents(e.getKey(), contents)).map(Map.Entry::getValue)
					.findFirst();
		}

		@Override
		public boolean isFluidValid(int i, FluidStack fluidStack) {
			return !contents.isEmpty()
					? FluidStack.isSameFluidSameComponents(contents, fluidStack)
					: fullContainers.keySet().stream().anyMatch(validFluid -> FluidStack.isSameFluidSameComponents(validFluid, fluidStack));
		}

		@Override
		public int fill(FluidStack fluidStack, FluidAction fluidAction) {
			if (!isFluidValid(0, fluidStack) || container.getItem() != empty) {
				return 0;
			}

			return findFirstFullContainer(fluidStack).map(fullContainer -> {
				int result = 0;
				int capacity = fullContainer.capacity();
				if (fluidStack.getAmount() >= capacity) {
					result = capacity;
					if (fluidAction == FluidAction.EXECUTE) {
						container = fullContainer.full().copy();
						contents = fluidStack.copyWithAmount(capacity);
					}
				}
				return result;
			}).orElse(0);
		}

		private Optional<FullContainerDefinition> findFirstFullContainer(FluidStack fluidStack) {
			return fullContainers.entrySet().stream().filter(e -> FluidStack.isSameFluidSameComponents(e.getKey(), fluidStack)).findFirst()
					.map(Map.Entry::getValue);
		}

		@Override
		public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
			return findFirstFullContainer(contents).map(fullContainer -> {
				if (fluidStack.isEmpty() || !ItemStack.isSameItemSameComponents(container, fullContainer.full())) {
					return FluidStack.EMPTY;
				}

				FluidStack result = FluidStack.EMPTY;
				if (isFluidValid(0, fluidStack) && fluidStack.getAmount() >= fullContainer.capacity()) {
					result = fluidStack.copyWithAmount(fullContainer.capacity());
					if (fluidAction == FluidAction.EXECUTE) {
						container = new ItemStack(empty);
						contents = FluidStack.EMPTY;
					}
				}

				return result;
			}).orElse(FluidStack.EMPTY);
		}

		@Override
		public FluidStack drain(int toDrain, FluidAction fluidAction) {
			if (container.getItem() == empty || toDrain < contents.getAmount()) {
				return FluidStack.EMPTY;
			}

			return drain(contents, fluidAction);
		}

		private record FullContainerDefinition(ItemStack full, int capacity, FluidStack validFluid) {
			public FullContainerDefinition(ItemStack full, int capacity, Fluid validFluid) {
				this(full, capacity, new FluidStack(validFluid, capacity));
			}
		}
	}
}
