package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderDataValidator;

public class CookingUpgradeRenderDataValidator implements IUpgradeRenderDataValidator<CookingUpgradeClientData> {
	@Override
	public boolean isValid(IStorageWrapper storageWrapper, Level level, CookingUpgradeClientData upgradeClientData) {
		return upgradeClientData.burning() && storageWrapper.getUpgradeHandler().getWrappersThatImplement(ICookingUpgrade.class).stream()
				.anyMatch(wrapper -> wrapper.getCookingLogic().getBurnTimeFinish() >= level.getGameTime());
	}
}
