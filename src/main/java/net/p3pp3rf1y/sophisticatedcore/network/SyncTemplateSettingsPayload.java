package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTemplateStorage;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.HashMap;
import java.util.Map;

public record SyncTemplateSettingsPayload(Map<Integer, CompoundTag> playerTemplates,
										  Map<String, CompoundTag> playerNamedTemplates) implements CustomPacketPayload {
	public static final Type<SyncTemplateSettingsPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_template_settings"));
	public static final StreamCodec<ByteBuf, SyncTemplateSettingsPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, ByteBufCodecs.COMPOUND_TAG, HashMap::new),
			SyncTemplateSettingsPayload::playerTemplates,
			StreamCodecHelper.ofMap(ByteBufCodecs.STRING_UTF8, ByteBufCodecs.COMPOUND_TAG, HashMap::new),
			SyncTemplateSettingsPayload::playerNamedTemplates,
			SyncTemplateSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncTemplateSettingsPayload payload, IPayloadContext context) {
		Player player = context.player();
		SettingsTemplateStorage settingsTemplateStorage = SettingsTemplateStorage.get();
		settingsTemplateStorage.clearPlayerTemplates(player);
		payload.playerTemplates.forEach((k, v) -> settingsTemplateStorage.putPlayerTemplate(player, k, v));
		payload.playerNamedTemplates.forEach((k, v) -> settingsTemplateStorage.putPlayerNamedTemplate(player, k, v));
		if (player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu) {
			settingsContainerMenu.refreshTemplateSlots();
		}
	}
}
