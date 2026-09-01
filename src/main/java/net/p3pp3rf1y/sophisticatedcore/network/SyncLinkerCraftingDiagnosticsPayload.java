package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.LinkerCraftingDiagnostics;

import java.util.List;

public record SyncLinkerCraftingDiagnosticsPayload(int containerId, List<Diagnostic> diagnostics) implements CustomPacketPayload {
	public static final Type<SyncLinkerCraftingDiagnosticsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("sync_linker_crafting_diagnostics"));
	private static final StreamCodec<ByteBuf, Diagnostic> DIAGNOSTIC_STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, Diagnostic::slot,
			ByteBufCodecs.STRING_UTF8, Diagnostic::statusMessageKey, Diagnostic::new);
	public static final StreamCodec<ByteBuf, SyncLinkerCraftingDiagnosticsPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT,
			SyncLinkerCraftingDiagnosticsPayload::containerId, DIAGNOSTIC_STREAM_CODEC.apply(ByteBufCodecs.list()),
			SyncLinkerCraftingDiagnosticsPayload::diagnostics, SyncLinkerCraftingDiagnosticsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncLinkerCraftingDiagnosticsPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> LinkerCraftingDiagnostics.update(payload.containerId, payload.diagnostics));
	}

	public record Diagnostic(int slot, String statusMessageKey) {
	}
}
