package net.p3pp3rf1y.sophisticatedcore.mixin;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.p3pp3rf1y.sophisticatedcore.util.CoreFakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {
	@Inject(method = "syncData", at = @At("HEAD"), cancellable = true)
	private void skipSyncForCoreFakePlayer(AttachmentType<?> type, CallbackInfo ci) {
		if ((Object) this instanceof CoreFakePlayer) {
			ci.cancel();
		}
	}
}
