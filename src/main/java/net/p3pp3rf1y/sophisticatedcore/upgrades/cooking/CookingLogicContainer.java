package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CookingLogicContainer<T extends AbstractCookingRecipe> {
	private final Supplier<CookingLogic<T>> supplyCoookingLogic;

	private final List<Slot> smeltingSlots = new ArrayList<>();
	private int removeCount;
	private final Player player;

	public CookingLogicContainer(Player player, Supplier<CookingLogic<T>> supplyCoookingLogic, Consumer<Slot> addSlot) {
		this.player = player;
		this.supplyCoookingLogic = supplyCoookingLogic;

		addSmeltingSlot(addSlot, new SlotSuppliedHandler(() -> supplyCoookingLogic.get().getCookingInventory(), CookingLogic.COOK_INPUT_SLOT, -100, -100));
		addSmeltingSlot(addSlot, new SlotSuppliedHandler(() -> supplyCoookingLogic.get().getCookingInventory(), CookingLogic.FUEL_SLOT, -100, -100));
		addSmeltingSlot(addSlot, new SlotSuppliedHandler(() -> supplyCoookingLogic.get().getCookingInventory(), CookingLogic.COOK_OUTPUT_SLOT, -100, -100) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false; //needs to not allow player putting anything in
			}

			@Override
			public ItemStack remove(int amount) {
				if (hasItem()) {
					removeCount = removeCount + Math.min(amount, getItem().getCount());
				}

				return super.remove(amount);
			}

			@Override
			public void onTake(Player player, ItemStack stack) {
				checkTakeAchievements(stack);
				super.onTake(player, stack);
			}

			@Override
			protected void checkTakeAchievements(ItemStack stack) {
				stack.onCraftedBy(player.level(), player, removeCount);
				if (player instanceof ServerPlayer serverplayer) {
					supplyCoookingLogic.get().awardUsedRecipesAndPopExperience(serverplayer);
				}

				removeCount = 0;
				ForgeEventFactory.firePlayerSmeltedEvent(player, stack);
			}
		});
	}

	private void addSmeltingSlot(Consumer<Slot> addSlot, Slot slot) {
		addSlot.accept(slot);
		smeltingSlots.add(slot);
	}

	public int getBurnTimeTotal() {
		return supplyCoookingLogic.get().getBurnTimeTotal();
	}

	public long getBurnTimeFinish() {
		return supplyCoookingLogic.get().getBurnTimeFinish();
	}

	public long getCookTimeFinish() {
		return supplyCoookingLogic.get().getCookTimeFinish();
	}

	public int getCookTimeTotal() {
		return supplyCoookingLogic.get().getCookTimeTotal();
	}

	public boolean isCooking() {
		return supplyCoookingLogic.get().isCooking();
	}

	public boolean isBurning(Level world) {
		return supplyCoookingLogic.get().isBurning(world);
	}

	public List<Slot> getCookingSlots() {
		return smeltingSlots;
	}
}
