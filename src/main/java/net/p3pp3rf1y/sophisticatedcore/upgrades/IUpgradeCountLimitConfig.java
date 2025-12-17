package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public interface IUpgradeCountLimitConfig {
	int getMaxUpgradesPerStorage(String storageType, @Nullable Identifier upgradeRegistryName);

	int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup);
}
