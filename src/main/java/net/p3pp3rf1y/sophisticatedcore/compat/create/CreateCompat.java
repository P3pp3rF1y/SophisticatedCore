package net.p3pp3rf1y.sophisticatedcore.compat.create;

import net.minecraftforge.network.NetworkDirection;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class CreateCompat implements ICompat {
	@Override
	public void setup() {
		PacketHandler.INSTANCE.registerMessage(MountedStorageContentsMessage.class, MountedStorageContentsMessage::encode,
				MountedStorageContentsMessage::decode, MountedStorageContentsMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		PacketHandler.INSTANCE.registerMessage(MountedStorageUpdateMessage.class, MountedStorageUpdateMessage::encode, MountedStorageUpdateMessage::decode,
				MountedStorageUpdateMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
	}
}
