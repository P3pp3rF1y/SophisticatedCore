package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;

public class AdvancedCraftingUpgradeItem extends UpgradeItemBase<AdvancedCraftingUpgradeWrapper> {
	private static final UpgradeType<AdvancedCraftingUpgradeWrapper> TYPE = new UpgradeType<>(AdvancedCraftingUpgradeWrapper::new);

	public AdvancedCraftingUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
	}

	@Override
	public UpgradeType<AdvancedCraftingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}
}
