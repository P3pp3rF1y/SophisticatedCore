package net.p3pp3rf1y.sophisticatedcore.renderdata;

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;
import java.util.function.Function;

public class UpgradeClientDataType<T extends IUpgradeClientData> {
	private final String name;
	private final Class<T> clazz;
	private final Function<CompoundTag, T> deserialize;

	public UpgradeClientDataType(String name, Class<T> clazz, Function<CompoundTag, T> deserialize) {
		this.name = name;
		this.clazz = clazz;
		this.deserialize = deserialize;
	}

	public String getName() {
		return name;
	}

	public Optional<T> cast(IUpgradeClientData upgradeClientData) {
		if (clazz.isInstance(upgradeClientData)) {
			return Optional.of(clazz.cast(upgradeClientData));
		}
		return Optional.empty();
	}

	public T deserialize(CompoundTag nbt) {
		return deserialize.apply(nbt);
	}
}
