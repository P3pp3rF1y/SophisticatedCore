package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoidUpgradeWrapperTest {

	@Test
	void overflowMatchIncludesPartialStackWhenComponentsAreIgnored() {
		ItemStack stackWithDifferentComponents = customizeName(new ItemStack(Items.DIAMOND, 1), "different component");

		Assertions.assertTrue(VoidUpgradeWrapper.hasOverflowMatch(Set.of(), Set.of(ItemStackKey.of(new ItemStack(Items.DIAMOND, 1))),
				stackKey -> stackKey.stack().getItem() == stackWithDifferentComponents.getItem()));
	}

	@Test
	void containedFluidFilterMatchesAndInvalidatesItsCache() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper();
		wrapper.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));

		assertTrue(wrapper.shouldVoidFluid(new FluidStack(Fluids.WATER, 1_000), VoidType.ALWAYS));
		assertFalse(wrapper.shouldVoidFluid(new FluidStack(Fluids.LAVA, 1_000), VoidType.ALWAYS));

		wrapper.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));

		assertFalse(wrapper.shouldVoidFluid(new FluidStack(Fluids.WATER, 1_000), VoidType.ALWAYS));
		assertTrue(wrapper.shouldVoidFluid(new FluidStack(Fluids.LAVA, 1_000), VoidType.ALWAYS));
	}

	@Test
	void denyListDoesNotVoidContainedFilteredFluid() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper();
		wrapper.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));
		wrapper.getFilterLogic().setAllowList(false);

		assertFalse(wrapper.shouldVoidFluid(new FluidStack(Fluids.LAVA, 1_000), VoidType.ALWAYS));
		assertTrue(wrapper.shouldVoidFluid(new FluidStack(Fluids.WATER, 1_000), VoidType.ALWAYS));
	}

	@Test
	void emptyContainedFilterDoesNotMatchFluid() {
		assertFalse(getVoidUpgradeWrapper().shouldVoidFluid(new FluidStack(Fluids.WATER, 1_000), VoidType.ALWAYS));
	}

	@Test
	void setAllowByDefaultUpdatesEmptyFilterAttributes() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper();

		wrapper.getFilterLogic().setAllowByDefault(false);

		assertTrue(wrapper.getFilterLogic().matchesFilter(new ItemStack(Items.DIAMOND)));
	}

	@Test
	void setAllowByDefaultDoesNotOverridePersistedFilterAttributes() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper();
		wrapper.getFilterLogic().setAllowList(true);

		wrapper.getFilterLogic().setAllowByDefault(false);

		assertFalse(wrapper.getFilterLogic().matchesFilter(new ItemStack(Items.DIAMOND)));
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapper() {
		VoidUpgradeItem upgradeItem = mock(VoidUpgradeItem.class);
		when(upgradeItem.getFilterSlotCount()).thenReturn(1);
		when(upgradeItem.isVoidAlwaysEnabled()).thenReturn(true);
		ItemStack upgrade = mock(ItemStack.class);
		Map<Object, Object> components = new HashMap<>();
		when(upgrade.getItem()).thenReturn(upgradeItem);
		when(upgrade.getOrDefault(anyDataComponentSupplier(), any()))
				.thenAnswer(invocation -> components.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
		doAnswer(invocation -> components.put(invocation.getArgument(0), invocation.getArgument(1))).when(upgrade).set(anySetDataComponentSupplier(), any());
		return new VoidUpgradeWrapper(mock(IStorageWrapper.class), upgrade, stack -> {
		});
	}

	@SuppressWarnings("unchecked")
	private static <T> Supplier<? extends DataComponentType<? extends T>> anyDataComponentSupplier() {
		return (Supplier<? extends DataComponentType<? extends T>>) any(Supplier.class);
	}

	@SuppressWarnings("unchecked")
	private static Supplier<? extends DataComponentType<Object>> anySetDataComponentSupplier() {
		return (Supplier<? extends DataComponentType<Object>>) any(Supplier.class);
	}

	private static ItemStack customizeName(ItemStack stack, String customName) {
		ItemStack result = stack.copy();
		result.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
		return result;
	}
}
