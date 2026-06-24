package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Map;

public enum RepeatMode implements StringRepresentable {
	ALL("all"), ONE("one"), NO("no");

	public static final Codec<RepeatMode> CODEC = StringRepresentable.fromEnum(RepeatMode::values);
	public static final StreamCodec<FriendlyByteBuf, RepeatMode> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(RepeatMode.class);

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
		for (RepeatMode value : values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public static RepeatMode fromName(String name) {
		return NAME_VALUES.getOrDefault(name, NO);
	}
}
