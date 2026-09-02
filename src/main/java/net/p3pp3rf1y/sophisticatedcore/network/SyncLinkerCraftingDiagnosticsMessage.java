package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.client.LinkerCraftingDiagnostics;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SyncLinkerCraftingDiagnosticsMessage(int containerId, Map<Integer, String> diagnostics) {
	public static void encode(SyncLinkerCraftingDiagnosticsMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.containerId);
		buffer.writeVarInt(message.diagnostics.size());
		message.diagnostics.forEach((slot, status) -> {
			buffer.writeVarInt(slot);
			buffer.writeUtf(status);
		});
	}

	public static SyncLinkerCraftingDiagnosticsMessage decode(FriendlyByteBuf buffer) {
		int containerId = buffer.readInt();
		int size = buffer.readVarInt();
		Map<Integer, String> diagnostics = new HashMap<>();
		for (int index = 0; index < size; index++) {
			diagnostics.put(buffer.readVarInt(), buffer.readUtf());
		}
		return new SyncLinkerCraftingDiagnosticsMessage(containerId, diagnostics);
	}

	public static void onMessage(SyncLinkerCraftingDiagnosticsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> LinkerCraftingDiagnostics.update(message.containerId, message.diagnostics));
		context.setPacketHandled(true);
	}
}
