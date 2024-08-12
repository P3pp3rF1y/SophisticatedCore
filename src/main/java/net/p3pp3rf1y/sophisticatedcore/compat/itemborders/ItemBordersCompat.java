package net.p3pp3rf1y.sophisticatedcore.compat.itemborders;

import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class ItemBordersCompat implements ICompat {
	@Override
	public void setup() {
		if (FMLEnvironment.dist.isClient()) {
			ItemBordersCompatClient.registerBorderDecorationRenderer();
		}
	}
}
