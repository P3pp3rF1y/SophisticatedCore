package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerEndpointRecipe;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public record RequestLinkerCraftingDiagnosticsPayload(int containerId) implements CustomPacketPayload {
	public static final Type<RequestLinkerCraftingDiagnosticsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("request_linker_crafting_diagnostics"));
	public static final StreamCodec<ByteBuf, RequestLinkerCraftingDiagnosticsPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT,
			RequestLinkerCraftingDiagnosticsPayload::containerId, RequestLinkerCraftingDiagnosticsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestLinkerCraftingDiagnosticsPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player) || player.containerMenu.containerId != payload.containerId) {
				return;
			}

			Map<Container, List<Slot>> craftingSlots = new IdentityHashMap<>();
			for (Slot slot : player.containerMenu.slots) {
				if (slot.container instanceof CraftingContainer) {
					craftingSlots.computeIfAbsent(slot.container, ignored -> new ArrayList<>()).add(slot);
				}
			}

			List<SyncLinkerCraftingDiagnosticsPayload.Diagnostic> diagnostics = new ArrayList<>();
			craftingSlots.forEach((container, slots) -> addDiagnostic(player, container, slots, diagnostics));
			if (player.containerMenu instanceof StorageContainerMenuBase<?> storageContainer) {
				storageContainer.getUpgradeContainers().values().stream().filter(ICraftingContainer.class::isInstance).map(ICraftingContainer.class::cast)
						.forEach(container -> addDiagnostic(player, container.getCraftMatrix(), container.getRecipeSlots(), diagnostics));
			}
			PacketDistributor.sendToPlayer(player, new SyncLinkerCraftingDiagnosticsPayload(payload.containerId, diagnostics));
		});
	}

	private static void addDiagnostic(ServerPlayer player, Container craftingContainer, List<Slot> slots,
			List<SyncLinkerCraftingDiagnosticsPayload.Diagnostic> diagnostics) {
		EnderLinkerEndpointRecipe.getCraftingDiagnostic(player.level(), craftingContainer)
				.ifPresent(diagnostic -> slots.stream().filter(slot -> slot.getContainerSlot() == diagnostic.slot()).findFirst().ifPresent(
						slot -> diagnostics.add(new SyncLinkerCraftingDiagnosticsPayload.Diagnostic(slot.index, diagnostic.failure().statusMessage()))));
	}
}
