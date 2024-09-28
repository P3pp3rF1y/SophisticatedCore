package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

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

public class BatteryUpgradeItem extends UpgradeItemBase<BatteryUpgradeWrapper> {
	public static final UpgradeType<BatteryUpgradeWrapper> TYPE = new UpgradeType<>(BatteryUpgradeWrapper::new);
	public static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = List.of(new UpgradeConflictDefinition(BatteryUpgradeItem.class::isInstance, 0, TranslationHelper.INSTANCE.translError("add.battery_exists")));

	private final BatteryUpgradeConfig batteryUpgradeConfig;

	public BatteryUpgradeItem(BatteryUpgradeConfig batteryUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.batteryUpgradeConfig = batteryUpgradeConfig;
	}

	public BatteryUpgradeConfig getBatteryUpgradeConfig() {
		return batteryUpgradeConfig;
	}

	@Override
	public UpgradeType<BatteryUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public int getInventoryColumnsTaken() {
		return 2;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return UPGRADE_CONFLICT_DEFINITIONS;
	}

	@Override
	public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide, @Nullable IUpgradeItem<?> upgradeInSlot) {
		int maxEnergyAfter = (int) (getMaxEnergyStored(storageWrapper) / (upgradeInSlot instanceof StackUpgradeItem stackUpgrade ? stackUpgrade.getStackSizeMultiplier() : 1));
		double multiplierRequired = (double) BatteryUpgradeWrapper.getEnergyStored(upgradeStack) / maxEnergyAfter;
		if (multiplierRequired > 1) {
			DecimalFormat multiplierFormat = new DecimalFormat("0.#");
			String formattedMultiplierRequired = multiplierFormat.format(Math.ceil(10 * multiplierRequired) / 10);
			return new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("add.battery_energy_high", formattedMultiplierRequired), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
		}

		return new UpgradeSlotChangeResult.Success();
	}

	public int getMaxEnergyStored(IStorageWrapper storageWrapper) {
		double stackMultiplier = getAdjustedStackMultiplier(storageWrapper);
		int maxEnergyBase = getMaxEnergyBase(storageWrapper);
		return Integer.MAX_VALUE / stackMultiplier < maxEnergyBase ? Integer.MAX_VALUE : (int) (maxEnergyBase * stackMultiplier);
	}

	public double getAdjustedStackMultiplier(IStorageWrapper storageWrapper) {
		return 1 + (batteryUpgradeConfig.stackMultiplierRatio.get() * (storageWrapper.getInventoryHandler().getStackSizeMultiplier() - 1));
	}

	public int getMaxEnergyBase(IStorageWrapper storageWrapper) {
		return batteryUpgradeConfig.energyPerSlotRow.get() * storageWrapper.getNumberOfSlotRows();
	}
}
