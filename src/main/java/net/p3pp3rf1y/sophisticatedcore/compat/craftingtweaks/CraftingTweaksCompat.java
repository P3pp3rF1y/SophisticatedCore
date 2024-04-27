package net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks;

import net.blay09.mods.craftingtweaks.api.CraftingTweaksAPI;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class CraftingTweaksCompat implements ICompat {
	@Override
	public void setup() {
		CraftingTweaksAPI.registerCraftingGridProvider(new CraftingUpgradeTweakProvider());
		if (FMLEnvironment.dist.isClient()) {
			CraftingTweaksCompatClient.setup();
		}
	}
}
