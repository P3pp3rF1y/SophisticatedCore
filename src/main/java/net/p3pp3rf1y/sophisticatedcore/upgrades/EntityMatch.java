package net.p3pp3rf1y.sophisticatedcore.upgrades;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Locale;
import java.util.Map;

public enum EntityMatch implements StringRepresentable {
	PLAYERS,
	PLAYERS_AND_ENTITIES,
	ENTITIES;

	public static final Codec<EntityMatch> CODEC = StringRepresentable.fromEnum(EntityMatch::values);
	public static final StreamCodec<FriendlyByteBuf, EntityMatch> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(EntityMatch.class);

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	private static final Map<String, EntityMatch> NAME_VALUES;
	private static final EntityMatch[] VALUES;

	static {
		ImmutableMap.Builder<String, EntityMatch> builder = new ImmutableMap.Builder<>();
		for (EntityMatch value : EntityMatch.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public EntityMatch next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static EntityMatch fromName(String name) {
		return NAME_VALUES.getOrDefault(name, PLAYERS_AND_ENTITIES);
	}
}