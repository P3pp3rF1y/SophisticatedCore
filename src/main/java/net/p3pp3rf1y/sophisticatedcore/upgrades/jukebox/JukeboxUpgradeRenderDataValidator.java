package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderDataValidator;

public class JukeboxUpgradeRenderDataValidator implements IUpgradeRenderDataValidator<JukeboxUpgradeClientData> {
	@Override
	public boolean isValid(IStorageWrapper storageWrapper, Level level, JukeboxUpgradeClientData upgradeClientData) {
		return upgradeClientData.playing()
				&& storageWrapper.getUpgradeHandler().getTypeWrappers(JukeboxUpgradeItem.TYPE).stream().anyMatch(JukeboxUpgradeWrapper::isPlaying);
	}
}
