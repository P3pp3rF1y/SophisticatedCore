package net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.ISlotColorCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemDisplaySettingsCategory implements ISettingsCategory<ItemDisplaySettingsCategory>, ISlotColorCategory {
	public static final String NAME = "item_display";
	private static final String SLOT_TAG = "slot";
	private static final String ROTATION_TAG = "rotation";
	private static final String SLOTS_TAG = "slots";
	private static final String ROTATIONS_TAG = "rotations";
	private static final String COLOR_TAG = "color";
	private static final String DISPLAY_SIDE_TAG = "displaySide";
	private final Supplier<InventoryHandler> inventoryHandlerSupplier;
	private final Supplier<RenderInfo> renderInfoSupplier;
	private CompoundTag categoryNbt;
	private final Consumer<CompoundTag> saveNbt;
	private final int itemNumberLimit;
	private final Supplier<MemorySettingsCategory> getMemorySettings;
	private DyeColor color = DyeColor.RED;
	private final List<Integer> slotIndexes = new LinkedList<>();
	private Map<Integer, Integer> slotRotations = new HashMap<>();
	private DisplaySide displaySide = DisplaySide.FRONT;

	public ItemDisplaySettingsCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderInfo> renderInfoSupplier, CompoundTag categoryNbt, Consumer<CompoundTag> saveNbt, int itemNumberLimit, Supplier<MemorySettingsCategory> getMemorySettings) {
		this.inventoryHandlerSupplier = inventoryHandlerSupplier;
		this.renderInfoSupplier = renderInfoSupplier;
		this.categoryNbt = categoryNbt;
		this.saveNbt = saveNbt;
		this.itemNumberLimit = itemNumberLimit;
		this.getMemorySettings = getMemorySettings;

		deserialize();
	}

	public int getItemNumberLimit() {
		return itemNumberLimit;
	}

	public void unselectSlot(int slotIndex) {
		int orderIndex = slotIndexes.indexOf(slotIndex);

		//noinspection RedundantCollectionOperation
		slotIndexes.remove(orderIndex);
		slotRotations.remove(slotIndex);
		if (slotIndexes.isEmpty()) {
			categoryNbt.remove(SLOTS_TAG);
			categoryNbt.remove(ROTATIONS_TAG);
		}
		serializeSlotIndexes();

		updateFullRenderInfo();
	}

	private boolean haveRenderedItemsChanged() {
		List<RenderInfo.DisplayItem> previousDisplayItems = renderInfoSupplier.get().getItemDisplayRenderInfo().getDisplayItems();
		List<Integer> inaccessibleSlots = renderInfoSupplier.get().getItemDisplayRenderInfo().getInaccessibleSlots();

		if (previousDisplayItems.size() != slotIndexes.size()) {
			return true;
		}

		int i = 0;
		for (int slotIndex : slotIndexes) {
			ItemStack newItem = getSlotItemCopy(slotIndex).orElse(ItemStack.EMPTY);

			ItemStack stack = previousDisplayItems.get(i).getItem();
			if (ItemStack.hashItemAndComponents(newItem) != ItemStack.hashItemAndComponents(stack)
					|| (inaccessibleSlots.contains(slotIndex) == inventoryHandlerSupplier.get().isSlotAccessible(slotIndex))) {
				return true;
			}

			i++;
		}

		return i != previousDisplayItems.size();
	}

	private void updateFullRenderInfo() {
		List<RenderInfo.DisplayItem> displayItems = new ArrayList<>();
		List<Integer> inaccessibleSlots = new ArrayList<>();
		for (int slotIndex : slotIndexes) {
			getSlotItemCopy(slotIndex).ifPresent(stackCopy ->
					displayItems.add(new RenderInfo.DisplayItem(stackCopy, slotRotations.getOrDefault(slotIndex, 0), slotIndex, displaySide)));
			if (!inventoryHandlerSupplier.get().isSlotAccessible(slotIndex)) {
				inaccessibleSlots.add(slotIndex);
			}
		}

		renderInfoSupplier.get().refreshItemDisplayRenderInfo(displayItems, inaccessibleSlots);
	}

	private Optional<ItemStack> getSlotItemCopy(int slotIndex) {
		ItemStack slotStack = inventoryHandlerSupplier.get().getStackInSlot(slotIndex);
		if (slotStack.isEmpty()) {
			Item filterItem = inventoryHandlerSupplier.get().getFilterItem(slotIndex);
			if (filterItem != Items.AIR) {
				return Optional.of(new ItemStack(filterItem));
			}

			return getMemorySettings.get().getSlotFilterStack(slotIndex, true);
		}
		ItemStack stackCopy = slotStack.copy();
		stackCopy.setCount(1);
		return Optional.of(stackCopy);
	}

	public void selectSlot(int slotIndex) {
		if (slotIndexes.size() + 1 > itemNumberLimit) {
			return;
		}
		slotIndexes.add(slotIndex);
		serializeSlotIndexes();

		updateFullRenderInfo();
	}

	private void serializeSlotIndexes() {
		categoryNbt.putIntArray(SLOTS_TAG, slotIndexes);
		saveNbt.accept(categoryNbt);
	}

	public List<Integer> getSlots() {
		return slotIndexes;
	}

	public int getRotation(int slotIndex) {
		return slotRotations.getOrDefault(slotIndex, 0);
	}

	public void rotate(int slotIndex, boolean clockwise) {
		if (!slotIndexes.contains(slotIndex)) {
			return;
		}

		int rotation = getRotation(slotIndex);
		rotation = (rotation + ((clockwise ? 1 : -1) * 45) + 360) % 360;
		slotRotations.put(slotIndex, rotation);
		serializeRotations();
		updateFullRenderInfo();
	}

	private void serializeRotations() {
		NBTHelper.putMap(categoryNbt, ROTATIONS_TAG, slotRotations, String::valueOf, IntTag::valueOf);
		saveNbt.accept(categoryNbt);
	}

	public void setColor(DyeColor color) {
		this.color = color;
		categoryNbt.putInt(COLOR_TAG, color.getId());
		saveNbt.accept(categoryNbt);
	}

	public DyeColor getColor() {
		return color;
	}

	public DisplaySide getDisplaySide() {
		return displaySide;
	}

	public void setDisplaySide(DisplaySide displaySide) {
		this.displaySide = displaySide;
		categoryNbt.putString(DISPLAY_SIDE_TAG, displaySide.getSerializedName());
		saveNbt.accept(categoryNbt);
		updateFullRenderInfo();
	}

	@Override
	public void reloadFrom(CompoundTag categoryNbt) {
		this.categoryNbt = categoryNbt;
		deserialize();
	}

	@Override
	public void overwriteWith(ItemDisplaySettingsCategory otherCategory) {
		slotIndexes.clear();
		slotIndexes.addAll(otherCategory.getSlots());
		serializeSlotIndexes();
		slotRotations.clear();
		slotRotations.putAll(otherCategory.slotRotations);
		serializeRotations();
		setColor(otherCategory.getColor());

		itemsChanged();
	}

	private void deserialize() {
		slotIndexes.clear();
		NBTHelper.getIntArray(categoryNbt, SLOTS_TAG).ifPresent(slots -> {
			for (int slot : slots) {
				slotIndexes.add(slot);
			}
		});
		slotRotations = NBTHelper.getMap(categoryNbt, ROTATIONS_TAG, Integer::valueOf, (k, v) -> Optional.of(((IntTag) v).getAsInt())).orElseGet(HashMap::new);
		color = NBTHelper.getInt(categoryNbt, COLOR_TAG).map(DyeColor::byId).orElse(DyeColor.RED);

		//legacy nbt support to be removed in the future
		NBTHelper.getInt(categoryNbt, SLOT_TAG).ifPresent(e -> {
			slotIndexes.add(e);
			categoryNbt.remove(SLOT_TAG);
			serializeSlotIndexes();
		});
		NBTHelper.getInt(categoryNbt, ROTATION_TAG).ifPresent(r -> {
			if (!slotIndexes.isEmpty()) {
				slotRotations.put(slotIndexes.iterator().next(), r);
			}
			categoryNbt.remove(ROTATION_TAG);
			serializeRotations();
		});
		NBTHelper.getEnumConstant(categoryNbt, DISPLAY_SIDE_TAG, DisplaySide::fromName).ifPresent(ds -> displaySide = ds);
	}

	public void itemChanged(int changedSlotIndex) {
		if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER || !slotIndexes.contains(changedSlotIndex)) {
			return;
		}

		if (haveRenderedItemsChanged()) {
			updateFullRenderInfo();
		}
	}

	public void itemsChanged() {
		if (haveRenderedItemsChanged()) {
			updateFullRenderInfo();
		}
	}

	@Override
	public Optional<Integer> getSlotColor(int slotNumber) {
		return slotIndexes.contains(slotNumber) ? Optional.of(color.getTextureDiffuseColor()) : Optional.empty();
	}

	/**
	 * Selects slots that shouldn't be sorted
	 *
	 * @param minSlot inclusive
	 * @param maxSlot exclusive
	 */

	public void selectSlots(int minSlot, int maxSlot) {
		for (int slotIndex = minSlot; slotIndex < maxSlot; slotIndex++) {
			if (slotIndexes.size() + 1 > itemNumberLimit) {
				return;
			}
			slotIndexes.add(slotIndex);
		}
		serializeSlotIndexes();
		updateFullRenderInfo();
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return slotIndexes.stream().anyMatch(slotIndex -> slotIndex >= slots);
	}

	@Override
	public void copyTo(ItemDisplaySettingsCategory otherCategory, int startFromSlot, int slotOffset) {
		//noop - keep the display item of the other category
	}

	@Override
	public void deleteSlotSettingsFrom(int slotIndex) {
		slotIndexes.removeIf(slot -> slot >= slotIndex);
		serializeSlotIndexes();
	}

}
