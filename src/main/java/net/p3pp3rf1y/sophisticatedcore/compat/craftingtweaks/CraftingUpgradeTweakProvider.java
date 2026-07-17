package net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import net.blay09.mods.craftingtweaks.CraftingTweaks;
import net.blay09.mods.craftingtweaks.api.CraftingGrid;
import net.blay09.mods.craftingtweaks.api.CraftingGridBuilder;
import net.blay09.mods.craftingtweaks.api.CraftingGridProvider;
import net.blay09.mods.craftingtweaks.api.CraftingTweaksAPI;
import net.blay09.mods.craftingtweaks.api.GridBalanceHandler;
import net.blay09.mods.craftingtweaks.api.GridClearHandler;
import net.blay09.mods.craftingtweaks.api.GridGuiSettings;
import net.blay09.mods.craftingtweaks.api.GridRefillHandler;
import net.blay09.mods.craftingtweaks.api.GridRotateHandler;
import net.blay09.mods.craftingtweaks.api.GridTransferHandler;
import net.blay09.mods.craftingtweaks.api.RecipeMapper;
import net.blay09.mods.craftingtweaks.api.TweakType;
import net.blay09.mods.craftingtweaks.crafting.ContainerIngredientProvider;
import net.blay09.mods.craftingtweaks.crafting.CraftingContext;
import net.blay09.mods.craftingtweaks.crafting.CraftingOperation;
import net.blay09.mods.craftingtweaks.crafting.IngredientToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("java:S3776") // keeping this as close as possible to default implementation in crafting tweaks hence higher complexity but easier porting
public class CraftingUpgradeTweakProvider implements CraftingGridProvider {
	private static final Identifier DEFAULT_GRID_ID = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "default");

	@Override
	public String getModId() {
		return SophisticatedCore.MOD_ID;
	}

	@Override
	public boolean handles(AbstractContainerMenu abstractContainerMenu) {
		return abstractContainerMenu instanceof StorageContainerMenuBase<?>;
	}

	@Override
	public void buildCraftingGrids(CraftingGridBuilder builder, AbstractContainerMenu containerMenu) {
		if (!(containerMenu instanceof StorageContainerMenuBase<?>)) {
			return;
		}
		builder.addCustomGrid(new StorageCraftingGrid());
	}

	private class StorageCraftingGrid implements CraftingGrid, GridGuiSettings {
		@Override
		public Identifier getId() {
			return DEFAULT_GRID_ID;
		}

		@Override
		@Nullable
		public Container getCraftingMatrix(Player player, AbstractContainerMenu menu) {
			return menu instanceof StorageContainerMenuBase<?> storageMenu ? getCraftMatrix(storageMenu).orElse(null) : null;
		}

		@Override
		public int getGridStartSlot(Player player, AbstractContainerMenu menu) {
			return menu instanceof StorageContainerMenuBase<?> storageMenu ? getCraftingGridStart(storageMenu) : 0;
		}

		@Override
		public int getGridSize(Player player, AbstractContainerMenu menu) {
			return menu instanceof StorageContainerMenuBase<?> storageMenu ? getCraftingGridSize(storageMenu) : 0;
		}

		@Override
		public GridClearHandler<AbstractContainerMenu> clearHandler() {
			return (craftingGrid, player, menu, forced) -> clearGrid(player, menu, forced);
		}

		@Override
		public GridRotateHandler<AbstractContainerMenu> rotateHandler() {
			return (craftingGrid, player, menu, reverse) -> rotateGrid(menu, reverse);
		}

		@Override
		public GridBalanceHandler<AbstractContainerMenu> balanceHandler() {
			return new StorageCraftingGridBalanceHandler();
		}

		@Override
		public GridTransferHandler<AbstractContainerMenu> transferHandler() {
			return new StorageCraftingGridTransferHandler();
		}

		@Override
		public GridRefillHandler<AbstractContainerMenu> refillHandler() {
			return new StorageCraftingGridRefillHandler();
		}

		@Override
		public boolean isButtonVisible(TweakType tweakType) {
			return false;
		}
	}

	public void clearGrid(Player player, AbstractContainerMenu menu, boolean forced) {
		if (!(menu instanceof StorageContainerMenuBase<?> storageContainer)) {
			return;
		}

		getCraftMatrix(storageContainer).ifPresent(craftMatrix -> {
			for (Slot recipeSlot : getRecipeSlots(storageContainer)) {
				int slotIndex = recipeSlot.getSlotIndex();
				ItemStack itemStack = craftMatrix.getItem(slotIndex);
				if (!itemStack.isEmpty()) {
					ItemStack returnStack = itemStack.copy();
					player.getInventory().add(returnStack);
					craftMatrix.setItem(slotIndex, returnStack.getCount() == 0 ? ItemStack.EMPTY : returnStack);
					if (returnStack.getCount() > 0 && forced) {
						player.drop(returnStack, false);
						craftMatrix.setItem(slotIndex, ItemStack.EMPTY);
					}
				}
			}

			storageContainer.broadcastChanges();
		});
	}

	private int rotateSlotId(int slotId, boolean counterClockwise) {
		if (!counterClockwise) {
			switch (slotId) {
				case 0 :
					return 1;
				case 1 :
					return 2;
				case 2 :
					return 5;
				case 3 :
					return 0;
				case 5 :
					return 8;
				case 6 :
					return 3;
				case 7 :
					return 6;
				case 8 :
					return 7;
				default :
					break;
			}
		} else {
			switch (slotId) {
				case 0 :
					return 3;
				case 1 :
					return 0;
				case 2 :
					return 1;
				case 3 :
					return 6;
				case 5 :
					return 2;
				case 6 :
					return 7;
				case 7 :
					return 8;
				case 8 :
					return 5;
				default :
					break;
			}
		}

		return 0;
	}

	private boolean ignoresSlotId(int slotId) {
		return slotId == 4;
	}

	private void rotateGrid(AbstractContainerMenu containerMenu, boolean counterClockwise) {
		if (!(containerMenu instanceof StorageContainerMenuBase<?> storageContainer)) {
			return;
		}
		getCraftMatrix(storageContainer).ifPresent(craftMatrix -> {
			List<Slot> recipeSlots = getRecipeSlots(storageContainer);
			int size = recipeSlots.size();
			if (size != 9) {
				return;
			}
			Container matrixClone = new SimpleContainer(size);

			for (int i = 0; i < size; ++i) {
				int slotIndex = recipeSlots.get(i).getSlotIndex();
				matrixClone.setItem(i, craftMatrix.getItem(slotIndex));
			}

			for (int i = 0; i < size; ++i) {
				if (!ignoresSlotId(i)) {
					int slotIndex = recipeSlots.get(rotateSlotId(i, counterClockwise)).getSlotIndex();
					craftMatrix.setItem(slotIndex, matrixClone.getItem(i));
				}
			}

			storageContainer.broadcastChanges();
		});
	}

	private static Optional<Container> getCraftMatrix(StorageContainerMenuBase<?> container) {
		return getOpenCraftingContainer(container).map(ICraftingContainer::getCraftMatrix);
	}

	@Override
	public boolean requiresServerSide() {
		return true;
	}

	private static Optional<ICraftingContainer> getOpenCraftingContainer(StorageContainerMenuBase<?> container) {
		return container.getOpenContainer()
				.flatMap(c -> (c instanceof ICraftingContainer craftingContainer) ? Optional.of(craftingContainer) : Optional.empty());
	}

	private static int getCraftingGridStart(StorageContainerMenuBase<?> container) {
		return getOpenCraftingContainer(container).map(cc -> {
			List<Slot> recipeSlots = cc.getRecipeSlots();
			if (!recipeSlots.isEmpty()) {
				return recipeSlots.get(0).index;
			}
			return 0;
		}).orElse(0);
	}

	private static int getCraftingGridSize(StorageContainerMenuBase<?> container) {
		return getOpenCraftingContainer(container).map(cc -> cc.getRecipeSlots().size()).orElse(0);
	}

	private static List<Slot> getRecipeSlots(StorageContainerMenuBase<?> container) {
		return getOpenCraftingContainer(container).map(ICraftingContainer::getRecipeSlots).orElse(List.of());
	}

	private static class StorageCraftingGridBalanceHandler implements GridBalanceHandler<AbstractContainerMenu> {
		@Override
		public void balanceGrid(CraftingGrid grid, Player player, AbstractContainerMenu menu) {
			if (!(menu instanceof StorageContainerMenuBase<?> storageContainer)) {
				return;
			}
			getCraftMatrix(storageContainer).ifPresent(craftMatrix -> {
				ArrayListMultimap<String, Integer> itemMap = ArrayListMultimap.create();
				Multiset<String> itemCount = HashMultiset.create();
				for (Slot recipeSlot : getRecipeSlots(storageContainer)) {
					int slotIndex = recipeSlot.getSlotIndex();
					ItemStack itemStack = craftMatrix.getItem(slotIndex);
					if (!itemStack.isEmpty() && itemStack.getMaxStackSize() > 1) {
						Identifier registryName = BuiltInRegistries.ITEM.getKey(itemStack.getItem());

						String key = Objects.toString(registryName);
						if (!itemStack.getComponentsPatch().isEmpty()) {
							key = key + "@" + itemStack.getComponentsPatch();
						}
						itemMap.put(key, slotIndex);
						itemCount.add(key, itemStack.getCount());
					}
				}

				for (String key : itemMap.keySet()) {
					List<Integer> balanceList = itemMap.get(key);
					int totalCount = itemCount.count(key);
					int countPerStack = totalCount / balanceList.size();
					int restCount = totalCount % balanceList.size();
					for (int slotIndex : balanceList) {
						ItemStack itemStack = craftMatrix.getItem(slotIndex);
						itemStack.setCount(countPerStack);
						craftMatrix.setItem(slotIndex, itemStack);
					}

					int idx = 0;
					while (restCount > 0) {
						int slotIndex = balanceList.get(idx);
						ItemStack itemStack = craftMatrix.getItem(slotIndex);
						if (itemStack.getCount() < itemStack.getMaxStackSize()) {
							itemStack.grow(1);
							craftMatrix.setItem(slotIndex, itemStack);
							restCount--;
						}
						idx++;
						if (idx >= balanceList.size()) {
							idx = 0;
						}
					}
				}

				menu.broadcastChanges();
			});
		}

		@Override
		public void spreadGrid(CraftingGrid grid, Player player, AbstractContainerMenu menu) {
			if (!(menu instanceof StorageContainerMenuBase<?> storageContainer)) {
				return;
			}
			getCraftMatrix(storageContainer).ifPresent(craftMatrix -> {
				while (true) {
					ItemStack biggestSlotStack = null;
					int biggestSlotSize = 1;
					int biggestSlotIndex = -1;
					for (Slot recipeSlot : getRecipeSlots(storageContainer)) {
						int slotIndex = recipeSlot.getSlotIndex();
						ItemStack itemStack = craftMatrix.getItem(slotIndex);
						if (!itemStack.isEmpty() && itemStack.getCount() > biggestSlotSize) {
							biggestSlotStack = itemStack;
							biggestSlotSize = itemStack.getCount();
							biggestSlotIndex = slotIndex;
						}
					}

					if (biggestSlotStack == null) {
						return;
					}

					boolean emptyBiggestSlot = false;
					for (Slot recipeSlot : getRecipeSlots(storageContainer)) {
						int slotIndex = recipeSlot.getSlotIndex();
						ItemStack itemStack = craftMatrix.getItem(slotIndex);
						if (itemStack.isEmpty()) {
							if (biggestSlotStack.getCount() > 1) {
								craftMatrix.setItem(slotIndex, biggestSlotStack.split(1));
								craftMatrix.setItem(biggestSlotIndex, biggestSlotStack);
							} else {
								emptyBiggestSlot = true;
							}
						}
					}

					if (!emptyBiggestSlot) {
						break;
					}
				}

				balanceGrid(grid, player, menu);
			});
		}
	}

	private static class StorageCraftingGridRefillHandler implements GridRefillHandler<AbstractContainerMenu> {
		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void refillRecipe(CraftingGrid grid, Player player, AbstractContainerMenu menu, RecipeHolder<?> recipeHolder, boolean stack) {
			if (!(menu instanceof StorageContainerMenuBase<?> storageContainer)) {
				return;
			}

			grid.clearHandler().clearGrid(grid, player, menu, true);
			Container craftMatrix = grid.getCraftingMatrix(player, menu);
			if (craftMatrix == null) {
				return;
			}

			Recipe<?> recipe = recipeHolder.value();
			CraftingContext context = new CraftingContext(List.of(new ContainerIngredientProvider(player.getInventory()),
					new ContainerIngredientProvider(new ItemHandlerContainer(storageContainer.getStorageWrapper().getInventoryHandler()))));
			CraftingOperation operation = context.createOperation((RecipeHolder) recipeHolder).prepare();
			if (!operation.canCraft()) {
				return;
			}

			int operations = 0;
			outer:
			do {
				List<IngredientToken> ingredientTokens = operation.getIngredientTokens();
				RecipeMapper recipeMapper = CraftingTweaksAPI.getRecipeMapper((Class) recipe.getClass());

				HashMap<Integer, IngredientToken> matrixDiff = new HashMap<>();
				for (int i = 0; i < ingredientTokens.size(); i++) {
					IngredientToken ingredientToken = ingredientTokens.get(i);
					int matrixSlot = recipeMapper.mapToMatrixSlot(recipe, i);
					if (matrixSlot != -1) {
						ItemStack itemStack = ingredientToken.peek();
						ItemStack slotStack = craftMatrix.getItem(matrixSlot);
						if (!slotStack.isEmpty()) {
							if (slotStack.getCount() >= slotStack.getMaxStackSize()) {
								break outer;
							} else if (!slotStack.isStackable()) {
								break outer;
							} else if (!ItemStack.isSameItemSameComponents(slotStack, itemStack)) {
								break outer;
							}
						}

						matrixDiff.put(matrixSlot, ingredientToken);
					}
				}
				matrixDiff.forEach((slot, ingredientToken) -> {
					ItemStack slotStack = craftMatrix.getItem(slot);
					ItemStack itemStack = ingredientToken.consume();
					craftMatrix.setItem(slot, itemStack.copyWithCount(itemStack.getCount() + slotStack.getCount()));
				});
				operations++;
				if (operations > 64) {
					CraftingTweaks.logger.warn("Something went wrong trying to refill recipe. Too many iterations. Recipe: {}", recipeHolder.id().identifier());
					break;
				}
				if (!stack || matrixDiff.isEmpty()) {
					break;
				}
			} while (operation.prepare().canCraft());

			menu.broadcastChanges();
		}
	}

	private static class ItemHandlerContainer implements Container {
		private final InventoryHandler itemHandler;

		private ItemHandlerContainer(InventoryHandler itemHandler) {
			this.itemHandler = itemHandler;
		}

		@Override
		public int getContainerSize() {
			return itemHandler.size();
		}

		@Override
		public boolean isEmpty() {
			for (int slot = 0; slot < itemHandler.size(); slot++) {
				if (!itemHandler.getStackInSlot(slot).isEmpty()) {
					return false;
				}
			}

			return true;
		}

		@Override
		public ItemStack getItem(int slot) {
			return isValidSlot(slot) ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			if (!isValidSlot(slot) || amount <= 0) {
				return ItemStack.EMPTY;
			}

			ItemStack itemStack = itemHandler.getStackInSlot(slot);
			try (Transaction transaction = Transaction.openRoot()) {
				int extracted = itemHandler.extract(slot, ItemResource.of(itemStack), Math.min(amount, itemStack.getCount()), transaction);
				transaction.commit();
				return extracted > 0 ? itemStack.copyWithCount(extracted) : ItemStack.EMPTY;
			}
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			if (!isValidSlot(slot)) {
				return ItemStack.EMPTY;
			}

			ItemStack itemStack = itemHandler.getStackInSlot(slot);
			itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
			return itemStack;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			if (!isValidSlot(slot)) {
				return;
			}

			if (stack.isEmpty() || itemHandler.isItemValid(slot, stack)) {
				itemHandler.setStackInSlot(slot, stack);
			}
		}

		@Override
		public void setChanged() {
			// Handled by the backing item handler when slots are mutated.
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return isValidSlot(slot) && itemHandler.isItemValid(slot, stack);
		}

		@Override
		public boolean canTakeItem(Container target, int slot, ItemStack stack) {
			ItemStack itemStack = itemHandler.getStackInSlot(slot);
			try (Transaction transaction = Transaction.openRoot()) {
				return itemHandler.extract(slot, ItemResource.of(itemStack), 1, transaction) > 0;
			}
		}

		@Override
		public void clearContent() {
			for (int slot = 0; slot < itemHandler.size(); slot++) {
				itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
			}
		}

		private boolean isValidSlot(int slot) {
			return slot >= 0 && slot < itemHandler.size();
		}
	}

	private static class StorageCraftingGridTransferHandler implements GridTransferHandler<AbstractContainerMenu> {
		@Override
		public ItemStack putIntoGrid(CraftingGrid craftingGrid, Player player, AbstractContainerMenu menu, int slotId, ItemStack itemStack) {
			if (!(menu instanceof StorageContainerMenuBase<?> storageContainer)) {
				return itemStack;
			}
			return getCraftMatrix(storageContainer).map(craftMatrix -> {
				ItemStack craftStack = craftMatrix.getItem(slotId);
				if (!craftStack.isEmpty()) {
					if (ItemStack.isSameItemSameComponents(craftStack, itemStack)) {
						int spaceLeft = Math.min(craftMatrix.getMaxStackSize(), craftStack.getMaxStackSize()) - craftStack.getCount();
						if (spaceLeft > 0) {
							ItemStack splitStack = itemStack.split(Math.min(spaceLeft, itemStack.getCount()));
							craftStack.grow(splitStack.getCount());
							if (itemStack.getCount() <= 0) {
								return ItemStack.EMPTY;
							}
						}
					}
				} else {
					ItemStack transferStack = itemStack.split(Math.min(itemStack.getCount(), craftMatrix.getMaxStackSize()));
					craftMatrix.setItem(slotId, transferStack);
				}

				return itemStack.getCount() <= 0 ? ItemStack.EMPTY : itemStack;
			}).orElse(itemStack);
		}

		@Override
		public boolean transferIntoGrid(CraftingGrid craftingGrid, Player player, AbstractContainerMenu menu, Slot fromSlot) {
			if (!(menu instanceof StorageContainerMenuBase<?> storageContainer)) {
				return false;
			}
			return getCraftMatrix(storageContainer).map(craftMatrix -> {
				int start = getCraftingGridStart(storageContainer);
				int size = getCraftingGridSize(storageContainer);
				ItemStack itemStack = fromSlot.getItem();
				if (itemStack.isEmpty()) {
					return false;
				} else {
					int firstEmptySlot = -1;

					for (int i = start; i < start + size; ++i) {
						int slotIndex = menu.getSlot(i).getContainerSlot();
						ItemStack craftStack = craftMatrix.getItem(slotIndex);
						if (!craftStack.isEmpty()) {
							if (ItemStack.isSameItemSameComponents(craftStack, itemStack)) {
								int spaceLeft = Math.min(craftMatrix.getMaxStackSize(), craftStack.getMaxStackSize()) - craftStack.getCount();
								if (spaceLeft > 0) {
									ItemStack splitStack = itemStack.split(Math.min(spaceLeft, itemStack.getCount()));
									craftStack.grow(splitStack.getCount());
									if (itemStack.getCount() <= 0) {
										return true;
									}
								}
							}
						} else if (firstEmptySlot == -1) {
							firstEmptySlot = slotIndex;
						}
					}

					if (itemStack.getCount() > 0 && firstEmptySlot != -1) {
						ItemStack transferStack = itemStack.split(Math.min(itemStack.getCount(), craftMatrix.getMaxStackSize()));
						craftMatrix.setItem(firstEmptySlot, transferStack);
						return true;
					} else {
						return false;
					}
				}
			}).orElse(false);
		}

		@Override
		public boolean canTransferFrom(Player player, AbstractContainerMenu menu, Slot sourceSlot, CraftingGrid craftingGrid) {
			if (!(menu instanceof StorageContainerMenuBase<?> storageContainer)) {
				return false;
			}
			return sourceSlot.mayPickup(player) && sourceSlot.index < storageContainer.getInventorySlotsSize();
		}
	}
}
