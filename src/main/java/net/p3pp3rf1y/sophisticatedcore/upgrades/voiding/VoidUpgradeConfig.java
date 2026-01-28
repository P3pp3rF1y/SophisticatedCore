package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraftforge.common.ForgeConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilteredUpgradeConfigBase;

public class VoidUpgradeConfig extends FilteredUpgradeConfigBase {
	public final ForgeConfigSpec.BooleanValue voidAlwaysEnabled;

	public VoidUpgradeConfig(ForgeConfigSpec.Builder builder, String name, String path, int defaultFilterSlots, int defaultSlotsInRow) {
		super(builder, name, path, defaultFilterSlots, defaultSlotsInRow);

		voidAlwaysEnabled = builder.comment("Determines whether void upgrade allows voiding always or it only has overflow options").define("voidAlwaysEnabled", true);

		builder.pop();
	}
}
