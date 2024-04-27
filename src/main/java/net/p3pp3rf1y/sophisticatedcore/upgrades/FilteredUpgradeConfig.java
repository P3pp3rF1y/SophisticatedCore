package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FilteredUpgradeConfig extends FilteredUpgradeConfigBase {
	public FilteredUpgradeConfig(ModConfigSpec.Builder builder, String name, String path, int defaultFilterSlots, int defaultSlotsInRow) {
		super(builder, name, path, defaultFilterSlots, defaultSlotsInRow);
		builder.pop();
	}
}
