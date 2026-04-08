package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.util.ItemStackHelper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class VoidUpgradeWrapper extends UpgradeWrapperBase<VoidUpgradeWrapper, VoidUpgradeItem>
		implements IInsertResponseUpgrade, IFilteredUpgrade, ISlotChangeResponseUpgrade, ITickableUpgrade, IOverflowResponseUpgrade {
	private final FilterLogic filterLogic;
	private final Set<Integer> slotsToVoid = new HashSet<>();
	private VoidType voidType;

	public VoidUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), ModCoreDataComponents.FILTER_ATTRIBUTES);
		filterLogic.setAllowByDefault(true);

		setFromLegacyComponent();

		setVoidOverflowDefaultOrLoadFromNbt(VoidType.ALWAYS);
	}

	//TODO remove in or after 26.1
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

		if (filterLogic.matchesFilter(inventoryHandler.getResource(slot))) {
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
				storageInventory.extract(slot, storageInventory.getResource(slot), storageInventory.getAmountAsInt(slot), tx);
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

		return inventoryHandler.getSlotTracker().hasMatchingFullStack(stack, fullStack -> stacksMatchForOverflow(stack, fullStack));
	}

	@Override
	public boolean hasSlotOverflowMatch(InventoryHandler inventoryHandler, ItemResource resource) {
		if (filterLogic.shouldMatchDurability() && filterLogic.shouldMatchComponents()) {
			return IOverflowResponseUpgrade.super.hasSlotOverflowMatch(inventoryHandler, resource);
		}

		return inventoryHandler.getSlotTracker().hasMatchingFullStack(resource.toStack(), fullStack -> stacksMatchForOverflow(resource, fullStack));
	}

	private boolean stacksMatchForOverflow(ItemStack stack, ItemStack fullStack) {
		if (stack.getItem() != fullStack.getItem()) {
			return false;
		}

		if (filterLogic.shouldMatchDurability() && stack.getDamageValue() != fullStack.getDamageValue()) {
			return false;
		}

		return !filterLogic.shouldMatchComponents() || ItemStackHelper.areItemStackComponentsEqualIgnoreDurability(stack.isEmpty(), stack.getComponents(), fullStack.isEmpty(), fullStack.getComponents());
	}

	private boolean stacksMatchForOverflow(ItemResource resource, ItemStack fullStack) {
		if (resource.getItem() != fullStack.getItem()) {
			return false;
		}

		if (filterLogic.shouldMatchDurability() && resource.getOrDefault(DataComponents.DAMAGE, 0) != fullStack.getDamageValue()) {
			return false;
		}

		return !filterLogic.shouldMatchComponents() || ItemStackHelper.areItemStackComponentsEqualIgnoreDurability(resource.isEmpty(), resource.getComponents(), fullStack.isEmpty(), fullStack.getComponents());
	}

	public boolean isVoidAlwaysEnabled() {
		return upgradeItem.isVoidAlwaysEnabled();
	}
}
