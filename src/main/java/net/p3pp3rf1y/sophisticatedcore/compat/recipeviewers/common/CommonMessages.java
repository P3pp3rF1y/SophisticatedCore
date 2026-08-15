package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraftforge.network.NetworkDirection;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class CommonMessages {
	private static boolean registered = false;

	public static void registerMessages() {
		if (registered) {
			return;
		}

		registered = true;
		PacketHandler.INSTANCE.registerMessage(SetGhostSlotMessage.class, SetGhostSlotMessage::encode, SetGhostSlotMessage::decode,
				SetGhostSlotMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
		PacketHandler.INSTANCE.registerMessage(SetMemorySlotMessage.class, SetMemorySlotMessage::encode, SetMemorySlotMessage::decode,
				SetMemorySlotMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
	}
}
