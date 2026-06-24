package net.p3pp3rf1y.sophisticatedcore.renderdata;

import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderDataValidator;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeRenderDataValidator;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeRenderDataValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UpgradeRenderDataValidatorRegistry {
	private UpgradeRenderDataValidatorRegistry() {
	}

	private static final Map<UpgradeClientDataType<?>, IUpgradeRenderDataValidator<?>> VALIDATORS = new HashMap<>();

	private static <T extends IUpgradeClientData> void registerValidator(UpgradeClientDataType<T> upgradeClientDataType,
			IUpgradeRenderDataValidator<T> validator) {
		VALIDATORS.put(upgradeClientDataType, validator);
	}

	static {
		registerValidator(CookingUpgradeClientData.TYPE, new CookingUpgradeRenderDataValidator());
		registerValidator(JukeboxUpgradeClientData.TYPE, new JukeboxUpgradeRenderDataValidator());
	}

	public static <T extends IUpgradeClientData> Optional<IUpgradeRenderDataValidator<T>> getValidator(UpgradeClientDataType<T> upgradeClientDataType) {
		// noinspection unchecked
		return Optional.ofNullable((IUpgradeRenderDataValidator<T>) VALIDATORS.get(upgradeClientDataType));
	}
}
