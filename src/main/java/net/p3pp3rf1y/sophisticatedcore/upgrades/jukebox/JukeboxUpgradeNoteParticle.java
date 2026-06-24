package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class JukeboxUpgradeNoteParticle extends SingleQuadParticle {
	protected JukeboxUpgradeNoteParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
		super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprite);
		xd *= 0.01F;
		yd *= 0.05F;
		zd *= 0.01F;
		yd += 0.01D;
		double color = level.getRandom().nextDouble();
		rCol = Math.max(0.0F, Mth.sin(((float) color + 0.0F) * ((float) Math.PI * 2F)) * 0.65F + 0.35F);
		gCol = Math.max(0.0F, Mth.sin(((float) color + 0.33333334F) * ((float) Math.PI * 2F)) * 0.65F + 0.35F);
		bCol = Math.max(0.0F, Mth.sin(((float) color + 0.6666667F) * ((float) Math.PI * 2F)) * 0.65F + 0.35F);
		quadSize *= 1.5F;
		lifetime = 20;
	}

	@Override
	protected Layer getLayer() {
		return Layer.OPAQUE;
	}

	@Override
	public float getQuadSize(float scaleFactor) {
		return quadSize * Mth.clamp((age + scaleFactor) / lifetime * 32.0F, 0.0F, 1.0F);
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime) {
			remove();
		} else {
			move(xd, yd, zd);
			if (y == yo) {
				xd *= 1.1D;
				zd *= 1.1D;
			}
			if (onGround) {
				xd *= 0.7F;
				zd *= 0.7F;
			}
		}
	}

	public static class Factory implements ParticleProvider<JukeboxUpgradeNoteParticleData> {
		private final SpriteSet spriteSet;

		public Factory(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Nullable
		@Override
		public Particle createParticle(JukeboxUpgradeNoteParticleData type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
				double zSpeed, RandomSource randomSource) {
			return new JukeboxUpgradeNoteParticle(level, x, y, z, spriteSet.get(randomSource));
		}
	}
}
