package net.p3pp3rf1y.sophisticatedcore.upgrades.stack;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class StackUpgradeItem extends UpgradeItemBase<StackUpgradeItem.Wrapper> {
	public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
	public static final UpgradeGroup UPGRADE_GROUP = new UpgradeGroup("stack_upgrades", TranslationHelper.INSTANCE.translUpgradeGroup("stack_upgrades"));
	private final double stackSizeMultiplier;

	public StackUpgradeItem(double stackSizeMultiplier, IUpgradeCountLimitConfig upgradeTypeLimitConfig, Properties properties) {
		super(upgradeTypeLimitConfig, properties);
		this.stackSizeMultiplier = stackSizeMultiplier;
	}

	public static int getInventorySlotLimit(IStorageWrapper storageWrapper) {
		return getInventorySlotLimit(storageWrapper, 1);
	}

	private static int getInventorySlotLimit(IStorageWrapper storageWrapper, double skipMultiplier) {
		double multiplier = storageWrapper.getBaseStackSizeMultiplier();
		boolean multiplierSkipped = false;
		for (Wrapper stackWrapper : storageWrapper.getUpgradeHandler().getTypeWrappers(TYPE)) {
			if (!multiplierSkipped && stackWrapper.getStackSizeMultiplier() == skipMultiplier) {
				multiplierSkipped = true;
				continue;
			}

			if (Integer.MAX_VALUE / stackWrapper.getStackSizeMultiplier() < multiplier) {
				return Integer.MAX_VALUE;
			}
			multiplier *= stackWrapper.getStackSizeMultiplier();
		}

		return getInventorySlotLimit(multiplier);
	}

	private static int getInventorySlotLimit(double multiplier) {
		return Integer.MAX_VALUE / 64D < multiplier ? Integer.MAX_VALUE : Math.max(1, (int) (multiplier * 64));
	}

	@Override
	public UpgradeType<Wrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	public double getStackSizeMultiplier() {
		return stackSizeMultiplier;
	}

	@Override
	public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide) {
		if (isClientSide) {
			return UpgradeSlotChangeResult.success();
		}

		double multiplierWhenRemoved = getInventorySlotLimit(storageWrapper, stackSizeMultiplier) / 64D;
		return isMultiplierHighEnough(storageWrapper, multiplierWhenRemoved, -1);
	}

	@Override
	public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide, int upgradeSlot,
			@Nullable IUpgradeItem<?> upgradeInSlot) {
		if (isClientSide) {
			return UpgradeSlotChangeResult.success();
		}

		double multiplierWhenAdded = getInventorySlotLimit(storageWrapper) / 64D * stackSizeMultiplier;
		UpgradeSlotChangeResult result = isMultiplierHighEnough(storageWrapper, multiplierWhenAdded, upgradeSlot);
		if (!result.successful()) {
			return result;
		}

		return super.checkExtraInsertConditions(upgradeStack, storageWrapper, isClientSide, upgradeInSlot);
	}

	@Override
	public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, int upgradeSlot, IStorageWrapper storageWrapper, boolean isClientSide) {
		if (isClientSide) {
			return UpgradeSlotChangeResult.success();
		}

		UpgradeSlotChangeResult result = super.canSwapUpgradeFor(upgradeStackToPut, upgradeSlot, storageWrapper, isClientSide);
		if (!result.successful()) {
			return result;
		}

		if (!(upgradeStackToPut.getItem() instanceof StackUpgradeItem otherStackUpgradeItem)) {
			return canRemoveUpgradeFrom(storageWrapper, isClientSide);
		}

		if (otherStackUpgradeItem.stackSizeMultiplier >= stackSizeMultiplier) {
			return UpgradeSlotChangeResult.success();
		}

		double multiplierWhenRemoved = getInventorySlotLimit(storageWrapper, stackSizeMultiplier) / 64D;

		return isMultiplierHighEnough(storageWrapper, multiplierWhenRemoved * otherStackUpgradeItem.stackSizeMultiplier, -1);
	}

	static UpgradeSlotChangeResult isMultiplierHighEnough(IStorageWrapper storageWrapper, double multiplier, int ignoreUpgradeSlot) {
		int inventorySlotLimit = getInventorySlotLimit(multiplier);
		double effectiveMultiplier = inventorySlotLimit / 64D;
		Set<Integer> slotsOverMultiplier = new HashSet<>();

		for (int slot = 0; slot < storageWrapper.getInventoryHandler().getSlots(); slot++) {
			ItemStack stack = storageWrapper.getInventoryHandler().getSlotStack(slot);
			if (stack.getCount() <= 1) {
				continue;
			}
			double stackMultiplierNeeded = (double) stack.getCount() / stack.getMaxStackSize();
			if (stackMultiplierNeeded > effectiveMultiplier) {
				slotsOverMultiplier.add(slot);
			}
		}

		Set<Integer> errorUpgradeSlots = new HashSet<>();
		Set<Integer> errorInventoryParts = new HashSet<>();
		for (Map.Entry<Integer, IUpgradeWrapper> entry : storageWrapper.getUpgradeHandler().getSlotWrappers().entrySet()) {
			Integer slot = entry.getKey();
			IUpgradeWrapper wrapper = entry.getValue();
			if (slot == ignoreUpgradeSlot) {
				continue;
			}
			if (wrapper instanceof IStackableContentsUpgrade stackableContentsUpgrade
					&& stackableContentsUpgrade.getMinimumMultiplierRequired() > effectiveMultiplier) {
				errorUpgradeSlots.add(slot);
				errorInventoryParts.add(slot);
			}
		}

		if (!slotsOverMultiplier.isEmpty() || !errorInventoryParts.isEmpty()) {
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("remove.stack_low_multiplier", inventorySlotLimit), errorUpgradeSlots,
					slotsOverMultiplier, errorInventoryParts);
		}

		return UpgradeSlotChangeResult.success();
	}

	@Override
	public UpgradeGroup getUpgradeGroup() {
		return UPGRADE_GROUP;
	}

	public static class Wrapper extends UpgradeWrapperBase<Wrapper, StackUpgradeItem> {
		protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler);
		}

		public double getStackSizeMultiplier() {
			return upgradeItem.getStackSizeMultiplier();
		}

		@Override
		public boolean hideSettingsTab() {
			return true;
		}

		@Override
		public boolean canBeDisabled() {
			return false;
		}
	}
}
