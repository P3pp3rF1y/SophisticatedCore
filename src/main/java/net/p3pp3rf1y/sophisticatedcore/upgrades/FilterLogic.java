package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.p3pp3rf1y.sophisticatedcore.util.FilterItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemStackHelper;

import javax.annotation.Nullable;
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
	private final int filterSlotCount;
	private final Predicate<ItemStack> isItemValid;
	@Nullable
	protected Set<TagKey<Item>> tagKeys = null;
	private ObservableFilterItemStackHandler filterHandler = null;
	private boolean emptyAllowListMatchesEverything = false;
	private boolean allowListDefault = false;

	@Nullable
	private FilterAttributes emptyAttributes = null;

	public FilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int filterSlotCount, DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> filterAttributesComponent) {
		this(upgrade, saveHandler, filterSlotCount, s -> true, filterAttributesComponent);
	}

	public FilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int filterSlotCount, Predicate<ItemStack> isItemValid, DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> filterAttributesComponent) {
		this.upgrade = upgrade;
		this.saveHandler = saveHandler;
		this.filterAttributesComponent = filterAttributesComponent;
		this.filterSlotCount = filterSlotCount;
		this.isItemValid = isItemValid;
	}

	public void setEmptyAllowListMatchesEverything() {
		emptyAllowListMatchesEverything = true;
	}

	public ObservableFilterItemStackHandler getFilterHandler() {
		if (filterHandler == null) {
			filterHandler = new ObservableFilterItemStackHandler();
			filterHandler.initFilters(getAttributes().filterItems());
			if (getAttributes().filterItems().size() < filterSlotCount) {
				setAttributes(contents -> contents.expandFilterItems(filterSlotCount));
			}
		}

		return filterHandler;
	}

	public boolean matchesFilter(ItemStack stack) {
		if (isAllowList()) {
			if (getPrimaryMatch() == PrimaryMatch.TAGS) {
				return isTagMatch(stack);
			} else {
				return (getFilterHandler().hasOnlyEmptyFilters() && emptyAllowListMatchesEverything)
						|| InventoryHelper.iterate(getFilterHandler(), (slot, filter) -> stackMatchesFilter(stack, filter), () -> false, returnValue -> returnValue);
			}
		} else {
			if (getPrimaryMatch() == PrimaryMatch.TAGS) {
				return !isTagMatch(stack);
			} else {
				return getFilterHandler().hasOnlyEmptyFilters()
						|| InventoryHelper.iterate(getFilterHandler(), (slot, filter) -> !stackMatchesFilter(stack, filter), () -> true, returnValue -> !returnValue);
			}
		}
	}

	private boolean isTagMatch(ItemStack stack) {
		if (shouldMatchAnyTag()) {
			return anyTagMatches(stack.getTags());
		}
		return allTagsMatch(stack.getTags());
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
			emptyAttributes = new FilterAttributes(Collections.emptySet(), allowListDefault, false, false, PrimaryMatch.ITEM, true, NonNullList.withSize(filterSlotCount, ItemStack.EMPTY), false, false);
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

	public boolean stackMatchesFilter(ItemStack stack, ItemStack filter) {
		if (filter.isEmpty()) {
			return false;
		}

		PrimaryMatch primaryMatch = getPrimaryMatch();
		if (primaryMatch == PrimaryMatch.MOD) {
			if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(BuiltInRegistries.ITEM.getKey(filter.getItem()).getNamespace())) {
				return false;
			}
		} else if (primaryMatch == PrimaryMatch.ITEM && stack.getItem() != filter.getItem()) {
			return false;
		}

		if (shouldMatchDurability() && stack.getDamageValue() != filter.getDamageValue()) {
			return false;
		}

		return !shouldMatchComponents() || ItemStackHelper.areItemStackComponentsEqualIgnoreDurability(stack, filter);
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
		public ObservableFilterItemStackHandler() {
			super(filterSlotCount);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			setAttributes(contents -> contents.setFilterItem(slot, stacks.get(slot)));
			save();
			onSlotChange.accept(slot);
		}

		public void setOnSlotChange(IntConsumer onSlotChange) {
			this.onSlotChange = onSlotChange;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.isEmpty() || (doesNotContain(stack) && isItemValid.test(stack));
		}

		private boolean doesNotContain(ItemStack stack) {
			return !InventoryHelper.hasItem(this, s -> ItemStack.isSameItemSameComponents(s, stack));
		}

		public void initFilters(List<ItemStack> filterItems) {
			for (int slot = 0; slot < filterItems.size(); slot++) {
				setStackInSlot(slot, filterItems.get(slot).copy());
			}
			onLoad();
		}
	}
}