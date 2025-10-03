package net.p3pp3rf1y.sophisticatedcore.client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.render.IItemActionPayloadBuilder;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.network.DepositItemsMessage;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.RestockItemsMessage;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.*;

public class ItemInteractionHandler {
	public static final int INTERACTION_RANGE = 10;

	private static List<IItemActionPayloadBuilder<?>> payloadBuilders = new ArrayList<>();

	public static void registerPayloadBuilder(IItemActionPayloadBuilder<?> payloadBuilder) {
		payloadBuilders.add(payloadBuilder);
	}

	public static void depositMultipleItems(Player player, boolean mainInventory, boolean hotbar, boolean onlyMatching) {
		if (!mainInventory && !hotbar) {
			throw new IllegalArgumentException("At least one of mainInventory or hotbar must be true");
		}

		int minSlot = hotbar ? 0 : 9;
		int maxSlot = mainInventory ? 36 : 9;
		depositItem(player, minSlot, maxSlot, onlyMatching);
	}

	public static void depositItem(Player player, int itemSlot, boolean onlyMatching) {
		depositItem(player, itemSlot, itemSlot + 1, onlyMatching);
	}

	private static void depositItem(Player player, int minSlot, int maxSlot, boolean onlyMatching) {
		Set<BlockPos> storages = new HashSet<>();
		Set<BlockPos> controllers = new HashSet<>();
		WorldHelper.getBlockEntitiesInRange(player.level(), player.blockPosition(), INTERACTION_RANGE, IControllableStorage.class).forEach(s -> {
			s.getControllerPos().ifPresentOrElse(pos -> addIfPlayerCanInteractWith(player, controllers, pos), () -> addIfPlayerCanInteractWith(player, storages, s.getStorageBlockPos()));
		});
		WorldHelper.getBlockEntitiesInRange(player.level(), player.blockPosition(), INTERACTION_RANGE, ControllerBlockEntityBase.class).forEach(c ->  addIfPlayerCanInteractWith(player, controllers, c.getBlockPos()));

		Map<ResourceLocation, Object> extras = new LinkedHashMap<>();
		payloadBuilders.forEach(h -> {
			h.buildClientRequestData(player).ifPresent(data -> extras.put(h.getPayloadHandlerId(), data));
		});
		if (!storages.isEmpty() || !controllers.isEmpty() || !extras.isEmpty()) {
			PacketHandler.INSTANCE.sendToServer(new DepositItemsMessage(minSlot, maxSlot, new ArrayList<>(storages), new ArrayList<>(controllers), extras, onlyMatching));
		} else {
			playError(player, TranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)));
		}
	}

	private static void addIfPlayerCanInteractWith(Player player, Set<BlockPos> positions, BlockPos pos) {
		if (player.mayInteract(player.level(), pos)) {
			positions.add(pos);
		}
	}

	public static void restockMultipleItems(Player player, ItemStack filter, boolean mainInventory, boolean hotbar, boolean fillEmpty) {
		if (!mainInventory && !hotbar) {
			throw new IllegalArgumentException("At least one of mainInventory or hotbar must be true");
		}

		int minSlot = hotbar ? 0 : 9;
		int maxSlot = mainInventory ? 36 : 9;
		restockItem(player, filter, minSlot, maxSlot, fillEmpty);
	}

	public static void restockItem(Player player, ItemStack filter, int itemSlot, boolean fillEmpty) {
		ItemStack item = player.getInventory().getItem(itemSlot);
		if (!fillEmpty && item.getCount() == item.getMaxStackSize()) {
			playError(player, TranslationHelper.INSTANCE.translStatusMessage("cannot_restock_full_stack", item.getHoverName().copy().setStyle(Style.EMPTY.withColor(0xFF5555))));
			return;
		}

		restockItem(player, filter, itemSlot, itemSlot + 1, fillEmpty);
	}

	private static void restockItem(Player player, ItemStack filter, int minSlot, int maxSlot, boolean fillEmpty) {
		if (maxSlot - minSlot > 1 && checkStacksDoNotAllowRestock(player, minSlot, maxSlot, fillEmpty)) {
			playError(player, TranslationHelper.INSTANCE.translStatusMessage("cannot_restock_full_stacks").setStyle(Style.EMPTY.withColor(0xFF5555)));
			return;
		}
		Set<BlockPos> storages = new HashSet<>();
		Set<BlockPos> visitedControllers = new HashSet<>();
		WorldHelper.getBlockEntitiesInRange(player.level(), player.blockPosition(), INTERACTION_RANGE, ControllerBlockEntityBase.class).forEach(c -> {
			visitedControllers.add(c.getBlockPos());
			c.getStoragePositions().forEach(pos -> addIfPlayerCanInteractWith(player, storages, pos));
		});
		WorldHelper.getBlockEntitiesInRange(player.level(), player.blockPosition(), INTERACTION_RANGE, IControllableStorage.class).forEach(s -> {
			s.getControllerPos().ifPresentOrElse(controllerPos -> {
				if (!visitedControllers.contains(controllerPos)) {
					visitedControllers.add(controllerPos);
					List<BlockPos> storagePositions = WorldHelper.getBlockEntity(player.level(), controllerPos, ControllerBlockEntityBase.class).map(ControllerBlockEntityBase::getStoragePositions).orElse(Collections.emptyList());
					storagePositions.forEach(pos -> addIfPlayerCanInteractWith(player, storages, pos));
				}
			}, () -> addIfPlayerCanInteractWith(player, storages, s.getStorageBlockPos()));
		});

		Map<ResourceLocation, Object> extras = new LinkedHashMap<>();
		payloadBuilders.forEach(h -> {
			h.buildClientRequestData(player).ifPresent(data -> extras.put(h.getPayloadHandlerId(), data));
		});
		if (!storages.isEmpty() || !extras.isEmpty()) {
			PacketHandler.INSTANCE.sendToServer(new RestockItemsMessage(filter, minSlot, maxSlot, fillEmpty, new ArrayList<>(storages), extras));
		} else {
			playError(player, TranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)));
		}
	}

	private static boolean checkStacksDoNotAllowRestock(Player player, int minSlot, int maxSlot, boolean fillEmpty) {
		for (int slot = minSlot; slot < maxSlot; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if ((fillEmpty && stack.isEmpty()) || (!stack.isEmpty() && stack.getCount() < stack.getMaxStackSize())) {
				return false;
			}
		}
		return true;
	}

	private static void playError(Player player, MutableComponent message) {
		player.displayClientMessage(message, true);
		player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(player.level().random) * 0.1F);
	}
}
