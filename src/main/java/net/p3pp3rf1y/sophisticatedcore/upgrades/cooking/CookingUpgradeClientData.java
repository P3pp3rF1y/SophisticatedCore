package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeClientDataType;

public record CookingUpgradeClientData(boolean burning) implements IUpgradeClientData {
	public static final UpgradeClientDataType<CookingUpgradeClientData> TYPE =
			new UpgradeClientDataType<>("smelting", CookingUpgradeClientData.class,
					RecordCodecBuilder.create(inst -> inst.group(
							Codec.BOOL.fieldOf("burning").forGetter(CookingUpgradeClientData::burning)
					).apply(inst, CookingUpgradeClientData::new)),
					StreamCodec.composite(ByteBufCodecs.BOOL, CookingUpgradeClientData::burning, CookingUpgradeClientData::new)
			);

	@Override
	public IUpgradeClientData copy() {
		return new CookingUpgradeClientData(burning);
	}
}
