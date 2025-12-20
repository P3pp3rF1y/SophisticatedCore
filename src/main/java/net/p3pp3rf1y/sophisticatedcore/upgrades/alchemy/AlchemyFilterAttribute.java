package net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import javax.annotation.concurrent.Immutable;

@Immutable
public record AlchemyFilterAttribute(ItemStack filter, AlchemyCondition condition, float value) {
	public static final Codec<AlchemyFilterAttribute> CODEC = RecordCodecBuilder.create(
			builder -> builder
					.group(
							ItemStack.OPTIONAL_CODEC.orElse(ItemStack.EMPTY).fieldOf("filter").forGetter(AlchemyFilterAttribute::filter),
							AlchemyCondition.CODEC.fieldOf("condition").forGetter(AlchemyFilterAttribute::condition),
							Codec.FLOAT.fieldOf("value").forGetter(AlchemyFilterAttribute::value)
					).apply(builder, AlchemyFilterAttribute::new)
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyFilterAttribute> STREAM_CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC,
			AlchemyFilterAttribute::filter,
			AlchemyCondition.STREAM_CODEC,
			AlchemyFilterAttribute::condition,
			ByteBufCodecs.FLOAT,
			AlchemyFilterAttribute::value,
			AlchemyFilterAttribute::new
	);

	public AlchemyFilterAttribute(ItemStack filter, AlchemyCondition condition) {
		this(filter, condition, -1);
	}

	public AlchemyFilterAttribute setFilter(ItemStack filter) {
		return new CopyBuilder(this).filter(filter).build();
	}

	public AlchemyFilterAttribute setConditionAndValue(AlchemyCondition condition, float value) {
		return new CopyBuilder(this).condition(condition).value(value).build();
	}

	public AlchemyFilterAttribute setValue(float value) {
		return new CopyBuilder(this).value(value).build();
	}

	private static class CopyBuilder {
		private float value;
		private ItemStack filter;
		private AlchemyCondition condition;

		private CopyBuilder(AlchemyFilterAttribute attribute) {
			filter = attribute.filter;
			condition = attribute.condition;
			value = attribute.value;
		}

		public CopyBuilder filter(ItemStack filter) {
			this.filter = filter;
			return this;
		}

		public CopyBuilder condition(AlchemyCondition condition) {
			this.condition = condition;
			return this;
		}

		public CopyBuilder value(float value) {
			this.value = value;
			return this;
		}

		public AlchemyFilterAttribute build() {
			return new AlchemyFilterAttribute(filter, condition, value);
		}
	}
}
