package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class ContraptionHelper {
	public static MountedStorageManager getStorage(AbstractContraptionEntity contraptionEntity) {
		if (contraptionEntity.level().isClientSide() && contraptionEntity.getContraption() instanceof CarriageContraption carriageContraption && CONTRAPTION_STORAGE != null) {
			return getContraptionStorageFromField(carriageContraption);
		}
		return contraptionEntity.getContraption().getStorage();
	}

	private static MountedStorageManager getContraptionStorageFromField(CarriageContraption carriageContraption) {
		try {
			return (MountedStorageManager) CONTRAPTION_STORAGE.get(carriageContraption);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Unable to access field 'storage' in Contraption class", e);
		}
	}

	@Nullable
	private static final Field CONTRAPTION_STORAGE;

	static {
		Field storageField = null;
		try {
			storageField = Contraption.class.getDeclaredField("storage");
			storageField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			SophisticatedCore.LOGGER.error("Unable to find field 'storage' in Contraption class", e);
		}
		CONTRAPTION_STORAGE = storageField;
	}
}