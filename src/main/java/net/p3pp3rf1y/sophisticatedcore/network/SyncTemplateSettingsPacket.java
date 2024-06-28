package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTemplateStorage;

import java.util.Map;

public class SyncTemplateSettingsPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "sync_template_settings");
	private final Map<Integer, CompoundTag> playerTemplates;
	private final Map<String, CompoundTag> playerNamedTemplates;

	public SyncTemplateSettingsPacket(Map<Integer, CompoundTag> playerTemplates, Map<String, CompoundTag> playerNamedTemplates) {
		this.playerTemplates = playerTemplates;
		this.playerNamedTemplates = playerNamedTemplates;
	}

	public SyncTemplateSettingsPacket(FriendlyByteBuf buffer) {
		this(buffer.readMap(FriendlyByteBuf::readInt, b -> b.readNbt()), buffer.readMap(FriendlyByteBuf::readUtf, b -> b.readNbt()));
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		SettingsTemplateStorage settingsTemplateStorage = SettingsTemplateStorage.get();
		settingsTemplateStorage.clearPlayerTemplates(player);
		playerTemplates.forEach((k, v) -> settingsTemplateStorage.putPlayerTemplate(player, k, v));
		playerNamedTemplates.forEach((k, v) -> settingsTemplateStorage.putPlayerNamedTemplate(player, k, v));
		if (player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu) {
			settingsContainerMenu.refreshTemplateSlots();
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeMap(playerTemplates, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeNbt);
		buffer.writeMap(playerNamedTemplates, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeNbt);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
