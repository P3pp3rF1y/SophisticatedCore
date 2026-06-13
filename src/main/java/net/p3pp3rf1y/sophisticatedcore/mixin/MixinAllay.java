package net.p3pp3rf1y.sophisticatedcore.mixin;

import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.gameevent.GameEvent;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Allay.class)
public class MixinAllay {
	@Inject(method = "aiStep", at = @At("TAIL"))
	private void sophisticatedcore$startDancingNearStorageJukebox(CallbackInfo ci) {
		Allay allay = (Allay) (Object) this;
		if (!allay.level().isClientSide && !allay.isDancing() && sophisticatedcore$isStorageJukeboxPlayingNear(allay)) {
			allay.setDancing(true);
		}
	}

	@Inject(method = "shouldStopDancing", at = @At("HEAD"), cancellable = true)
	private void sophisticatedcore$keepDancingNearStorageJukebox(CallbackInfoReturnable<Boolean> cir) {
		Allay allay = (Allay) (Object) this;
		if (!allay.level().isClientSide && sophisticatedcore$isStorageJukeboxPlayingNear(allay)) {
			cir.setReturnValue(false);
		}
	}

	@Unique
	private boolean sophisticatedcore$isStorageJukeboxPlayingNear(Allay allay) {
		return ServerStorageSoundHandler.isStorageSoundPlayingNear(allay.level(), allay.position(), GameEvent.JUKEBOX_PLAY.getNotificationRadius());
	}
}
