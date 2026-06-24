package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CommonMessages;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class ReiCompat implements ICompat {
	@Override
	public void setup() {
		PacketHandler.INSTANCE.registerMessage(ReiTransferRecipeMessage.class, ReiTransferRecipeMessage::encode, ReiTransferRecipeMessage::decode,
				ReiTransferRecipeMessage::onMessage);
		CommonMessages.registerMessages();
	}
}
