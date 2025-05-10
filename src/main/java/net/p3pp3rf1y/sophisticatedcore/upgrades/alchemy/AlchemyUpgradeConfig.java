package net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AlchemyUpgradeConfig {
	public final ModConfigSpec.IntValue filterSlots;

	public AlchemyUpgradeConfig(ModConfigSpec.Builder builder, String name, String path, int defaultFilterSlots) {
		builder.comment(name + " Settings").push(path);
		filterSlots = builder.comment("Number of " + name + "'s filter slots").defineInRange("filterSlots", defaultFilterSlots, 1, 20);
		builder.pop();
	}
}
