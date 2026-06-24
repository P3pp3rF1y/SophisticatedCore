package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.api.IDiscHandler;

import java.util.*;
import java.util.function.Supplier;

public class VanillaDiscHandler implements IDiscHandler<RecordItem> {

	private static List<Item> musicDiscs = null;

	private static Supplier<List<? extends String>> discBlockListGetter = Collections::emptyList;

	@Override
	public Optional<RecordItem> getSongInfo(ItemStack itemStack, Level level) {
		return itemStack.getItem() instanceof RecordItem recordItem ? Optional.of(recordItem) : Optional.empty();
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
		return getSongInfo(itemStack, level).map(RecordItem::getLengthInTicks);
	}

	@Override
	public boolean supports(ItemStack itemStack) {
		return itemStack.getItem() instanceof RecordItem;
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
			Map<SoundEvent, RecordItem> records = ObfuscationReflectionHelper.getPrivateValue(RecordItem.class, null, "f_43032_");
			if (records == null) {
				musicDiscs = new ArrayList<>();
			} else {
				Set<String> blockedDiscs = new HashSet<>(discBlockListGetter.get());
				musicDiscs = new ArrayList<>();
				records.forEach((sound, musicDisc) -> {
					// noinspection ConstantConditions - by this point the disc has registry name
					if (!blockedDiscs.contains(ForgeRegistries.ITEMS.getKey(musicDisc).toString())) {
						musicDiscs.add(musicDisc);
					}
				});
			}
		}

		return musicDiscs;
	}

	public static void setDiscBlockListGetter(Supplier<List<? extends String>> getter) {
		discBlockListGetter = getter;
	}
}
