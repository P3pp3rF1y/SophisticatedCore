package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.StringRepresentable;

import java.util.Map;

public enum CraftingRefillType implements StringRepresentable {
	DisableRefill("disable_refill_crafting_grid"),
	RefillFromStorage("refill_crafting_grid_from_storage"),
	RefillFromPlayer("refill_crafting_grid_from_player"),
	RefillFromPlayerThenStorage("refill_crafting_grid_from_player_then_storage"),
	RefillFromStorageThenPlayer("refill_crafting_grid_from_storage_then_player");

	private final String name;

	CraftingRefillType(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public CraftingRefillType next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	private static final Map<String, CraftingRefillType> NAME_VALUES;
	private static final CraftingRefillType[] VALUES;

	static {
		ImmutableMap.Builder<String, CraftingRefillType> builder = new ImmutableMap.Builder<>();
		for (CraftingRefillType value : values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public static CraftingRefillType fromName(String name) {
		return NAME_VALUES.getOrDefault(name, RefillFromStorageThenPlayer);
	}
}
