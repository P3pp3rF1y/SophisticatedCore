package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

import java.util.UUID;

public record EnderLinkPendingCraftData(EnderLinkPendingCraftPlan plan, @Nullable UUID claimId) {
	private static final String PLAN_TAG = "plan_kind";
	private static final String CLAIM_ID_TAG = "claim_id";

	public boolean resolvesToLinkerTarget() {
		return plan.resolvesToLinkerTarget();
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString(PLAN_TAG, plan.getSerializedName());
		if (claimId != null) {
			tag.putUUID(CLAIM_ID_TAG, claimId);
		}
		return tag;
	}

	public static EnderLinkPendingCraftData load(CompoundTag tag) {
		return new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.fromSerializedName(tag.getString(PLAN_TAG)),
				tag.hasUUID(CLAIM_ID_TAG) ? tag.getUUID(CLAIM_ID_TAG) : null);
	}
}
