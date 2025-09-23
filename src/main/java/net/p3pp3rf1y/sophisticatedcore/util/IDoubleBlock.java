package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public interface IDoubleBlock {
	Optional<BlockPos> getOtherPosition(BlockState state, BlockPos pos);
}
