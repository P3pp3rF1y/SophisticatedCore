package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.p3pp3rf1y.sophisticatedcore.api.IDiscHandler;

import java.util.*;
import java.util.function.Supplier;

public class VanillaDiscHandler implements IDiscHandler<Holder<JukeboxSong>> {

    private static List<Item> musicDiscs = null;

    private static Supplier<List<? extends String>> discBlockListGetter = Collections::emptyList;

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
    public Optional<Integer> getMusicLengthInTicks(ItemStack itemStack, Level level) {
        return getSongInfo(itemStack, level).map(jukeboxSong -> jukeboxSong.value().lengthInTicks());
    }

    @Override
    public boolean supports(ItemStack itemStack) {
        return itemStack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    @Override
    public Optional<ItemStack> getRandomDisc(RandomSource randomSource) {
        List<Item> discs = getMusicDiscs();
        if (!discs.isEmpty()) {
            Item disc = discs.get(randomSource.nextInt(discs.size()));
            return Optional.of(new ItemStack(disc, 1));
        }
        return Optional.empty();
    }

    @Override
    public int getMusicDiscSize() {
        return getMusicDiscs().size();
    }

    private List<Item> getMusicDiscs() {
        if (musicDiscs == null) {
            BuiltInRegistries.ITEM.get(Tags.Items.MUSIC_DISCS).ifPresentOrElse(records -> {
                Set<String> blockedDiscs = new HashSet<>(discBlockListGetter.get());
                musicDiscs = new ArrayList<>();
                records.forEach(musicDisc -> {
                    //noinspection ConstantConditions - by this point the disc has registry name
                    if (!blockedDiscs.contains(musicDisc.getKey().location().toString())) {
                        musicDiscs.add(musicDisc.value());
                    }
                });
            }, () -> musicDiscs = Collections.emptyList());
        }

        return musicDiscs;
    }

    public static void setDiscBlockListGetter(Supplier<List<? extends String>> getter) {
        discBlockListGetter = getter;
    }
}
