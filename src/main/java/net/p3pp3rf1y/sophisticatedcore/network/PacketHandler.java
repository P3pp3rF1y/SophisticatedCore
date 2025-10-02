package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.SoundFinishedNotificationMessage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StopDiscPlaybackMessage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankClickMessage;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketHandler {
	public static final PacketHandler INSTANCE = new PacketHandler(SophisticatedCore.MOD_ID);
	private static final String PROTOCOL = "1";

	private final SimpleChannel networkWrapper;
	private int idx = 0;

	protected PacketHandler(String modId) {
		networkWrapper = NetworkRegistry.newSimpleChannel(new ResourceLocation(modId, "channel"),
				() -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
	}

	public void init() {
		registerMessage(SyncContainerClientDataMessage.class, SyncContainerClientDataMessage::encode, SyncContainerClientDataMessage::decode, SyncContainerClientDataMessage::onMessage);
		registerMessage(TransferFullSlotMessage.class, TransferFullSlotMessage::encode, TransferFullSlotMessage::decode, TransferFullSlotMessage::onMessage);
		registerMessage(SyncContainerStacksMessage.class, SyncContainerStacksMessage::encode, SyncContainerStacksMessage::decode, SyncContainerStacksMessage::onMessage);
		registerMessage(SyncSlotStackMessage.class, SyncSlotStackMessage::encode, SyncSlotStackMessage::decode, SyncSlotStackMessage::onMessage);
		registerMessage(SyncPlayerSettingsMessage.class, SyncPlayerSettingsMessage::encode, SyncPlayerSettingsMessage::decode, SyncPlayerSettingsMessage::onMessage);
		registerMessage(PlayDiscMessage.class, PlayDiscMessage::encode, PlayDiscMessage::decode, PlayDiscMessage::onMessage);
		registerMessage(StopDiscPlaybackMessage.class, StopDiscPlaybackMessage::encode, StopDiscPlaybackMessage::decode, StopDiscPlaybackMessage::onMessage);
		registerMessage(SoundFinishedNotificationMessage.class, SoundFinishedNotificationMessage::encode, SoundFinishedNotificationMessage::decode, SoundFinishedNotificationMessage::onMessage);
		registerMessage(TankClickMessage.class, TankClickMessage::encode, TankClickMessage::decode, TankClickMessage::onMessage);
		registerMessage(SyncTemplateSettingsMessage.class, SyncTemplateSettingsMessage::encode, SyncTemplateSettingsMessage::decode, SyncTemplateSettingsMessage::onMessage);
		registerMessage(SyncAdditionalSlotInfoMessage.class, SyncAdditionalSlotInfoMessage::encode, SyncAdditionalSlotInfoMessage::decode, SyncAdditionalSlotInfoMessage::onMessage);
		registerMessage(SyncEmptySlotIconsMessage.class, SyncEmptySlotIconsMessage::encode, SyncEmptySlotIconsMessage::decode, SyncEmptySlotIconsMessage::onMessage);
		registerMessage(SyncSlotChangeErrorMessage.class, SyncSlotChangeErrorMessage::encode, SyncSlotChangeErrorMessage::decode, SyncSlotChangeErrorMessage::onMessage);
		registerMessage(SyncDatapackSettingsTemplateMessage.class, SyncDatapackSettingsTemplateMessage::encode, SyncDatapackSettingsTemplateMessage::decode, SyncDatapackSettingsTemplateMessage::onMessage);
		registerMessage(TransferItemsMessage.class, TransferItemsMessage::encode, TransferItemsMessage::decode, TransferItemsMessage::onMessage);
		registerMessage(RequestItemHighlightsMessage.class, RequestItemHighlightsMessage::encode, RequestItemHighlightsMessage::decode, RequestItemHighlightsMessage::onMessage);
		registerMessage(SyncItemHighlightsMessage.class, SyncItemHighlightsMessage::encode, SyncItemHighlightsMessage::decode, SyncItemHighlightsMessage::onMessage);
		registerMessage(DepositItemsMessage.class, DepositItemsMessage::encode, DepositItemsMessage::decode, DepositItemsMessage::onMessage);
		registerMessage(SyncItemTransfersMessage.class, SyncItemTransfersMessage::encode, SyncItemTransfersMessage::decode, SyncItemTransfersMessage::onMessage);
		registerMessage(RestockItemsMessage.class, RestockItemsMessage::encode, RestockItemsMessage::decode, RestockItemsMessage::onMessage);
	}

	@SuppressWarnings("SameParameterValue")
	public <M> void registerMessage(Class<M> messageType, BiConsumer<M, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, M> decoder, BiConsumer<M, Supplier<NetworkEvent.Context>> messageConsumer) {
		networkWrapper.registerMessage(idx++, messageType, encoder, decoder, messageConsumer);
	}

	public <M> void sendToServer(M message) {
		networkWrapper.sendToServer(message);
	}

	public <M> void sendToClient(ServerPlayer player, M message) {
		networkWrapper.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
	}

	public <M> void sendToAllTracking(M message, Entity entity) {
		networkWrapper.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
	}

	public <M> void sentToAllTrackingChunkOf(Level level, BlockPos pos, M message) {
		networkWrapper.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), message);
	}

	public <M> void sendToAllNear(ServerLevel world, ResourceKey<Level> dimension, Vec3 position, int range, M message) {
		world.players().forEach(player -> {
			if (player.level().dimension() == dimension && player.distanceToSqr(position) <= range * range) {
				sendToClient(player, message);
			}
		});
	}
}
