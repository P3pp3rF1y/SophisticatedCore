package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;

public enum EnderLinkPendingCraftPlan implements StringRepresentable {
	CREATE_PRIMARY("linker_endpoint_primary", true, true), BIND_EXISTING_ENDPOINT("linker_endpoint_bind", true,
			true), ADD_SECONDARY("linker_endpoint_secondary", false, false);

	public static final Codec<EnderLinkPendingCraftPlan> CODEC = StringRepresentable.fromEnum(EnderLinkPendingCraftPlan::values);
	public static final StreamCodec<ByteBuf, EnderLinkPendingCraftPlan> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
			.map(EnderLinkPendingCraftPlan::fromSerializedName, EnderLinkPendingCraftPlan::getSerializedName);

	private final String serializedName;
	private final boolean returnsLinker;
	private final boolean returnsEndpointRemainder;

	EnderLinkPendingCraftPlan(String serializedName, boolean returnsLinker, boolean returnsEndpointRemainder) {
		this.serializedName = serializedName;
		this.returnsLinker = returnsLinker;
		this.returnsEndpointRemainder = returnsEndpointRemainder;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}

	public boolean returnsLinker() {
		return returnsLinker;
	}

	public boolean returnsEndpointRemainder() {
		return returnsEndpointRemainder;
	}

	public boolean resolvesToLinkerTarget() {
		return this != ADD_SECONDARY;
	}

	public static EnderLinkPendingCraftPlan fromSerializedName(String serializedName) {
		return Arrays.stream(values()).filter(plan -> plan.serializedName.equals(serializedName)).findFirst()
				.orElseThrow(() -> new IllegalStateException("Unknown pending Ender Linker craft plan " + serializedName));
	}
}
