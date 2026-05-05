package net.p3pp3rf1y.sophisticatedcore.mixin.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.world.entity.Entity;
import net.p3pp3rf1y.sophisticatedcore.compat.create.MountedStorageBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContraptionEntity.class)
public class MixinAbstractContraptionEntity {
	@Shadow
	protected Contraption contraption;

	@Inject(method = "remove", at = @At("HEAD"))
	private void cleanupMountedStoragesOnDestroy(Entity.RemovalReason removalReason, CallbackInfo ci) {
		AbstractContraptionEntity contraptionEntity = (AbstractContraptionEntity) (Object) this;
		if (contraptionEntity.level().isClientSide() || contraption == null || contraption.disassembled || removalReason != Entity.RemovalReason.KILLED) {
			return;
		}

		contraption.getStorage().getAllItemStorages().values().forEach(storage -> {
			if (storage instanceof MountedStorageBase mountedStorage) {
				mountedStorage.onContraptionDestroyed();
			}
		});
	}
}
