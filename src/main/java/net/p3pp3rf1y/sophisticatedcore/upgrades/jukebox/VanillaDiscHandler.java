package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.api.ICustomDiscHandler;

import java.util.Optional;
import java.util.UUID;

public class VanillaDiscHandler implements ICustomDiscHandler<Holder<JukeboxSong>> {

    @Override
    public Optional<Holder<JukeboxSong>> getSongInfo(ItemStack itemStack, Level level) {
        return JukeboxSong.fromStack(level.registryAccess(), itemStack);
    }

    @Override
    public void playDisc(ServerLevel serverLevel, BlockPos position, UUID storageUuid, ItemStack discItemStack, Runnable onFinished) {
        getSongInfo(discItemStack, serverLevel).ifPresent(jukeboxSong -> {
            ServerStorageSoundHandler.startPlayingDisc(serverLevel, position, storageUuid, jukeboxSong, onFinished);
        });
    }

    @Override
    public void playDisc(ServerLevel serverLevel, Vec3 position, UUID storageUuid, ItemStack discItemStack, int entityId, Runnable onFinished) {
        getSongInfo(discItemStack, serverLevel).ifPresent(jukeboxSong -> {
            ServerStorageSoundHandler.startPlayingDisc(serverLevel, position, storageUuid, entityId, jukeboxSong, onFinished);
        });
    }

    @Override
    public Optional<Long> getMusicLengthInTicks(ItemStack itemStack, Level level) {
        return getSongInfo(itemStack, level).map(jukeboxSong -> (long) jukeboxSong.value().lengthInTicks());
    }

    @Override
    public boolean isSupport(ItemStack itemStack) {
        return itemStack.has(DataComponents.JUKEBOX_PLAYABLE);
    }
}
