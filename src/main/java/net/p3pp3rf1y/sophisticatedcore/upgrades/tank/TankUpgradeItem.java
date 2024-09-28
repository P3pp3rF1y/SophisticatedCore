package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

public class TankUpgradeItem extends UpgradeItemBase<TankUpgradeWrapper> {
	public static final UpgradeType<TankUpgradeWrapper> TYPE = new UpgradeType<>(TankUpgradeWrapper::new);
	public static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = List.of(new UpgradeConflictDefinition(TankUpgradeItem.class::isInstance, 1, TranslationHelper.INSTANCE.translError("add.two_tank_upgrades_present")));

	private final TankUpgradeConfig tankUpgradeConfig;

	public TankUpgradeItem(TankUpgradeConfig tankUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.tankUpgradeConfig = tankUpgradeConfig;
	}

	public int getBaseCapacity(IStorageWrapper storageWrapper) {
		return tankUpgradeConfig.capacityPerSlotRow.get() * storageWrapper.getNumberOfSlotRows();
	}

	public double getAdjustedStackMultiplier(IStorageWrapper storageWrapper) {
		return 1 + (tankUpgradeConfig.stackMultiplierRatio.get() * (storageWrapper.getInventoryHandler().getStackSizeMultiplier() - 1));
	}

	public int getTankCapacity(IStorageWrapper storageWrapper) {
		double stackMultiplier = getAdjustedStackMultiplier(storageWrapper);
		int baseCapacity = getBaseCapacity(storageWrapper);
		return Integer.MAX_VALUE / stackMultiplier < baseCapacity ? Integer.MAX_VALUE : (int) (baseCapacity * stackMultiplier);
	}

	public TankUpgradeConfig getTankUpgradeConfig() {
		return tankUpgradeConfig;
	}

	@Override
	public UpgradeType<TankUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide, @Nullable IUpgradeItem<?> upgradeInSlot) {
		int capacityAfter = (int) (getTankCapacity(storageWrapper) / (upgradeInSlot instanceof StackUpgradeItem stackUpgrade ? stackUpgrade.getStackSizeMultiplier() : 1));
		double multiplierRequired = (double) TankUpgradeWrapper.getContents(upgradeStack).getAmount() / capacityAfter;
		if (multiplierRequired > 1) {
			DecimalFormat multiplierFormat = new DecimalFormat("0.#");
			String formattedMultiplierRequired = multiplierFormat.format(Math.ceil(10 * multiplierRequired) / 10);
			return new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("add.tank_capacity_high", formattedMultiplierRequired), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
		}

		return new UpgradeSlotChangeResult.Success();
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return UPGRADE_CONFLICT_DEFINITIONS;
	}

	@Override
	public int getInventoryColumnsTaken() {
		return 2;
	}
}
