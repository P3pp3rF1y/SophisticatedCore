package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;

public class SmeltingUpgradeItem extends UpgradeItemBase<CookingUpgradeWrapper.SmeltingUpgradeWrapper> implements ICookingUpgradeItem {
	public static final UpgradeType<CookingUpgradeWrapper.SmeltingUpgradeWrapper> TYPE = new UpgradeType<>(CookingUpgradeWrapper.SmeltingUpgradeWrapper::new);

	private final CookingUpgradeConfig smeltingUpgradeConfig;

	public SmeltingUpgradeItem(CookingUpgradeConfig smeltingUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
        super(upgradeTypeLimitConfig);
		this.smeltingUpgradeConfig = smeltingUpgradeConfig;
	}

	@Override
	public UpgradeType<CookingUpgradeWrapper.SmeltingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	@Override
	public CookingUpgradeConfig getCookingUpgradeConfig() {
		return smeltingUpgradeConfig;
	}

	@Override
	public UpgradeGroup getUpgradeGroup() {
		return ICookingUpgrade.UPGRADE_GROUP;
	}
}
