package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import javax.annotation.Nullable;

public record SyncDatapackSettingsTemplatePayload(String datapack, String templateName, @Nullable CompoundTag settingsNbt) implements CustomPacketPayload {
	public static final Type<SyncDatapackSettingsTemplatePayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_datapack_settings_template"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncDatapackSettingsTemplatePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			SyncDatapackSettingsTemplatePayload::datapack,
			ByteBufCodecs.STRING_UTF8,
			SyncDatapackSettingsTemplatePayload::templateName,
			StreamCodecHelper.ofNullable(ByteBufCodecs.COMPOUND_TAG),
			SyncDatapackSettingsTemplatePayload::settingsNbt,
			SyncDatapackSettingsTemplatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncDatapackSettingsTemplatePayload payload, IPayloadContext context) {
		if (payload.settingsNbt == null) {
			return;
		}
		DatapackSettingsTemplateManager.putTemplate(payload.datapack, payload.templateName, payload.settingsNbt);
		if (context.player().containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu) {
			settingsContainerMenu.refreshTemplateSlots();
		}
	}
}
