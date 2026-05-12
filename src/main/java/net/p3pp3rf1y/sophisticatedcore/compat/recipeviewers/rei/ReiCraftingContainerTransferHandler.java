package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.impl.client.transfer.SimpleTransferHandlerImpl;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.List;

public class ReiCraftingContainerTransferHandler<C extends StorageContainerMenuBase<?>, D extends Display> implements SimpleTransferHandler {
	public static <C extends StorageContainerMenuBase<?>> TransferHandler crafting(Class<? extends C> containerClass) {
		return new ReiCraftingContainerTransferHandler<>(containerClass, BuiltinPlugin.CRAFTING, RecipeType.CRAFTING);
	}
	public static <C extends StorageContainerMenuBase<?>> TransferHandler smithing(Class<? extends C> containerClass) {
		return new ReiCraftingContainerTransferHandler<>(containerClass, BuiltinPlugin.SMITHING, RecipeType.SMITHING);
	}

	private final Class<? extends C> containerClass;
	private final CategoryIdentifier<D> categoryIdentifier;
	private final RecipeType<? extends Recipe<?>> recipeType;

	public ReiCraftingContainerTransferHandler(Class<? extends C> containerClass, CategoryIdentifier<D> categoryIdentifier, RecipeType<? extends Recipe<?>> recipeType) {
		this.containerClass = containerClass;
		this.categoryIdentifier = categoryIdentifier;
		this.recipeType = recipeType;
	}

	@Override
	public ApplicabilityResult checkApplicable(Context context) {
		if (!containerClass.isInstance(context.getMenu())
				|| ((StorageContainerMenuBase<?>) context.getMenu()).getOpenOrFirstCraftingContainer(recipeType).isEmpty()
				|| !categoryIdentifier.equals(context.getDisplay().getCategoryIdentifier())
				|| context.getContainerScreen() == null) {
			return ApplicabilityResult.createNotApplicable();
		}

		return ApplicabilityResult.createApplicable();
	}

	@Override
	public Iterable<SlotAccessor> getInputSlots(Context context) {
		if (!(context.getMenu() instanceof StorageContainerMenuBase<?> storageContainerMenuBase)) {
			return List.of();
		}

		return storageContainerMenuBase.getOpenOrFirstCraftingContainer(recipeType)
				.map(c -> c.getRecipeSlots().stream().map(ReiSlotAccessor::fromSlot).toList())
				.orElse(List.of());
	}

	@Override
	public Iterable<SlotAccessor> getInventorySlots(Context context) {
		if (!(context.getMenu() instanceof StorageContainerMenuBase<?> storageContainerMenuBase)) {
			return List.of();
		}

		return storageContainerMenuBase.realInventorySlots.stream().map(ReiSlotAccessor::fromSlot).toList();
	}

	@Override
	public Result handle(Context context) {
		List<InputIngredient<ItemStack>> inputs = getInputsIndexed(context);
		List<SlotAccessor> inputSlots = (List<SlotAccessor>) getInputSlots(context);
		List<SlotAccessor> inventorySlots = (List<SlotAccessor>) getInventorySlots(context);

		List<InputIngredient<ItemStack>> missing = SimpleTransferHandlerImpl.hasItemsIndexed(context, inventorySlots, inputs);
		if (!missing.isEmpty()) {
			IntSet missingIndices = new IntLinkedOpenHashSet(missing.size());
			for (InputIngredient<ItemStack> ingredient : missing) {
				missingIndices.add(ingredient.getDisplayIndex());
			}
			return Result.createFailed(Component.translatable("error.rei.not.enough.materials"))
					.renderer((matrices, mouseX, mouseY, delta, widgets, bounds, d) ->
							getMissingInputRenderer().renderMissingInput(context, inputs, missing, missingIndices, matrices, mouseX, mouseY, delta, widgets, bounds))
					.tooltipMissing(CollectionUtils.map(missing, ingredient -> EntryIngredients.ofItemStacks(ingredient.get())));
		}

		if (!context.isActuallyCrafting()) {
			return Result.createSuccessful();
		}

		StorageContainerMenuBase<?> storageContainerMenuBase = (StorageContainerMenuBase<?>) context.getMenu();
		storageContainerMenuBase.getOpenOrFirstCraftingContainer(recipeType)
				.ifPresent(openOrFirstCraftingContainer -> {
			if (!openOrFirstCraftingContainer.isOpen()) {
				storageContainerMenuBase.getOpenContainer().ifPresent(c -> {
					c.setIsOpen(false);
					storageContainerMenuBase.setOpenTabId(-1);
				});
				openOrFirstCraftingContainer.setIsOpen(true);
				storageContainerMenuBase.setOpenTabId(openOrFirstCraftingContainer.getUpgradeContainerId());
			}
		});

		AbstractContainerScreen<?> containerScreen = context.getContainerScreen();
		context.getMinecraft().setScreen(containerScreen);

		ResourceLocation recipeTypeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType);
		if (recipeTypeId != null) {
			List<Integer> inputSlotIds = inputSlots.stream()
					.map(ReiSlotAccessor.class::cast)
					.map(ReiSlotAccessor::getIndex)
					.toList();

			List<Integer> inventorySlotIds = inventorySlots.stream()
					.map(ReiSlotAccessor.class::cast)
					.map(ReiSlotAccessor::getIndex)
					.toList();

			PacketDistributor.sendToServer(
					new ReiTransferRecipePayload(
							context.getDisplay().getDisplayLocation().get(),
							recipeTypeId,
							save(inputs),
							inputSlotIds,
							inventorySlotIds,
							context.isStackedCrafting()
					)
			);
		}
		return Result.createSuccessful();
	}

	private static CompoundTag save(List<InputIngredient<ItemStack>> inputs) {
		CompoundTag tag = new CompoundTag();
		tag.put("Inputs", saveInputs(inputs));
		return tag;
	}

	private static Tag saveInputs(List<InputIngredient<ItemStack>> inputs) {
		ListTag tag = new ListTag();

		for (InputIngredient<ItemStack> input : inputs) {
			CompoundTag innerTag = new CompoundTag();
			innerTag.put("Ingredient", EntryIngredients.ofItemStacks(input.get()).saveIngredient());
			innerTag.putInt("Index", input.getIndex());
			tag.add(innerTag);
		}

		return tag;
	}
}
