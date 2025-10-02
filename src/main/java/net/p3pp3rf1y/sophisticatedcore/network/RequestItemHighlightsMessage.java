package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.IItemActionPayloadHandler;
import net.p3pp3rf1y.sophisticatedcore.common.ItemActionHandlerRegistry;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public record RequestItemHighlightsMessage(ItemStack stack, List<BlockPos> storagePositions,
										   Map<ResourceLocation, Object> extras) {
	public static void encode(RequestItemHighlightsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeItemStack(msg.stack(), false);
		packetBuffer.writeCollection(msg.storagePositions(), FriendlyByteBuf::writeBlockPos);
		encodeExtras(msg.extras(), packetBuffer);
	}

	private static void encodeExtras(Map<ResourceLocation, Object> extras, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeInt(extras.size());
		for (var e : extras.entrySet()) {
			ResourceLocation id = e.getKey();
			ItemActionHandlerRegistry.get(id).ifPresent(h -> {
				packetBuffer.writeResourceLocation(id);
				encodeWith(h, packetBuffer, e.getValue());
			});
		}
	}

	@SuppressWarnings({"unchecked"})
	private static <T> void encodeWith(IItemActionPayloadHandler<T> handler, FriendlyByteBuf packetBuffer, Object v) {
		handler.encode(packetBuffer, (T) v);
	}

	public static RequestItemHighlightsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RequestItemHighlightsMessage(packetBuffer.readItem(), packetBuffer.readList(FriendlyByteBuf::readBlockPos), decodeExtras(packetBuffer));
	}

	private static Map<ResourceLocation, Object> decodeExtras(FriendlyByteBuf packetBuffer) {
		int size = packetBuffer.readInt();
		Map<ResourceLocation, Object> extras = new LinkedHashMap<>(size);
		for (int i = 0; i < size; i++) {
			ResourceLocation id = packetBuffer.readResourceLocation();
			ItemActionHandlerRegistry.get(id).ifPresent(h -> extras.put(id, h.decode(packetBuffer)));
		}
		return extras;
	}

	static void onMessage(RequestItemHighlightsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer player, RequestItemHighlightsMessage msg) {
		if (player == null) {
			return;
		}
		ItemStackKey stackKey = ItemStackKey.of(msg.stack());
		Level level = player.level();

		AtomicInteger stackMatchNumber = new AtomicInteger(0);
		AtomicInteger itemMatchNumber = new AtomicInteger(0);

		List<BlockPos> stackPositions = new ArrayList<>();
		List<BlockPos> itemPositions = new ArrayList<>();
		msg.storagePositions().forEach(pos -> {
			WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(storage -> {
				ISlotTracker slotTracker = storage.getStorageWrapper().getInventoryHandler().getSlotTracker();
				if (slotTracker.getPartialStacks().contains(stackKey) || slotTracker.getFullStacks().contains(stackKey)) {
					stackPositions.add(pos);
				} else if (slotTracker.getItems().contains(msg.stack().getItem())) {
					itemPositions.add(pos);
				}
			});
		});
		stackMatchNumber.addAndGet(stackPositions.size());
		itemMatchNumber.addAndGet(itemPositions.size());
		PacketHandler.INSTANCE.sendToClient(player, new SyncItemHighlightsMessage(stackPositions, itemPositions, List.of()));
		msg.extras().forEach((id, extra) -> ItemActionHandlerRegistry.get(id).ifPresent(h -> {

			IItemActionPayloadHandler.HighlightResult result = compute(h, player, stackKey, extra);
			stackMatchNumber.addAndGet(result.stackCounts());
			itemMatchNumber.addAndGet(result.itemCounts());
		}));
		Component message = null;
		if (stackMatchNumber.get() == 0 && itemMatchNumber.get() == 0) {
			message = Component.translatable("gui.sophisticatedcore.status.no_matching_items_found");
			player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
		} else {
			if (stackMatchNumber.get() > 0) {
				message = Component.translatable("gui.sophisticatedcore.status.matching_stacks_found", Component.literal(String.valueOf(stackMatchNumber.get())).withStyle(Style.EMPTY.withColor(0x4CAF50)));
			}
			if (itemMatchNumber.get() > 0) {
				MutableComponent itemMessage = Component.translatable("gui.sophisticatedcore.status.matching_items_found", Component.literal(String.valueOf(itemMatchNumber.get())).withStyle(Style.EMPTY.withColor(0x42A5F5)));
				if (message != null) {
					message = message.plainCopy().append(" ").append(itemMessage);
				} else {
					message = itemMessage;
				}
			}
			player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1, 0.95f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
		}

		player.displayClientMessage(message, true);
	}

	@SuppressWarnings({"unchecked"})
	private static <T> IItemActionPayloadHandler.HighlightResult compute(IItemActionPayloadHandler<T> handler, ServerPlayer serverPlayer, ItemStackKey stackKey, Object extra) {
		return handler.computeHighlight(serverPlayer, stackKey, (T) extra);
	}
}
