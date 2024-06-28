
package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IAdditionalSlotInfoMenu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SyncAdditionalSlotInfoPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "sync_additional_slot_info");
	private final Set<Integer> inaccessibleSlots;
	private final Map<Integer, Integer> slotLimitOverrides;
	private final Map<Integer, Item> slotFilterItems;

	public SyncAdditionalSlotInfoPacket(Set<Integer> inaccessibleSlots, Map<Integer, Integer> slotLimitOverrides, Map<Integer, Item> slotFilterItems) {
		this.inaccessibleSlots = inaccessibleSlots;
		this.slotLimitOverrides = slotLimitOverrides;
		this.slotFilterItems = slotFilterItems;
	}

	public SyncAdditionalSlotInfoPacket(FriendlyByteBuf buffer) {
		this(Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet()), deserializeSlotLimitOverrides(buffer), deserializeSlotFilterItems(buffer));

	}

	private static Map<Integer, Item> deserializeSlotFilterItems(FriendlyByteBuf buffer) {
		Map<Integer, Item> ret = new HashMap<>();
		int size = buffer.readInt();

		for (int i = 0; i < size; i++) {
			ret.put(buffer.readInt(), Item.byId(buffer.readInt()));
		}

		return ret;
	}

	private void serializeSlotFilterItems(FriendlyByteBuf buffer, Map<Integer, Item> slotFilterItems) {
		buffer.writeInt(slotFilterItems.size());

		slotFilterItems.forEach((slot, item) -> {
			buffer.writeInt(slot);
			buffer.writeInt(Item.getId(item));
		});
	}

	private static Map<Integer, Integer> deserializeSlotLimitOverrides(FriendlyByteBuf buffer) {
		Map<Integer, Integer> ret = new HashMap<>();

		int size = buffer.readInt();
		for (int i = 0; i < size; i++) {
			ret.put(buffer.readInt(), buffer.readInt());
		}

		return ret;
	}

	private void serializeSlotLimitOverrides(FriendlyByteBuf buffer, Map<Integer, Integer> slotLimitOverrides) {
		buffer.writeInt(slotLimitOverrides.size());
		slotLimitOverrides.forEach((slot, limit) -> {
			buffer.writeInt(slot);
			buffer.writeInt(limit);
		});
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (!(player.containerMenu instanceof IAdditionalSlotInfoMenu menu)) {
			return;
		}
		menu.updateAdditionalSlotInfo(inaccessibleSlots, slotLimitOverrides, slotFilterItems);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarIntArray(inaccessibleSlots.stream().mapToInt(i -> i).toArray());
		serializeSlotLimitOverrides(buffer, slotLimitOverrides);
		serializeSlotFilterItems(buffer, slotFilterItems);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
