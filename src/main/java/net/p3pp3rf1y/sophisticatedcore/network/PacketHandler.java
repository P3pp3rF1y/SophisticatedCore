package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.PlayDiscMessage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StopDiscPlaybackMessage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankClickMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketHandler {
	private static final int PAYLOAD_TO_CLIENT_MAX = 1048576;
	private static final int PART_SIZE = PAYLOAD_TO_CLIENT_MAX - 1 - 5; // 1 byte for part state, 5 bytes for message id

	private final Map<Class<?>, MessageType<?>> byType = new HashMap<>();
	private final Map<Integer, MessageType<?>> byId = new HashMap<>();

	public static final PacketHandler INSTANCE = new PacketHandler(SophisticatedCore.MOD_ID, SophisticatedCore.getNetworkProtocolVersion());

	private final SimpleChannel networkWrapper;
	private int idx = 0;

	protected PacketHandler(String modId, String protocol) {
		networkWrapper = NetworkRegistry.newSimpleChannel(new ResourceLocation(modId, "channel"), () -> protocol, protocol::equals, protocol::equals);
	}

	public final void init() {
		registerSplitPacket();
		registerMessages();
	}

	public void registerSplitPacket() {
		registerMessage(SplitPacket.class, SplitPacket::encode, SplitPacket::decode, this::handleSplitPacket, NetworkDirection.PLAY_TO_CLIENT);
	}

	public void registerMessages() {
		registerMessage(SyncContainerClientDataMessage.class, SyncContainerClientDataMessage::encode, SyncContainerClientDataMessage::decode,
				SyncContainerClientDataMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(TransferFullSlotMessage.class, TransferFullSlotMessage::encode, TransferFullSlotMessage::decode, TransferFullSlotMessage::onMessage,
				NetworkDirection.PLAY_TO_SERVER);
		registerMessage(SyncContainerStacksMessage.class, SyncContainerStacksMessage::encode, SyncContainerStacksMessage::decode,
				SyncContainerStacksMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncSlotStackMessage.class, SyncSlotStackMessage::encode, SyncSlotStackMessage::decode, SyncSlotStackMessage::onMessage,
				NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncRecentCraftedResultsMessage.class, SyncRecentCraftedResultsMessage::encode, SyncRecentCraftedResultsMessage::decode,
				SyncRecentCraftedResultsMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncPlayerSettingsMessage.class, SyncPlayerSettingsMessage::encode, SyncPlayerSettingsMessage::decode,
				SyncPlayerSettingsMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(PlayDiscMessage.class, PlayDiscMessage::encode, PlayDiscMessage::decode, PlayDiscMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(StopDiscPlaybackMessage.class, StopDiscPlaybackMessage::encode, StopDiscPlaybackMessage::decode, StopDiscPlaybackMessage::onMessage,
				NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(TankClickMessage.class, TankClickMessage::encode, TankClickMessage::decode, TankClickMessage::onMessage,
				NetworkDirection.PLAY_TO_SERVER);
		registerMessage(SyncTemplateSettingsMessage.class, SyncTemplateSettingsMessage::encode, SyncTemplateSettingsMessage::decode,
				SyncTemplateSettingsMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncAdditionalSlotInfoMessage.class, SyncAdditionalSlotInfoMessage::encode, SyncAdditionalSlotInfoMessage::decode,
				SyncAdditionalSlotInfoMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncEmptySlotIconsMessage.class, SyncEmptySlotIconsMessage::encode, SyncEmptySlotIconsMessage::decode,
				SyncEmptySlotIconsMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncSlotChangeErrorMessage.class, SyncSlotChangeErrorMessage::encode, SyncSlotChangeErrorMessage::decode,
				SyncSlotChangeErrorMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncDatapackSettingsTemplateMessage.class, SyncDatapackSettingsTemplateMessage::encode, SyncDatapackSettingsTemplateMessage::decode,
				SyncDatapackSettingsTemplateMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(TransferItemsMessage.class, TransferItemsMessage::encode, TransferItemsMessage::decode, TransferItemsMessage::onMessage,
				NetworkDirection.PLAY_TO_SERVER);
		registerMessage(SyncBlockHighlightsMessage.class, SyncBlockHighlightsMessage::encode, SyncBlockHighlightsMessage::decode,
				SyncBlockHighlightsMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
	}

	public <M> void registerMessage(Class<M> messageType, BiConsumer<M, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, M> decoder,
			BiConsumer<M, Supplier<NetworkEvent.Context>> handler, NetworkDirection direction) {

		int id = idx++;

		MessageType<M> entry = new MessageType<>(id, messageType, encoder, decoder, handler);
		byType.put(messageType, entry);
		byId.put(id, entry);

		networkWrapper.registerMessage(id, messageType, encoder, decoder, handler, Optional.of(direction));
	}

	public <M> void sendToServer(M message) {
		networkWrapper.sendToServer(message);
	}

	public <M> void sendToClient(ServerPlayer player, M message) {
		sendPossiblySplit(PacketDistributor.PLAYER.with(() -> player), message);
	}

	public <M> void sendToAllTracking(M message, Entity entity) {
		sendPossiblySplit(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
	}

	public <M> void sentToAllTrackingChunkOf(Level level, BlockPos pos, M message) {
		sendPossiblySplit(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), message);
	}

	private <M> void sendPossiblySplit(PacketDistributor.PacketTarget target, M message) {
		if (!(message instanceof ISplittableMessage)) {
			networkWrapper.send(target, message);
			return;
		}

		if (target.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
			networkWrapper.send(target, message);
			return;
		}

		MessageType<M> messageType = typeFor(message);
		byte[] fullStream = encodeVirtualStream(messageType.id, messageType::encodeTo, message);

		if (fullStream.length <= PART_SIZE) {
			networkWrapper.send(target, message);
			return;
		}

		splitter.splitAndSend(fullStream, payloadSlice -> networkWrapper.send(target, new SplitPacket(payloadSlice)));
	}

	@SuppressWarnings("unchecked")
	private <M> MessageType<M> typeFor(M message) {
		MessageType<?> mt = byType.get(message.getClass());
		if (mt == null) {
			throw new IllegalStateException("Unregistered message type: " + message.getClass().getName());
		}
		return (MessageType<M>) mt;
	}

	public <M> void sendToAllNear(ResourceKey<Level> dimension, Vec3 position, int range, M message) {
		sendPossiblySplit(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(position.x, position.y, position.z, range, dimension)), message);
	}

	private static <M> byte[] encodeVirtualStream(int msgId, BiConsumer<M, FriendlyByteBuf> encoder, M message) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			buf.writeVarInt(msgId);
			encoder.accept(message, buf);
			byte[] out = new byte[buf.readableBytes()];
			buf.getBytes(buf.readerIndex(), out);
			return out;
		} finally {
			buf.release();
		}
	}

	private final PacketSplitter splitter = new PacketSplitter(PART_SIZE);

	private void handleSplitPacket(SplitPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			FriendlyByteBuf full = splitter.acceptPart(msg.payload());
			if (full == null) {
				return;
			}

			try {
				int originalId = full.readVarInt();
				MessageType<?> messageType = byId.get(originalId);
				if (messageType == null) {
					return;
				}

				messageType.decodeAndHandle(full, contextSupplier);
			} finally {
				full.release();
			}
		});
		context.setPacketHandled(true);
	}

	private record MessageType<M>(int id, Class<M> type, BiConsumer<M, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, M> decoder,
			BiConsumer<M, Supplier<NetworkEvent.Context>> handler) {
		public void decodeAndHandle(FriendlyByteBuf buf, Supplier<NetworkEvent.Context> ctx) {
			M msg = decoder.apply(buf);
			handler.accept(msg, ctx);
		}

		void encodeTo(M msg, FriendlyByteBuf buf) {
			encoder.accept(msg, buf);
		}
	}
}
