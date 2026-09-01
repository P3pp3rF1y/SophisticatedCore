package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public interface IJukeboxPlaybackLocationProvider {
	Optional<JukeboxPlaybackLocation> getJukeboxPlaybackLocation(ServerLevel initiatingLevel);
}
