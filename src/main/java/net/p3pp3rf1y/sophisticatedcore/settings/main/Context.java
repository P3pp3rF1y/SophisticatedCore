package net.p3pp3rf1y.sophisticatedcore.settings.main;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

public enum Context implements StringRepresentable {
	PLAYER("player"), CONTAINER("container");

	public static final Codec<Context> CODEC = StringRepresentable.fromEnum(Context::values);
	public static final StreamCodec<FriendlyByteBuf, Context> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Context.class);

	private final String name;

	Context(String name) {
		this.name = name;
	}

	public static Context fromName(String name) {
		if (PLAYER.name.equals(name)) {
			return PLAYER;
		} else {
			return CONTAINER;
		}
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
