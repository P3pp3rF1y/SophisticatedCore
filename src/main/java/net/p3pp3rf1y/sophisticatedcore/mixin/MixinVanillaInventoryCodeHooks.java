package net.p3pp3rf1y.sophisticatedcore.mixin;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.VanillaInventoryCodeHooks;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInsertBlockOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VanillaInventoryCodeHooks.class)
public class MixinVanillaInventoryCodeHooks {
	@Inject(method = "isFull", at = @At("HEAD"), cancellable = true)
	private static void isSophisticatedInventoryInsertBlocked(IItemHandler handler, CallbackInfoReturnable<Boolean> cir) {
		if (handler instanceof IInsertBlockOverride insertBlockOverride) {
			cir.setReturnValue(insertBlockOverride.isInsertBlocked());
		}
	}
}
