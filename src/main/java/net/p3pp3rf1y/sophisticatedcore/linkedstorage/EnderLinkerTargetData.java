package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record EnderLinkerTargetData(UUID groupId, Component groupName) {
	public static final Codec<EnderLinkerTargetData> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(UUIDUtil.CODEC.fieldOf("group_id").forGetter(EnderLinkerTargetData::groupId),
					ComponentSerialization.CODEC.fieldOf("group_name").forGetter(EnderLinkerTargetData::groupName))
			.apply(instance, EnderLinkerTargetData::new));
	public static final StreamCodec<ByteBuf, EnderLinkerTargetData> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, EnderLinkerTargetData::groupId,
			ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC, EnderLinkerTargetData::groupName, EnderLinkerTargetData::new);
}
