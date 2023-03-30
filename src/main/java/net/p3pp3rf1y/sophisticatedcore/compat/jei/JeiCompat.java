package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class JeiCompat implements ICompat {
	@Override
	public void setup() {
		PacketHandler.INSTANCE.registerMessage(TransferRecipeMessage.class, TransferRecipeMessage::encode, TransferRecipeMessage::decode, TransferRecipeMessage::onMessage);
		PacketHandler.INSTANCE.registerMessage(SetGhostSlotMessage.class, SetGhostSlotMessage::encode, SetGhostSlotMessage::decode, SetGhostSlotMessage::onMessage);
		PacketHandler.INSTANCE.registerMessage(SetMemorySlotMessage.class, SetMemorySlotMessage::encode, SetMemorySlotMessage::decode, SetMemorySlotMessage::onMessage);
	}
}
