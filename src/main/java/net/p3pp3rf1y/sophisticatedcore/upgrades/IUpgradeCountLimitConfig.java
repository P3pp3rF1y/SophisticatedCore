package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public interface IUpgradeCountLimitConfig {
	int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeRegistryName);

	int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup);
}
