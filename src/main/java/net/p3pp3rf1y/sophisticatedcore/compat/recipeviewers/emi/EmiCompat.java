package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CommonMessages;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class EmiCompat implements ICompat {
	@Override
	public void setup() {
		PacketHandler.INSTANCE.registerMessage(EmiTransferRecipeMessage.class, EmiTransferRecipeMessage::encode, EmiTransferRecipeMessage::decode, EmiTransferRecipeMessage::onMessage);
		CommonMessages.registerMessages();
	}
}
