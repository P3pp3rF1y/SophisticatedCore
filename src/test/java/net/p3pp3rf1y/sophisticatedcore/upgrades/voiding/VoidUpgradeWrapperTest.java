package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class VoidUpgradeWrapperTest {

	@Test
	void overflowMatchIncludesPartialStackWhenComponentsAreIgnored() {
		ItemStack stackWithDifferentComponents = customizeName(new ItemStack(Items.DIAMOND, 1), "different component");

		Assertions.assertTrue(VoidUpgradeWrapper.hasOverflowMatch(Set.of(), Set.of(ItemStackKey.of(new ItemStack(Items.DIAMOND, 1))),
				stackKey -> stackKey.getStack().getItem() == stackWithDifferentComponents.getItem()));
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

	@Test
	void setFromLegacyComponentMapsTrueToAlways() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper(true, true);

		assertEquals(VoidType.ALWAYS, wrapper.getVoidType());
	}

	@Test
	void setFromLegacyComponentMapsFalseToSlotOverflow() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper(true, false);

		assertEquals(VoidType.SLOT_OVERFLOW, wrapper.getVoidType());
	}

	@Test
	void setFromLegacyComponentOnlyMigratesOnce() {
		VoidUpgradeFixture fixture = createVoidUpgradeFixture(true, true);

		assertEquals(VoidType.ALWAYS, fixture.wrapper().getVoidType());
		assertEquals(VoidType.ALWAYS, fixture.upgrade().get(ModCoreDataComponents.VOID_TYPE));
		assertFalse(fixture.upgrade().has(ModCoreDataComponents.LEGACY_SHOULD_VOID_OVERFLOW));
		assertEquals(1, fixture.saveCount().get());

		VoidUpgradeWrapper secondWrapper = new VoidUpgradeWrapper(mock(IStorageWrapper.class), fixture.upgrade(),
				stack -> fixture.saveCount().incrementAndGet());

		assertEquals(VoidType.ALWAYS, secondWrapper.getVoidType());
		assertEquals(1, fixture.saveCount().get());
	}

	@Test
	void setFromLegacyComponentPreservesExistingVoidTypeAndDropsLegacyComponent() {
		VoidUpgradeFixture fixture = createVoidUpgradeFixture(true, true, VoidType.STORAGE_OVERFLOW);

		assertEquals(VoidType.STORAGE_OVERFLOW, fixture.wrapper().getVoidType());
		assertEquals(VoidType.STORAGE_OVERFLOW, fixture.upgrade().get(ModCoreDataComponents.VOID_TYPE));
		assertFalse(fixture.upgrade().has(ModCoreDataComponents.LEGACY_SHOULD_VOID_OVERFLOW));
		assertEquals(1, fixture.saveCount().get());

		new VoidUpgradeWrapper(mock(IStorageWrapper.class), fixture.upgrade(), stack -> fixture.saveCount().incrementAndGet());
		assertEquals(1, fixture.saveCount().get());
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapper() {
		return getVoidUpgradeWrapper(false, false);
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapper(boolean hasLegacyShouldVoidOverflow, boolean legacyShouldVoidOverflow) {
		return createVoidUpgradeFixture(hasLegacyShouldVoidOverflow, legacyShouldVoidOverflow).wrapper();
	}

	private static VoidUpgradeFixture createVoidUpgradeFixture(boolean hasLegacyShouldVoidOverflow, boolean legacyShouldVoidOverflow) {
		return createVoidUpgradeFixture(hasLegacyShouldVoidOverflow, legacyShouldVoidOverflow, null);
	}

	private static VoidUpgradeFixture createVoidUpgradeFixture(boolean hasLegacyShouldVoidOverflow, boolean legacyShouldVoidOverflow, VoidType voidType) {
		VoidUpgradeItem upgradeItem = mock(VoidUpgradeItem.class);
		when(upgradeItem.getFilterSlotCount()).thenReturn(1);
		when(upgradeItem.isVoidAlwaysEnabled()).thenReturn(true);
		ItemStack upgrade = spy(new ItemStack(Items.STICK));
		doReturn(upgradeItem).when(upgrade).getItem();
		if (voidType != null) {
			upgrade.set(ModCoreDataComponents.VOID_TYPE, voidType);
		}
		if (hasLegacyShouldVoidOverflow) {
			upgrade.set(ModCoreDataComponents.LEGACY_SHOULD_VOID_OVERFLOW, legacyShouldVoidOverflow);
		}
		AtomicInteger saveCount = new AtomicInteger();
		VoidUpgradeWrapper wrapper = new VoidUpgradeWrapper(mock(IStorageWrapper.class), upgrade, stack -> saveCount.incrementAndGet());
		return new VoidUpgradeFixture(wrapper, upgrade, saveCount);
	}

	private record VoidUpgradeFixture(VoidUpgradeWrapper wrapper, ItemStack upgrade, AtomicInteger saveCount) {
	}

	private static ItemStack customizeName(ItemStack stack, String customName) {
		ItemStack result = stack.copy();
		result.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
		return result;
	}
}
