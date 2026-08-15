package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PacketBufferHelper {
	private PacketBufferHelper() {
	}

	public static <T> List<T> readList(FriendlyByteBuf buffer, FriendlyByteBuf.Reader<T> elementReader, int maxSize) {
		int count = readCount(buffer, maxSize);
		List<T> result = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			result.add(elementReader.apply(buffer));
		}
		return result;
	}

	public static <K, V> Map<K, V> readMap(FriendlyByteBuf buffer, FriendlyByteBuf.Reader<K> keyReader, FriendlyByteBuf.Reader<V> valueReader, int maxSize) {
		int count = readCount(buffer, maxSize);
		Map<K, V> result = new HashMap<>(count);
		for (int index = 0; index < count; index++) {
			result.put(keyReader.apply(buffer), valueReader.apply(buffer));
		}
		return result;
	}

	private static int readCount(FriendlyByteBuf buffer, int maxSize) {
		int count = buffer.readVarInt();
		if (count < 0 || count > maxSize) {
			throw new DecoderException("Collection size " + count + " exceeds maximum of " + maxSize);
		}
		return count;
	}
}
