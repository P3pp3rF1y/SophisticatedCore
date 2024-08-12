package net.p3pp3rf1y.sophisticatedcore.common.gui;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Map;

public enum SortBy implements StringRepresentable {
	NAME("name"),
	MOD("mod"),
	COUNT("count"),
	TAGS("tags");

	public static final Codec<SortBy> CODEC = StringRepresentable.fromEnum(SortBy::values);
	public static final StreamCodec<FriendlyByteBuf, SortBy> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(SortBy.class);

	private final String name;

	SortBy(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public SortBy next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	private static final Map<String, SortBy> NAME_VALUES;
	private static final SortBy[] VALUES;

	static {
		ImmutableMap.Builder<String, SortBy> builder = new ImmutableMap.Builder<>();
		for (SortBy value : SortBy.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public static SortBy fromName(String name) {
		return NAME_VALUES.getOrDefault(name, NAME);
	}
}
