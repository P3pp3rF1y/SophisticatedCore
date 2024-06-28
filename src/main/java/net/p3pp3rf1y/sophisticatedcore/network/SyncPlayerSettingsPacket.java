package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;

import javax.annotation.Nullable;

public class SyncPlayerSettingsPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "sync_player_settings");
	private final String playerTagName;
	@Nullable
	private final CompoundTag settingsNbt;

	public SyncPlayerSettingsPacket(String playerTagName, @Nullable CompoundTag settingsNbt) {
		this.playerTagName = playerTagName;
		this.settingsNbt = settingsNbt;
	}

	public SyncPlayerSettingsPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf(), buffer.readNbt());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (settingsNbt == null) {
			return;
		}
		SettingsManager.setPlayerSettingsTag(player, playerTagName, settingsNbt);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(playerTagName);
		buffer.writeNbt(settingsNbt);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
