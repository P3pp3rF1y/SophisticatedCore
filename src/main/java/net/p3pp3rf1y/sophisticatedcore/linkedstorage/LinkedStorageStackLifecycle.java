package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class LinkedStorageStackLifecycle {
	private LinkedStorageStackLifecycle() {
	}

	public static EnderLinkerStackState classifyLinker(ItemStack stack) {
		EnderLinkerTargetData linkerTarget = LinkedStorageStackData.getLinkerTarget(stack);
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(stack);
		if (linkerTarget != null && pendingCraft != null) {
			throw new IllegalStateException("Ender Linker cannot be both linked and pending crafting");
		}
		return linkerTarget != null
				? EnderLinkerStackState.TARGET
				: pendingCraft == null ? EnderLinkerStackState.UNLINKED : EnderLinkerStackState.PENDING_CRAFT;
	}

	public static LinkedStorageEndpointStackState classifyEndpoint(ItemStack stack) {
		return LinkedStorageStackData.getEndpoint(stack) == null ? LinkedStorageEndpointStackState.UNLINKED : LinkedStorageEndpointStackState.ENDPOINT;
	}

	public static Optional<LinkedStorageEndpointRole> getEndpointRole(ItemStack stack) {
		if (classifyEndpoint(stack) != LinkedStorageEndpointStackState.ENDPOINT) {
			return Optional.empty();
		}
		return Optional.of(LinkedStorageStackData.isPrimaryEndpoint(stack) ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY);
	}

	public static void clear(ItemStack stack) {
		LinkedStorageStackData.clear(stack);
	}
}
