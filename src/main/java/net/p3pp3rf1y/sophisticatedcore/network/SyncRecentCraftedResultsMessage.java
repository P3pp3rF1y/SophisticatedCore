package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.IRecentCraftedResultsRefresh;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.RecentCraftedResultStorage;

import java.util.function.Supplier;

public class SyncRecentCraftedResultsMessage {
	private final CompoundTag recentResults;

	public SyncRecentCraftedResultsMessage(CompoundTag recentResults) {
		this.recentResults = recentResults;
	}

	public static void encode(SyncRecentCraftedResultsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeNbt(msg.recentResults);
	}

	public static SyncRecentCraftedResultsMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncRecentCraftedResultsMessage(packetBuffer.readNbt());
	}

	public static void onMessage(SyncRecentCraftedResultsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(SyncRecentCraftedResultsMessage msg) {
		RecentCraftedResultStorage.updateClientRecentResults(msg.recentResults);
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !(player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
			return;
		}
		menu.getOpenContainer().ifPresent(container -> {
			if (container instanceof IRecentCraftedResultsRefresh refresh) {
				refresh.refreshRecentResultsFromClientCache();
			}
		});
	}
}
