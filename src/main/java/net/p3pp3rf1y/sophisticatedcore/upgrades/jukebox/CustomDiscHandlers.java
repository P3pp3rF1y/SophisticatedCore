package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.ICustomDiscHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomDiscHandlers {

    private static final List<ICustomDiscHandler<?>> HANDLERS = new ArrayList<>();

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

    public static Optional<Long> getMusicLengthInTicks(ItemStack itemStack) {
        return findHandler(itemStack).flatMap(handler -> handler.getMusicLengthInTicks(itemStack));
    }

    public static boolean isVanillaDisc(ItemStack itemStack) {
        return itemStack.has(DataComponents.JUKEBOX_PLAYABLE);
    }
}
