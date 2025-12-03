package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.neoforged.neoforge.common.ModConfigSpec;

public class JukeboxUpgradeConfig {
	public final ModConfigSpec.IntValue numberOfSlots;
	public final ModConfigSpec.IntValue slotsInRow;

	public JukeboxUpgradeConfig(ModConfigSpec.Builder builder, String upgradeName, String path, int defaultNumberOfSlots) {
		builder.comment(upgradeName + " Settings").push(path);
		numberOfSlots = builder.comment("Number of slots for discs in jukebox upgrade").defineInRange("size", defaultNumberOfSlots, 1, 16);
		slotsInRow = builder.comment("Number of lots displayed in a row").defineInRange("slotsInRow", 4, 1, 6);

		builder.pop();
	}
}
