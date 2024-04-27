package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FilteredUpgradeConfigBase {
	public final ModConfigSpec.IntValue filterSlots;
	public final ModConfigSpec.IntValue slotsInRow;

	protected FilteredUpgradeConfigBase(ModConfigSpec.Builder builder, String name, String path, int defaultFilterSlots, int defaultSlotsInRow) {
		builder.comment(name + " Settings").push(path);
		filterSlots = builder.comment("Number of " + name + "'s filter slots").defineInRange("filterSlots", defaultFilterSlots, 1, 20);
		slotsInRow = builder.comment("Number of filter slots displayed in a row").defineInRange("slotsInRow", defaultSlotsInRow, 1, 6);
	}
}
