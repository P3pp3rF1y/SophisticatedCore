package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;

public interface IUpgradeRenderDataValidator<T extends IUpgradeClientData> {
	boolean isValid(IStorageWrapper storageWrapper, Level level, T upgradeClientData);
}
