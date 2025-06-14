package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;
import org.joml.Vector3f;

import java.util.function.UnaryOperator;

public interface IUpgradeClientTickHandler<T extends IUpgradeClientData> {
	void onClientTick(Level level, RandomSource rand, UnaryOperator<Vector3f> getPositionFromOffset, T upgradeClientData);
}
