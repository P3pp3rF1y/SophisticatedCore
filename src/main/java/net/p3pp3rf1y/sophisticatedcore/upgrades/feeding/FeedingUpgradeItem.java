package net.p3pp3rf1y.sophisticatedcore.upgrades.feeding;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;
import java.util.function.IntSupplier;

public class FeedingUpgradeItem extends UpgradeItemBase<FeedingUpgradeWrapper> {
	public static final UpgradeType<FeedingUpgradeWrapper> TYPE = new UpgradeType<>(FeedingUpgradeWrapper::new);

	private final IntSupplier filterSlotCount;

	public FeedingUpgradeItem(IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.filterSlotCount = filterSlotCount;
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}

	@Override
	public UpgradeType<FeedingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}
}
