package net.p3pp3rf1y.sophisticatedcore.upgrades.stack;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class StackUpgradeItem extends UpgradeItemBase<StackUpgradeItem.Wrapper> {
	public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
	public static final UpgradeGroup UPGRADE_GROUP = new UpgradeGroup("stack_upgrades", TranslationHelper.INSTANCE.translUpgradeGroup("stack_upgrades"));
	private final double stackSizeMultiplier;

	public StackUpgradeItem(double stackSizeMultiplier, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.stackSizeMultiplier = stackSizeMultiplier;
	}

	public static int getInventorySlotLimit(IStorageWrapper storageWrapper) {
		double multiplier = storageWrapper.getBaseStackSizeMultiplier();

		for (Wrapper stackWrapper : storageWrapper.getUpgradeHandler().getTypeWrappers(TYPE)) {
			if (Integer.MAX_VALUE / stackWrapper.getStackSizeMultiplier() < multiplier) {
				return Integer.MAX_VALUE;
			}
			multiplier *= stackWrapper.getStackSizeMultiplier();
		}

		return Integer.MAX_VALUE / 64D < multiplier ? Integer.MAX_VALUE : (int) (multiplier * 64);
	}

	@Override
	public UpgradeType<Wrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	double getStackSizeMultiplier() {
		return stackSizeMultiplier;
	}

	@Override
	public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide) {
		if (isClientSide) {
			return new UpgradeSlotChangeResult.Success();
		}

		double currentInventoryMultiplier = getInventorySlotLimit(storageWrapper) / 64D;
		double multiplierWhenRemoved = currentInventoryMultiplier / stackSizeMultiplier;
		return isMultiplierHighEnough(storageWrapper, multiplierWhenRemoved);
	}

	@Override
	public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, int upgradeSlot, IStorageWrapper storageWrapper, boolean isClientSide) {
		UpgradeSlotChangeResult result = super.canSwapUpgradeFor(upgradeStackToPut, upgradeSlot, storageWrapper, isClientSide);
		if (!result.isSuccessful()) {
			return result;
		}

		if (isClientSide) {
			return new UpgradeSlotChangeResult.Success();
		}

		if (!(upgradeStackToPut.getItem() instanceof StackUpgradeItem otherStackUpgradeItem)) {
			return canRemoveUpgradeFrom(storageWrapper, isClientSide);
		}

		if (otherStackUpgradeItem.stackSizeMultiplier >= stackSizeMultiplier) {
			return new UpgradeSlotChangeResult.Success();
		}

		int currentInventoryMultiplier = getInventorySlotLimit(storageWrapper) / 64;
		double multiplierWhenRemoved = currentInventoryMultiplier / stackSizeMultiplier;

		return isMultiplierHighEnough(storageWrapper, multiplierWhenRemoved * otherStackUpgradeItem.stackSizeMultiplier);
	}

	private UpgradeSlotChangeResult isMultiplierHighEnough(IStorageWrapper storageWrapper, double multiplier) {
		Set<Integer> slotsOverMultiplier = new HashSet<>();

		for (int slot = 0; slot < storageWrapper.getInventoryHandler().getSlots(); slot++) {
			ItemStack stack = storageWrapper.getInventoryHandler().getSlotStack(slot);
			double stackMultiplierNeeded = (double) stack.getCount() / stack.getMaxStackSize();
			if (stackMultiplierNeeded > multiplier) {
				slotsOverMultiplier.add(slot);
			}
		}

		Set<Integer> errorInventoryParts = new HashSet<>();

		storageWrapper.getUpgradeHandler().getSlotWrappers().forEach((slot, wrapper) -> {
			if (wrapper instanceof IStackableContentsUpgrade stackableContentsUpgrade && stackableContentsUpgrade.getMinimumMultiplierRequired() > multiplier) {
				errorInventoryParts.add(slot);
			}
		});

		if (!slotsOverMultiplier.isEmpty() || !errorInventoryParts.isEmpty()) {
			return new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("remove.stack_low_multiplier", multiplier), Collections.emptySet(), slotsOverMultiplier, errorInventoryParts);
		}

		return new UpgradeSlotChangeResult.Success();
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
