package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IDiscHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiscHandlerRegistry {

    private static final List<IDiscHandler<?>> HANDLERS = new ArrayList<>();

    static {
        registerHandler(new VanillaDiscHandler());
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

    public static Optional<Long> getMusicLengthInTicks(ItemStack itemStack, Level level) {
        return findHandler(itemStack).flatMap(handler -> handler.getMusicLengthInTicks(itemStack, level));
    }

    public static boolean isSupported(ItemStack itemStack) {
        return findHandler(itemStack).isPresent();
    }
}
