
package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IAdditionalSlotInfoMenu;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record SyncAdditionalSlotInfoPayload(Set<Integer> inaccessibleSlots, Map<Integer, Integer> slotLimitOverrides,
											Map<Integer, Holder<Item>> slotFilterItems) implements CustomPacketPayload {
	public static final Type<SyncAdditionalSlotInfoPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_additional_slot_info"));
	private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> ITEM_STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ITEM);
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncAdditionalSlotInfoPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofCollection(ByteBufCodecs.INT, HashSet::new),
			SyncAdditionalSlotInfoPayload::inaccessibleSlots,
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, ByteBufCodecs.INT, HashMap::new),
			SyncAdditionalSlotInfoPayload::slotLimitOverrides,
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, ITEM_STREAM_CODEC, HashMap::new),
			SyncAdditionalSlotInfoPayload::slotFilterItems,
			SyncAdditionalSlotInfoPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncAdditionalSlotInfoPayload payload, IPayloadContext context) {
		if (!(context.player().containerMenu instanceof IAdditionalSlotInfoMenu menu)) {
			return;
		}
		menu.updateAdditionalSlotInfo(payload.inaccessibleSlots, payload.slotLimitOverrides, payload.slotFilterItems);
	}
}
