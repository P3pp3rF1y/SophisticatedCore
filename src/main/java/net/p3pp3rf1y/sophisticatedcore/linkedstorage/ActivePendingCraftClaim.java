package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record ActivePendingCraftClaim(UUID claimId, UUID groupId, UUID endpointId, EnderLinkPendingCraftPlan plan) {
	static final Codec<ActivePendingCraftClaim> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(UUIDUtil.CODEC.fieldOf("claim_id").forGetter(ActivePendingCraftClaim::claimId),
					UUIDUtil.CODEC.fieldOf("group_id").forGetter(ActivePendingCraftClaim::groupId),
					UUIDUtil.CODEC.fieldOf("endpoint_id").forGetter(ActivePendingCraftClaim::endpointId),
					EnderLinkPendingCraftPlan.CODEC.fieldOf("plan_kind").forGetter(ActivePendingCraftClaim::plan))
			.apply(instance, ActivePendingCraftClaim::new));
}
