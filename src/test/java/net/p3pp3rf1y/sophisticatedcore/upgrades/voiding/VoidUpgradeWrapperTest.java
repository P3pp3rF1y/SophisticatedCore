package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class VoidUpgradeWrapperTest {

	@Test
	void overflowMatchIncludesPartialStackWhenComponentsAreIgnored() {
		ItemStack stackWithDifferentComponents = customizeName(new ItemStack(Items.DIAMOND, 1), "different component");

		Assertions.assertTrue(VoidUpgradeWrapper.hasOverflowMatch(
				Set.of(),
				Set.of(ItemStackKey.of(new ItemStack(Items.DIAMOND, 1))),
				stackKey -> stackKey.getStack().getItem() == stackWithDifferentComponents.getItem()
		));
	}

	private static ItemStack customizeName(ItemStack stack, String customName) {
		ItemStack result = stack.copy();
		result.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
		return result;
	}
}
