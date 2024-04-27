package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public class StopDiscPlaybackPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedCore.MOD_ID, "stop_disc_playback");
	private final UUID storageUuid;

	public StopDiscPlaybackPacket(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	public StopDiscPlaybackPacket(FriendlyByteBuf buffer) {
		this(buffer.readUUID());
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(this::handlePacket);
	}

	private void handlePacket() {
		StorageSoundHandler.stopStorageSound(storageUuid);
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
