package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTemplateStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncTemplateSettingsMessage {
	private final Map<Integer, CompoundTag> playerTemplates;

	public SyncTemplateSettingsMessage(Map<Integer, CompoundTag> playerTemplates) {
		this.playerTemplates = playerTemplates;
	}

	public static void encode(SyncTemplateSettingsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeInt(msg.playerTemplates.size());
		msg.playerTemplates.forEach((k, v) -> {
			packetBuffer.writeInt(k);
			packetBuffer.writeNbt(v);
		});
	}

	public static SyncTemplateSettingsMessage decode(FriendlyByteBuf packetBuffer) {
		int numberOfRecords = packetBuffer.readInt();
		Map<Integer, CompoundTag> playerTemplates = new HashMap<>();
		for (int i = 0; i < numberOfRecords; i++) {
			playerTemplates.put(packetBuffer.readInt(), packetBuffer.readNbt());
		}

		return new SyncTemplateSettingsMessage(playerTemplates);
	}

	public static void onMessage(SyncTemplateSettingsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleMessage(SyncTemplateSettingsMessage message) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		SettingsTemplateStorage settingsTemplateStorage = SettingsTemplateStorage.get();
		message.playerTemplates.forEach((k, v) -> settingsTemplateStorage.putPlayerTemplate(player, k, v));
	}
}
