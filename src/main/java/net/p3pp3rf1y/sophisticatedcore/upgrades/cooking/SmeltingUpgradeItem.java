package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class SmeltingUpgradeItem extends UpgradeItemBase<CookingUpgradeWrapper.SmeltingUpgradeWrapper> implements ICookingUpgradeItem {
	public static final UpgradeType<CookingUpgradeWrapper.SmeltingUpgradeWrapper> TYPE = new UpgradeType<>(CookingUpgradeWrapper.SmeltingUpgradeWrapper::new);

	private final CookingUpgradeConfig smeltingUpgradeConfig;

	public SmeltingUpgradeItem(CookingUpgradeConfig smeltingUpgradeConfig) {super();
		this.smeltingUpgradeConfig = smeltingUpgradeConfig;
	}

	@Override
	public UpgradeType<CookingUpgradeWrapper.SmeltingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public CookingUpgradeConfig getCookingUpgradeConfig() {
		return smeltingUpgradeConfig;
	}
}
