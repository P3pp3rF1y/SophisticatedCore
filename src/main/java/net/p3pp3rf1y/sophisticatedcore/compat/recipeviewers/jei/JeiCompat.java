package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CommonMessages;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class JeiCompat implements ICompat {
	@Override
	public void setup() {
		PacketHandler.INSTANCE.registerMessage(JeiTransferRecipeMessage.class, JeiTransferRecipeMessage::encode, JeiTransferRecipeMessage::decode, JeiTransferRecipeMessage::onMessage);
		CommonMessages.registerMessages();
		if (FMLEnvironment.dist == Dist.CLIENT) {
			JeiClientCompat.init();
		}
	}
}
