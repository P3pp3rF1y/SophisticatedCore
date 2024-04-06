package net.p3pp3rf1y.sophisticatedcore.upgrades.magnet;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;
import java.util.function.IntSupplier;

public class MagnetUpgradeItem extends UpgradeItemBase<MagnetUpgradeWrapper> {
	public static final UpgradeType<MagnetUpgradeWrapper> TYPE = new UpgradeType<>(MagnetUpgradeWrapper::new);
	private final IntSupplier radius;
	private final IntSupplier filterSlotCount;

	public MagnetUpgradeItem(IntSupplier radius, IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.radius = radius;
		this.filterSlotCount = filterSlotCount;
	}

	@Override
	public UpgradeType<MagnetUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}

	public int getRadius() {
		return radius.getAsInt();
	}
}
