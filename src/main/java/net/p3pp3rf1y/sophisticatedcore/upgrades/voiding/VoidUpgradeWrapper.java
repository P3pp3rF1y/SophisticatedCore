package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class VoidUpgradeWrapper extends UpgradeWrapperBase<VoidUpgradeWrapper, VoidUpgradeItem>
		implements IInsertResponseUpgrade, IFilteredUpgrade, ISlotChangeResponseUpgrade, ITickableUpgrade, IOverflowResponseUpgrade {
	private final FilterLogic filterLogic;
	private final Set<Integer> slotsToVoid = new HashSet<>();
	private boolean shouldVoidOverflow;

	public VoidUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), ModCoreDataComponents.FILTER_ATTRIBUTES);
		filterLogic.setAllowByDefault(true);
		setShouldVoidOverflowDefaultOrLoadFromNbt(false);
	}

	@Override
	public int onBeforeInsert(InventoryHandler inventoryHandler, int slot, ItemResource resource, int amount) {
		if (shouldVoidOverflow && inventoryHandler.getResource(slot).isEmpty() && (!filterLogic.shouldMatchComponents() || !filterLogic.shouldMatchDurability() || filterLogic.getPrimaryMatch() != PrimaryMatch.ITEM) && filterLogic.matchesFilter(resource)) {
			for (int s = 0; s < inventoryHandler.size(); s++) {
				if (s == slot) {
					continue;
				}
				ItemResource filterResource = inventoryHandler.getResource(s);
				if (matchesFilter(filterResource.getItem(), filterResource.getOrDefault(DataComponents.DAMAGE, 0), filterResource.isEmpty(), filterResource.getComponents(),
						resource.getItem(), resource.getOrDefault(DataComponents.DAMAGE, 0), resource.isEmpty(), resource.getComponents())) {
					return amount;
				}
			}
			return 0;
		}

		return !shouldVoidOverflow && filterLogic.matchesFilter(resource) ? amount : 0;
	}

	@Override
	public void onAfterInsert(InventoryHandler inventoryHandler, int slot, TransactionContext tx) {
		//noop
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

	public void setShouldVoidOverflow(boolean shouldVoidOverflow) {
		if (!shouldVoidOverflow && !upgradeItem.isVoidAnythingEnabled()) {
			return;
		}

		this.shouldVoidOverflow = shouldVoidOverflow;
		upgrade.set(ModCoreDataComponents.SHOULD_VOID_OVERFLOW, shouldVoidOverflow);
		save();
	}

	public void setShouldVoidOverflowDefaultOrLoadFromNbt(boolean shouldVoidOverflowDefault) {
		shouldVoidOverflow = !upgradeItem.isVoidAnythingEnabled() || upgrade.getOrDefault(ModCoreDataComponents.SHOULD_VOID_OVERFLOW, shouldVoidOverflowDefault);
	}

	@Override
	public boolean voidsOverflow() {
		return !upgradeItem.isVoidAnythingEnabled() || shouldVoidOverflow;
	}

	@Override
	public void onSlotChange(InventoryHandler inventoryHandler, int slot) {
		if (!shouldWorkInGUI() || voidsOverflow()) {
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
	public ItemStack onOverflow(ItemStack stack) {
		return filterLogic.matchesFilter(stack) ? ItemStack.EMPTY : stack;
	}

	@Override
	public int onOverflow(ItemResource resource, int amount) {
		return filterLogic.matchesFilter(resource) ? amount : 0;
	}

	@Override
	public boolean stackMatchesFilter(ItemStack stack) {
		return filterLogic.matchesFilter(stack);
	}

	@Override
	public boolean matchesFilter(ItemResource resource) {
		return filterLogic.matchesFilter(resource);
	}

	public boolean isVoidAnythingEnabled() {
		return upgradeItem.isVoidAnythingEnabled();
	}
}
