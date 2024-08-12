package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;

public class JukeboxUpgradeNoteParticleData extends ParticleType<JukeboxUpgradeNoteParticleData> implements ParticleOptions {
	public JukeboxUpgradeNoteParticleData() {
		super(false);
	}

	@Override
	public JukeboxUpgradeNoteParticleData getType() {
		return ModParticles.JUKEBOX_NOTE.get();
	}

	private final MapCodec<JukeboxUpgradeNoteParticleData> codec = MapCodec.unit(this::getType);
	private final StreamCodec<RegistryFriendlyByteBuf, JukeboxUpgradeNoteParticleData> streamCodec = StreamCodec.unit(this);

	@Override
	public MapCodec<JukeboxUpgradeNoteParticleData> codec() {
		return codec;
	}

	@Override
	public StreamCodec<? super RegistryFriendlyByteBuf, JukeboxUpgradeNoteParticleData> streamCodec() {
		return streamCodec;
	}
}
