package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeClientDataType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class JukeboxUpgradeClientData implements IUpgradeClientData {
	public static final UpgradeClientDataType<JukeboxUpgradeClientData> TYPE = new UpgradeClientDataType<>("jukebox", JukeboxUpgradeClientData.class,
			JukeboxUpgradeClientData::deserializeNBT);

	private final boolean playing;

	public JukeboxUpgradeClientData(boolean playing) {
		this.playing = playing;
	}

	public boolean isPlaying() {
		return playing;
	}

	@Override
	public CompoundTag serializeNBT() {
		return NBTHelper.putBoolean(new CompoundTag(), "playing", playing);
	}

	public static JukeboxUpgradeClientData deserializeNBT(CompoundTag nbt) {
		return new JukeboxUpgradeClientData(nbt.getBoolean("playing"));
	}
}
