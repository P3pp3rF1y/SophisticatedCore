package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public class PlayDiscPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "play_disc");
	private final boolean blockStorage;
	private final UUID storageUuid;
	private final int musicDiscItemId;
	private int entityId;
	private BlockPos pos;

	public PlayDiscPacket(UUID storageUuid, int musicDiscItemId, BlockPos pos) {
		blockStorage = true;
		this.storageUuid = storageUuid;
		this.musicDiscItemId = musicDiscItemId;
		this.pos = pos;
	}

	public PlayDiscPacket(UUID storageUuid, int musicDiscItemId, int entityId) {
		blockStorage = false;
		this.storageUuid = storageUuid;
		this.musicDiscItemId = musicDiscItemId;
		this.entityId = entityId;
	}

	public static PlayDiscPacket read(FriendlyByteBuf buffer) {
		if (buffer.readBoolean()) {
			return new PlayDiscPacket(buffer.readUUID(), buffer.readInt(), buffer.readBlockPos());
		}
		return new PlayDiscPacket(buffer.readUUID(), buffer.readInt(), buffer.readInt());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(this::handlePacket);
	}

	private void handlePacket() {
		Item discItem = Item.byId(musicDiscItemId);
		if (!(discItem instanceof RecordItem)) {
			return;
		}
		SoundEvent soundEvent = ((RecordItem) discItem).getSound();
		if (blockStorage) {
			StorageSoundHandler.playStorageSound(soundEvent, storageUuid, pos);
		} else {
			StorageSoundHandler.playStorageSound(soundEvent, storageUuid, entityId);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(blockStorage);
		buffer.writeUUID(storageUuid);
		buffer.writeInt(musicDiscItemId);
		if (blockStorage) {
			buffer.writeBlockPos(pos);
		} else {
			buffer.writeInt(entityId);
		}
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
