package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public interface IDiscHandler<I> {

    Optional<I> getSongInfo(ItemStack itemStack, Level level);

    void playDisc(ServerLevel serverLevel, BlockPos position, UUID storageUuid, ItemStack discItemStack, Runnable onFinished);

    void playDisc(ServerLevel serverLevel, Vec3 position, UUID storageUuid, ItemStack discItemStack, int entityId, Runnable onFinished);

    Optional<Long> getMusicLengthInTicks(ItemStack itemStack, Level level);

    boolean supports(ItemStack itemStack);

}
