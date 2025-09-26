package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.ArrayList;
import java.util.List;

public record RequestItemHighlightsPayload(ItemStack stack,
										   List<BlockPos> storagePositions) implements CustomPacketPayload {
	public static final Type<RequestItemHighlightsPayload> TYPE = new Type<>(SophisticatedCore.getRL("request_item_highlights"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestItemHighlightsPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC,
			RequestItemHighlightsPayload::stack,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			RequestItemHighlightsPayload::storagePositions,
			RequestItemHighlightsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestItemHighlightsPayload payload, IPayloadContext context) {
		ItemStackKey stackKey = ItemStackKey.of(payload.stack());
		Player player = context.player();
		Level level = player.level();

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
			PacketDistributor.sendToPlayer(serverPlayer, new SyncItemHighlightsPayload(stackPositions, itemPositions, List.of()));
		}
	}
}
