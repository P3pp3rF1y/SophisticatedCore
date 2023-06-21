package net.p3pp3rf1y.sophisticatedcore.client.init;

import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeNoteParticle;

public class ModParticles {
	private ModParticles() {}

	@SuppressWarnings("unused") // need this to register the event correctly
	public static void registerFactories(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(net.p3pp3rf1y.sophisticatedcore.init.ModParticles.JUKEBOX_NOTE.get(), JukeboxUpgradeNoteParticle.Factory::new);
	}
}
