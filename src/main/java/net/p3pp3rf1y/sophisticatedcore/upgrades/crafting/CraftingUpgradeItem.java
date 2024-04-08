package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;

public class CraftingUpgradeItem extends UpgradeItemBase<CraftingUpgradeWrapper> {
	private static final UpgradeType<CraftingUpgradeWrapper> TYPE = new UpgradeType<>(CraftingUpgradeWrapper::new);

	public CraftingUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
	}

	@Override
	public UpgradeType<CraftingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}
}
