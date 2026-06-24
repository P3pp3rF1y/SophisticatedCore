package net.p3pp3rf1y.sophisticatedcore.compat;

import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jspecify.annotations.Nullable;

public record CompatInfo(String modId, @Nullable VersionRange supportedVersionRange) {
	public CompatInfo(String modId) {
		this(modId, null);
	}

	public boolean isLoaded() {
		return ModList.get().getModContainerById(modId())
				.map(container -> supportedVersionRange() == null || supportedVersionRange().containsVersion(container.getModInfo().getVersion()))
				.orElse(false);
	}
}
