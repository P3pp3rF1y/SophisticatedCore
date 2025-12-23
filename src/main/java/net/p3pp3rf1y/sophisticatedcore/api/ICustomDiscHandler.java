package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public interface ICustomDiscHandler<I> {

    Optional<I> getSongInfo(ItemStack itemStack);

    void playDisc(ServerLevel serverLevel, BlockPos position, UUID storageUuid, ItemStack discItemStack, Runnable onFinished);

    void playDisc(ServerLevel serverLevel, Vec3 position, UUID storageUuid, ItemStack discItemStack, int entityId, Runnable onFinished);

    Optional<Long> getMusicLengthInTicks(ItemStack itemStack);

    boolean isSupport(ItemStack itemStack);

}
