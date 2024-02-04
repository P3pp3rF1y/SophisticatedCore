package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class BlastingUpgradeItem extends UpgradeItemBase<CookingUpgradeWrapper.BlastingUpgradeWrapper> implements ICookingUpgradeItem {
	public static final UpgradeType<CookingUpgradeWrapper.BlastingUpgradeWrapper> TYPE = new UpgradeType<>(CookingUpgradeWrapper.BlastingUpgradeWrapper::new);
	private final CookingUpgradeConfig blastingUpgradeConfig;

	public BlastingUpgradeItem(CookingUpgradeConfig blastingUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
        super(upgradeTypeLimitConfig);
		this.blastingUpgradeConfig = blastingUpgradeConfig;
	}

	@Override
	public UpgradeType<CookingUpgradeWrapper.BlastingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public CookingUpgradeConfig getCookingUpgradeConfig() {
		return blastingUpgradeConfig;
	}

	@Override
	public UpgradeGroup getUpgradeGroup() {
		return ICookingUpgrade.UPGRADE_GROUP;
	}
}
