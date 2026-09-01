package net.p3pp3rf1y.sophisticatedcore.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;
import org.jetbrains.annotations.Nullable;

public record EnderLinkerBound() implements ConditionalItemModelProperty {
	public static final MapCodec<EnderLinkerBound> MAP_CODEC = MapCodec.unit(new EnderLinkerBound());

	@Override
	public boolean get(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int seed,
			ItemDisplayContext itemDisplayContext) {
		return EnderLinkerItem.hasBoundPresentation(itemStack);
	}

	@Override
	public MapCodec<? extends ConditionalItemModelProperty> type() {
		return MAP_CODEC;
	}
}
