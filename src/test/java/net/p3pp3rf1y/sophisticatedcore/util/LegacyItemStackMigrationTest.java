package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyItemStackMigrationTest {
	@Test
	void convertsLegacyEnchantmentsAndPreservesOversizedCountsAndNestedBackpackData() {
		CompoundTag sword = new CompoundTag();
		sword.putString("id", "minecraft:diamond_sword");
		sword.putByte("Count", (byte) 1);
		CompoundTag swordData = new CompoundTag();
		ListTag enchantments = new ListTag();
		CompoundTag enchantment = new CompoundTag();
		enchantment.putString("id", "minecraft:sharpness");
		enchantment.putShort("lvl", (short) 5);
		enchantments.add(enchantment);
		swordData.put("Enchantments", enchantments);
		sword.put("tag", swordData);

		CompoundTag migratedSword = LegacyItemStackMigration.normalizeItemStack(sword);
		assertEquals(5, migratedSword.getCompound("components").getCompound("minecraft:enchantments").getCompound("levels").getInt("minecraft:sharpness"));
		assertFalse(migratedSword.contains("tag"));

		CompoundTag backpack = new CompoundTag();
		backpack.putString("id", "sophisticatedbackpacks:backpack");
		backpack.putByte("Count", (byte) 1);
		CompoundTag customData = new CompoundTag();
		customData.putInt("clothColor", 0x112233);
		backpack.put("tag", customData);
		CompoundTag migratedBackpack = LegacyItemStackMigration.normalizeItemStack(backpack);
		assertEquals(0x112233, migratedBackpack.getCompound("components").getCompound("minecraft:custom_data").getInt("clothColor"));

		CompoundTag oversized = new CompoundTag();
		oversized.putString("id", "minecraft:stone");
		oversized.putByte("Count", (byte) 64);
		oversized.putInt("realCount", 200);
		CompoundTag inventory = new CompoundTag();
		ListTag items = new ListTag();
		items.add(oversized);
		inventory.put("Items", items);
		LegacyItemStackMigration.normalizeInventory(inventory);
		assertEquals(200, inventory.getList("Items", Tag.TAG_COMPOUND).getCompound(0).getInt("count"));
		assertTrue(inventory.getList("Items", Tag.TAG_COMPOUND).getCompound(0).contains("id"));
		assertEquals(200, LegacyItemStackMigration.normalizeItemStack(inventory.getList("Items", Tag.TAG_COMPOUND).getCompound(0)).getInt("count"));

		backpack.putInt("count", 1);
		assertEquals(0x112233,
				LegacyItemStackMigration.normalizeItemStack(backpack).getCompound("components").getCompound("minecraft:custom_data").getInt("clothColor"));
	}
}
