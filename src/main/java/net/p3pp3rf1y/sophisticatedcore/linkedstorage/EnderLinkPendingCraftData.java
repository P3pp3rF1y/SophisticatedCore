package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.UUID;

public record EnderLinkPendingCraftData(EnderLinkPendingCraftPlan plan, @Nullable UUID claimId) {
	public boolean resolvesToLinkerTarget() {
		return plan.resolvesToLinkerTarget();
	}

	public static final Codec<EnderLinkPendingCraftData> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(EnderLinkPendingCraftPlan.CODEC.fieldOf("plan_kind").forGetter(EnderLinkPendingCraftData::plan),
					UUIDUtil.CODEC.optionalFieldOf("claim_id").forGetter(data -> Optional.ofNullable(data.claimId())))
			.apply(instance, (plan, claimId) -> new EnderLinkPendingCraftData(plan, claimId.orElse(null))));
	public static final StreamCodec<ByteBuf, EnderLinkPendingCraftData> STREAM_CODEC = StreamCodec.composite(EnderLinkPendingCraftPlan.STREAM_CODEC,
			EnderLinkPendingCraftData::plan, StreamCodecHelper.ofNullable(UUIDUtil.STREAM_CODEC), EnderLinkPendingCraftData::claimId,
			EnderLinkPendingCraftData::new);
}
