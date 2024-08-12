package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public record UpgradeSlotChangeResult(boolean successful, @Nullable Component errorMessage, Set<Integer> errorUpgradeSlots, Set<Integer> errorInventorySlots, Set<Integer> errorInventoryParts) {

	public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeSlotChangeResult> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			UpgradeSlotChangeResult::successful,
			StreamCodecHelper.ofNullable(ComponentSerialization.STREAM_CODEC),
			UpgradeSlotChangeResult::errorMessage,
			StreamCodecHelper.ofCollection(ByteBufCodecs.INT, HashSet::new),
			UpgradeSlotChangeResult::errorUpgradeSlots,
			StreamCodecHelper.ofCollection(ByteBufCodecs.INT, HashSet::new),
			UpgradeSlotChangeResult::errorInventorySlots,
			StreamCodecHelper.ofCollection(ByteBufCodecs.INT, HashSet::new),
			UpgradeSlotChangeResult::errorInventoryParts,
			UpgradeSlotChangeResult::new);

	public Optional<Component> getErrorMessage() {
		return Optional.ofNullable(errorMessage);
	}

	public static UpgradeSlotChangeResult fail(Component errorMessage, Set<Integer> errorUpgradeSlots, Set<Integer> errorInventorySlots, Set<Integer> errorInventoryParts) {
		return new UpgradeSlotChangeResult(false, errorMessage, errorUpgradeSlots, errorInventorySlots, errorInventoryParts);
	}


	public static UpgradeSlotChangeResult success() {
		return new UpgradeSlotChangeResult(true, null, Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
	}
}
