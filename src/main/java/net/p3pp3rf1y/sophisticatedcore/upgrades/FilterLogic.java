package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.util.FilterItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemStackHelper;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterLogic {
	protected final ItemStack upgrade;
	protected final Consumer<ItemStack> saveHandler;
	protected final DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> filterAttributesComponent;
	private final int defaultFilterSlotCount;
	private final Predicate<ItemStack> isItemValid;
	@Nullable
	protected Set<TagKey<Item>> tagKeys = null;
	private ObservableFilterItemStackHandler filterHandler = null;
	private boolean emptyAllowListMatchesEverything = false;
	private boolean allowListDefault = false;

	@Nullable
	private FilterAttributes emptyAttributes = null;

	public FilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int defaultFilterSlotCount, DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> filterAttributesComponent) {
		this(upgrade, saveHandler, defaultFilterSlotCount, s -> true, filterAttributesComponent);
	}

	public FilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int defaultFilterSlotCount, Predicate<ItemStack> isItemValid, DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> filterAttributesComponent) {
		this.upgrade = upgrade;
		this.saveHandler = saveHandler;
		this.filterAttributesComponent = filterAttributesComponent;
		this.defaultFilterSlotCount = defaultFilterSlotCount;
		this.isItemValid = isItemValid;
	}

	public void setEmptyAllowListMatchesEverything() {
		emptyAllowListMatchesEverything = true;
	}

	public ObservableFilterItemStackHandler getFilterHandler() {
		if (filterHandler == null) {
			ItemContainerContents filterItemContents = getAttributes().filterItems();
			NonNullList<ItemStack> filterItems = NonNullList.withSize(filterItemContents.getSlots(), ItemStack.EMPTY);
			filterItemContents.copyInto(filterItems);

			int filterSlotCount = Math.max(filterItems.size(), defaultFilterSlotCount);
			filterHandler = new ObservableFilterItemStackHandler(filterSlotCount);
			filterHandler.initFilters(filterItems);
		}

		return filterHandler;
	}

	public boolean matchesFilter(ItemStack stack) {
		return matchesFilter(stack.getTags(), stack.getItem(), stack.getDamageValue(), stack.isEmpty(), stack.getComponents());
	}

	protected boolean matchesFilter(Stream<TagKey<Item>> tags, Item item, int damageValue, boolean empty, DataComponentMap components) {
		if (empty) {
			return false; // Filters should not match empty stacks as empty stacks are not supposed to be manipulated
		}

		if (isAllowList()) {
			if (getPrimaryMatch() == PrimaryMatch.TAGS) {
				return isTagMatch(tags);
			} else {
				return (getFilterHandler().hasOnlyEmptyFilters() && emptyAllowListMatchesEverything)
						|| InventoryHelper.iterate(getFilterHandler(), (slot, filter) -> stackMatchesFilter(filter, item, damageValue, empty, components), () -> false, returnValue -> returnValue);
			}
		} else {
			if (getPrimaryMatch() == PrimaryMatch.TAGS) {
				return !isTagMatch(tags);
			} else {
				return getFilterHandler().hasOnlyEmptyFilters()
						|| InventoryHelper.iterate(getFilterHandler(), (slot, filter) -> !stackMatchesFilter(filter, item, damageValue, empty, components), () -> true, returnValue -> !returnValue);
			}
		}
	}

	public boolean matchesFilter(ItemResource resource) {
		return matchesFilter(resource.getItem().builtInRegistryHolder().tags(), resource.getItem(), resource.getOrDefault(DataComponents.DAMAGE, 0), resource.isEmpty(), resource.getComponents());
	}

	private boolean isTagMatch(Stream<TagKey<Item>> tags) {
		if (shouldMatchAnyTag()) {
			return anyTagMatches(tags);
		}
		return allTagsMatch(tags);
	}

	private boolean allTagsMatch(Stream<TagKey<Item>> tagsStream) {
		if (tagKeys == null) {
			initTags();
		}
		Set<TagKey<Item>> tags = tagsStream.collect(Collectors.toSet());
		for (TagKey<Item> tagName : tagKeys) {
			if (!tags.contains(tagName)) {
				return false;
			}
		}
		return true;
	}

	private boolean anyTagMatches(Stream<TagKey<Item>> tags) {
		if (tagKeys == null) {
			initTags();
		}
		return tags.anyMatch(t -> tagKeys.contains(t));
	}

	protected FilterAttributes getAttributes() {
		return upgrade.getOrDefault(filterAttributesComponent, getEmptyAttributes());
	}

	private FilterAttributes getEmptyAttributes() {
		if (emptyAttributes == null) {
			emptyAttributes = new FilterAttributes(Collections.emptySet(), allowListDefault, false, false, PrimaryMatch.ITEM, true, ItemContainerContents.EMPTY, false, false);
		}
		return emptyAttributes;
	}

	protected void setAttributes(Function<FilterAttributes, FilterAttributes> setter) {
		upgrade.set(filterAttributesComponent, setter.apply(getAttributes()));
	}

	public void setAllowByDefault(boolean allowListDefault) {
		this.allowListDefault = allowListDefault;
	}

	protected void save() {
		saveHandler.accept(upgrade);
	}

	public boolean stackMatchesFilter(ItemStack filter, Item item, int damageValue, boolean isEmpty, DataComponentMap components) {
		if (filter.isEmpty()) {
			return false;
		}

		PrimaryMatch primaryMatch = getPrimaryMatch();
		if (primaryMatch == PrimaryMatch.MOD) {
			if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(BuiltInRegistries.ITEM.getKey(filter.getItem()).getNamespace())) {
				return false;
			}
		} else if (primaryMatch == PrimaryMatch.ITEM && item != filter.getItem()) {
			return false;
		}

		if (shouldMatchDurability() && damageValue != filter.getDamageValue()) {
			return false;
		}

		return !shouldMatchComponents() || ItemStackHelper.areItemStackComponentsEqualIgnoreDurability(filter.isEmpty(), filter.getComponents(), isEmpty, components);
	}

	public Set<TagKey<Item>> getTagKeys() {
		if (tagKeys == null) {
			initTags();
		}
		return Collections.unmodifiableSet(tagKeys);
	}

	public void addTag(TagKey<Item> tagName) {
		if (tagKeys == null) {
			initTags();
		}
		tagKeys.add(tagName);
		serializeTags();
		save();
	}

	private void serializeTags() {
		if (tagKeys == null) {
			return;
		}
		setAttributes(contents -> contents.setTagKeys(tagKeys));
	}

	public void removeTagName(TagKey<Item> tagName) {
		if (tagKeys == null) {
			initTags();
		}
		tagKeys.remove(tagName);
		serializeTags();
		save();
	}

	protected void initTags() {
		tagKeys = new TreeSet<>(Comparator.comparing(TagKey::location));
		tagKeys.addAll(getAttributes().tagKeys());
	}

	public void setAllowList(boolean isAllowList) {
		setAttributes(contents -> contents.setAllowList(isAllowList));
		save();
	}

	public boolean isAllowList() {
		return getAttributes().isAllowList();
	}

	public boolean shouldMatchDurability() {
		return getAttributes().matchDurability();
	}

	public void setMatchDurability(boolean matchDurability) {
		setAttributes(contents -> contents.setMatchDurability(matchDurability));
		save();
	}

	public void setMatchComponents(boolean matchComponents) {
		setAttributes(contents -> contents.setMatchComponents(matchComponents));
		save();
	}

	public boolean shouldMatchComponents() {
		return getAttributes().matchComponents();
	}

	public void setPrimaryMatch(PrimaryMatch primaryMatch) {
		setAttributes(contents -> contents.setPrimaryMatch(primaryMatch));
		save();
	}

	public PrimaryMatch getPrimaryMatch() {
		return getAttributes().primaryMatch();
	}

	public boolean shouldMatchAnyTag() {
		return getAttributes().matchAnyTag();
	}

	public void setMatchAnyTag(boolean matchAnyTag) {
		setAttributes(contents -> contents.setMatchAnyTag(matchAnyTag));
		save();
	}

	public DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> getAttributesComponent() {
		return filterAttributesComponent;
	}

	public class ObservableFilterItemStackHandler extends FilterItemStackHandler {
		private IntConsumer onSlotChange = s -> {};
		public ObservableFilterItemStackHandler(int filterSlotCount) {
			super(filterSlotCount);
		}

		@Override
		protected void onContentsChanged(int slot, ItemStack previousContents) {
			super.onContentsChanged(slot, previousContents);
			setAttributes(contents -> contents.setFilterItem(slot, stacks.get(slot)));
			save();
			onSlotChange.accept(slot);
		}

		public void setOnSlotChange(IntConsumer onSlotChange) {
			this.onSlotChange = onSlotChange;
		}

		@Override
		public boolean isValid(int slot, ItemResource resource) {
			return resource.isEmpty() || (doesNotContain(resource) && isItemValid.test(resource.toStack()));
		}

		private boolean doesNotContain(ItemResource resource) {
			return !InventoryHelper.hasItem(this, s -> s.equals(resource));
		}

		public void initFilters(List<ItemStack> filterItems) {
			for (int slot = 0; slot < filterItems.size(); slot++) {
				ItemStack filterStack = filterItems.get(slot);
				if (!filterStack.isEmpty()) {
					set(slot, ItemResource.of(filterStack), 1);
				}
			}
			updateEmptyFilters();
		}
	}
}