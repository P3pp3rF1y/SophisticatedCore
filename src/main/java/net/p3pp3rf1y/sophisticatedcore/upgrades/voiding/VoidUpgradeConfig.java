package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilteredUpgradeConfigBase;

public class VoidUpgradeConfig extends FilteredUpgradeConfigBase {
	public final ModConfigSpec.BooleanValue voidAnythingEnabled;

	public VoidUpgradeConfig(ModConfigSpec.Builder builder, String name, String path, int defaultFilterSlots, int defaultSlotsInRow) {
		super(builder, name, path, defaultFilterSlots, defaultSlotsInRow);

		voidAnythingEnabled = builder.comment("Determines whether void upgrade allows voiding anything or it only has overflow option").define("voidAnythingEnabled", true);

		builder.pop();
	}
}
