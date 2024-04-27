package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SyncSlotChangeErrorPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedCore.MOD_ID, "sync_slot_change_error");
	private final UpgradeSlotChangeResult slotChangeError;

	public SyncSlotChangeErrorPacket(UpgradeSlotChangeResult slotChangeError) {
		this.slotChangeError = slotChangeError;
	}

	public SyncSlotChangeErrorPacket(FriendlyByteBuf buffer) {
		this(new UpgradeSlotChangeResult.Fail(buffer.readComponent(),
				Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet()),
				Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet()),
				Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet())));
	}

	private void writeSlotChangeResult(FriendlyByteBuf buffer, UpgradeSlotChangeResult slotChangeResult) {
		buffer.writeComponent(slotChangeResult.getErrorMessage().orElse(Component.empty()));
		buffer.writeVarIntArray(slotChangeResult.getErrorUpgradeSlots().stream().mapToInt(i -> i).toArray());
		buffer.writeVarIntArray(slotChangeResult.getErrorInventorySlots().stream().mapToInt(i -> i).toArray());
		buffer.writeVarIntArray(slotChangeResult.getErrorInventoryParts().stream().mapToInt(i -> i).toArray());
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
			return;
		}
		menu.updateSlotChangeError(slotChangeError);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		writeSlotChangeResult(buffer, slotChangeError);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
