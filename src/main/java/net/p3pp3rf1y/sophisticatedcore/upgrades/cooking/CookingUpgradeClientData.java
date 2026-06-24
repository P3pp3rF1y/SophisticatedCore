package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeClientDataType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class CookingUpgradeClientData implements IUpgradeClientData {
	public static final UpgradeClientDataType<CookingUpgradeClientData> TYPE = new UpgradeClientDataType<>("smelting", CookingUpgradeClientData.class,
			CookingUpgradeClientData::deserializeNBT);

	private final boolean burning;

	public CookingUpgradeClientData(boolean burning) {
		this.burning = burning;
	}

	public boolean isBurning() {
		return burning;
	}

	@Override
	public CompoundTag serializeNBT() {
		return NBTHelper.putBoolean(new CompoundTag(), "burning", burning);
	}

	public static CookingUpgradeClientData deserializeNBT(CompoundTag nbt) {
		return new CookingUpgradeClientData(nbt.getBooleanOr("burning", false));
	}
}
