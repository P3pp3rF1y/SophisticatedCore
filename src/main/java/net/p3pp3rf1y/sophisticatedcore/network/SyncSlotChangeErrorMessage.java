package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SyncSlotChangeErrorMessage {
	private final UpgradeSlotChangeResult slotChangeError;

	public SyncSlotChangeErrorMessage(UpgradeSlotChangeResult slotChangeError) {
		this.slotChangeError = slotChangeError;
	}

	public static void encode(SyncSlotChangeErrorMessage msg, FriendlyByteBuf packetBuffer) {
		writeSlotChangeResult(packetBuffer, msg.slotChangeError);
	}

	private static void writeSlotChangeResult(FriendlyByteBuf packetBuffer, UpgradeSlotChangeResult slotChangeResult) {
		packetBuffer.writeComponent(slotChangeResult.getErrorMessage().orElse(TextComponent.EMPTY));
		packetBuffer.writeVarIntArray(slotChangeResult.getErrorUpgradeSlots().stream().mapToInt(i -> i).toArray());
		packetBuffer.writeVarIntArray(slotChangeResult.getErrorInventorySlots().stream().mapToInt(i -> i).toArray());
		packetBuffer.writeVarIntArray(slotChangeResult.getErrorInventoryParts().stream().mapToInt(i -> i).toArray());
	}

	public static SyncSlotChangeErrorMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncSlotChangeErrorMessage(new UpgradeSlotChangeResult.Fail(packetBuffer.readComponent(),
				Arrays.stream(packetBuffer.readVarIntArray()).boxed().collect(Collectors.toSet()),
				Arrays.stream(packetBuffer.readVarIntArray()).boxed().collect(Collectors.toSet()),
				Arrays.stream(packetBuffer.readVarIntArray()).boxed().collect(Collectors.toSet())));
	}

	public static void onMessage(SyncSlotChangeErrorMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(SyncSlotChangeErrorMessage msg) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !(player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
			return;
		}
		menu.updateSlotChangeError(msg.slotChangeError);
	}
}
