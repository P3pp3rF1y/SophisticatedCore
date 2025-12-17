package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface ITickableUpgrade {
	void tick(@Nullable Entity entity, Level level, BlockPos pos);
}
