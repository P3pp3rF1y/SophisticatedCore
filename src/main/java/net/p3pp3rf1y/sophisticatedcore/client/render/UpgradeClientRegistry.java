package net.p3pp3rf1y.sophisticatedcore.client.render;

import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeClientTickHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeClientDataType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeClientTickHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeClientTickHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UpgradeClientRegistry {
	private UpgradeClientRegistry() {
	}

	private static final Map<UpgradeClientDataType<?>, IUpgradeClientTickHandler<?>> UPGRADE_RENDERERS = new HashMap<>();

	private static <T extends IUpgradeClientData> void registerUpgradeRenderer(UpgradeClientDataType<T> upgradeClientDataType,
			IUpgradeClientTickHandler<T> upgradeRenderer) {
		UPGRADE_RENDERERS.put(upgradeClientDataType, upgradeRenderer);
	}

	static {
		registerUpgradeRenderer(CookingUpgradeClientData.TYPE, new CookingUpgradeClientTickHandler());
		registerUpgradeRenderer(JukeboxUpgradeClientData.TYPE, new JukeboxUpgradeClientTickHandler());
	}

	public static <T extends IUpgradeClientData> Optional<IUpgradeClientTickHandler<T>> getUpgradeClientTickHandler(
			UpgradeClientDataType<T> upgradeClientDataType) {
		// noinspection unchecked
		return Optional.ofNullable((IUpgradeClientTickHandler<T>) UPGRADE_RENDERERS.get(upgradeClientDataType));
	}
}
