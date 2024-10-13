package net.p3pp3rf1y.sophisticatedcore.compat;

import net.neoforged.bus.api.IEventBus;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class CompatRegistry {
	private static final Map<CompatInfo, List<Supplier<Function<IEventBus, ICompat>>>> compatFactories = new ConcurrentHashMap<>();
	private static final Map<CompatInfo, List<ICompat>> loadedCompats = new ConcurrentHashMap<>();

	public static void registerCompat(CompatInfo info, Supplier<Function<IEventBus, ICompat>> factory) {
		compatFactories.computeIfAbsent(info, k -> new ArrayList<>()).add(factory);
	}

	public static void setupCompats() {
		loadedCompats.values().forEach(compats -> compats.forEach(ICompat::setup));
	}

	public static void initCompats(IEventBus modBus) {
		compatFactories.forEach((compatInfo, factories) -> {
			if (compatInfo.isLoaded()) {
				factories.forEach(factory -> {
					try {
						loadedCompats.computeIfAbsent(compatInfo, k -> new ArrayList<>()).add(factory.get().apply(modBus));
					} catch (Exception e) {
						SophisticatedCore.LOGGER.error("Error instantiating compatibility ", e);
					}
				});
			}
		});
		loadedCompats.values().forEach(compats -> compats.forEach(compat -> compat.init(modBus)));
	}
}
