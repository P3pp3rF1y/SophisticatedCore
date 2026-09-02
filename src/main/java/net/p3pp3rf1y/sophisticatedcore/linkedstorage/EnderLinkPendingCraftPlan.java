package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import java.util.Arrays;

public enum EnderLinkPendingCraftPlan {
	CREATE_PRIMARY("linker_endpoint_primary", true, true), BIND_EXISTING_ENDPOINT("linker_endpoint_bind", true,
			true), ADD_SECONDARY("linker_endpoint_secondary", false, false);

	private final String serializedName;
	private final boolean returnsLinker;
	private final boolean returnsEndpointRemainder;

	EnderLinkPendingCraftPlan(String serializedName, boolean returnsLinker, boolean returnsEndpointRemainder) {
		this.serializedName = serializedName;
		this.returnsLinker = returnsLinker;
		this.returnsEndpointRemainder = returnsEndpointRemainder;
	}

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
