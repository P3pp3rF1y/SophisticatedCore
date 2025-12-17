package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class EmiGridMenuInfo<C extends StorageContainerMenuBase<?>> implements StandardRecipeHandler<C> {
	private final RecipeType<? extends Recipe<?>> recipeType;

	public static <C extends StorageContainerMenuBase<?>> EmiGridMenuInfo<C> crafting() {
		return new EmiGridMenuInfo<>(RecipeType.CRAFTING);
	}

	public static <C extends StorageContainerMenuBase<?>> EmiGridMenuInfo<C> smithing() {
		return new EmiGridMenuInfo<>(RecipeType.SMITHING);
	}

	private EmiGridMenuInfo(RecipeType<? extends Recipe<?>> recipeType) {
		this.recipeType = recipeType;
	}

	@Override
	public List<Slot> getInputSources(C handler) {
		List<Slot> slots = new ArrayList<>(handler.realInventorySlots.stream().filter(s -> s.mayPickup(Minecraft.getInstance().player)).toList());
		slots.addAll(getCraftingSlots(handler));
		return slots;
	}

	@Override
	public List<Slot> getCraftingSlots(C handler) {
		UpgradeContainerBase<?, ?> openOrFirstCraftingContainer = handler.getOpenOrFirstCraftingContainer(recipeType).orElse(null);
		return Collections.unmodifiableList(openOrFirstCraftingContainer instanceof ICraftingContainer cc ? cc.getRecipeSlots() : Collections.emptyList());
	}

	@Override
	public @Nullable Slot getOutputSlot(C handler) {
		return handler.getOpenOrFirstCraftingContainer(recipeType).map(c -> c.getSlots().getLast()).orElse(null);
	}

	@Override
	public boolean supportsRecipe(EmiRecipe recipe) {
		return VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory()) && recipe.supportsRecipeTree();
	}

	@Override
	public boolean canCraft(EmiRecipe recipe, EmiCraftContext<C> context) {
		return context.getScreenHandler().getOpenOrFirstCraftingContainer(recipeType).isPresent() && StandardRecipeHandler.super.canCraft(recipe, context);
	}

	@Override
	public boolean craft(EmiRecipe recipe, EmiCraftContext<C> context) {
		// We only need a stack of 1 here as the maxTransfer will be handled server side differently
		List<ItemStack> stacks = EmiRecipeFiller.getStacks(this, recipe, context.getScreen(), 1);
		if (stacks != null) {
			C container = context.getScreenHandler();
			Optional<? extends UpgradeContainerBase<?, ?>> potentialCraftingContainer = container.getOpenOrFirstCraftingContainer(recipeType);
			//noinspection OptionalGetWithoutIsPresent - Can be suppressed cause emi does a canCraft check before calling the craft method, and we test for a crafting container there
			UpgradeContainerBase<?, ?> openOrFirstCraftingContainer = potentialCraftingContainer.get();
			if (!openOrFirstCraftingContainer.isOpen()) {
				container.getOpenContainer().ifPresent(c -> {
					c.setIsOpen(false);
					container.setOpenTabId(-1);
				});
				openOrFirstCraftingContainer.setIsOpen(true);
				container.setOpenTabId(openOrFirstCraftingContainer.getUpgradeContainerId());
			}

			Minecraft.getInstance().setScreen(context.getScreen());
			if (!EmiClient.onServer) {
				return EmiRecipeFiller.clientFill(this, recipe, context.getScreen(), stacks, context.getDestination());
			} else {
				int action = switch (context.getDestination()) {
					case NONE -> 0;
					case CURSOR -> 1;
					case INVENTORY -> 2;
				};

				Identifier recipeTypeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType);
				if (recipeTypeId != null) {
					Slot output = getOutputSlot(container);
					ClientPacketDistributor.sendToServer(
							new EmiTransferRecipePayload(
									ResourceKey.create(Registries.RECIPE, recipe.getId()),
									recipeTypeId,
									action,
									getInputSources(container).stream().map(s -> s == null ? -1 : s.index).toList(),
									getCraftingSlots(recipe, container).stream().map(s -> s == null ? -1 : s.index).toList(),
									output == null ? -1 : output.index,
									stacks,
									context.getAmount() > 1
							)
					);
				}
			}
			return true;
		}
		return false;
	}

}