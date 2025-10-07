package net.p3pp3rf1y.sophisticatedcore.compat.ftbchunks;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.Protection;
import net.minecraft.world.InteractionHand;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

public class FTBChunksCompat implements ICompat {
	@Override
	public void setup() {
		WorldHelper.addAdditionalInteractionCheck((player, pos) ->
				!FTBChunksAPI.api().getManager().shouldPreventInteraction(player, InteractionHand.MAIN_HAND, pos, Protection.INTERACT_BLOCK, null)
		);
	}
}
