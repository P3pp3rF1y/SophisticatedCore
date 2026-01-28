package net.p3pp3rf1y.sophisticatedcore.compat.openpartiesandclaims;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import xaero.pac.common.server.api.OpenPACServerAPI;

public class OpenPACCompat implements ICompat {
	@Override
	public void setup() {
		WorldHelper.addAdditionalInteractionCheck((player, pos) -> {
			if (player.level() instanceof ServerLevel serverLevel) {
				return !OpenPACServerAPI.get(serverLevel.getServer())
						.getChunkProtection()
						.onBlockInteraction(player, InteractionHand.MAIN_HAND, null, serverLevel, pos, Direction.UP, false, false, false);
			}
			return true;
		});
	}
}
