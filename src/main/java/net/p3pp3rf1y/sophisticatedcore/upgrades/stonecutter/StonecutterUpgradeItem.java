package net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter;

import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class StonecutterUpgradeItem extends UpgradeItemBase<StonecutterUpgradeWrapper> {
	private static final UpgradeType<StonecutterUpgradeWrapper> TYPE = new UpgradeType<>(StonecutterUpgradeWrapper::new);

	public StonecutterUpgradeItem() {super();}

	@Override
	public UpgradeType<StonecutterUpgradeWrapper> getType() {
		return TYPE;
	}
}
