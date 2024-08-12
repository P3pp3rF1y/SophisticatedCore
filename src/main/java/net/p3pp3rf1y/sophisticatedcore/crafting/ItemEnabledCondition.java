package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.p3pp3rf1y.sophisticatedcore.Config;

public record ItemEnabledCondition(ResourceLocation itemRegistryName) implements ICondition {
	public static final MapCodec<ItemEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
			builder -> builder
					.group(
							ResourceLocation.CODEC.fieldOf("itemRegistryName").forGetter(ItemEnabledCondition::itemRegistryName))
					.apply(builder, ItemEnabledCondition::new));

	public ItemEnabledCondition(Item item) {
		this(BuiltInRegistries.ITEM.getKey(item));
	}

	@Override
	public boolean test(IContext context) {
		return Config.COMMON.enabledItems.isItemEnabled(itemRegistryName);
	}

	@Override
	public MapCodec<? extends ICondition> codec() {
		return CODEC;
	}
}
