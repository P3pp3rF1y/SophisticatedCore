package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ISyncedContainer;

import javax.annotation.Nullable;

public class SyncContainerClientDataPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedCore.MOD_ID, "sync_container_client_data");
	@Nullable
	private final CompoundTag data;

	public SyncContainerClientDataPacket(@Nullable CompoundTag data) {
		this.data = data;
	}

	public SyncContainerClientDataPacket(FriendlyByteBuf buffer) {
		this(buffer.readNbt());
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (data == null) {
			return;
		}

		if (player.containerMenu instanceof ISyncedContainer container) {
			container.handlePacket(data);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeNbt(data);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
