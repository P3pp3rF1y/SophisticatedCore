package net.p3pp3rf1y.sophisticatedcore.renderdata;

import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderDataValidator;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeRenderData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeRenderDataValidator;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeRenderData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeRenderDataValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UpgradeRenderDataValidatorRegistry {
	private static final Map<UpgradeRenderDataType<?>, IUpgradeRenderDataValidator<?>> VALIDATORS = new HashMap<>();

	private UpgradeRenderDataValidatorRegistry() {
	}

	static {
		registerValidator(CookingUpgradeRenderData.TYPE, new CookingUpgradeRenderDataValidator());
		registerValidator(JukeboxUpgradeRenderData.TYPE, new JukeboxUpgradeRenderDataValidator());
	}

	private static <T extends IUpgradeRenderData> void registerValidator(UpgradeRenderDataType<T> upgradeRenderDataType, IUpgradeRenderDataValidator<T> validator) {
		VALIDATORS.put(upgradeRenderDataType, validator);
	}

	public static <T extends IUpgradeRenderData> Optional<IUpgradeRenderDataValidator<T>> getValidator(UpgradeRenderDataType<T> upgradeRenderDataType) {
		//noinspection unchecked
		return Optional.ofNullable((IUpgradeRenderDataValidator<T>) VALIDATORS.get(upgradeRenderDataType));
	}
}
