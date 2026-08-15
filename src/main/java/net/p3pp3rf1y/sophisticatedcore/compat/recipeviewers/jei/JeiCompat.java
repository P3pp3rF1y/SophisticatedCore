package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import net.minecraftforge.network.NetworkDirection;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CommonMessages;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class JeiCompat implements ICompat {
	@Override
	public void setup() {
		PacketHandler.INSTANCE.registerMessage(JeiTransferRecipeMessage.class, JeiTransferRecipeMessage::encode, JeiTransferRecipeMessage::decode,
				JeiTransferRecipeMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
		CommonMessages.registerMessages();
	}
}
