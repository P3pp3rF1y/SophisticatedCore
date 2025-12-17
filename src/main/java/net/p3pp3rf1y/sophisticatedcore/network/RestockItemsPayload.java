package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.IItemActionPayloadHandler;
import net.p3pp3rf1y.sophisticatedcore.common.ItemActionHandlerRegistry;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.*;

public record RestockItemsPayload(ItemStack filter, int minSlot, int maxSlot, boolean fillEmpty,
								  List<BlockPos> storagePositions,
								  Map<Identifier, Object> extras) implements CustomPacketPayload {
	public static final Type<RestockItemsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("restock_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RestockItemsPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC,
			RestockItemsPayload::filter,
			ByteBufCodecs.INT,
			RestockItemsPayload::minSlot,
			ByteBufCodecs.INT,
			RestockItemsPayload::maxSlot,
			ByteBufCodecs.BOOL,
			RestockItemsPayload::fillEmpty,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			RestockItemsPayload::storagePositions,
			ItemActionHandlerRegistry.EXTRAS_STREAM_CODEC,
			RestockItemsPayload::extras,
			RestockItemsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RestockItemsPayload payload, IPayloadContext context) {
		Player player = context.player();
		Level level = player.level();
		Map<Vec3, InventoryHandler> restockHandlers = new TreeMap<>(Comparator.<Vec3>comparingDouble(player::distanceToSqr).thenComparingDouble(Vec3::x).thenComparingDouble(Vec3::y).thenComparingDouble(Vec3::z));

		if (player.level() instanceof ServerLevel serverLevel) {
			payload.storagePositions().stream()
					.filter(pos -> WorldHelper.playerMayInteract(player, pos))
					.map(pos -> WorldHelper.getBlockEntity(level, pos, IControllableStorage.class))
					.filter(Optional::isPresent).map(Optional::get)
					.forEach(s -> restockHandlers.put(s.getStorageBlockPos().getCenter(), s.getStorageWrapper().getInventoryHandler()));
		}

		payload.extras().forEach((id, extraData) -> {
			ItemActionHandlerRegistry.get(id).ifPresent(handler -> {
				restockHandlers.putAll(getTargetInventories(handler, player, extraData));
			});
		});

		ItemStackKey filterStackKey = ItemStackKey.of(payload.filter());
		Map<Vec3, ItemStack> transferredItems = new HashMap<>();
		Set<Integer> restockedPlayerSlots = new HashSet<>();
		for (int playerInventorySlot = payload.minSlot(); playerInventorySlot < payload.maxSlot(); playerInventorySlot++) {
			ItemStack playerInventoryStack = player.getInventory().getItem(playerInventorySlot);
			if (payload.fillEmpty() && !payload.filter().isEmpty()) {
				if (playerInventoryStack.isEmpty() || ItemStack.isSameItemSameComponents(playerInventoryStack, payload.filter())) {
					restockSlot(restockHandlers, filterStackKey, playerInventoryStack, transferredItems, restockedPlayerSlots, player, playerInventorySlot);
				}
			} else {
				if (!playerInventoryStack.isEmpty()) {
					restockSlot(restockHandlers, ItemStackKey.of(playerInventoryStack), playerInventoryStack, transferredItems, restockedPlayerSlots, player, playerInventorySlot);
				}
			}
		}

		if (player instanceof ServerPlayer serverPlayer) {
			Vec3 playerPos = player.getEyePosition().add(0, -0.3, 0);
			PacketDistributor.sendToPlayer(serverPlayer, new SyncItemTransfersPayload(transferredItems, playerPos, false));
			PacketDistributor.sendToPlayersTrackingEntity(serverPlayer, new SyncItemTransfersPayload(transferredItems, playerPos, false));
		}
		Component message;
		if (payload.maxSlot() - payload.minSlot() == 1) {
			if (transferredItems.isEmpty()) {
				ItemStack item;
				if (payload.fillEmpty()) {
					item = filterStackKey.stack();
				} else {
					item = player.getInventory().getItem(payload.minSlot());
				}
				message = TranslationHelper.INSTANCE.translStatusMessage("cannot_restock_item",
						Component.literal(item.getHoverName().getString()).withStyle(ChatFormatting.RED));
				level.playSound(null, player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
			} else {
				message = TranslationHelper.INSTANCE.translStatusMessage("restocked_item",
						Component.literal(transferredItems.values().iterator().next().getHoverName().getString()).withStyle(ChatFormatting.DARK_GREEN));
			}
		} else {
			if (transferredItems.isEmpty()) {
				message = TranslationHelper.INSTANCE.translStatusMessage("cannot_restock_items");
				level.playSound(null, player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
			} else {
				message = TranslationHelper.INSTANCE.translStatusMessage("restocked_items", Component.literal(String.valueOf(restockedPlayerSlots.size())).withStyle(ChatFormatting.DARK_GREEN));
			}
		}
		player.displayClientMessage(message, true);
	}

	private static void restockSlot(Map<Vec3, InventoryHandler> restockHandlers, ItemStackKey stackKey, ItemStack playerInventoryStack, Map<Vec3, ItemStack> restocked, Set<Integer> restockedPlayerSlots, Player player, int playerInventorySlot) {
		for (Map.Entry<Vec3, InventoryHandler> entry : restockHandlers.entrySet()) {
			Vec3 pos = entry.getKey();
			InventoryHandler handler = entry.getValue();
			int matchingStackSlot = handler.getSlotTracker().getFirstMatchingSlot(stackKey);
			while (matchingStackSlot != -1 && playerInventoryStack.getCount() < playerInventoryStack.getMaxStackSize()) {
				int extracted = InventoryHelper.extract(handler, matchingStackSlot, stackKey.toResource(), playerInventoryStack.getMaxStackSize() - playerInventoryStack.getCount());
				if (extracted > 0) {
					restocked.put(pos, stackKey.stack().copyWithCount(restocked.getOrDefault(pos, ItemStack.EMPTY).getCount() + extracted));
					if (playerInventoryStack.isEmpty()) {
						playerInventoryStack = stackKey.stack().copyWithCount(extracted);
					} else {
						playerInventoryStack.grow(extracted);
					}
					player.getInventory().setItem(playerInventorySlot, playerInventoryStack);
					restockedPlayerSlots.add(playerInventorySlot);
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
