package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;

import javax.annotation.Nullable;

public class SyncDatapackSettingsTemplatePacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "sync_datapack_settings_template");
	private final String datapack;
	private final String templateName;
	private final CompoundTag settingsNbt;

	public SyncDatapackSettingsTemplatePacket(String datapack, String templateName, @Nullable CompoundTag settingsNbt) {
		this.datapack = datapack;
		this.templateName = templateName;
		this.settingsNbt = settingsNbt;
	}

	public SyncDatapackSettingsTemplatePacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf(), buffer.readUtf(), buffer.readNbt());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (settingsNbt == null) {
			return;
		}
		DatapackSettingsTemplateManager.putTemplate(datapack, templateName, settingsNbt);
		if (player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu) {
			settingsContainerMenu.refreshTemplateSlots();
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(datapack);
		buffer.writeUtf(templateName);
		buffer.writeNbt(settingsNbt);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
