package net.p3pp3rf1y.sophisticatedcore.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ItemActionHandlerRegistry {
	public static final StreamCodec<ByteBuf, Map<ResourceLocation, Object>> EXTRAS_STREAM_CODEC = new StreamCodec<>() {
		@Override
		public void encode(ByteBuf buf, Map<ResourceLocation, Object> extras) {
			buf.writeInt(extras.size());
			for (var e : extras.entrySet()) {
				ResourceLocation id = e.getKey();
				get(id).ifPresent(h -> {
					ResourceLocation.STREAM_CODEC.encode(buf, id);
					encodeWith(h.codec(), e.getValue(), buf);
				});
			}
		}

		@SuppressWarnings({"unchecked"})
		private static <T> void encodeWith(StreamCodec<ByteBuf, T> c, Object v, ByteBuf buf) {
			c.encode(buf, (T) v);
		}

		@Override
		public Map<ResourceLocation, Object> decode(ByteBuf buf) {
			int size = buf.readInt();
			Map<ResourceLocation, Object> extras = new LinkedHashMap<>(size);
			for (int i = 0; i < size; i++) {
				ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
				get(id).ifPresent(h -> extras.put(id, h.codec().decode(buf)));
			}
			return extras;
		}
	};

	private ItemActionHandlerRegistry() {}

	private static final Map<ResourceLocation, IItemActionPayloadHandler<?>> registry = new HashMap<>();

	public static void register(IItemActionPayloadHandler<?> handler) {
		registry.put(handler.id(), handler);
	}

	public static Optional<IItemActionPayloadHandler<?>> get(ResourceLocation id) {
		return Optional.ofNullable(registry.get(id));
	}
}
