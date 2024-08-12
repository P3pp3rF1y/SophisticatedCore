package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.List;

public record SyncContainerStacksPayload(int windowId, int stateId, List<ItemStack> itemStacks,
										 ItemStack carriedStack) implements CustomPacketPayload {
	public static final Type<SyncContainerStacksPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_container_stacks"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncContainerStacksPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			SyncContainerStacksPayload::windowId,
			ByteBufCodecs.INT,
			SyncContainerStacksPayload::stateId,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC,
			SyncContainerStacksPayload::itemStacks,
			ItemStack.OPTIONAL_STREAM_CODEC,
			SyncContainerStacksPayload::carriedStack,
			SyncContainerStacksPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncContainerStacksPayload payload, IPayloadContext context) {
		Player player = context.player();
		if (!(player.containerMenu instanceof StorageContainerMenuBase) || player.containerMenu.containerId != payload.windowId) {
			return;
		}
		player.containerMenu.initializeContents(payload.stateId, payload.itemStacks, payload.carriedStack);
	}
}
