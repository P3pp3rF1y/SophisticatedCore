package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AutoCookingUpgradeConfig extends CookingUpgradeConfig {
	public final ModConfigSpec.IntValue inputFilterSlots;
	public final ModConfigSpec.IntValue inputFilterSlotsInRow;
	public final ModConfigSpec.IntValue fuelFilterSlots;
	public final ModConfigSpec.IntValue fuelFilterSlotsInRow;

	public AutoCookingUpgradeConfig(ModConfigSpec.Builder builder, String upgradeName, String path) {
		super(builder, upgradeName, path);
		inputFilterSlots = builder.comment("Number of input filter slots").defineInRange("inputFilterSlots", 8, 1, 20);
		inputFilterSlotsInRow = builder.comment("Number of input filter slots displayed in a row").defineInRange("inputFilterSlotsInRow", 4, 1, 6);
		fuelFilterSlots = builder.comment("Number of fuel filter slots").defineInRange("fuelFilterSlots", 4, 1, 20);
		fuelFilterSlotsInRow = builder.comment("Number of fuel filter slots displayed in a row").defineInRange("fuelFilterSlotsInRow", 4, 1, 6);
		builder.pop();
	}
}
