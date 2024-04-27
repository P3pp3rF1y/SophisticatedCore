package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LootHelper {
	private LootHelper() {}

	public static List<ItemStack> getLoot(ResourceLocation lootTableName, MinecraftServer server, ServerLevel level, Entity entity) {
		LootTable lootTable = server.getLootData().getLootTable(lootTableName);
		LootContext.Builder lootBuilder = new LootContext.Builder((new LootParams.Builder(level)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(entity.blockPosition())).create(LootContextParamSets.CHEST)).withOptionalRandomSeed(level.random.nextLong());
		List<ItemStack> lootStacks = new ArrayList<>();
		lootTable.getRandomItems(lootBuilder.create(Optional.empty()), lootStacks::add);
		return lootStacks;
	}

	public static void fillWithLoot(RandomSource rand, List<ItemStack> loot, IItemHandlerModifiable inventory) {
		List<Integer> slots = InventoryHelper.getEmptySlotsRandomized(inventory);
		InventoryHelper.shuffleItems(loot, slots.size(), rand);

		for (ItemStack lootStack : loot) {
			if (slots.isEmpty()) {
				SophisticatedCore.LOGGER.warn("Too much loot to add to container. Overflow is voided.");
				return;
			}

			if (!lootStack.isEmpty()) {
				inventory.setStackInSlot(slots.remove(slots.size() - 1), lootStack);
			}
		}
	}
}
