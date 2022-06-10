package net.p3pp3rf1y.sophisticatedcore.compat.quark;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.content.management.module.EasyTransferingModule;

public class QuarkCompat implements ICompat {
	@Override
	public void setup() {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (ModuleLoader.INSTANCE.isModuleEnabled(EasyTransferingModule.class)) {
				QuarkButtonManager.addButtons();
			}
		});
		SophisticatedCore.PACKET_HANDLER.registerMessage(TransferMessage.class, TransferMessage::encode, TransferMessage::decode, TransferMessage::onMessage);
	}
}
