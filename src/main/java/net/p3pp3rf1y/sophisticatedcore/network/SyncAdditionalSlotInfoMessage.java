
package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IAdditionalSlotInfoMenu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SyncAdditionalSlotInfoMessage {
	private final Set<Integer> inaccessibleSlots;
	private final Map<Integer, Integer> slotLimitOverrides;
	private final Map<Integer, Item> slotFilterItems;
	public SyncAdditionalSlotInfoMessage(Set<Integer> inaccessibleSlots, Map<Integer, Integer> slotLimitOverrides, Map<Integer, Item> slotFilterItems) {
		this.inaccessibleSlots = inaccessibleSlots;
		this.slotLimitOverrides = slotLimitOverrides;
		this.slotFilterItems = slotFilterItems;
	}

	public static void encode(SyncAdditionalSlotInfoMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeVarIntArray(msg.inaccessibleSlots.stream().mapToInt(i->i).toArray());
		serializeSlotLimitOverrides(packetBuffer, msg.slotLimitOverrides);
		serializeSlotFilterItems(packetBuffer, msg.slotFilterItems);
	}

	public static SyncAdditionalSlotInfoMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncAdditionalSlotInfoMessage(Arrays.stream(packetBuffer.readVarIntArray()).boxed().collect(Collectors.toSet()), deserializeSlotLimitOverrides(packetBuffer), deserializeSlotFilterItems(packetBuffer));
	}

	private static void serializeSlotFilterItems(FriendlyByteBuf packetBuffer, Map<Integer, Item> slotFilterItems) {
		packetBuffer.writeInt(slotFilterItems.size());

		slotFilterItems.forEach((slot, item) -> {
			packetBuffer.writeInt(slot);
			packetBuffer.writeInt(Item.getId(item));
		});
	}

	private static Map<Integer, Item> deserializeSlotFilterItems(FriendlyByteBuf packetBuffer) {
		Map<Integer, Item> ret = new HashMap<>();
		int size = packetBuffer.readInt();

		for (int i = 0; i < size; i++) {
			ret.put(packetBuffer.readInt(), Item.byId(packetBuffer.readInt()));
		}

		return ret;
	}

	private static Map<Integer, Integer> deserializeSlotLimitOverrides(FriendlyByteBuf packetBuffer) {
		Map<Integer, Integer> ret = new HashMap<>();

		int size = packetBuffer.readInt();
		for (int i = 0; i < size; i++) {
			ret.put(packetBuffer.readInt(), packetBuffer.readInt());
		}

		return ret;
	}

	private static void serializeSlotLimitOverrides(FriendlyByteBuf packetBuffer, Map<Integer, Integer> slotLimitOverrides) {
		packetBuffer.writeInt(slotLimitOverrides.size());
		slotLimitOverrides.forEach((slot, limit) -> {
			packetBuffer.writeInt(slot);
			packetBuffer.writeInt(limit);
		});
	}

	public static void onMessage(SyncAdditionalSlotInfoMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(SyncAdditionalSlotInfoMessage msg) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !(player.containerMenu instanceof IAdditionalSlotInfoMenu menu)) {
			return;
		}
		menu.updateAdditionalSlotInfo(msg.inaccessibleSlots, msg.slotLimitOverrides, msg.slotFilterItems);
	}
}
