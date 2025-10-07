package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.*;

public record DepositItemsPayload(int minSlot, int maxSlot,
								  List<BlockPos> storagePositions,
								  List<BlockPos> controllerPositions,
								  Map<ResourceLocation, Object> extras,
								  boolean onlyMatching) implements CustomPacketPayload {
	public static final Type<DepositItemsPayload> TYPE = new Type<>(SophisticatedCore.getRL("deposit_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DepositItemsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			DepositItemsPayload::minSlot,
			ByteBufCodecs.INT,
			DepositItemsPayload::maxSlot,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			DepositItemsPayload::storagePositions,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			DepositItemsPayload::controllerPositions,
			ItemActionHandlerRegistry.EXTRAS_STREAM_CODEC,
			DepositItemsPayload::extras,
			ByteBufCodecs.BOOL,
			DepositItemsPayload::onlyMatching,
			DepositItemsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(DepositItemsPayload payload, IPayloadContext context) {
		Player player = context.player();
		Level level = player.level();
		Map<Vec3, IDepositHandler> depositHandlers = new TreeMap<>(Comparator.<Vec3>comparingDouble(player::distanceToSqr).thenComparingDouble(Vec3::x).thenComparingDouble(Vec3::y).thenComparingDouble(Vec3::z));

		Set<BlockPos> controllerPositions = new HashSet<>(payload.controllerPositions());

		if (player.level() instanceof ServerLevel serverLevel) {
			payload.storagePositions().stream()
					.filter(pos -> WorldHelper.playerMayInteract(player, pos))
					.map(pos -> WorldHelper.getBlockEntity(level, pos, IControllableStorage.class))
					.filter(Optional::isPresent).map(Optional::get)
					.forEach(s -> s.getControllerPos().ifPresentOrElse(controllerPositions::add,
							() -> depositHandlers.put(s.getStorageBlockPos().getCenter(), new StorageDepositHandler(s.getStorageWrapper().getInventoryHandler()))));

			controllerPositions.stream()
					.filter(pos -> WorldHelper.playerMayInteract(player, pos))
					.map(pos -> WorldHelper.getBlockEntity(player.level(), pos, ControllerBlockEntityBase.class))
					.filter(Optional::isPresent).map(Optional::get)
					.forEach(c -> depositHandlers.put(c.getBlockPos().getCenter(), new ControllerDepositHandler(c)));
		}

		payload.extras().forEach((id, extraData) -> {
			ItemActionHandlerRegistry.get(id).ifPresent(handler -> {
				getTargetInventories(handler, player, extraData).forEach((pos, inventory) -> {
					depositHandlers.put(pos, new StorageDepositHandler(inventory));
				});
			});
		});

		Map<Vec3, ItemStack> inserted = new HashMap<>();
		Set<Integer> depositedFromSlots = new HashSet<>();
		depositSlotsToHandlers(payload, player, depositHandlers, inserted, depositedFromSlots, true);
		if (!payload.onlyMatching()) {
			depositSlotsToHandlers(payload, player, depositHandlers, inserted, depositedFromSlots, false);
		}

		if (player instanceof ServerPlayer serverPlayer) {
			Vec3 playerPos = player.getEyePosition().add(0, -0.1, 0);
			PacketDistributor.sendToPlayer(serverPlayer, new SyncItemTransfersPayload(inserted, playerPos, true));
			PacketDistributor.sendToPlayersTrackingEntity(serverPlayer, new SyncItemTransfersPayload(inserted, playerPos, true));
		}
		Component message;
		if (payload.maxSlot() - payload.minSlot() == 1) {
			if (inserted.isEmpty()) {
				message = TranslationHelper.INSTANCE.translStatusMessage("cannot_deposit_item",
						Component.literal(player.getInventory().getItem(payload.minSlot()).getHoverName().getString()).withStyle(ChatFormatting.RED));
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

	private static void depositSlotsToHandlers(DepositItemsPayload payload, Player player, Map<Vec3, IDepositHandler> depositHandlers, Map<Vec3, ItemStack> inserted, Set<Integer> depositedFromSlots, boolean checkPresent) {
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
