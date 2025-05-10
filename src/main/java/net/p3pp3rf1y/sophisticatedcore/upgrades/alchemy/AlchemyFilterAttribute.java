package net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.concurrent.Immutable;

@Immutable
public record AlchemyFilterAttribute(ItemStack filter, AlchemyCondition condition, float value) {
	public AlchemyFilterAttribute(ItemStack filter, AlchemyCondition condition) {
		this(filter, condition, -1);
	}

	public AlchemyFilterAttribute setFilter(ItemStack filter) {
		CopyBuilder builder = new CopyBuilder(this);
		builder.filter(filter);
		return builder.build();
	}

	public AlchemyFilterAttribute setConditionAndValue(AlchemyCondition condition, float value) {
		CopyBuilder builder = new CopyBuilder(this);
		builder.condition(condition);
		builder.value(value);
		return builder.build();
	}

	public AlchemyFilterAttribute setValue(float value) {
		CopyBuilder builder = new CopyBuilder(this);
		builder.value(value);
		return builder.build();
	}

	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.put("filter", filter.serializeNBT());
		tag.putString("condition", condition.getSerializedName());
		tag.putFloat("value", value);
		return tag;
	}

	public static AlchemyFilterAttribute deserializeNBT(CompoundTag tag) {
		ItemStack filter = ItemStack.of(tag.getCompound("filter"));
		AlchemyCondition condition = AlchemyCondition.fromName(tag.getString("condition"));
		float value = tag.getFloat("value");
		return new AlchemyFilterAttribute(filter, condition, value);
	}

	private static class CopyBuilder {
		private float value;
		private ItemStack filter;
		private AlchemyCondition condition;

		private CopyBuilder(AlchemyFilterAttribute attribute) {
			this.filter = attribute.filter;
			this.condition = attribute.condition;
			this.value = attribute.value;
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
