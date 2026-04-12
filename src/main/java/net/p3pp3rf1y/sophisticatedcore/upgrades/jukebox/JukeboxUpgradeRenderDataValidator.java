package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderDataValidator;

public class JukeboxUpgradeRenderDataValidator implements IUpgradeRenderDataValidator<JukeboxUpgradeRenderData> {
	@Override
	public boolean isValid(IStorageWrapper storageWrapper, Level level, JukeboxUpgradeRenderData upgradeRenderData) {
		return upgradeRenderData.isPlaying()
				&& storageWrapper.getUpgradeHandler().getTypeWrappers(JukeboxUpgradeItem.TYPE).stream().anyMatch(JukeboxUpgradeWrapper::isPlaying);
	}
}
