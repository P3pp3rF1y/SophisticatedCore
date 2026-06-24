package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class ItemStackHelper {
	private ItemStackHelper() {
	}

	public static boolean areItemStackComponentsEqualIgnoreDurability(boolean aIsEmpty, DataComponentMap componentsA, boolean bIsEmpty,
			DataComponentMap componentsB) {
		if (aIsEmpty && bIsEmpty) {
			return true;
		} else if (!aIsEmpty && !bIsEmpty) {
			if (componentsA.isEmpty() && !componentsB.isEmpty()) {
				return false;
			} else {
				return (componentsA.isEmpty() || areComponentsEqualIgnoreDurability(componentsA, componentsB));
			}
		} else {
			return false;
		}
	}

	public static boolean areComponentsEqualIgnoreDurability(DataComponentMap componentsA, @Nullable DataComponentMap componentsB) {
		if (componentsA == componentsB) {
			return true;
		}
		if (componentsB == null || componentsA.size() != componentsB.size()) {
			return false;
		}

		for (TypedDataComponent<?> typedDataComponent : componentsA) {
			if (!componentsB.has(typedDataComponent.type())) {
				return false;
			}
			if (typedDataComponent.type().equals(DataComponents.DAMAGE)) {
				continue;
			}
			if (!Objects.equals(typedDataComponent.value(), componentsB.get(typedDataComponent.type()))) {
				return false;
			}
		}
		return true;
	}
}
