package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTemplateStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncTemplateSettingsMessage {
	private final Map<Integer, CompoundTag> playerTemplates;
	private final Map<String, CompoundTag> playerNamedTemplates;

	public SyncTemplateSettingsMessage(Map<Integer, CompoundTag> playerTemplates, Map<String, CompoundTag> playerNamedTemplates) {
		this.playerTemplates = playerTemplates;
		this.playerNamedTemplates = playerNamedTemplates;
	}

	public static void encode(SyncTemplateSettingsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeInt(msg.playerTemplates.size());
		msg.playerTemplates.forEach((k, v) -> {
			packetBuffer.writeInt(k);
			packetBuffer.writeNbt(v);
		});
		packetBuffer.writeInt(msg.playerNamedTemplates.size());
		msg.playerNamedTemplates.forEach((k, v) -> {
			packetBuffer.writeUtf(k);
			packetBuffer.writeNbt(v);
		});
	}

	public static SyncTemplateSettingsMessage decode(FriendlyByteBuf packetBuffer) {
		int numberOfRecords = packetBuffer.readInt();
		Map<Integer, CompoundTag> playerTemplates = new HashMap<>();
		for (int i = 0; i < numberOfRecords; i++) {
			playerTemplates.put(packetBuffer.readInt(), packetBuffer.readNbt());
		}
		numberOfRecords = packetBuffer.readInt();
		Map<String, CompoundTag> playerNamedTemplates = new HashMap<>();
		for (int i = 0; i < numberOfRecords; i++) {
			playerNamedTemplates.put(packetBuffer.readUtf(), packetBuffer.readNbt());
		}

		return new SyncTemplateSettingsMessage(playerTemplates, playerNamedTemplates);
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
		settingsTemplateStorage.clearPlayerTemplates(player);
		message.playerTemplates.forEach((k, v) -> settingsTemplateStorage.putPlayerTemplate(player, k, v));
		message.playerNamedTemplates.forEach((k, v) -> settingsTemplateStorage.putPlayerNamedTemplate(player, k, v));
		if (player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu) {
			settingsContainerMenu.refreshTemplateSlots();
		}
	}
}
