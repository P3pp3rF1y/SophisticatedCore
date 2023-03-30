package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IAdditionalSlotInfoMenu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SyncEmptySlotIconsMessage {
	private final Map<ResourceLocation, Set<Integer>> emptySlotIcons;

	public SyncEmptySlotIconsMessage(Map<ResourceLocation, Set<Integer>> emptySlotIcons) {
		this.emptySlotIcons = emptySlotIcons;
	}

	public static void encode(SyncEmptySlotIconsMessage msg, FriendlyByteBuf packetBuffer) {
		writeEmptySlotTextures(packetBuffer, msg.emptySlotIcons);
	}

	public static void writeEmptySlotTextures(FriendlyByteBuf buffer, Map<ResourceLocation, Set<Integer>> map) {
		buffer.writeInt(map.size());

		for (Map.Entry<ResourceLocation, Set<Integer>> entry : map.entrySet()) {
			buffer.writeResourceLocation(entry.getKey());
			buffer.writeVarIntArray(entry.getValue().stream().mapToInt(i -> i).toArray());
		}
	}

	public static SyncEmptySlotIconsMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncEmptySlotIconsMessage(readEmptySlotTextures(packetBuffer));
	}

	public static Map<ResourceLocation, Set<Integer>> readEmptySlotTextures(FriendlyByteBuf buffer) {
		Map<ResourceLocation, Set<Integer>> map = new HashMap<>();

		int size = buffer.readInt();
		for (int i = 0; i < size; i++) {
			ResourceLocation resourceLocation = buffer.readResourceLocation();
			map.put(resourceLocation, Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet()));
		}

		return map;
	}

	public static void onMessage(SyncEmptySlotIconsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(SyncEmptySlotIconsMessage msg) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !(player.containerMenu instanceof IAdditionalSlotInfoMenu menu)) {
			return;
		}
		menu.updateEmptySlotIcons(msg.emptySlotIcons);
	}
}
