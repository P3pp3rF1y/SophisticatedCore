package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class SmokingUpgradeItem extends UpgradeItemBase<CookingUpgradeWrapper.SmokingUpgradeWrapper> implements ICookingUpgradeItem {
	public static final UpgradeType<CookingUpgradeWrapper.SmokingUpgradeWrapper> TYPE = new UpgradeType<>(CookingUpgradeWrapper.SmokingUpgradeWrapper::new);
	private final CookingUpgradeConfig smokingUpgradeConfig;

	public SmokingUpgradeItem(CookingUpgradeConfig smokingUpgradeConfig) {super();
		this.smokingUpgradeConfig = smokingUpgradeConfig;
	}

	@Override
	public UpgradeType<CookingUpgradeWrapper.SmokingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public CookingUpgradeConfig getCookingUpgradeConfig() {
		return smokingUpgradeConfig;
	}
}
