package net.p3pp3rf1y.sophisticatedcore.upgrades.feeding;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Map;

public enum HungerLevel implements StringRepresentable {
	ANY("any"),
	HALF("half"),
	FULL("full");

	public static final Codec<HungerLevel> CODEC = StringRepresentable.fromEnum(HungerLevel::values);
	public static final StreamCodec<FriendlyByteBuf, HungerLevel> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(HungerLevel.class);

	private final String name;

	HungerLevel(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public HungerLevel next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	private static final Map<String, HungerLevel> NAME_VALUES;
	private static final HungerLevel[] VALUES;

	static {
		ImmutableMap.Builder<String, HungerLevel> builder = new ImmutableMap.Builder<>();
		for (HungerLevel value : HungerLevel.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public static HungerLevel fromName(String name) {
		return NAME_VALUES.getOrDefault(name, HALF);
	}
}
