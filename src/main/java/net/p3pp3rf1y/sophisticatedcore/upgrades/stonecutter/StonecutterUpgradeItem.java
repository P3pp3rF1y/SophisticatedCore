package net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;

public class StonecutterUpgradeItem extends UpgradeItemBase<StonecutterUpgradeWrapper> {
	private static final UpgradeType<StonecutterUpgradeWrapper> TYPE = new UpgradeType<>(StonecutterUpgradeWrapper::new);

	public StonecutterUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
        super(upgradeTypeLimitConfig);
    }

	@Override
	public UpgradeType<StonecutterUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}
}
