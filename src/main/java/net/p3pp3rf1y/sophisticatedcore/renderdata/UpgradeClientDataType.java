package net.p3pp3rf1y.sophisticatedcore.renderdata;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public class UpgradeClientDataType<T extends IUpgradeClientData> {
	private final String name;
	private final Class<T> clazz;
	private final Codec<T> codec;
	private StreamCodec<? extends ByteBuf, T> streamCodec;

	public UpgradeClientDataType(String name, Class<T> clazz, Codec<T> codec, StreamCodec<? extends ByteBuf, T> streamCodec) {
		this.name = name;
		this.clazz = clazz;
		this.codec = codec;
		this.streamCodec = streamCodec;
	}

	public String getName() {
		return name;
	}

	public Optional<T> cast(IUpgradeClientData upgradeClientData) {
		if (clazz.isInstance(upgradeClientData)) {
			return Optional.of(clazz.cast(upgradeClientData));
		}
		return Optional.empty();
	}

	public Codec<T> codec() {
		return codec;
	}

	public StreamCodec<? extends ByteBuf, T> streamCodec() {
		return streamCodec;
	}
}
