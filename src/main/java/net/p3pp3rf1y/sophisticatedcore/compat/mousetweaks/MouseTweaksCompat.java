package net.p3pp3rf1y.sophisticatedcore.compat.mousetweaks;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class MouseTweaksCompat implements ICompat {
	@Override
	public void setup() {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			MouseTweaksCompatClient.restrictSophisticatedScrollInteraction();
		}
	}
}
