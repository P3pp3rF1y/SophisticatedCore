package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterAttributes;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoidUpgradeWrapperTest {
	private static final FilterAttributes BLOCK_LIST_FILTER_ATTRIBUTES = new FilterAttributes(Collections.emptySet(), false, false, false, PrimaryMatch.ITEM,
			true, ItemContainerContents.EMPTY, false, false);

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		bindTestComponents(Items.DIAMOND, Items.WATER_BUCKET, Items.LAVA_BUCKET);
		bindTestComponents(Fluids.WATER, Fluids.LAVA);
	}

	private static void bindTestComponents(Item... items) {
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : items) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	private static void bindTestComponents(Fluid... fluids) {
		for (Fluid fluid : fluids) {
			fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		}
	}

	@Test
	void overflowMatchIncludesPartialStackWhenComponentsAreIgnored() {
		Object partiallyFilledStack = new Object();

		Assertions.assertTrue(VoidUpgradeWrapper.hasOverflowMatch(Set.of(), Set.of(partiallyFilledStack), stackKey -> stackKey == partiallyFilledStack));
	}

	@Test
	void tickSkipsQueuedSlotThatBecameEmptyBeforeTick() {
		InventoryHandler inventoryHandler = mock(InventoryHandler.class);
		when(inventoryHandler.getResource(0)).thenReturn(ItemResource.of(Items.DIAMOND), ItemResource.EMPTY);
		when(inventoryHandler.getAmountAsInt(0)).thenReturn(0);
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper(inventoryHandler);

		wrapper.onSlotChange(inventoryHandler, 0);
		wrapper.tick(null, null, BlockPos.ZERO);

		verify(inventoryHandler, never()).extract(anyInt(), any(ItemResource.class), anyInt(), any(TransactionContext.class));
	}

	@Test
	void onSlotChangeDoesNotQueueEmptySlot() {
		InventoryHandler inventoryHandler = mock(InventoryHandler.class);
		when(inventoryHandler.getResource(0)).thenReturn(ItemResource.EMPTY);
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper(storageWrapper);

		wrapper.onSlotChange(inventoryHandler, 0);
		wrapper.tick(null, null, BlockPos.ZERO);

		verify(storageWrapper, never()).getInventoryHandler();
	}

	@Test
	void shouldVoidFluidMatchesContainedFilterAndInvalidatesItsCache() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper();
		wrapper.getFilterLogic().setAllowList(true);
		wrapper.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));

		assertTrue(wrapper.shouldVoidFluid(FluidResource.of(Fluids.WATER), VoidType.ALWAYS));
		assertFalse(wrapper.shouldVoidFluid(FluidResource.of(Fluids.LAVA), VoidType.ALWAYS));

		wrapper.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));

		assertFalse(wrapper.shouldVoidFluid(FluidResource.of(Fluids.WATER), VoidType.ALWAYS));
		assertTrue(wrapper.shouldVoidFluid(FluidResource.of(Fluids.LAVA), VoidType.ALWAYS));
	}

	@Test
	void shouldVoidFluidHonorsDenyListForContainedFilter() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper();
		wrapper.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));
		wrapper.getFilterLogic().setAllowList(false);

		assertFalse(wrapper.shouldVoidFluid(FluidResource.of(Fluids.LAVA), VoidType.ALWAYS));
		assertTrue(wrapper.shouldVoidFluid(FluidResource.of(Fluids.WATER), VoidType.ALWAYS));
	}

	@Test
	void shouldVoidFluidSkipsEmptyContainedFilters() {
		assertTrue(getVoidUpgradeWrapper().shouldVoidFluid(FluidResource.of(Fluids.WATER), VoidType.ALWAYS));
	}

	@Test
	void setAllowByDefaultUpdatesEmptyFilterAttributes() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapperWithEmptyFilterAttributes();

		wrapper.getFilterLogic().setAllowByDefault(false);

		assertTrue(wrapper.getFilterLogic().matchesFilter(ItemResource.of(Items.DIAMOND)));
	}

	@Test
	void setAllowByDefaultDoesNotOverridePersistedFilterAttributes() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapperWithEmptyFilterAttributes();
		wrapper.getFilterLogic().setAllowList(true);

		wrapper.getFilterLogic().setAllowByDefault(false);

		assertFalse(wrapper.getFilterLogic().matchesFilter(ItemResource.of(Items.DIAMOND)));
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapper(InventoryHandler inventoryHandler) {
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		when(storageWrapper.getInventoryHandler()).thenReturn(inventoryHandler);

		return getVoidUpgradeWrapper(storageWrapper);
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapper(IStorageWrapper storageWrapper) {
		return getVoidUpgradeWrapper(storageWrapper, 0);
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapper() {
		return getVoidUpgradeWrapper(mock(IStorageWrapper.class), 1);
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapperWithEmptyFilterAttributes() {
		return getVoidUpgradeWrapper(mock(IStorageWrapper.class), 1, false);
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapper(IStorageWrapper storageWrapper, int filterSlotCount) {
		return getVoidUpgradeWrapper(storageWrapper, filterSlotCount, true);
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapper(IStorageWrapper storageWrapper, int filterSlotCount, boolean useBlockListFilterAttributesDefault) {
		VoidUpgradeItem upgradeItem = mock(VoidUpgradeItem.class);
		when(upgradeItem.getFilterSlotCount()).thenReturn(filterSlotCount);
		when(upgradeItem.isVoidAlwaysEnabled()).thenReturn(true);
		ItemStack upgrade = mock(ItemStack.class);
		Map<Object, Object> components = new HashMap<>();
		when(upgrade.getItem()).thenReturn(upgradeItem);
		when(upgrade.getOrDefault(anyDataComponentSupplier(), any())).thenAnswer(invocation -> {
			Supplier<?> component = invocation.getArgument(0);
			if (component == ModCoreDataComponents.SHOULD_WORK_IN_GUI) {
				return true;
			}
			if (component == ModCoreDataComponents.FILTER_ATTRIBUTES) {
				return components.getOrDefault(component, useBlockListFilterAttributesDefault ? BLOCK_LIST_FILTER_ATTRIBUTES : invocation.getArgument(1));
			}
			return components.getOrDefault(component, invocation.getArgument(1));
		});
		when(upgrade.has(anyDataComponentSupplier())).thenAnswer(invocation -> components.containsKey(invocation.getArgument(0)));
		doAnswer(invocation -> components.put(invocation.getArgument(0), invocation.getArgument(1))).when(upgrade).set(anySetDataComponentSupplier(), any());

		return new VoidUpgradeWrapper(storageWrapper, upgrade, stack -> {
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
}
