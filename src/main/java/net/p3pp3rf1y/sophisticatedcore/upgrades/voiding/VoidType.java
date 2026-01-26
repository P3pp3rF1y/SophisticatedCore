package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Map;

public enum VoidType implements StringRepresentable {
	ALWAYS("always"),
	SLOT_OVERFLOW("slot_overflow"),
	STORAGE_OVERFLOW("storage_overflow");

	private final String name;
	VoidType(String name) {
		this.name = name;
	}

	public static final Codec<VoidType> CODEC = StringRepresentable.fromEnum(VoidType::values);
	public static final StreamCodec<FriendlyByteBuf, VoidType> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(VoidType.class);

	@Override
	public String getSerializedName() {
		return name;
	}

	public VoidType next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	private static final Map<String, VoidType> NAME_VALUES;
	private static final VoidType[] VALUES;

	static {
		ImmutableMap.Builder<String, VoidType> builder = new ImmutableMap.Builder<>();
		for (VoidType value : VoidType.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public static VoidType fromName(String name) {
		return NAME_VALUES.getOrDefault(name, ALWAYS);
	}
}
