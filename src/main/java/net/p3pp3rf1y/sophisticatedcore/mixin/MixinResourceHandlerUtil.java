package net.p3pp3rf1y.sophisticatedcore.mixin;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInsertBlockOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourceHandlerUtil.class)
public class MixinResourceHandlerUtil {
	@Inject(method = "isFull", at = @At("HEAD"), cancellable = true)
	private static <T extends Resource> void isSophisticatedInventoryInsertBlocked(ResourceHandler<T> handler, CallbackInfoReturnable<Boolean> cir) {
		if (handler instanceof IInsertBlockOverride insertBlockOverride) {
			cir.setReturnValue(insertBlockOverride.isInsertBlocked());
		}
	}
}
