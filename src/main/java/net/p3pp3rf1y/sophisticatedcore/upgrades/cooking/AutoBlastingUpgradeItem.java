package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class AutoBlastingUpgradeItem extends UpgradeItemBase<AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper> implements IAutoCookingUpgradeItem {
	public static final UpgradeType<AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper> TYPE = new UpgradeType<>(AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper::new);
	private final AutoCookingUpgradeConfig autoBlastingUpgradeConfig;

	public AutoBlastingUpgradeItem(AutoCookingUpgradeConfig autoBlastingUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
        super(upgradeTypeLimitConfig);
        this.autoBlastingUpgradeConfig = autoBlastingUpgradeConfig;
	}

	@Override
	public UpgradeType<AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public AutoCookingUpgradeConfig getAutoCookingUpgradeConfig() {
		return autoBlastingUpgradeConfig;
	}

	@Override
	public UpgradeGroup getUpgradeGroup() {
		return ICookingUpgrade.UPGRADE_GROUP;
	}
}
