package net.p3pp3rf1y.sophisticatedcore.compat.create;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class CreateCompat implements ICompat {
	@Override
	public void setup() {
		PacketHandler.INSTANCE.registerMessage(MountedStorageContentsMessage.class, MountedStorageContentsMessage::encode, MountedStorageContentsMessage::decode, MountedStorageContentsMessage::onMessage);
		PacketHandler.INSTANCE.registerMessage(OpenMountedStorageInventoryMessage.class, OpenMountedStorageInventoryMessage::encode, OpenMountedStorageInventoryMessage::decode, OpenMountedStorageInventoryMessage::onMessage);
	}
}
