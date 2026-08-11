package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.util.ItemStackHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class VoidUpgradeWrapper extends UpgradeWrapperBase<VoidUpgradeWrapper, VoidUpgradeItem>
		implements
			IInsertResponseUpgrade,
			IFilteredUpgrade,
			ISlotChangeResponseUpgrade,
			ITickableUpgrade,
			IOverflowResponseUpgrade {
	private final FilterLogic filterLogic;
	private final FluidFilterLogic fluidFilterLogic;
	private final Set<Integer> slotsToVoid = new HashSet<>();
	private List<FluidResource> containedFilterFluids = null;
	private VoidType voidType;

	public VoidUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), ModCoreDataComponents.FILTER_ATTRIBUTES);
		filterLogic.setAllowByDefault(true);
		filterLogic.getFilterHandler().setOnSlotChange(this::updateContainedFilterFluid);
		fluidFilterLogic = new FluidFilterLogic(upgradeItem.getFilterSlotCount(), upgrade, upgradeSaveHandler, false);

		setFromLegacyComponent();

		setVoidOverflowDefaultOrLoadFromNbt(VoidType.ALWAYS);
	}

	// TODO remove in or after 26.1
	private void setFromLegacyComponent() {
		if (upgrade.has(ModCoreDataComponents.LEGACY_SHOULD_VOID_OVERFLOW)) {
			VoidType migratedVoidType = upgrade.get(ModCoreDataComponents.LEGACY_SHOULD_VOID_OVERFLOW) ? VoidType.SLOT_OVERFLOW : VoidType.ALWAYS;
			upgrade.remove(ModCoreDataComponents.LEGACY_SHOULD_VOID_OVERFLOW);
			setVoidType(migratedVoidType);
		}
	}

	@Override
	public int onBeforeInsert(InventoryHandler inventoryHandler, int slot, ItemResource resource, int amount) {
		if (voidType == VoidType.SLOT_OVERFLOW && inventoryHandler.getStackInSlot(slot).isEmpty() && filterLogic.matchesFilter(resource)) {
			if (hasSlotOverflowMatch(inventoryHandler, resource)) {
				return amount;
			}
			return 0;
		}

		return voidType == VoidType.ALWAYS && filterLogic.matchesFilter(resource) ? amount : 0;
	}

	@Override
	public int onBeforeInsert(InventoryHandler inventoryHandler, ItemResource resource, int amount) {
		return voidType == VoidType.ALWAYS && filterLogic.matchesFilter(resource) ? amount : 0;
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public FluidFilterLogic getFluidFilterLogic() {
		return fluidFilterLogic;
	}

	public boolean shouldVoidFluid(FluidResource fluid, VoidType voidType) {
		if (getVoidType() != voidType) {
			return false;
		}

		boolean matchesFilter = fluidFilterLogic.fluidMatches(fluid) || containedFilterFluidMatches(fluid);
		return filterLogic.isAllowList() == matchesFilter;
	}

	private boolean containedFilterFluidMatches(FluidResource fluid) {
		for (FluidResource containedFluid : getContainedFilterFluids()) {
			if (containedFluid.equals(fluid)) {
				return true;
			}
		}

		return false;
	}

	private List<FluidResource> getContainedFilterFluids() {
		if (containedFilterFluids == null) {
			var filterHandler = filterLogic.getFilterHandler();
			containedFilterFluids = new ArrayList<>(filterHandler.size());
			for (int slot = 0; slot < filterHandler.size(); slot++) {
				containedFilterFluids.add(FluidFilterContainer.getContainedFluid(filterHandler.getStackInSlot(slot)));
			}
		}

		return containedFilterFluids;
	}

	private void updateContainedFilterFluid(int slot) {
		if (containedFilterFluids != null) {
			containedFilterFluids.set(slot, FluidFilterContainer.getContainedFluid(filterLogic.getFilterHandler().getStackInSlot(slot)));
		}
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		upgrade.set(ModCoreDataComponents.SHOULD_WORK_IN_GUI, shouldWorkdInGUI);
		save();
	}

	public boolean shouldWorkInGUI() {
		return upgrade.getOrDefault(ModCoreDataComponents.SHOULD_WORK_IN_GUI, false);
	}

	public void setVoidType(VoidType voidType) {
		if (voidType == VoidType.ALWAYS && !upgradeItem.isVoidAlwaysEnabled()) {
			return;
		}

		this.voidType = voidType;
		upgrade.set(ModCoreDataComponents.VOID_TYPE, voidType);
		save();
	}

	public void setVoidOverflowDefaultOrLoadFromNbt(VoidType voidOverflowDefault) {
		VoidType vt = upgrade.getOrDefault(ModCoreDataComponents.VOID_TYPE, voidOverflowDefault);
		if (!upgradeItem.isVoidAlwaysEnabled() && vt == VoidType.ALWAYS) {
			vt = VoidType.SLOT_OVERFLOW;
		}
		this.voidType = vt;
	}

	public boolean shouldVoidOverflow() {
		return !upgradeItem.isVoidAlwaysEnabled() || voidType != VoidType.ALWAYS;
	}

	public VoidType getVoidType() {
		if (voidType == VoidType.ALWAYS && !upgradeItem.isVoidAlwaysEnabled()) {
			return VoidType.SLOT_OVERFLOW;
		}
		return voidType;
	}

	@Override
	public void onSlotChange(InventoryHandler inventoryHandler, int slot) {
		if (!shouldWorkInGUI() || voidType != VoidType.ALWAYS) {
			return;
		}

		ItemResource resource = inventoryHandler.getResource(slot);
		if (!resource.isEmpty() && filterLogic.matchesFilter(resource)) {
			slotsToVoid.add(slot);
		}
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (slotsToVoid.isEmpty()) {
			return;
		}

		InventoryHandler storageInventory = storageWrapper.getInventoryHandler();
		try (Transaction tx = Transaction.openRoot()) {
			for (int slot : slotsToVoid) {
				ItemResource resource = storageInventory.getResource(slot);
				int amount = storageInventory.getAmountAsInt(slot);
				if (!resource.isEmpty() && amount > 0 && filterLogic.matchesFilter(resource)) {
					storageInventory.extract(slot, resource, amount, tx);
				}
			}
			tx.commit();
		}

		slotsToVoid.clear();
	}

	@Override
	public boolean worksInGui() {
		return shouldWorkInGUI();
	}

	@Override
	public ItemStack onSlotOverflow(ItemStack stack) {
		return voidType == VoidType.SLOT_OVERFLOW && filterLogic.matchesFilter(stack) ? ItemStack.EMPTY : stack;
	}

	@Override
	public int onSlotOverflow(ItemResource resource, int amount) {
		return voidType == VoidType.SLOT_OVERFLOW && filterLogic.matchesFilter(resource) ? amount : 0;
	}

	@Override
	public int onStorageOverflow(ItemResource resource, int amount) {
		return voidType == VoidType.STORAGE_OVERFLOW && filterLogic.matchesFilter(resource) ? amount : 0;
	}

	@Override
	public boolean stackMatchesFilter(ItemStack stack) {
		return filterLogic.matchesFilter(stack);
	}

	@Override
	public boolean matchesFilter(ItemResource resource) {
		return filterLogic.matchesFilter(resource);
	}

	@Override
	public boolean hasSlotOverflowMatch(InventoryHandler inventoryHandler, ItemStack stack) {
		if (filterLogic.shouldMatchDurability() && filterLogic.shouldMatchComponents()) {
			return IOverflowResponseUpgrade.super.hasSlotOverflowMatch(inventoryHandler, stack);
		}

		return hasOverflowMatch(inventoryHandler.getSlotTracker().getFullStacks(), inventoryHandler.getSlotTracker().getPartialStacks(),
				stackKey -> stacksMatchForOverflow(stack, stackKey.stack()));
	}

	@Override
	public boolean hasSlotOverflowMatch(InventoryHandler inventoryHandler, ItemResource resource) {
		if (filterLogic.shouldMatchDurability() && filterLogic.shouldMatchComponents()) {
			return IOverflowResponseUpgrade.super.hasSlotOverflowMatch(inventoryHandler, resource);
		}

		return hasOverflowMatch(inventoryHandler.getSlotTracker().getFullStacks(), inventoryHandler.getSlotTracker().getPartialStacks(),
				stackKey -> stacksMatchForOverflow(resource, stackKey.stack()));
	}

	static <T> boolean hasOverflowMatch(Set<T> fullStacks, Set<T> partialStacks, Predicate<T> stackMatcher) {
		for (T stackKey : fullStacks) {
			if (stackMatcher.test(stackKey)) {
				return true;
			}
		}

		for (T stackKey : partialStacks) {
			if (stackMatcher.test(stackKey)) {
				return true;
			}
		}

		return false;
	}

	private boolean stacksMatchForOverflow(ItemStack stack, ItemStack matchingStack) {
		if (stack.getItem() != matchingStack.getItem()) {
			return false;
		}

		if (filterLogic.shouldMatchDurability() && stack.getDamageValue() != matchingStack.getDamageValue()) {
			return false;
		}

		return !filterLogic.shouldMatchComponents() || ItemStackHelper.areItemStackComponentsEqualIgnoreDurability(stack.isEmpty(), stack.getComponents(),
				matchingStack.isEmpty(), matchingStack.getComponents());
	}

	private boolean stacksMatchForOverflow(ItemResource resource, ItemStack matchingStack) {
		if (resource.getItem() != matchingStack.getItem()) {
			return false;
		}

		if (filterLogic.shouldMatchDurability() && resource.getOrDefault(DataComponents.DAMAGE, 0) != matchingStack.getDamageValue()) {
			return false;
		}

		return !filterLogic.shouldMatchComponents() || ItemStackHelper.areItemStackComponentsEqualIgnoreDurability(resource.isEmpty(), resource.getComponents(),
				matchingStack.isEmpty(), matchingStack.getComponents());
	}

	public boolean isVoidAlwaysEnabled() {
		return upgradeItem.isVoidAlwaysEnabled();
	}
}
