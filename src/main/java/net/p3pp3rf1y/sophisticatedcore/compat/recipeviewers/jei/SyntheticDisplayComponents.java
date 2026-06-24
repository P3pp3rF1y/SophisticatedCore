package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.util.List;
import java.util.function.Supplier;

final class SyntheticDisplayComponents {
	private static final List<Supplier<? extends DataComponentType<?>>> COMPONENTS = List.of(ModCoreDataComponents.MAIN_COLOR,
			ModCoreDataComponents.ACCENT_COLOR, ModCoreDataComponents.RENDER_INFO_TAG);

	private SyntheticDisplayComponents() {
	}

	static boolean hasAny(ItemStack stack) {
		return COMPONENTS.stream().anyMatch(component -> stack.has(component.get()));
	}
}
