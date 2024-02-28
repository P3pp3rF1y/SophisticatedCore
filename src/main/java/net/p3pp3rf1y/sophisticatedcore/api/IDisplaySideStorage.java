package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.world.level.block.state.BlockState;

public interface IDisplaySideStorage {
	default boolean canChangeDisplaySide(BlockState state) {
		return false;
	}
}
