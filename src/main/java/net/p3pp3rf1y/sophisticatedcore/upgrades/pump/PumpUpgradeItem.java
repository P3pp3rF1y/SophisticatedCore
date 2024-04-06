package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;

public class PumpUpgradeItem extends UpgradeItemBase<PumpUpgradeWrapper> {
	private static final UpgradeType<PumpUpgradeWrapper> TYPE = new UpgradeType<>(PumpUpgradeWrapper::new);
	private final boolean interactWithHandDefault;
	private final boolean interactWithWorldDefault;
	private final PumpUpgradeConfig pumpUpgradeConfig;

	public PumpUpgradeItem(boolean interactWithHandDefault, boolean interactWithWorldDefault, PumpUpgradeConfig pumpUpgradeConfig, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.interactWithHandDefault = interactWithHandDefault;
		this.interactWithWorldDefault = interactWithWorldDefault;
		this.pumpUpgradeConfig = pumpUpgradeConfig;
	}

	@Override
	public UpgradeType<PumpUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	public boolean getInteractWithHandDefault() {
		return interactWithHandDefault;
	}

	public boolean getInteractWithWorldDefault() {
		return interactWithWorldDefault;
	}

	public PumpUpgradeConfig getPumpUpgradeConfig() {
		return pumpUpgradeConfig;
	}
}
