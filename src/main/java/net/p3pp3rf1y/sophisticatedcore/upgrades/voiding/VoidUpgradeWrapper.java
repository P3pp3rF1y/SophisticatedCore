package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.util.ItemStackHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("PMD.UnnecessaryImport")
public class VoidUpgradeWrapper extends UpgradeWrapperBase<VoidUpgradeWrapper, VoidUpgradeItem>
		implements
			IInsertResponseUpgrade,
			IFilteredUpgrade,
			ISlotChangeResponseUpgrade,
			ITickableUpgrade,
			IOverflowResponseUpgrade {
	private final FilterLogic filterLogic;
	private final Set<Integer> slotsToVoid = new HashSet<>();
	private VoidType voidType;

	public VoidUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount());
		filterLogic.setAllowByDefault(true);

		setFromLegacyComponent();

		setVoidOverflowDefaultOrLoadFromNbt(VoidType.ALWAYS);
	}

	// TODO remove in or after 26.1
	private void setFromLegacyComponent() {
		if (NBTHelper.hasTag(upgrade, "shouldVoidOverflow")) {
			VoidType migratedVoidType = NBTHelper.getBoolean(upgrade, "shouldVoidOverflow").orElse(false) ? VoidType.ALWAYS : VoidType.SLOT_OVERFLOW;
			NBTHelper.removeTag(upgrade, "shouldVoidOverflow");
			setVoidType(migratedVoidType);
		}
	}
	@Override
	public ItemStack onBeforeInsert(InventoryHandler inventoryHandler, int slot, ItemStack stack, boolean simulate) {
		if (voidType == VoidType.SLOT_OVERFLOW && inventoryHandler.getStackInSlot(slot).isEmpty() && filterLogic.matchesFilter(stack)) {
			if (hasSlotOverflowMatch(inventoryHandler, stack)) {
				return ItemStack.EMPTY;
			}
			return stack;
		}

		return voidType == VoidType.ALWAYS && filterLogic.matchesFilter(stack) ? ItemStack.EMPTY : stack;
	}

	@Override
	public ItemStack onBeforeInsert(InventoryHandler inventoryHandler, ItemStack stack, boolean simulate) {
		return voidType == VoidType.ALWAYS && filterLogic.matchesFilter(stack) ? ItemStack.EMPTY : stack;
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		NBTHelper.setBoolean(upgrade, "shouldWorkInGUI", shouldWorkdInGUI);
		save();
	}

	public boolean shouldWorkInGUI() {
		return NBTHelper.getBoolean(upgrade, "shouldWorkInGUI").orElse(false);
	}

	public void setVoidType(VoidType voidType) {
		if (voidType == VoidType.ALWAYS && !upgradeItem.isVoidAlwaysEnabled()) {
			return;
		}

		this.voidType = voidType;
		NBTHelper.setEnumConstant(upgrade, "voidType", voidType);
		save();
	}

	public void setVoidOverflowDefaultOrLoadFromNbt(VoidType voidOverflowDefault) {
		VoidType vt = NBTHelper.getEnumConstant(upgrade, "voidType", VoidType::fromName).orElse(voidOverflowDefault);
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
	public void onSlotChange(IItemHandler inventoryHandler, int slot) {
		if (!shouldWorkInGUI() || shouldVoidOverflow()) {
			return;
		}

		ItemStack slotStack = inventoryHandler.getStackInSlot(slot);
		if (filterLogic.matchesFilter(slotStack)) {
			slotsToVoid.add(slot);
		}
	}

	@Override
	public void tick(@Nullable Entity entity, Level world, BlockPos pos) {
		if (slotsToVoid.isEmpty()) {
			return;
		}

		InventoryHandler storageInventory = storageWrapper.getInventoryHandler();
		for (int slot : slotsToVoid) {
			storageInventory.extractItem(slot, storageInventory.getStackInSlot(slot).getCount(), false);
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
	public ItemStack onStorageOverflow(ItemStack stack) {
		return voidType == VoidType.STORAGE_OVERFLOW && filterLogic.matchesFilter(stack) ? ItemStack.EMPTY : stack;
	}

	@Override
	public boolean stackMatchesFilter(ItemStack stack) {
		return filterLogic.matchesFilter(stack);
	}

	@Override
	public boolean hasSlotOverflowMatch(InventoryHandler inventoryHandler, ItemStack stack) {
		if (filterLogic.shouldMatchDurability() && filterLogic.shouldMatchNbt()) {
			return IOverflowResponseUpgrade.super.hasSlotOverflowMatch(inventoryHandler, stack);
		}

		return hasOverflowMatch(inventoryHandler.getSlotTracker().getFullStacks(), inventoryHandler.getSlotTracker().getPartialStacks(),
				stackKey -> stacksMatchForOverflow(stack, stackKey.getStack()));
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

		return !filterLogic.shouldMatchNbt() || ItemStackHelper.areItemStackTagsEqualIgnoreDurability(stack, matchingStack);
	}

	public boolean isVoidAlwaysEnabled() {
		return upgradeItem.isVoidAlwaysEnabled();
	}
}
