package net.p3pp3rf1y.sophisticatedcore.upgrades.filter;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;
import java.util.function.IntSupplier;

public class FilterUpgradeItem extends UpgradeItemBase<FilterUpgradeWrapper> {
	public static final UpgradeType<FilterUpgradeWrapper> TYPE = new UpgradeType<>(FilterUpgradeWrapper::new);
	private final IntSupplier filterSlotCount;

	public FilterUpgradeItem(IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.filterSlotCount = filterSlotCount;
	}

	@Override
	public UpgradeType<FilterUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}
}
