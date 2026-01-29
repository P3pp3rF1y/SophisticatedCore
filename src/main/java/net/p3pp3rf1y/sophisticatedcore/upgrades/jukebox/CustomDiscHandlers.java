package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.ICustomDiscHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomDiscHandlers {

    private static final List<ICustomDiscHandler<?>> HANDLERS = new ArrayList<>();

    static {
        registerHandler(new VanillaDiscHandler());
    }

    public static void registerHandler(ICustomDiscHandler<?> handler) {
        HANDLERS.add(handler);
    }

    public static Optional<ICustomDiscHandler<?>> findHandler(ItemStack itemStack) {
        for (ICustomDiscHandler<?> handler : HANDLERS) {
            if (handler.isSupport(itemStack)) {
                return Optional.of(handler);
            }
        }
        return Optional.empty();
    }

    public static Optional<Long> getMusicLengthInTicks(ItemStack itemStack, Level level) {
        return findHandler(itemStack).flatMap(handler -> handler.getMusicLengthInTicks(itemStack, level));
    }

    public static boolean isSupport(ItemStack itemStack) {
        return findHandler(itemStack).isPresent();
    }
}
