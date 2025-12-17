package net.p3pp3rf1y.sophisticatedcore.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ItemActionHandlerRegistry {
	public static final StreamCodec<ByteBuf, Map<Identifier, Object>> EXTRAS_STREAM_CODEC = new StreamCodec<>() {
		@Override
		public void encode(ByteBuf buf, Map<Identifier, Object> extras) {
			buf.writeInt(extras.size());
			for (var e : extras.entrySet()) {
				Identifier id = e.getKey();
				get(id).ifPresent(h -> {
					Identifier.STREAM_CODEC.encode(buf, id);
					encodeWith(h.codec(), e.getValue(), buf);
				});
			}
		}

		@SuppressWarnings({"unchecked"})
		private static <T> void encodeWith(StreamCodec<ByteBuf, T> c, Object v, ByteBuf buf) {
			c.encode(buf, (T) v);
		}

		@Override
		public Map<Identifier, Object> decode(ByteBuf buf) {
			int size = buf.readInt();
			Map<Identifier, Object> extras = new LinkedHashMap<>(size);
			for (int i = 0; i < size; i++) {
				Identifier id = Identifier.STREAM_CODEC.decode(buf);
				get(id).ifPresent(h -> extras.put(id, h.codec().decode(buf)));
			}
			return extras;
		}
	};

	private ItemActionHandlerRegistry() {}

	private static final Map<Identifier, IItemActionPayloadHandler<?>> registry = new HashMap<>();

	public static void register(IItemActionPayloadHandler<?> handler) {
		registry.put(handler.id(), handler);
	}

	public static Optional<IItemActionPayloadHandler<?>> get(Identifier id) {
		return Optional.ofNullable(registry.get(id));
	}
}
