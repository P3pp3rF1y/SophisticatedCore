package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.IRecentCraftedResultsRefresh;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.RecentCraftedResultStorage;

public record SyncRecentCraftedResultsPayload(CompoundTag recentResults) implements CustomPacketPayload {
	public static final Type<SyncRecentCraftedResultsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("sync_recent_crafted_results"));
	public static final StreamCodec<ByteBuf, SyncRecentCraftedResultsPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,
			SyncRecentCraftedResultsPayload::recentResults, SyncRecentCraftedResultsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncRecentCraftedResultsPayload payload, IPayloadContext context) {
		RecentCraftedResultStorage.updateClientRecentResults(payload.recentResults);
		if (context.player().containerMenu instanceof StorageContainerMenuBase<?> menu) {
			menu.getOpenContainer().ifPresent(container -> {
				if (container instanceof IRecentCraftedResultsRefresh refresh) {
					refresh.refreshRecentResultsFromClientCache();
				}
			});
		}
	}
}
