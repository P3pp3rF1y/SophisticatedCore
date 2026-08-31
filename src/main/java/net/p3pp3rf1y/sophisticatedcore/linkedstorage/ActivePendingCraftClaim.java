package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import java.util.UUID;

public record ActivePendingCraftClaim(UUID claimId, UUID groupId, UUID endpointId, EnderLinkPendingCraftPlan plan) {
}
