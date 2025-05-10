package net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy;

import net.minecraftforge.common.ForgeConfigSpec;

public class AlchemyUpgradeConfig {
	public final ForgeConfigSpec.IntValue filterSlots;

	public AlchemyUpgradeConfig(ForgeConfigSpec.Builder builder, String name, String path, int defaultFilterSlots) {
		builder.comment(name + " Settings").push(path);
		filterSlots = builder.comment("Number of " + name + "'s filter slots").defineInRange("filterSlots", defaultFilterSlots, 1, 20);
		builder.pop();
	}
}
