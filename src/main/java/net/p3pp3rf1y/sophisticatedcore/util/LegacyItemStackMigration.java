package net.p3pp3rf1y.sophisticatedcore.util;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

public final class LegacyItemStackMigration {
	private static final int LEGACY_DATA_VERSION = 3465;

	private LegacyItemStackMigration() {
	}

	public static void normalizeInventory(CompoundTag inventoryNbt) {
		ListTag items = inventoryNbt.getList("Items", Tag.TAG_COMPOUND);
		for (int i = 0; i < items.size(); i++) {
			items.set(i, normalizeItemStack(items.getCompound(i)));
		}
	}

	public static CompoundTag normalizeItemStack(CompoundTag itemTag) {
		CompoundTag normalized = itemTag;
		if (itemTag.contains("Count", Tag.TAG_ANY_NUMERIC) && !itemTag.contains("components") && itemTag.contains("id", Tag.TAG_STRING)) {
			normalized = (CompoundTag) DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, itemTag), LEGACY_DATA_VERSION,
					SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
		}
		if (itemTag.contains("realCount", Tag.TAG_ANY_NUMERIC) && (normalized != itemTag || !normalized.contains("count", Tag.TAG_ANY_NUMERIC))) {
			normalized.putInt("count", itemTag.getInt("realCount"));
		} else if (!normalized.contains("count", Tag.TAG_ANY_NUMERIC) && itemTag.contains("Count", Tag.TAG_ANY_NUMERIC)) {
			normalized.putInt("count", itemTag.getInt("Count"));
		}
		return normalized;
	}
}
