package net.p3pp3rf1y.sophisticatedcore.upgrades;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.concurrent.Immutable;
import java.util.*;

@Immutable
public record FilterAttributes(Set<TagKey<Item>> tagKeys, boolean isAllowList, boolean matchDurability,
							   boolean matchComponents, PrimaryMatch primaryMatch, boolean matchAnyTag,
							   @Unmodifiable List<ItemStack> filterItems, boolean filterByStorage, boolean filterByInventory) {
	public static final Codec<FilterAttributes> CODEC = RecordCodecBuilder.create(
			builder -> builder
					.group(
							CodecHelper.setOf(TagKey.codec(Registries.ITEM)).optionalFieldOf("tag_keys", Collections.emptySet()).forGetter(FilterAttributes::tagKeys),
							Codec.BOOL.optionalFieldOf("is_allow_list", false).forGetter(FilterAttributes::isAllowList),
							Codec.BOOL.optionalFieldOf("match_durability", false).forGetter(FilterAttributes::matchDurability),
							Codec.BOOL.optionalFieldOf("match_components", false).forGetter(FilterAttributes::matchComponents),
							PrimaryMatch.CODEC.optionalFieldOf("primary_match", PrimaryMatch.ITEM).forGetter(FilterAttributes::primaryMatch),
							Codec.BOOL.optionalFieldOf("match_any_tag", false).forGetter(FilterAttributes::matchAnyTag),
							Codec.list(ItemStack.OPTIONAL_CODEC).optionalFieldOf("filter_items", Collections.emptyList()).forGetter(FilterAttributes::filterItems),
							Codec.BOOL.optionalFieldOf("filter_by_storage", false).forGetter(FilterAttributes::filterByStorage),
							Codec.BOOL.optionalFieldOf("filter_by_inventory", false).forGetter(FilterAttributes::filterByInventory)
					)
					.apply(builder, FilterAttributes::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, FilterAttributes> STREAM_CODEC = StreamCodecHelper.composite(
			StreamCodecHelper.ofCollection(StreamCodecHelper.ofTagkey(Registries.ITEM), HashSet::new),
			FilterAttributes::tagKeys,
			ByteBufCodecs.BOOL,
			FilterAttributes::isAllowList,
			ByteBufCodecs.BOOL,
			FilterAttributes::matchDurability,
			ByteBufCodecs.BOOL,
			FilterAttributes::matchComponents,
			PrimaryMatch.STREAM_CODEC,
			FilterAttributes::primaryMatch,
			ByteBufCodecs.BOOL,
			FilterAttributes::matchAnyTag,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC,
			FilterAttributes::filterItems,
			ByteBufCodecs.BOOL,
			FilterAttributes::filterByStorage,
			ByteBufCodecs.BOOL,
			FilterAttributes::filterByInventory,
			FilterAttributes::new);

	public FilterAttributes setTagKeys(Set<TagKey<Item>> tagKeys) {
		return new CopyBuilder(this).setTagKeys(tagKeys).build();
	}

	public FilterAttributes setAllowList(boolean isAllowList) {
		return new CopyBuilder(this).setAllowList(isAllowList).build();
	}

	public FilterAttributes setMatchDurability(boolean matchDurability) {
		return new CopyBuilder(this).setMatchDurability(matchDurability).build();
	}

	public FilterAttributes setMatchComponents(boolean matchComponents) {
		return new CopyBuilder(this).setMatchComponents(matchComponents).build();
	}

	public FilterAttributes setPrimaryMatch(PrimaryMatch primaryMatch) {
		return new CopyBuilder(this).setPrimaryMatch(primaryMatch).build();
	}

	public FilterAttributes setMatchAnyTag(boolean matchAnyTag) {
		return new CopyBuilder(this).setMatchAnyTag(matchAnyTag).build();
	}

	public FilterAttributes setFilterItem(int slot, ItemStack filterItem) {
		return new CopyBuilder(this).setFilterItem(slot, filterItem).build();
	}

	public FilterAttributes setFilterByStorage(boolean filterByStorage) {
		return new CopyBuilder(this).setFilterByStorage(filterByStorage).build();
	}

	public FilterAttributes setFilterByInventory(boolean filterByInventory) {
		return new CopyBuilder(this).setFilterByInventory(filterByInventory).build();
	}

	public FilterAttributes expandFilterItems(int targetFilterCount) {
		return new CopyBuilder(this).expandFilterItems(targetFilterCount).build();
	}

	protected static class CopyBuilder {
		private Set<TagKey<Item>> tagKeys;
		private boolean isAllowList;
		private boolean matchDurability;
		private boolean matchComponents;
		private PrimaryMatch primaryMatch;
		private boolean matchAnyTag;
		private List<ItemStack> filterItems;
		private boolean filterByStorage;
		private boolean filterByInventory;

		public CopyBuilder(FilterAttributes original) {
			this.tagKeys = original.tagKeys();
			this.isAllowList = original.isAllowList();
			this.matchDurability = original.matchDurability();
			this.matchComponents = original.matchComponents();
			this.primaryMatch = original.primaryMatch();
			this.matchAnyTag = original.matchAnyTag();
			this.filterItems = new ArrayList<>(original.filterItems());
			this.filterByStorage = original.filterByStorage();
			this.filterByInventory = original.filterByInventory();
		}

		public CopyBuilder setTagKeys(Set<TagKey<Item>> tagKeys) {
			this.tagKeys = tagKeys;
			return this;
		}

		public FilterAttributes build() {
			return new FilterAttributes(tagKeys, isAllowList, matchDurability, matchComponents, primaryMatch, matchAnyTag, Collections.unmodifiableList(filterItems), filterByStorage, filterByInventory);
		}

		public CopyBuilder setAllowList(boolean isAllowList) {
			this.isAllowList = isAllowList;
			return this;
		}

		public CopyBuilder setMatchDurability(boolean matchDurability) {
			this.matchDurability = matchDurability;
			return this;
		}

		public CopyBuilder setMatchComponents(boolean matchComponents) {
			this.matchComponents = matchComponents;
			return this;
		}

		public CopyBuilder setPrimaryMatch(PrimaryMatch primaryMatch) {
			this.primaryMatch = primaryMatch;
			return this;
		}

		public CopyBuilder setMatchAnyTag(boolean matchAnyTag) {
			this.matchAnyTag = matchAnyTag;
			return this;
		}

		public CopyBuilder setFilterItem(int slot, ItemStack filterItem) {
			filterItems.set(slot, filterItem.copy());
			return this;
		}

		public CopyBuilder setFilterByStorage(boolean filterByStorage) {
			this.filterByStorage = filterByStorage;
			return this;
		}

		public CopyBuilder setFilterByInventory(boolean filterByInventory) {
			this.filterByInventory = filterByInventory;
			return this;
		}

		public CopyBuilder expandFilterItems(int targetFilterCount) {
			NonNullList<ItemStack> targetFilterItems = NonNullList.withSize(targetFilterCount, ItemStack.EMPTY);
			for (int slot = 0; slot < filterItems.size(); slot++) {
				if (slot >= targetFilterCount) {
					break;
				}
				targetFilterItems.set(slot, filterItems.get(slot));
			}
			filterItems = targetFilterItems;
			return this;
		}
	}
}
