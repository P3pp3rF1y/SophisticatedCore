package net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;

import java.util.List;

public abstract class BlockConverterUpgradeItem<U extends BlockConverterUpgradeItem<U, W>, W extends BlockConverterUpgradeWrapper<U, W>>
		extends
			UpgradeItemBase<W> {
	public BlockConverterUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}
}
