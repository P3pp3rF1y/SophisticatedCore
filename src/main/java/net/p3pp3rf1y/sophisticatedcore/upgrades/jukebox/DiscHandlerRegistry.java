package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IDiscHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DiscHandlerRegistry {

	private static final List<IDiscHandler<?>> HANDLERS = new ArrayList<>();

	static {
		registerHandler(new VanillaDiscHandler());
	}

	public static List<IDiscHandler<?>> getHandlers() {
		return HANDLERS;
	}

	public static void registerHandler(IDiscHandler<?> handler) {
		HANDLERS.add(handler);
	}

	public static Optional<IDiscHandler<?>> findHandler(ItemStack itemStack) {
		for (IDiscHandler<?> handler : HANDLERS) {
			if (handler.supports(itemStack)) {
				return Optional.of(handler);
			}
		}
		return Optional.empty();
	}

	public static Optional<Integer> getMusicLengthInTicks(ItemStack itemStack, Level level) {
		return findHandler(itemStack).flatMap(handler -> handler.getMusicLengthInTicks(itemStack, level));
	}

	public static boolean isSupported(ItemStack itemStack) {
		return findHandler(itemStack).isPresent();
	}

	public static Optional<ItemStack> getRandomDisc(RandomSource rnd) {
		Map<IDiscHandler<?>, Integer> handlerWeightMap = HANDLERS.stream().collect(Collectors.toMap(handler -> handler, IDiscHandler::getMusicDiscSize));
		int totalWeight = handlerWeightMap.values().stream().mapToInt(Integer::intValue).sum();

		int randomWeight = rnd.nextInt(totalWeight);

		int cumulativeWeight = 0;
		for (Map.Entry<IDiscHandler<?>, Integer> entry : handlerWeightMap.entrySet()) {
			cumulativeWeight += entry.getValue();
			if (randomWeight < cumulativeWeight) {
				return entry.getKey().getRandomDisc(rnd);
			}
		}
		return Optional.empty();
	}
}
