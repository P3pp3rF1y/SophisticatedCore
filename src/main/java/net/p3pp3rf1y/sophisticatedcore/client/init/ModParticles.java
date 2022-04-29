package net.p3pp3rf1y.sophisticatedcore.client.init;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeNoteParticle;

public class ModParticles {
	private ModParticles() {}

	@SuppressWarnings("unused") // need this to register the event correctly
	public static void registerFactories(ParticleFactoryRegisterEvent event) {
		Minecraft.getInstance().particleEngine.register(net.p3pp3rf1y.sophisticatedcore.init.ModParticles.JUKEBOX_NOTE.get(), JukeboxUpgradeNoteParticle.Factory::new);
	}
}
