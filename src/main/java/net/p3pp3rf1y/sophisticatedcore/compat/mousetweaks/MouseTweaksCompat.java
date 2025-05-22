package net.p3pp3rf1y.sophisticatedcore.compat.mousetweaks;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class MouseTweaksCompat implements ICompat {
	@Override
	public void setup() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			MouseTweaksCompatClient.restrictSophisticatedScrollInteraction();
		}
	}
}
