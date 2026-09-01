package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

public record JukeboxPlaybackLocation(ServerLevel level, @Nullable BlockPos blockPos, @Nullable Entity entity) {
	public static JukeboxPlaybackLocation forBlock(ServerLevel level, BlockPos blockPos) {
		return new JukeboxPlaybackLocation(level, blockPos, null);
	}

	public static JukeboxPlaybackLocation forEntity(Entity entity) {
		return new JukeboxPlaybackLocation((ServerLevel) entity.level(), null, entity);
	}
}
