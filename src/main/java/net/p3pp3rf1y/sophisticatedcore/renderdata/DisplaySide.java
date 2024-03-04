package net.p3pp3rf1y.sophisticatedcore.renderdata;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.StringRepresentable;

import java.util.Map;

public enum DisplaySide implements StringRepresentable {
	FRONT("front"), LEFT("left"), RIGHT("right");

	private final String name;

	DisplaySide(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public DisplaySide next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public DisplaySide previous() {
		return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
	}

	private static final Map<String, DisplaySide> NAME_VALUES;
	private static final DisplaySide[] VALUES;

	static {
		ImmutableMap.Builder<String, DisplaySide> builder = new ImmutableMap.Builder<>();
		for (DisplaySide value : values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public static DisplaySide fromName(String name) {
		return NAME_VALUES.getOrDefault(name, FRONT);
	}
}
