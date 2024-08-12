package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IAdditionalSlotInfoMenu;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record SyncEmptySlotIconsPayload(Map<ResourceLocation, Set<Integer>> emptySlotIcons) implements CustomPacketPayload {
	public static final Type<SyncEmptySlotIconsPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_empty_slot_icons"));
	public static final StreamCodec<ByteBuf, SyncEmptySlotIconsPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofMap(ResourceLocation.STREAM_CODEC, StreamCodecHelper.ofCollection(ByteBufCodecs.INT, HashSet::new), HashMap::new),
			SyncEmptySlotIconsPayload::emptySlotIcons,
			SyncEmptySlotIconsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncEmptySlotIconsPayload payload, IPayloadContext context) {
		Player player = context.player();
		if (!(player.containerMenu instanceof IAdditionalSlotInfoMenu menu)) {
			return;
		}
		menu.updateEmptySlotIcons(payload.emptySlotIcons);
	}
}
