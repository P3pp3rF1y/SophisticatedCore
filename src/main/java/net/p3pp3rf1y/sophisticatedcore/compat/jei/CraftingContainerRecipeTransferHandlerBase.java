package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.common.transfer.RecipeTransferOperationsResult;
import mezz.jei.common.transfer.RecipeTransferUtil;
import mezz.jei.common.transfer.TransferOperation;
import mezz.jei.common.util.StringUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class CraftingContainerRecipeTransferHandlerBase<C extends StorageContainerMenuBase<?>> implements IRecipeTransferHandler<C, CraftingRecipe> {
	private final IRecipeTransferHandlerHelper handlerHelper;
	private final IStackHelper stackHelper;

	protected CraftingContainerRecipeTransferHandlerBase(IRecipeTransferHandlerHelper handlerHelper, IStackHelper stackHelper) {
		this.handlerHelper = handlerHelper;
		this.stackHelper = stackHelper;
	}

	@Override
	public Optional<MenuType<C>> getMenuType() {
		return Optional.empty();
	}

	@Override
	public RecipeType<CraftingRecipe> getRecipeType() {
		return RecipeTypes.CRAFTING;
	}

	@Nullable
	@Override
	public IRecipeTransferError transferRecipe(C container, CraftingRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
		Optional<? extends UpgradeContainerBase<?, ?>> potentialCraftingContainer = container.getOpenOrFirstCraftingContainer();
		if (potentialCraftingContainer.isEmpty()) {
			return handlerHelper.createInternalError();
		}

		UpgradeContainerBase<?, ?> openOrFirstCraftingContainer = potentialCraftingContainer.get();

		List<Slot> craftingSlots = Collections.unmodifiableList(openOrFirstCraftingContainer instanceof ICraftingContainer cc ? cc.getRecipeSlots() : Collections.emptyList());
		List<Slot> inventorySlots = container.realInventorySlots.stream().filter(s -> s.mayPickup(player)).toList();
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

		RecipeTransferOperationsResult transferOperations = RecipeTransferUtil.getRecipeTransferOperations(
				stackHelper,
				inventoryState.availableItemStacks,
				inputItemSlotViews,
				craftingSlots
		);

		if (transferOperations.missingItems.size() > 0) {
			Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
			return handlerHelper.createUserErrorForMissingSlots(message, transferOperations.missingItems);
		}

		if (!RecipeTransferUtil.validateSlots(player, transferOperations.results, craftingSlots, inventorySlots)) {
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
			TransferRecipeMessage message = new TransferRecipeMessage(
					recipe.getId(),
					toMap(transferOperations.results),
					craftingSlotIndexes,
					inventorySlotIndexes,
					maxTransfer);
			PacketHandler.INSTANCE.sendToServer(message);
		}

		return null;
	}

	private Map<Integer, Integer> toMap(List<TransferOperation> transferOperations) {
		Map<Integer, Integer> ret = new HashMap<>();
		transferOperations.forEach(to -> ret.put(to.craftingSlotId(), to.inventorySlotId()));
		return ret;
	}

	private boolean validateTransferInfo(
			C container,
			List<Slot> craftingSlots,
			List<Slot> inventorySlots
	) {
		Collection<Integer> craftingSlotIndexes = slotIndexes(craftingSlots);
		Collection<Integer> inventorySlotIndexes = slotIndexes(inventorySlots);
		ArrayList<Slot> allSlots = new ArrayList<>(container.realInventorySlots);
		allSlots.addAll(container.upgradeSlots);
		Collection<Integer> containerSlotIndexes = slotIndexes(allSlots);

		if (!containerSlotIndexes.containsAll(craftingSlotIndexes)) {
			SophisticatedCore.LOGGER.error("Recipe Transfer helper {} does not work for container {}. " +
							"The Recipes Transfer Helper references crafting slot indexes [{}] that are not found in the inventory container slots [{}]",
					getClass(), container.getClass(), StringUtil.intsToString(craftingSlotIndexes), StringUtil.intsToString(containerSlotIndexes)
			);
			return false;
		}

		if (!containerSlotIndexes.containsAll(inventorySlotIndexes)) {
			SophisticatedCore.LOGGER.error("Recipe Transfer helper {} does not work for container {}. " +
							"The Recipes Transfer Helper references inventory slot indexes [{}] that are not found in the inventory container slots [{}]",
					getClass(), container.getClass(), StringUtil.intsToString(inventorySlotIndexes), StringUtil.intsToString(containerSlotIndexes)
			);
			return false;
		}

		return true;
	}

	private boolean validateRecipeView(
			C container,
			List<Slot> craftingSlots,
			List<IRecipeSlotView> inputSlots
	) {
		if (inputSlots.size() > craftingSlots.size()) {
			SophisticatedCore.LOGGER.error("Recipe View {} does not work for container {}. " +
							"The Recipe View has more input slots ({}) than the number of inventory crafting slots ({})",
					getClass(), container.getClass(), inputSlots.size(), craftingSlots.size()
			);
			return false;
		}

		return true;
	}

	@Nullable
	private InventoryState getInventoryState(
			Collection<Slot> craftingSlots,
			Collection<Slot> inventorySlots,
			Player player,
			C container
	) {
		Map<Slot, ItemStack> availableItemStacks = new HashMap<>();
		int filledCraftSlotCount = 0;
		int emptySlotCount = 0;

		for (Slot slot : craftingSlots) {
			final ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				if (!slot.mayPickup(player)) {
					SophisticatedCore.LOGGER.error(
							"Recipe Transfer helper {} does not work for container {}. " +
									"The Player is not able to move items out of Crafting Slot number {}",
							getClass(), container.getClass(), slot.index
					);
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
					SophisticatedCore.LOGGER.error(
							"Recipe Transfer helper {} does not work for container {}. " +
									"The Player is not able to move items out of Inventory Slot number {}",
							getClass(), container.getClass(), slot.index
					);
					return null;
				}
				availableItemStacks.put(slot, stack.copy());
			} else {
				emptySlotCount++;
			}
		}

		return new InventoryState(availableItemStacks, filledCraftSlotCount, emptySlotCount);
	}

	private Set<Integer> slotIndexes(Collection<Slot> slots) {
		return slots.stream()
				.map(s -> s.index)
				.collect(Collectors.toSet());
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

	public record InventoryState(
			Map<Slot, ItemStack> availableItemStacks,
			int filledCraftSlotCount,
			int emptySlotCount
	) {
		/**
		 * check if we have enough inventory space to shuffle items around to their final locations
		 */
		public boolean hasRoom(int inputCount) {
			return filledCraftSlotCount - inputCount <= emptySlotCount;
		}
	}

}
