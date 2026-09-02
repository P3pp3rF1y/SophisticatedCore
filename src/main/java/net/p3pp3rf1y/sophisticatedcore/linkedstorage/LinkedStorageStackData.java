package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** NBT schema shared by Core and endpoint modules for linked item stacks. */
public final class LinkedStorageStackData {
	private static final String LINKED_STORAGE_TAG = "sophisticatedcore:linked_storage";
	private static final String ENDPOINT_TAG = "endpoint";
	private static final String LINKER_TARGET_TAG = "linker_target";
	private static final String PENDING_CRAFT_TAG = "pending_craft";
	private static final String PRIMARY_ENDPOINT_TAG = "primary_endpoint";
	private static final String RENDER_REVISION_TAG = "render_revision";

	private LinkedStorageStackData() {
	}

	@Nullable
	public static LinkedStorageEndpointData getEndpoint(ItemStack stack) {
		CompoundTag linkedStorage = getLinkedStorageTag(stack);
		return linkedStorage != null && linkedStorage.contains(ENDPOINT_TAG) ? LinkedStorageEndpointData.load(linkedStorage.getCompound(ENDPOINT_TAG)) : null;
	}

	public static void setEndpoint(ItemStack stack, LinkedStorageEndpointData endpoint) {
		getOrCreateLinkedStorageTag(stack).put(ENDPOINT_TAG, endpoint.save());
	}

	@Nullable
	public static EnderLinkerTargetData getLinkerTarget(ItemStack stack) {
		CompoundTag linkedStorage = getLinkedStorageTag(stack);
		return linkedStorage != null && linkedStorage.contains(LINKER_TARGET_TAG)
				? EnderLinkerTargetData.load(linkedStorage.getCompound(LINKER_TARGET_TAG))
				: null;
	}

	public static void setLinkerTarget(ItemStack stack, EnderLinkerTargetData target) {
		getOrCreateLinkedStorageTag(stack).put(LINKER_TARGET_TAG, target.save());
	}

	@Nullable
	public static EnderLinkPendingCraftData getPendingCraft(ItemStack stack) {
		CompoundTag linkedStorage = getLinkedStorageTag(stack);
		return linkedStorage != null && linkedStorage.contains(PENDING_CRAFT_TAG)
				? EnderLinkPendingCraftData.load(linkedStorage.getCompound(PENDING_CRAFT_TAG))
				: null;
	}

	public static void setPendingCraft(ItemStack stack, EnderLinkPendingCraftData pendingCraft) {
		getOrCreateLinkedStorageTag(stack).put(PENDING_CRAFT_TAG, pendingCraft.save());
	}

	public static boolean isPrimaryEndpoint(ItemStack stack) {
		CompoundTag linkedStorage = getLinkedStorageTag(stack);
		return linkedStorage != null && linkedStorage.getBoolean(PRIMARY_ENDPOINT_TAG);
	}

	public static void setPrimaryEndpoint(ItemStack stack, boolean primary) {
		getOrCreateLinkedStorageTag(stack).putBoolean(PRIMARY_ENDPOINT_TAG, primary);
	}

	public static long getRenderRevision(ItemStack stack) {
		CompoundTag linkedStorage = getLinkedStorageTag(stack);
		return linkedStorage == null ? 0 : linkedStorage.getLong(RENDER_REVISION_TAG);
	}

	public static void setRenderRevision(ItemStack stack, long revision) {
		getOrCreateLinkedStorageTag(stack).putLong(RENDER_REVISION_TAG, revision);
	}

	public static void clear(ItemStack stack) {
		if (stack.hasTag()) {
			stack.getTag().remove(LINKED_STORAGE_TAG);
		}
	}

	private static CompoundTag getOrCreateLinkedStorageTag(ItemStack stack) {
		return stack.getOrCreateTagElement(LINKED_STORAGE_TAG);
	}

	@Nullable
	private static CompoundTag getLinkedStorageTag(ItemStack stack) {
		return stack.getTagElement(LINKED_STORAGE_TAG);
	}
}
