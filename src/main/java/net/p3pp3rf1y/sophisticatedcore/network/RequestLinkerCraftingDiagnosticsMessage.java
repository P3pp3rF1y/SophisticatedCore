package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerEndpointRecipe;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record RequestLinkerCraftingDiagnosticsMessage(int containerId) {
	public static void encode(RequestLinkerCraftingDiagnosticsMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.containerId);
	}

	public static RequestLinkerCraftingDiagnosticsMessage decode(FriendlyByteBuf buffer) {
		return new RequestLinkerCraftingDiagnosticsMessage(buffer.readInt());
	}

	public static void onMessage(RequestLinkerCraftingDiagnosticsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handle(context.getSender(), message));
		context.setPacketHandled(true);
	}

	private static void handle(ServerPlayer player, RequestLinkerCraftingDiagnosticsMessage message) {
		if (player == null || player.containerMenu.containerId != message.containerId) {
			return;
		}
		Map<Container, Slot> craftingSlots = new HashMap<>();
		for (Slot slot : player.containerMenu.slots) {
			if (slot.container instanceof CraftingContainer) {
				craftingSlots.put(slot.container, slot);
			}
		}
		Map<Integer, String> diagnostics = new HashMap<>();
		craftingSlots.forEach((container, ignored) -> EnderLinkerEndpointRecipe.getCraftingDiagnostic(player.serverLevel(), container).ifPresent(diagnostic -> {
			for (Slot slot : player.containerMenu.slots) {
				if (slot.container == container && slot.getContainerSlot() == diagnostic.slot()) {
					diagnostics.put(slot.index, diagnostic.failure().statusMessage());
				}
			}
		}));
		PacketHandler.INSTANCE.sendToClient(player, new SyncLinkerCraftingDiagnosticsMessage(message.containerId, diagnostics));
	}
}
