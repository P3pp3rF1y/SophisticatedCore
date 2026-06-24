package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.StringRepresentable;

import java.util.Map;

public enum RepeatMode implements StringRepresentable {
	ALL("all"), ONE("one"), NO("no");

	private final String name;

	RepeatMode(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public RepeatMode next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	private static final Map<String, RepeatMode> NAME_VALUES;
	private static final RepeatMode[] VALUES;

	static {
		ImmutableMap.Builder<String, RepeatMode> builder = new ImmutableMap.Builder<>();
		for (RepeatMode value : RepeatMode.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public static RepeatMode fromName(String name) {
		return NAME_VALUES.getOrDefault(name, NO);
	}
}
