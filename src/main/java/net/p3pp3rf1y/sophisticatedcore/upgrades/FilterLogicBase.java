package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.ItemStackHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class FilterLogicBase {
	protected final ItemStack upgrade;
	protected final Consumer<ItemStack> saveHandler;

	protected final String parentTagKey;

	private boolean allowListDefault = false;
	@Nullable
	protected Set<TagKey<Item>> tagKeys = null;
	public FilterLogicBase(ItemStack upgrade, Consumer<ItemStack> saveHandler, String parentTagKey) {
		this.upgrade = upgrade;
		this.saveHandler = saveHandler;
		this.parentTagKey = parentTagKey;
	}

	public String getParentTagKey() {
		return parentTagKey;
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

		return !shouldMatchNbt() || ItemStackHelper.areItemStackTagsEqualIgnoreDurability(stack, filter);
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
		NBTHelper.setList(upgrade, parentTagKey, "tags", tagKeys, t -> StringTag.valueOf(t.location().toString()));
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
		tagKeys = NBTHelper.getCollection(upgrade, parentTagKey, "tags", Tag.TAG_STRING,
						elementNbt -> Optional.of(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(elementNbt.getAsString()))), () -> new TreeSet<>(Comparator.comparing(TagKey::location)))
				.orElse(new TreeSet<>(Comparator.comparing(TagKey::location)));
	}

	public void setAllowList(boolean isAllowList) {
		NBTHelper.setBoolean(upgrade, parentTagKey, "isAllowList", isAllowList);
		save();
	}

	public boolean isAllowList() {
		return NBTHelper.getBoolean(upgrade, parentTagKey, "isAllowList").orElse(allowListDefault);
	}

	public boolean shouldMatchDurability() {
		return NBTHelper.getBoolean(upgrade, parentTagKey, "matchDurability").orElse(false);
	}

	public void setMatchDurability(boolean matchDurability) {
		NBTHelper.setBoolean(upgrade, parentTagKey, "matchDurability", matchDurability);
		save();
	}

	public void setMatchNbt(boolean matchNbt) {
		NBTHelper.setBoolean(upgrade, parentTagKey, "matchNbt", matchNbt);
		save();
	}

	public boolean shouldMatchNbt() {
		return NBTHelper.getBoolean(upgrade, parentTagKey, "matchNbt").orElse(false);
	}

	public void setPrimaryMatch(PrimaryMatch primaryMatch) {
		NBTHelper.setEnumConstant(upgrade, parentTagKey, "primaryMatch", primaryMatch);
		save();
	}

	public PrimaryMatch getPrimaryMatch() {
		return NBTHelper.getEnumConstant(upgrade, parentTagKey, "primaryMatch", PrimaryMatch::fromName).orElse(PrimaryMatch.ITEM);
	}

	public boolean shouldMatchAnyTag() {
		return NBTHelper.getBoolean(upgrade, parentTagKey, "matchAnyTag").orElse(true);
	}

	public void setMatchAnyTag(boolean matchAnyTag) {
		NBTHelper.setBoolean(upgrade, parentTagKey, "matchAnyTag", matchAnyTag);
		save();
	}
}
