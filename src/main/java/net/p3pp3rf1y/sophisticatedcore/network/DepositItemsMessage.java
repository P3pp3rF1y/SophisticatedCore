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
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.*;
import java.util.function.Supplier;

public record DepositItemsMessage(int minSlot, int maxSlot,
								  List<BlockPos> storagePositions,
								  List<BlockPos> controllerPositions,
								  Map<ResourceLocation, Object> extras,
								  boolean onlyMatching) {
	public static void encode(DepositItemsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeInt(msg.minSlot);
		packetBuffer.writeInt(msg.maxSlot);
		packetBuffer.writeCollection(msg.storagePositions, FriendlyByteBuf::writeBlockPos);
		packetBuffer.writeCollection(msg.controllerPositions, FriendlyByteBuf::writeBlockPos);
		encodeExtras(msg.extras, packetBuffer);
		packetBuffer.writeBoolean(msg.onlyMatching);
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

	public static DepositItemsMessage decode(FriendlyByteBuf packetBuffer) {
		return new DepositItemsMessage(
				packetBuffer.readInt(),
				packetBuffer.readInt(),
				packetBuffer.readList(FriendlyByteBuf::readBlockPos),
				packetBuffer.readList(FriendlyByteBuf::readBlockPos),
				decodeExtras(packetBuffer),
				packetBuffer.readBoolean()
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

	static void onMessage(DepositItemsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context));
		context.setPacketHandled(true);
	}

	public static void handleMessage(DepositItemsMessage msg, NetworkEvent.Context context) {
		Player player = context.getSender();
		Level level = player.level();
		Map<Vec3, IDepositHandler> depositHandlers = new TreeMap<>(Comparator.<Vec3>comparingDouble(player::distanceToSqr).thenComparingDouble(Vec3::x).thenComparingDouble(Vec3::y).thenComparingDouble(Vec3::z));

		Set<BlockPos> controllerPositions = new HashSet<>(msg.controllerPositions());

		msg.storagePositions().stream()
				.filter(pos -> player.mayInteract(level, pos))
				.map(pos -> WorldHelper.getBlockEntity(level, pos, IControllableStorage.class))
				.filter(Optional::isPresent).map(Optional::get)
				.forEach(s -> s.getControllerPos().ifPresentOrElse(controllerPositions::add,
							() -> depositHandlers.put(s.getStorageBlockPos().getCenter(), new StorageDepositHandler(s.getStorageWrapper().getInventoryHandler()))));

		controllerPositions.stream()
				.filter(pos -> player.mayInteract(level, pos))
				.map(pos -> WorldHelper.getBlockEntity(player.level(), pos, ControllerBlockEntityBase.class))
				.filter(Optional::isPresent).map(Optional::get)
				.forEach(c -> depositHandlers.put(c.getBlockPos().getCenter(), new ControllerDepositHandler(c)));

		msg.extras().forEach((id, extraData) -> {
			ItemActionHandlerRegistry.get(id).ifPresent(handler -> {
				getTargetInventories(handler, player, extraData).forEach((pos, inventory) -> {
					depositHandlers.put(pos, new StorageDepositHandler(inventory));
				});
			});
		});

		Map<Vec3, ItemStack> inserted = new HashMap<>();
		Set<Integer> depositedFromSlots = new HashSet<>();
		depositSlotsToHandlers(msg, player, depositHandlers, inserted, depositedFromSlots, true);
		if (!msg.onlyMatching()) {
			depositSlotsToHandlers(msg, player, depositHandlers, inserted, depositedFromSlots, false);
		}

		if (player instanceof ServerPlayer serverPlayer) {
			Vec3 playerPos = player.getEyePosition().add(0, -0.1, 0);
			PacketHandler.INSTANCE.sendToClient(serverPlayer, new SyncItemTransfersMessage(inserted, playerPos, true));
			PacketHandler.INSTANCE.sendToAllTracking(new SyncItemTransfersMessage(inserted, playerPos, true), serverPlayer);
		}

		Component message;
		if (msg.maxSlot() - msg.minSlot() == 1) {
			if (inserted.isEmpty()) {
				message = TranslationHelper.INSTANCE.translStatusMessage("cannot_deposit_item",
						Component.literal(player.getInventory().getItem(msg.minSlot()).getHoverName().getString()).withStyle(ChatFormatting.RED));
				player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
			} else {
				message = TranslationHelper.INSTANCE.translStatusMessage("deposited_item",
						Component.literal(inserted.values().iterator().next().getHoverName().getString()).withStyle(ChatFormatting.DARK_GREEN));
			}
		} else {
			if (inserted.isEmpty()) {
				message = TranslationHelper.INSTANCE.translStatusMessage("cannot_deposit_items");
				player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
			} else {
				message = TranslationHelper.INSTANCE.translStatusMessage("deposited_items", Component.literal(String.valueOf(depositedFromSlots.size())).withStyle(ChatFormatting.DARK_GREEN));
			}
		}
		player.displayClientMessage(message, true);
	}

	private static void depositSlotsToHandlers(DepositItemsMessage payload, Player player, Map<Vec3, IDepositHandler> depositHandlers, Map<Vec3, ItemStack> inserted, Set<Integer> depositedFromSlots, boolean checkPresent) {
		for (int slot = payload.minSlot(); slot < payload.maxSlot(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}

			ItemStackKey stackKey = ItemStackKey.of(stack);
			for (Map.Entry<Vec3, IDepositHandler> entry : depositHandlers.entrySet()) {
				Vec3 pos = entry.getKey();
				IDepositHandler handler = entry.getValue();
				if (!checkPresent || handler.isPresent(stackKey)) {
					ItemStack remaining = handler.deposit(stack);
					if (remaining.getCount() < stack.getCount()) {
						inserted.put(pos, stack.copyWithCount(stack.getCount() - remaining.getCount()));
						player.getInventory().setItem(slot, remaining);
						depositedFromSlots.add(slot);
						stack = remaining;
						if (stack.isEmpty()) {
							break;
						}
					}
				}
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	private static <T> Map<Vec3, InventoryHandler> getTargetInventories(IItemActionPayloadHandler<T> handler, Player player, Object extraData) {
		return handler.getTargetInventories(player, (T) extraData);
	}

	private interface IDepositHandler {
		boolean isPresent(ItemStackKey stackKey);

		ItemStack deposit(ItemStack stack);
	}

	private static class ControllerDepositHandler implements IDepositHandler {
		private final ControllerBlockEntityBase controller;

		public ControllerDepositHandler(ControllerBlockEntityBase controller) {
			this.controller = controller;
		}

		@Override
		public boolean isPresent(ItemStackKey stackKey) {
			return controller.hasMatchingStackOrItem(stackKey);
		}

		@Override
		public ItemStack deposit(ItemStack stack) {
			return controller.insertItem(stack, false);
		}
	}

	private static class StorageDepositHandler implements IDepositHandler {
		private final InventoryHandler inventoryHandler;

		public StorageDepositHandler(InventoryHandler inventoryHandler) {
			this.inventoryHandler = inventoryHandler;
		}

		@Override
		public boolean isPresent(ItemStackKey stackKey) {
			ISlotTracker slotTracker = inventoryHandler.getSlotTracker();
			return slotTracker.getPartialStacks().contains(stackKey) || slotTracker.getFullStacks().contains(stackKey)
					|| slotTracker.getItems().contains(stackKey.getStack().getItem())
					|| slotTracker.hasStackMemorizedOrFiltered(stackKey.getStack());
		}

		@Override
		public ItemStack deposit(ItemStack stack) {
			return inventoryHandler.insertItem(stack, false);
		}
	}
}
