package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.HighlightRequestPayloadHandlerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.IHighlightRequestPayloadHandler;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public record RequestItemHighlightsPayload(ItemStack stack,
										   List<BlockPos> storagePositions,
										   Map<ResourceLocation, Object> extras) implements CustomPacketPayload {
	public static final Type<RequestItemHighlightsPayload> TYPE = new Type<>(SophisticatedCore.getRL("request_item_highlights"));
	private static final StreamCodec<ByteBuf, Map<ResourceLocation, Object>> EXTRAS_STREAM_CODEC = new StreamCodec<>() {
		@Override
		public void encode(ByteBuf buf, Map<ResourceLocation, Object> extras) {
			buf.writeInt(extras.size());
			for (var e : extras.entrySet()) {
				ResourceLocation id = e.getKey();
				HighlightRequestPayloadHandlerRegistry.get(id).ifPresent(h -> {
					ResourceLocation.STREAM_CODEC.encode(buf, id);
					encodeWith(h.requestCodec(), e.getValue(), buf);
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
				HighlightRequestPayloadHandlerRegistry.get(id).ifPresent(h -> extras.put(id, h.requestCodec().decode(buf)));
			}
			return extras;
		}
	};
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestItemHighlightsPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC,
			RequestItemHighlightsPayload::stack,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			RequestItemHighlightsPayload::storagePositions,
			EXTRAS_STREAM_CODEC,
			RequestItemHighlightsPayload::extras,
			RequestItemHighlightsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestItemHighlightsPayload payload, IPayloadContext context) {
		ItemStackKey stackKey = ItemStackKey.of(payload.stack());
		Player player = context.player();
		Level level = player.level();

		AtomicInteger stackMatchNumber = new AtomicInteger(0);
		AtomicInteger itemMatchNumber = new AtomicInteger(0);

		if (player instanceof ServerPlayer serverPlayer) {
			List<BlockPos> stackPositions = new ArrayList<>();
			List<BlockPos> itemPositions = new ArrayList<>();
			payload.storagePositions().forEach(pos -> {
				WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(storage -> {
					ISlotTracker slotTracker = storage.getStorageWrapper().getInventoryHandler().getSlotTracker();
					if (slotTracker.getPartialStacks().contains(stackKey) || slotTracker.getFullStacks().contains(stackKey)) {
						stackPositions.add(pos);
					} else if (slotTracker.getItems().contains(payload.stack().getItem())) {
						itemPositions.add(pos);
					}
				});
			});
			stackMatchNumber.addAndGet(stackPositions.size());
			itemMatchNumber.addAndGet(itemPositions.size());
			PacketDistributor.sendToPlayer(serverPlayer, new SyncItemHighlightsPayload(stackPositions, itemPositions, List.of()));
			payload.extras().forEach((id, extra) -> HighlightRequestPayloadHandlerRegistry.get(id).ifPresent(h -> {
				IHighlightRequestPayloadHandler.HighlightResult result = compute(h, serverPlayer, stackKey, extra);
				stackMatchNumber.addAndGet(result.stackCounts());
				itemMatchNumber.addAndGet(result.itemCounts());
			}));
			Component message = null;
			if (stackMatchNumber.get() == 0 && itemMatchNumber.get() == 0) {
				message = Component.translatable("gui.sophisticatedcore.status.no_matching_items_found");
			} else {
				if (stackMatchNumber.get() > 0) {
					message = Component.translatable("gui.sophisticatedcore.status.matching_stacks_found", Component.literal(String.valueOf(stackMatchNumber.get())).withColor(0x4CAF50));
				}
				if (itemMatchNumber.get() > 0) {
					MutableComponent itemMessage = Component.translatable("gui.sophisticatedcore.status.matching_items_found", Component.literal(String.valueOf(itemMatchNumber.get())).withColor(0x42A5F5));
					if (message != null) {
						message = message.plainCopy().append(" ").append(itemMessage);
					} else {
						message = itemMessage;
					}
				}
			}

			player.displayClientMessage(message, true);
		}
	}

	@SuppressWarnings({"unchecked"})
	private static <T> IHighlightRequestPayloadHandler.HighlightResult compute(IHighlightRequestPayloadHandler<T> handler, ServerPlayer serverPlayer, ItemStackKey stackKey, Object extra) {
		return handler.compute(serverPlayer, stackKey, (T) extra);
	}

}
