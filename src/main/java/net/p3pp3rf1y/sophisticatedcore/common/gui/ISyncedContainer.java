package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.nbt.CompoundTag;

public interface ISyncedContainer {
	void handleMessage(CompoundTag data);

	default void handlePacket(CompoundTag data) {
		handleMessage(data);
	}
}
