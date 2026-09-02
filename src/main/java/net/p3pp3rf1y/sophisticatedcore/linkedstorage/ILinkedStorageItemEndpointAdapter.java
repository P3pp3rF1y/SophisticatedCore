package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.world.item.ItemStack;

public interface ILinkedStorageItemEndpointAdapter extends ILinkedStorageEndpointAdapter<ItemStack> {
	boolean supports(ItemStack stack);
}
