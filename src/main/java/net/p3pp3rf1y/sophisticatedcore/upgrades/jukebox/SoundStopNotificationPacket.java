package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public class SoundStopNotificationPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "sound_stop_notification");
	private final UUID storageUuid;

	public SoundStopNotificationPacket(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	public SoundStopNotificationPacket(FriendlyByteBuf buffer) {
		this(buffer.readUUID());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		ServerStorageSoundHandler.onSoundStopped(player.level(), storageUuid);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(storageUuid);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
