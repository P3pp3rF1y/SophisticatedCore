package net.p3pp3rf1y.sophisticatedcore.compat;

import net.neoforged.bus.api.IEventBus;

public interface ICompat {
	default void init(IEventBus modBus) {
		//noop
	}
	void setup();
}
