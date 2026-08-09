package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.common.transfer.RecipeTransferOperationsResult;
import mezz.jei.common.transfer.RecipeTransferUtil;
import mezz.jei.common.transfer.TransferOperation;
import mezz.jei.common.util.StringUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JeiCraftingContainerRecipeTransferHandlerBase<C extends StorageContainerMenuBase<?>, R extends Recipe<?>>
		implements
			IRecipeTransferHandler<C, R> {
	private final IRecipeTransferHandlerHelper handlerHelper;
	private final IStackHelper stackHelper;

	protected JeiCraftingContainerRecipeTransferHandlerBase(IRecipeTransferHandlerHelper handlerHelper, IStackHelper stackHelper) {
		this.handlerHelper = handlerHelper;
		this.stackHelper = stackHelper;
	}

	@Override
	public Optional<MenuType<C>> getMenuType() {
		return Optional.empty();
	}

	@Nullable
	@Override
	public IRecipeTransferError transferRecipe(C container, R recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
		Optional<? extends UpgradeContainerBase<?, ?>> potentialCraftingContainer = container.getOpenOrFirstCraftingContainer(recipe.getType());
		if (potentialCraftingContainer.isEmpty()) {
			return handlerHelper.createInternalError();
		}

		UpgradeContainerBase<?, ?> openOrFirstCraftingContainer = potentialCraftingContainer.get();

		List<Slot> craftingSlots = Collections
				.unmodifiableList(openOrFirstCraftingContainer instanceof ICraftingContainer cc ? cc.getRecipeSlots() : Collections.emptyList());
		List<Slot> inventorySlots = container.slots.stream().filter(s -> s.mayPickup(player) && !craftingSlots.contains(s)).toList();
		if (!validateTransferInfo(container, craftingSlots, inventorySlots)) {
			return handlerHelper.createInternalError();
		}
		List<IRecipeSlotView> inputItemSlotViews = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
		if (!validateRecipeView(container, craftingSlots, inputItemSlotViews)) {
			return handlerHelper.createInternalError();
		}

		InventoryState inventoryState = getInventoryState(craftingSlots, inventorySlots, player, container);
		if (inventoryState == null) {
			return handlerHelper.createInternalError();
		}

		// check if we have enough inventory space to shuffle items around to their final locations
		int inputCount = inputItemSlotViews.size();
		if (!inventoryState.hasRoom(inputCount)) {
			Component message = Component.translatable("jei.tooltip.error.recipe.transfer.inventory.full");
			return handlerHelper.createUserErrorWithTooltip(message);
		}

		RecipeTransferOperationsResult transferOperations = RecipeTransferUtil.getRecipeTransferOperations(stackHelper, inventoryState.availableItemStacks,
				inputItemSlotViews, craftingSlots);

		if (transferOperations.missingItems.size() > 0) {
			Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
			return handlerHelper.createUserErrorForMissingSlots(message, transferOperations.missingItems);
		}

		if (!validateSlots(container, transferOperations.results, craftingSlots, inventorySlots)) {
			return handlerHelper.createInternalError();
		}

		List<Integer> craftingSlotIndexes = craftingSlots.stream().map(s -> s.index).sorted().toList();
		List<Integer> inventorySlotIndexes = inventorySlots.stream().map(s -> s.index).sorted().toList();

		if (doTransfer) {
			if (!openOrFirstCraftingContainer.isOpen()) {
				container.getOpenContainer().ifPresent(c -> {
					c.setIsOpen(false);
					container.setOpenTabId(-1);
				});
				openOrFirstCraftingContainer.setIsOpen(true);
				container.setOpenTabId(openOrFirstCraftingContainer.getUpgradeContainerId());
			}
			ResourceLocation recipeTypeId = ForgeRegistries.RECIPE_TYPES.getKey(recipe.getType());
			if (recipeTypeId != null) {
				JeiTransferRecipeMessage message = new JeiTransferRecipeMessage(recipe.getId(), recipeTypeId, toMap(transferOperations.results),
						craftingSlotIndexes, inventorySlotIndexes, maxTransfer);
				PacketHandler.INSTANCE.sendToServer(message);
			}
		}

		return null;
	}

	private Map<Integer, Integer> toMap(List<TransferOperation> transferOperations) {
		Map<Integer, Integer> ret = new HashMap<>();
		transferOperations.forEach(to -> ret.put(to.craftingSlotId(), to.inventorySlotId()));
		return ret;
	}

	static boolean validateSlots(StorageContainerMenuBase<?> container, Collection<TransferOperation> transferOperations, Collection<Slot> craftingSlots,
			Collection<Slot> inventorySlots) {
		return validateSlots(container.getTotalSlotsNumber(), container::getSlot, transferOperations, craftingSlots, inventorySlots);
	}

	static boolean validateSlots(int totalSlotsNumber, IntFunction<Slot> getSlot, Collection<TransferOperation> transferOperations,
			Collection<Slot> craftingSlots, Collection<Slot> inventorySlots) {
		List<Integer> invalidSlotIds = transferOperations.stream().flatMap(to -> Stream.of(to.inventorySlotId(), to.craftingSlotId())).distinct()
				.filter(slotId -> slotId < 0 || slotId >= totalSlotsNumber).toList();
		if (!invalidSlotIds.isEmpty()) {
			SophisticatedCore.LOGGER.error("Transfer request has invalid slot ids in its transfer operations: {}", StringUtil.intsToString(invalidSlotIds));
			return false;
		}

		Set<Integer> craftingSlotIndexes = slotIndexes(craftingSlots);
		Set<Integer> inventorySlotIndexes = slotIndexes(inventorySlots);

		List<Integer> invalidCraftingSlots = transferOperations.stream().map(to -> getSlot.apply(to.craftingSlotId()).index)
				.filter(slotId -> !craftingSlotIndexes.contains(slotId)).toList();
		if (!invalidCraftingSlots.isEmpty()) {
			SophisticatedCore.LOGGER.error(
					"Transfer request has invalid slots for the destination of the recipe, the slots are not included in the list of crafting slots. {}",
					StringUtil.intsToString(invalidCraftingSlots));
			return false;
		}

		List<Integer> invalidInventorySlots = transferOperations.stream().map(to -> getSlot.apply(to.inventorySlotId()).index)
				.filter(slotId -> !inventorySlotIndexes.contains(slotId) && !craftingSlotIndexes.contains(slotId)).toList();
		if (!invalidInventorySlots.isEmpty()) {
			SophisticatedCore.LOGGER.error(
					"Transfer request has invalid source slots for the inventory stacks for the recipe, the slots are not included in the list of inventory slots or recipe slots. {}\n inventory slots: {}\n crafting slots: {}",
					StringUtil.intsToString(invalidInventorySlots), StringUtil.intsToString(inventorySlotIndexes),
					StringUtil.intsToString(craftingSlotIndexes));
			return false;
		}

		Set<Integer> sharedSlotIndexes = inventorySlotIndexes.stream().filter(craftingSlotIndexes::contains).collect(Collectors.toSet());
		if (!sharedSlotIndexes.isEmpty()) {
			SophisticatedCore.LOGGER.error("Transfer request has invalid slots, inventorySlots and craftingSlots should not share any slot, but both have: {}",
					StringUtil.intsToString(sharedSlotIndexes));
			return false;
		}

		List<Integer> inactiveSlotIndexes = Stream.concat(craftingSlots.stream(), inventorySlots.stream()).filter(slot -> !slot.isActive())
				.map(slot -> slot.index).toList();
		if (!inactiveSlotIndexes.isEmpty()) {
			SophisticatedCore.LOGGER.error("Transfer request has invalid slots, they are fake slots (recipe outputs): {}",
					StringUtil.intsToString(inactiveSlotIndexes));
			return false;
		}

		return true;
	}

	private boolean validateTransferInfo(C container, List<Slot> craftingSlots, List<Slot> inventorySlots) {
		Collection<Integer> craftingSlotIndexes = slotIndexes(craftingSlots);
		Collection<Integer> inventorySlotIndexes = slotIndexes(inventorySlots);
		ArrayList<Slot> allSlots = new ArrayList<>(container.slots);
		allSlots.addAll(container.upgradeSlots);
		Collection<Integer> containerSlotIndexes = slotIndexes(allSlots);

		if (!containerSlotIndexes.containsAll(craftingSlotIndexes)) {
			SophisticatedCore.LOGGER.error(
					"Recipe Transfer helper {} does not work for container {}. "
							+ "The Recipes Transfer Helper references crafting slot indexes [{}] that are not found in the inventory container slots [{}]",
					getClass(), container.getClass(), StringUtil.intsToString(craftingSlotIndexes), StringUtil.intsToString(containerSlotIndexes));
			return false;
		}

		if (!containerSlotIndexes.containsAll(inventorySlotIndexes)) {
			SophisticatedCore.LOGGER.error(
					"Recipe Transfer helper {} does not work for container {}. "
							+ "The Recipes Transfer Helper references inventory slot indexes [{}] that are not found in the inventory container slots [{}]",
					getClass(), container.getClass(), StringUtil.intsToString(inventorySlotIndexes), StringUtil.intsToString(containerSlotIndexes));
			return false;
		}

		return true;
	}

	private boolean validateRecipeView(C container, List<Slot> craftingSlots, List<IRecipeSlotView> inputSlots) {
		if (inputSlots.size() > craftingSlots.size()) {
			SophisticatedCore.LOGGER.error(
					"Recipe View {} does not work for container {}. "
							+ "The Recipe View has more input slots ({}) than the number of inventory crafting slots ({})",
					getClass(), container.getClass(), inputSlots.size(), craftingSlots.size());
			return false;
		}

		return true;
	}

	@Nullable
	private InventoryState getInventoryState(Collection<Slot> craftingSlots, Collection<Slot> inventorySlots, Player player, C container) {
		Map<Slot, ItemStack> availableItemStacks = new HashMap<>();
		int filledCraftSlotCount = 0;
		int emptySlotCount = 0;

		for (Slot slot : craftingSlots) {
			final ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				if (!slot.mayPickup(player)) {
					SophisticatedCore.LOGGER.error("Recipe Transfer helper {} does not work for container {}. "
							+ "The Player is not able to move items out of Crafting Slot number {}", getClass(), container.getClass(), slot.index);
					return null;
				}
				filledCraftSlotCount++;
				availableItemStacks.put(slot, stack.copy());
			}
		}

		for (Slot slot : inventorySlots) {
			final ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				if (!slot.mayPickup(player)) {
					SophisticatedCore.LOGGER.error("Recipe Transfer helper {} does not work for container {}. "
							+ "The Player is not able to move items out of Inventory Slot number {}", getClass(), container.getClass(), slot.index);
					return null;
				}
				availableItemStacks.put(slot, stack.copy());
			} else {
				emptySlotCount++;
			}
		}

		return new InventoryState(availableItemStacks, filledCraftSlotCount, emptySlotCount);
	}

	private static Set<Integer> slotIndexes(Collection<Slot> slots) {
		return slots.stream().map(s -> s.index).collect(Collectors.toSet());
	}

	private int getEmptySlotCount(Map<Integer, Slot> inventorySlots, Map<Integer, ItemStack> availableItemStacks) {
		int emptySlotCount = 0;
		for (Slot slot : inventorySlots.values()) {
			ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				availableItemStacks.put(slot.index, stack.copy());
			} else {
				++emptySlotCount;
			}
		}
		return emptySlotCount;
	}

	public record InventoryState(Map<Slot, ItemStack> availableItemStacks, int filledCraftSlotCount, int emptySlotCount) {
		/**
		 * check if we have enough inventory space to shuffle items around to their final locations
		 */
		public boolean hasRoom(int inputCount) {
			return filledCraftSlotCount - inputCount <= emptySlotCount;
		}
	}

}
