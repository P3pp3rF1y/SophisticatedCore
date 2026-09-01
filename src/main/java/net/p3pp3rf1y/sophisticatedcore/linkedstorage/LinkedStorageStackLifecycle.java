package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

public final class LinkedStorageStackLifecycle {
	private LinkedStorageStackLifecycle() {
	}

	public static EnderLinkerStackState classifyLinker(ItemStack stack) {
		EnderLinkerTargetData linkerTarget = stack.get(ModCoreDataComponents.ENDER_LINKER_TARGET);
		EnderLinkPendingCraftData pendingCraft = stack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		if (linkerTarget != null && pendingCraft != null) {
			throw new IllegalStateException("Ender linker cannot be both linked and pending crafting");
		}
		if (linkerTarget != null) {
			return EnderLinkerStackState.TARGET;
		}
		return pendingCraft == null ? EnderLinkerStackState.UNLINKED : EnderLinkerStackState.PENDING_CRAFT;
	}

	public static LinkedStorageEndpointStackState classifyEndpoint(ItemStack stack) {
		return stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT) ? LinkedStorageEndpointStackState.ENDPOINT : LinkedStorageEndpointStackState.UNLINKED;
	}

	public static void clear(ItemStack stack) {
		stack.remove(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		stack.remove(ModCoreDataComponents.LINKED_STORAGE_PRIMARY_ENDPOINT);
		stack.remove(ModCoreDataComponents.LINKED_STORAGE_RENDER_REVISION);
		stack.remove(ModCoreDataComponents.ENDER_LINKER_TARGET);
		stack.remove(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
	}
}
