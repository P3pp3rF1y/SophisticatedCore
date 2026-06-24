package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeClientDataType;

public record JukeboxUpgradeClientData(boolean playing) implements IUpgradeClientData {
	public static final UpgradeClientDataType<JukeboxUpgradeClientData> TYPE = new UpgradeClientDataType<>("jukebox", JukeboxUpgradeClientData.class,
			RecordCodecBuilder.create(
					inst -> inst.group(Codec.BOOL.fieldOf("playing").forGetter(JukeboxUpgradeClientData::playing)).apply(inst, JukeboxUpgradeClientData::new)),
			StreamCodec.composite(ByteBufCodecs.BOOL, JukeboxUpgradeClientData::playing, JukeboxUpgradeClientData::new));

	@Override
	public IUpgradeClientData copy() {
		return new JukeboxUpgradeClientData(playing);
	}
}
