package net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.ISlotColorCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ItemDisplaySettingsCategory implements ISettingsCategory<ItemDisplaySettingsCategory, ItemDisplaySettingsCategoryData>, ISlotColorCategory {
	public static final String NAME = "item_display";
	public static final int MIN_Z_OFFSET = -16;
	public static final int MAX_Z_OFFSET = 16;
	private final Supplier<InventoryHandler> inventoryHandlerSupplier;
	private final Supplier<RenderDataHandler> renderDataHandlerSupplier;
	private final Runnable save;
	private final int itemNumberLimit;
	private final boolean canDeselectSlots;
	private final Supplier<MemorySettingsCategory> getMemorySettings;
	private ItemDisplaySettingsCategoryData data;

	public ItemDisplaySettingsCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderDataHandler> renderDataHandlerSupplier,
			ItemDisplaySettingsCategoryData data, Runnable save, int itemNumberLimit, boolean canDeselectSlots,
			Supplier<MemorySettingsCategory> getMemorySettings) {
		this.inventoryHandlerSupplier = inventoryHandlerSupplier;
		this.renderDataHandlerSupplier = renderDataHandlerSupplier;
		this.data = data;
		this.save = save;
		this.itemNumberLimit = itemNumberLimit;
		this.canDeselectSlots = canDeselectSlots;
		this.getMemorySettings = getMemorySettings;
	}

	public int getItemNumberLimit() {
		return itemNumberLimit;
	}

	public void unselectSlot(int slotIndex) {
		if (!canDeselectSlots) {
			return;
		}

		data.removeSlotIndex(slotIndex);
		save();

		updateFullRenderData();
	}

	private boolean haveRenderedItemsChanged() {
		List<RenderData.DisplayItemData> previousDisplayItems = renderDataHandlerSupplier.get().getDisplayData().displayItems();
		List<Integer> inaccessibleSlots = renderDataHandlerSupplier.get().getDisplayData().inaccessibleSlots();

		if (previousDisplayItems.size() != data.slotIndexes().size()) {
			return true;
		}

		int i = 0;
		InventoryHandler inventoryHandler = inventoryHandlerSupplier.get();
		for (int slotIndex : data.slotIndexes()) {
			ItemStack newItem = getSlotItemCopy(slotIndex).orElse(ItemStack.EMPTY);
			RenderData.DisplayItemData previousDisplayItem = previousDisplayItems.get(i);

			ItemStack stack = previousDisplayItem.createItemStack();
			if (ItemStack.hashItemAndComponents(newItem) != ItemStack.hashItemAndComponents(stack) || previousDisplayItem.slotIndex() != slotIndex
					|| previousDisplayItem.rotation() != getRotation(slotIndex) || previousDisplayItem.zOffset() != getZOffset(slotIndex)
					|| previousDisplayItem.displaySide() != data.displaySide()
					|| (inaccessibleSlots.contains(slotIndex) == inventoryHandler.isSlotAccessible(slotIndex))) {
				return true;
			}

			i++;
		}

		return i != previousDisplayItems.size();
	}

	private boolean haveCountsOrFillRatiosChanged(InventoryHandler inventoryHandler) {
		if (renderDataHandlerSupplier.get().showsCountsAndFillRatios()) {
			List<Integer> previousSlotCounts = renderDataHandlerSupplier.get().getDisplayData().slotCounts();
			List<Float> previousSlotFillRatios = renderDataHandlerSupplier.get().getDisplayData().slotFillRatios();
			List<Integer> previousInfiniteSlots = renderDataHandlerSupplier.get().getDisplayData().infiniteSlots();

			if (previousSlotCounts.size() != inventoryHandler.size() || previousSlotFillRatios.size() != inventoryHandler.size()) {
				return true;
			}

			for (int slotIndex = 0; slotIndex < inventoryHandler.size(); slotIndex++) {
				int previousSlotCount = previousSlotCounts.get(slotIndex);
				float previousSlotFillRatio = previousSlotFillRatios.get(slotIndex);
				ItemStack stack = inventoryHandler.getStackInSlot(slotIndex);
				float currentSlotFillRatio = calculateSlotFillRatio(stack, inventoryHandler, slotIndex);
				if (previousSlotCount != stack.getCount() || previousInfiniteSlots.contains(slotIndex) != inventoryHandler.isInfinite(slotIndex)
						|| !MathHelper.epsilonEquals(previousSlotFillRatio, currentSlotFillRatio)) {
					return true;
				}
			}
		}
		return false;
	}

	private void updateFullRenderData() {
		List<RenderData.DisplayItemData> displayItems = new ArrayList<>();
		List<Integer> inaccessibleSlots = new ArrayList<>();
		InventoryHandler inventoryHandler = inventoryHandlerSupplier.get();
		collectDisplayItemsAndInaccessibleSlots(displayItems, inventoryHandler, inaccessibleSlots);

		List<Integer> slotCounts = new ArrayList<>();
		List<Float> slotFillRatios = new ArrayList<>();
		List<Integer> infiniteSlots = new ArrayList<>();

		RenderDataHandler renderDataHandler = renderDataHandlerSupplier.get();
		collectSlotCountsSlotFillRatiosAndInfiniteSlots(renderDataHandler, inventoryHandler, slotCounts, slotFillRatios, infiniteSlots);

		renderDataHandler.refreshDisplayData(displayItems, inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
	}

	private void collectSlotCountsSlotFillRatiosAndInfiniteSlots(RenderDataHandler renderDataHandler, InventoryHandler inventoryHandler,
			List<Integer> slotCounts, List<Float> slotFillRatios, List<Integer> infiniteSlots) {
		if (renderDataHandler.showsCountsAndFillRatios()) {
			for (int slotIndex = 0; slotIndex < inventoryHandler.size(); slotIndex++) {
				ItemStack stack = inventoryHandler.getStackInSlot(slotIndex);
				slotCounts.add(stack.getCount());
				slotFillRatios.add(calculateSlotFillRatio(stack, inventoryHandler, slotIndex));
				if (inventoryHandler.isInfinite(slotIndex)) {
					infiniteSlots.add(slotIndex);
				}
			}
		}
	}

	private void collectDisplayItemsAndInaccessibleSlots(List<RenderData.DisplayItemData> displayItems, InventoryHandler inventoryHandler,
			List<Integer> inaccessibleSlots) {
		for (int slotIndex : data.slotIndexes()) {
			displayItems.add(new RenderData.DisplayItemData(getSlotItemCopy(slotIndex).orElse(ItemStack.EMPTY), data.slotRotations().getOrDefault(slotIndex, 0),
					slotIndex, data.displaySide(), getZOffset(slotIndex)));
			if (!inventoryHandler.isSlotAccessible(slotIndex)) {
				inaccessibleSlots.add(slotIndex);
			}
		}
	}

	private void updateDisplayItemsAndInaccessibleSlots() {
		List<RenderData.DisplayItemData> displayItems = new ArrayList<>();
		List<Integer> inaccessibleSlots = new ArrayList<>();
		InventoryHandler inventoryHandler = inventoryHandlerSupplier.get();
		collectDisplayItemsAndInaccessibleSlots(displayItems, inventoryHandler, inaccessibleSlots);
		renderDataHandlerSupplier.get().refreshDisplayItemsAndInaccessibleSlots(displayItems, inaccessibleSlots);
	}

	private void updateCountsFillRatiosAndInfiniteSlots() {
		List<Integer> slotCounts = new ArrayList<>();
		List<Float> slotFillRatios = new ArrayList<>();
		List<Integer> infiniteSlots = new ArrayList<>();
		RenderDataHandler renderDataHandler = renderDataHandlerSupplier.get();
		collectSlotCountsSlotFillRatiosAndInfiniteSlots(renderDataHandler, inventoryHandlerSupplier.get(), slotCounts, slotFillRatios, infiniteSlots);
		renderDataHandler.refreshSlotCountsFillRatiosAndInfiniteSlots(slotCounts, slotFillRatios, infiniteSlots);
	}

	private static float calculateSlotFillRatio(ItemStack stack, InventoryHandler inventoryHandler, int slotIndex) {
		return stack.isEmpty() ? 0 : (float) stack.getCount() / inventoryHandler.getCapacityAsInt(slotIndex, ItemResource.of(stack));
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
		if (data.slotIndexes().size() + 1 > itemNumberLimit) {
			return;
		}
		data.slotIndexes().add(slotIndex);
		save();

		updateFullRenderData();
	}

	private void save() {
		save.run();
	}

	public List<Integer> getSlots() {
		return data.slotIndexes();
	}

	public int getRotation(int slotIndex) {
		return data.slotRotations().getOrDefault(slotIndex, 0);
	}

	public void rotate(int slotIndex, boolean clockwise) {
		if (!data.slotIndexes().contains(slotIndex)) {
			return;
		}

		int rotation = getRotation(slotIndex);
		rotation = (rotation + ((clockwise ? 1 : -1) * 45) + 360) % 360;
		data.setRotation(slotIndex, rotation);
		save();
		updateFullRenderData();
	}

	public int getZOffset(int slotIndex) {
		return data.slotZOffsets().getOrDefault(slotIndex, 0);
	}

	public void changeZOffset(int slotIndex, int offsetChange) {
		setZOffset(slotIndex, Math.max(MIN_Z_OFFSET, Math.min(MAX_Z_OFFSET, getZOffset(slotIndex) + offsetChange)));
	}

	public void setZOffset(int slotIndex, int zOffset) {
		if (!data.slotIndexes().contains(slotIndex)) {
			return;
		}

		data.setZOffset(slotIndex, zOffset);
		save();
		updateFullRenderData();
	}

	public void setColor(DyeColor color) {
		data.setColor(color);
		save();
	}

	public DyeColor getColor() {
		return data.color();
	}

	public DisplaySide getDisplaySide() {
		return data.displaySide();
	}

	public void setDisplaySide(DisplaySide displaySide) {
		data.setDisplaySide(displaySide);
		save();
		updateFullRenderData();
	}

	public boolean canDeselectSlots() {
		return canDeselectSlots;
	}

	@Override
	public void reloadFrom(ItemDisplaySettingsCategoryData data) {
		this.data = data;
	}

	@Override
	public void overwriteWith(ItemDisplaySettingsCategory otherCategory) {
		data = otherCategory.data.copy();
		save();
		itemsChanged();
	}

	public void itemChanged(int changedSlotIndex) {
		if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER || !data.slotIndexes().contains(changedSlotIndex)) {
			return;
		}

		if (haveRenderedItemsChanged()) {
			updateDisplayItemsAndInaccessibleSlots();
		}
		if (haveCountsOrFillRatiosChanged(inventoryHandlerSupplier.get())) {
			updateCountsFillRatiosAndInfiniteSlots();
		}
	}

	public void itemsChanged() {
		if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER) {
			return;
		}

		if (haveRenderedItemsChanged()) {
			updateDisplayItemsAndInaccessibleSlots();
		}
		if (haveCountsOrFillRatiosChanged(inventoryHandlerSupplier.get())) {
			updateCountsFillRatiosAndInfiniteSlots();
		}
	}

	@Override
	public Optional<Integer> getSlotColor(int slotNumber) {
		return data.slotIndexes().contains(slotNumber) ? Optional.of(data.color().getTextureDiffuseColor()) : Optional.empty();
	}

	/**
	 * Selects slots that shouldn't be sorted
	 *
	 * @param minSlot
	 *            inclusive
	 * @param maxSlot
	 *            exclusive
	 */

	public void selectSlots(int minSlot, int maxSlot) {
		for (int slotIndex = minSlot; slotIndex < maxSlot; slotIndex++) {
			if (data.slotIndexes().size() + 1 > itemNumberLimit) {
				return;
			}
			data.addSlot(slotIndex);
		}
		save();
		updateFullRenderData();
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return data.slotIndexes().stream().anyMatch(slotIndex -> slotIndex >= slots);
	}

	@Override
	public void copyTo(ItemDisplaySettingsCategory otherCategory, int startFromSlot, int slotOffset) {
		// noop - keep the display item of the other category
	}

	@Override
	public void deleteSlotSettingsFrom(int slotIndex) {
		data.removeSlot(slotIndex);
		save();
	}

}
