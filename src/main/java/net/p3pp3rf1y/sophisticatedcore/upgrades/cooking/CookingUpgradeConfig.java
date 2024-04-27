package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CookingUpgradeConfig {
	public final ModConfigSpec.DoubleValue cookingSpeedMultiplier;
	public final ModConfigSpec.DoubleValue fuelEfficiencyMultiplier;

	public CookingUpgradeConfig(ModConfigSpec.Builder builder, final String upgradeName, String path) {
		builder.comment(upgradeName + " Settings").push(path);
		cookingSpeedMultiplier = builder.comment("Smelting speed multiplier (1.0 equals speed at which vanilla furnace smelts items)")
				.defineInRange("smeltingSpeedMultiplier", 1.0D, 0.25D, 4.0D);
		fuelEfficiencyMultiplier = builder.comment("Fuel efficiency multiplier (1.0 equals speed at which it's used in vanilla furnace)")
				.defineInRange("fuelEfficiencyMultiplier", 1.0D, 0.25D, 4.0D);
	}

	public static CookingUpgradeConfig getInstance(ModConfigSpec.Builder builder, final String upgradeName, String path) {
		CookingUpgradeConfig instance = new CookingUpgradeConfig(builder, upgradeName, path);
		builder.pop();
		return instance;
	}
}
