package net.p3pp3rf1y.sophisticatedcore.renderdata;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Locale;

public enum TankPosition implements StringRepresentable {
	LEFT, RIGHT;

	public static final Codec<TankPosition> CODEC = StringRepresentable.fromEnum(TankPosition::values);
	public static final StreamCodec<FriendlyByteBuf, TankPosition> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(TankPosition.class);

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}
}
