package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.IItemActionPayloadHandler;
import net.p3pp3rf1y.sophisticatedcore.common.ItemActionHandlerRegistry;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.*;
import java.util.function.Supplier;

public record RestockItemsMessage(ItemStack filter, int minSlot, int maxSlot, boolean fillEmpty,
								  List<BlockPos> storagePositions,
								  Map<ResourceLocation, Object> extras) {
	public static void encode(RestockItemsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeItem(msg.filter);
		packetBuffer.writeInt(msg.minSlot);
		packetBuffer.writeInt(msg.maxSlot);
		packetBuffer.writeBoolean(msg.fillEmpty);
		packetBuffer.writeCollection(msg.storagePositions, FriendlyByteBuf::writeBlockPos);
		encodeExtras(msg.extras, packetBuffer);
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

	public static RestockItemsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RestockItemsMessage(
				packetBuffer.readItem(),
				packetBuffer.readInt(),
				packetBuffer.readInt(),
				packetBuffer.readBoolean(),
				packetBuffer.readList(FriendlyByteBuf::readBlockPos),
				decodeExtras(packetBuffer)
		);
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

	static void onMessage(RestockItemsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context));
		context.setPacketHandled(true);
	}


	public static void handleMessage(RestockItemsMessage msg, NetworkEvent.Context context) {
		Player player = context.getSender();
		Level level = player.level();
		Map<Vec3, InventoryHandler> restockHandlers = new TreeMap<>(Comparator.<Vec3>comparingDouble(player::distanceToSqr).thenComparingDouble(Vec3::x).thenComparingDouble(Vec3::y).thenComparingDouble(Vec3::z));

		msg.storagePositions().stream()
				.map(pos -> WorldHelper.getBlockEntity(level, pos, IControllableStorage.class))
				.filter(Optional::isPresent).map(Optional::get)
				.forEach(s -> restockHandlers.put(s.getStorageBlockPos().getCenter(), s.getStorageWrapper().getInventoryHandler()));

		msg.extras().forEach((id, extraData) -> {
			ItemActionHandlerRegistry.get(id).ifPresent(handler -> {
				restockHandlers.putAll(getTargetInventories(handler, player, extraData));
			});
		});

		ItemStackKey filterStackKey = ItemStackKey.of(msg.filter());
		Map<Vec3, ItemStack> restocked = new HashMap<>();
		for (int playerInventorySlot = msg.minSlot(); playerInventorySlot < msg.maxSlot(); playerInventorySlot++) {
			ItemStack playerInventoryStack = player.getInventory().getItem(playerInventorySlot);
			if (!msg.filter().isEmpty()) {
				if (playerInventoryStack.isEmpty() || ItemStack.isSameItemSameTags(playerInventoryStack, msg.filter())) {
					restockSlot(restockHandlers, filterStackKey, playerInventoryStack, restocked, player, playerInventorySlot);
				}
			} else {
				if (!playerInventoryStack.isEmpty()) {
					restockSlot(restockHandlers, ItemStackKey.of(playerInventoryStack), playerInventoryStack, restocked, player, playerInventorySlot);
				}
			}
		}

		if (player instanceof ServerPlayer serverPlayer) {
			Vec3 playerPos = player.getEyePosition().add(0, -0.1, 0);
			PacketHandler.INSTANCE.sendToClient(serverPlayer, new SyncItemTransfersMessage(restocked, playerPos, false));
			PacketHandler.INSTANCE.sendToAllTracking(new SyncItemTransfersMessage(restocked, playerPos, false), serverPlayer);
		}
		Component message;
		if (msg.maxSlot() - msg.minSlot() == 1) {
			if (restocked.isEmpty()) {
				message = TranslationHelper.INSTANCE.translStatusMessage("cannot_restock_item",
						Component.literal(player.getInventory().getItem(msg.minSlot()).getHoverName().getString()).withStyle(ChatFormatting.RED));
				player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
			} else {
				message = TranslationHelper.INSTANCE.translStatusMessage("restocked_item",
						Component.literal(restocked.values().iterator().next().getHoverName().getString()).withStyle(ChatFormatting.DARK_GREEN));
			}
		} else {
			if (restocked.isEmpty()) {
				message = TranslationHelper.INSTANCE.translStatusMessage("cannot_restock_items");
				player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
			} else {
				message = TranslationHelper.INSTANCE.translStatusMessage("restocked_items", Component.literal(String.valueOf(restocked.size())).withStyle(ChatFormatting.DARK_GREEN));
			}
		}
		player.displayClientMessage(message, true);
	}

	private static void restockSlot(Map<Vec3, InventoryHandler> restockHandlers, ItemStackKey stackKey, ItemStack playerInventoryStack, Map<Vec3, ItemStack> restocked, Player player, int playerInventorySlot) {
		for (Map.Entry<Vec3, InventoryHandler> entry : restockHandlers.entrySet()) {
			Vec3 pos = entry.getKey();
			InventoryHandler handler = entry.getValue();
			int matchingStackSlot = handler.getSlotTracker().getFirstMatchingSlot(stackKey);
			while (matchingStackSlot != -1 && playerInventoryStack.getCount() < playerInventoryStack.getMaxStackSize()) {
				ItemStack extracted = handler.extractItem(matchingStackSlot, playerInventoryStack.getMaxStackSize() - playerInventoryStack.getCount(), false);
				if (!extracted.isEmpty()) {
					restocked.put(pos, extracted.copyWithCount(restocked.getOrDefault(pos, ItemStack.EMPTY).getCount() + extracted.getCount()));
					if (playerInventoryStack.isEmpty()) {
						playerInventoryStack = extracted.copy();
					} else {
						playerInventoryStack.grow(extracted.getCount());
					}
					player.getInventory().setItem(playerInventorySlot, playerInventoryStack);
				}
				matchingStackSlot = handler.getSlotTracker().getFirstMatchingSlot(stackKey);
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	private static <T> Map<Vec3, InventoryHandler> getTargetInventories(IItemActionPayloadHandler<T> handler, Player player, Object extraData) {
		return handler.getTargetInventories(player, (T) extraData);
	}
}
