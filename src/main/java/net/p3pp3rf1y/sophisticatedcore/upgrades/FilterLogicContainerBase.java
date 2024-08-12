package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FilterLogicContainerBase<T extends FilterLogic, S extends Slot> {
	private static final String DATA_IS_ALLOW_LIST = "isAllowList";
	private static final String DATA_MATCH_DURABILITY = "matchDurability";
	private static final String DATA_MATCH_NBT = "matchNbt";
	private static final String DATA_PRIMARY_MATCH = "primaryMatch";
	private static final String DATA_ADD_TAG_NAME = "addTagName";
	private static final String DATA_REMOVE_TAG_NAME = "removeTagName";
	private static final String DATA_MATCH_ANY_TAG = "matchAnyTag";
	private static final String DATA_COMPONENT_KEY = "parentTagKey";

	protected final List<S> filterSlots = new ArrayList<>();
	protected final IServerUpdater serverUpdater;
	protected final Supplier<T> filterLogic;

	private final TagSelectionSlot tagSelectionSlot;

	private int selectedTagToAdd = 0;

	private int selectedTagToRemove = 0;

	private final Set<TagKey<Item>> tagsToAdd = new TreeSet<>(Comparator.comparing(TagKey::location));

	public FilterLogicContainerBase(IServerUpdater serverUpdater, Supplier<T> filterLogic, Consumer<Slot> addSlot) {
		this.serverUpdater = serverUpdater;
		this.filterLogic = filterLogic;

		tagSelectionSlot = new TagSelectionSlot();
		addSlot.accept(tagSelectionSlot);
	}

	public int getSelectedTagToAdd() {
		return selectedTagToAdd;
	}

	public int getSelectedTagToRemove() {
		return selectedTagToRemove;
	}

	public TagSelectionSlot getTagSelectionSlot() {
		return tagSelectionSlot;
	}

	public List<S> getFilterSlots() {
		return filterSlots;
	}

	public Set<TagKey<Item>> getTagNames() {
		return filterLogic.get().getTagKeys();
	}

	public Set<TagKey<Item>> getTagsToAdd() {
		return tagsToAdd;
	}

	public void addSelectedTag() {
		getTagAtIndex(tagsToAdd, selectedTagToAdd).ifPresent(tagName -> {
			addTagName(tagName);
			sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), DATA_ADD_TAG_NAME, tagName.location().toString()));
			selectedTagToRemove = 0;
			tagsToAdd.remove(tagName);
			selectedTagToAdd = Math.max(0, selectedTagToAdd - 1);
		});
	}

	private void addTagName(TagKey<Item> tagName) {
		filterLogic.get().addTag(tagName);
	}

	public void removeSelectedTag() {
		getTagAtIndex(getTagNames(), selectedTagToRemove).ifPresent(tagName -> {
			removeSelectedTag(tagName);
			sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), DATA_REMOVE_TAG_NAME, tagName.location().toString()));
			if (tagSelectionSlot.getItem().is(tagName)) {
				tagsToAdd.add(tagName);
			}
			selectedTagToRemove = Math.max(0, selectedTagToRemove - 1);
		});
	}

	private void removeSelectedTag(TagKey<Item> tagName) {
		filterLogic.get().removeTagName(tagName);
	}

	public void selectNextTagToRemove() {
		selectedTagToRemove = getNextIndex(getTagNames().size(), selectedTagToRemove);
	}

	private int getNextIndex(int colSize, int selectedIndex) {
		return selectedIndex + 1 >= colSize ? 0 : selectedIndex + 1;
	}

	private int getPreviousIndex(int colSize, int selectedIndex) {
		return selectedIndex == 0 ? colSize - 1 : selectedIndex - 1;
	}

	public void selectPreviousTagToRemove() {
		selectedTagToRemove = getPreviousIndex(getTagNames().size(), selectedTagToRemove);
	}

	public void selectNextTagToAdd() {
		selectedTagToAdd = getNextIndex(tagsToAdd.size(), selectedTagToAdd);
	}

	public void selectPreviousTagToAdd() {
		selectedTagToAdd = getPreviousIndex(tagsToAdd.size(), selectedTagToAdd);
	}

	private Optional<TagKey<Item>> getTagAtIndex(Set<TagKey<Item>> col, int index) {
		int curIndex = 0;
		for (TagKey<Item> tagName : col) {
			if (curIndex == index) {
				return Optional.of(tagName);
			}
			curIndex++;
		}
		return Optional.empty();
	}

	public boolean isAllowList() {
		return filterLogic.get().isAllowList();
	}

	public boolean shouldMatchDurability() {
		return filterLogic.get().shouldMatchDurability();
	}

	public boolean shouldMatchNbt() {
		return filterLogic.get().shouldMatchComponents();
	}

	public PrimaryMatch getPrimaryMatch() {
		return filterLogic.get().getPrimaryMatch();
	}

	public boolean shouldMatchAnyTag() {
		return filterLogic.get().shouldMatchAnyTag();
	}

	public void setAllowList(boolean isAllowList) {
		filterLogic.get().setAllowList(isAllowList);
		sendBooleanToServer(DATA_IS_ALLOW_LIST, isAllowList);
	}

	private void sendBooleanToServer(String dataId, boolean value) {
		serverUpdater.sendDataToServer(() -> {
			CompoundTag tag = new CompoundTag();
			tag.putBoolean(dataId, value);
			tag.putString(DATA_COMPONENT_KEY, filterLogic.get().getAttributesComponent().getKey().location().toString());
			return tag;
		});
	}

	protected void sendDataToServer(Supplier<CompoundTag> dataSupplier) {
		serverUpdater.sendDataToServer(() -> {
			CompoundTag tag = dataSupplier.get();
			tag.putString(DATA_COMPONENT_KEY, filterLogic.get().getAttributesComponent().getKey().location().toString());
			return tag;
		});
	}

	public void setMatchDurability(boolean matchDurability) {
		filterLogic.get().setMatchDurability(matchDurability);
		sendBooleanToServer(DATA_MATCH_DURABILITY, matchDurability);
	}

	public void setMatchNbt(boolean matchNbt) {
		filterLogic.get().setMatchComponents(matchNbt);
		sendBooleanToServer(DATA_MATCH_NBT, matchNbt);
	}

	public void setPrimaryMatch(PrimaryMatch primaryMatch) {
		filterLogic.get().setPrimaryMatch(primaryMatch);
		sendDataToServer(() -> NBTHelper.putEnumConstant(new CompoundTag(), DATA_PRIMARY_MATCH, primaryMatch));
	}

	public void setMatchAnyTag(boolean matchAnyTag) {
		filterLogic.get().setMatchAnyTag(matchAnyTag);
		sendBooleanToServer(DATA_MATCH_ANY_TAG, matchAnyTag);
	}

	public boolean handlePacket(CompoundTag data) {
		if (isDifferentFilterLogicsData(data)) {
			return false;
		}

		for (String key : data.getAllKeys()) {
			switch (key) {
				case DATA_IS_ALLOW_LIST -> {
					setAllowList(data.getBoolean(DATA_IS_ALLOW_LIST));
					return true;
				}
				case DATA_MATCH_DURABILITY -> {
					setMatchDurability(data.getBoolean(DATA_MATCH_DURABILITY));
					return true;
				}
				case DATA_MATCH_NBT -> {
					setMatchNbt(data.getBoolean(DATA_MATCH_NBT));
					return true;
				}
				case DATA_PRIMARY_MATCH -> {
					setPrimaryMatch(PrimaryMatch.fromName(data.getString(DATA_PRIMARY_MATCH)));
					return true;
				}
				case DATA_ADD_TAG_NAME -> {
					addTagName(TagKey.create(Registries.ITEM, ResourceLocation.parse(data.getString(DATA_ADD_TAG_NAME))));
					return true;
				}
				case DATA_REMOVE_TAG_NAME -> {
					removeSelectedTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(data.getString(DATA_REMOVE_TAG_NAME))));
					return true;
				}
				case DATA_MATCH_ANY_TAG -> {
					setMatchAnyTag(data.getBoolean(DATA_MATCH_ANY_TAG));
					return true;
				}
				default -> {
					//noop
				}
			}
		}
		return false;
	}

	protected boolean isDifferentFilterLogicsData(CompoundTag data) {
		return data.contains(DATA_COMPONENT_KEY) && !filterLogic.get().getAttributesComponent().getKey().location().toString().equals(data.getString(DATA_COMPONENT_KEY));
	}

	public class TagSelectionSlot extends Slot implements IFilterSlot {
		private ItemStack stack = ItemStack.EMPTY;
		private Runnable onUpdate = () -> {
		};

		public TagSelectionSlot() {
			super(new SimpleContainer(0), 0, -1, -1);
		}

		public void setOnUpdate(Runnable onUpdate) {
			this.onUpdate = onUpdate;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.isEmpty() || stack.getTags().findAny().isPresent();
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public ItemStack getItem() {
			return stack;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public ItemStack remove(int amount) {
			stack = ItemStack.EMPTY;
			return stack;
		}

		@Override
		public boolean isSameInventory(Slot other) {
			return false;
		}

		@Override
		public void set(ItemStack stack) {
			this.stack = stack;
			tagsToAdd.clear();
			tagsToAdd.addAll(stack.getTags().toList());
			getTagNames().forEach(tagsToAdd::remove);
			selectedTagToAdd = 0;
			onUpdate.run();
		}

		@Override
		public void setChanged() {
			//noop
		}
	}
}
