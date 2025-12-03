package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class ItemResourceHelper {
	private ItemResourceHelper() {
	}

	public static boolean isDamageable(ItemResource resource) {
		return resource.has(DataComponents.MAX_DAMAGE) && !resource.has(DataComponents.UNBREAKABLE) && resource.has(DataComponents.DAMAGE);
	}

	public static int hashItemAndComponents(ItemResource resource) {
		int i = 31 + resource.getItem().hashCode();
		return 31 * i + resource.getComponents().hashCode();
	}
}
