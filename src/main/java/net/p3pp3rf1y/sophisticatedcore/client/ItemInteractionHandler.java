package net.p3pp3rf1y.sophisticatedcore.client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.render.IItemActionPayloadBuilder;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.network.DepositItemsPayload;
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
			s.getControllerPos().ifPresentOrElse(controllers::add, () -> storages.add(s.getStorageBlockPos()));
		});
		WorldHelper.getBlockEntitiesInRange(player.level(), player.blockPosition(), INTERACTION_RANGE, ControllerBlockEntityBase.class).forEach(c -> controllers.add(c.getBlockPos()));

		Map<ResourceLocation, Object> extras = new LinkedHashMap<>();
		payloadBuilders.forEach(h -> {
			h.buildClientRequestData(player).ifPresent(data -> extras.put(h.getPayloadHandlerId(), data));
		});
		if (!storages.isEmpty() || !controllers.isEmpty() || !extras.isEmpty()) {
			PacketDistributor.sendToServer(new DepositItemsPayload(minSlot, maxSlot, new ArrayList<>(storages), new ArrayList<>(controllers), extras, onlyMatching));
		} else {
			player.displayClientMessage(TranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)), true);
			player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(player.level().random) * 0.1F);
		}
	}
}
