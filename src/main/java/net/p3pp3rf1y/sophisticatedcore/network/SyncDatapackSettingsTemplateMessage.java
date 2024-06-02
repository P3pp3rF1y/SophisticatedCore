package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SyncDatapackSettingsTemplateMessage {
	private final String datapack;
	private final String templateName;
	private final CompoundTag settingsNbt;

	public SyncDatapackSettingsTemplateMessage(String datapack, String templateName, @Nullable CompoundTag settingsNbt) {
		this.datapack = datapack;
		this.templateName = templateName;
		this.settingsNbt = settingsNbt;
	}

	public static void encode(SyncDatapackSettingsTemplateMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeUtf(msg.datapack);
		packetBuffer.writeUtf(msg.templateName);
		packetBuffer.writeNbt(msg.settingsNbt);
	}

	public static SyncDatapackSettingsTemplateMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncDatapackSettingsTemplateMessage(packetBuffer.readUtf(), packetBuffer.readUtf(), packetBuffer.readNbt());
	}

	public static void onMessage(SyncDatapackSettingsTemplateMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleMessage(SyncDatapackSettingsTemplateMessage message) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		DatapackSettingsTemplateManager.putTemplate(message.datapack, message.templateName, message.settingsNbt);
		if (player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu) {
			settingsContainerMenu.refreshTemplateSlots();
		}
	}
}
