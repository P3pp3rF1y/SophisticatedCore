package net.p3pp3rf1y.sophisticatedcore.init;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.network.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.PlayDiscPacket;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.SoundStopNotificationPacket;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StopDiscPlaybackPacket;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankClickPacket;

public class ModPackets {
	private ModPackets() {
	}

	public static void registerPackets(final RegisterPayloadHandlerEvent event) {
		final IPayloadRegistrar registrar = event.registrar(SophisticatedCore.MOD_ID).versioned("1.0");
		registrar.play(SyncContainerClientDataPacket.ID, SyncContainerClientDataPacket::new, play -> play.server(SyncContainerClientDataPacket::handle));
		registrar.play(TransferFullSlotPacket.ID, TransferFullSlotPacket::new, play -> play.server(TransferFullSlotPacket::handle));
		registrar.play(SyncContainerStacksPacket.ID, SyncContainerStacksPacket::new, play -> play.client(SyncContainerStacksPacket::handle));
		registrar.play(SyncSlotStackPacket.ID, SyncSlotStackPacket::new, play -> play.client(SyncSlotStackPacket::handle));
		registrar.play(SyncPlayerSettingsPacket.ID, SyncPlayerSettingsPacket::new, play -> play.client(SyncPlayerSettingsPacket::handle));
		registrar.play(PlayDiscPacket.ID, PlayDiscPacket::read, play -> play.client(PlayDiscPacket::handle));
		registrar.play(StopDiscPlaybackPacket.ID, StopDiscPlaybackPacket::new, play -> play.client(StopDiscPlaybackPacket::handle));
		registrar.play(SoundStopNotificationPacket.ID, SoundStopNotificationPacket::new, play -> play.server(SoundStopNotificationPacket::handle));
		registrar.play(TankClickPacket.ID, TankClickPacket::new, play -> play.server(TankClickPacket::handle));
		registrar.play(SyncTemplateSettingsPacket.ID, SyncTemplateSettingsPacket::new, play -> play.client(SyncTemplateSettingsPacket::handle));
		registrar.play(SyncAdditionalSlotInfoPacket.ID, SyncAdditionalSlotInfoPacket::new, play -> play.client(SyncAdditionalSlotInfoPacket::handle));
		registrar.play(SyncEmptySlotIconsPacket.ID, SyncEmptySlotIconsPacket::new, play -> play.client(SyncEmptySlotIconsPacket::handle));
		registrar.play(SyncSlotChangeErrorPacket.ID, SyncSlotChangeErrorPacket::new, play -> play.client(SyncSlotChangeErrorPacket::handle));
		registrar.play(SyncDatapackSettingsTemplatePacket.ID, SyncDatapackSettingsTemplatePacket::new, play -> play.client(SyncDatapackSettingsTemplatePacket::handle));
	}
}
