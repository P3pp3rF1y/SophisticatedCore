package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeRenderData;

public interface IUpgradeRenderDataValidator<T extends IUpgradeRenderData> {
	boolean isValid(IStorageWrapper storageWrapper, Level level, T upgradeRenderData);
}
