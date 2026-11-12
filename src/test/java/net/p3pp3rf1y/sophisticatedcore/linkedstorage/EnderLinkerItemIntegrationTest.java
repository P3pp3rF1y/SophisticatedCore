package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static net.p3pp3rf1y.sophisticatedcore.HelperAssertions.assertStackEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderLinkerItemIntegrationTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		Items.BLAZE_ROD.builtInRegistryHolder().bindComponents(components);
		Items.BONE.builtInRegistryHolder().bindComponents(components);
	}

	private final ServerLevel level = Mockito.mock(ServerLevel.class);
	private final Player player = Mockito.mock(Player.class);
	private final Inventory inventory = Mockito.mock(Inventory.class);
	private final EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
	private final LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
	private final UUID groupId = UUID.randomUUID();
	private final ItemStack linker = new ItemStack(Items.BLAZE_ROD);
	private final ItemStack endpoint = new ItemStack(Items.BONE, 4);
	private final SlotAccess carriedAccess = Mockito.mock(SlotAccess.class);
	private boolean singleItemOnly;
	private ILinkedStorageEndpointAdapter.Compatibility compatibility = ILinkedStorageEndpointAdapter.Compatibility.COMPATIBLE;
	private final ILinkedStorageItemEndpointAdapter adapter = new ILinkedStorageItemEndpointAdapter() {
		private static final Identifier FACTORY_ID = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "test_endpoint_adapter");

		@Override
		public boolean supports(ItemStack stack) {
			return stack.is(Items.BONE) && (!singleItemOnly || stack.getCount() == 1);
		}

		@Override
		public Identifier factoryId() {
			return FACTORY_ID;
		}

		@Override
		public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ItemStack stack) {
			return new LinkedStorageHostDescriptor(FACTORY_ID, new CompoundTag());
		}

		@Override
		public ContainerContents copyCanonicalContents(ServerLevel level, ItemStack stack) {
			return new ContainerContents();
		}

		@Override
		public void bindEndpoint(ServerLevel level, ItemStack stack, LinkedStorageEndpointData endpoint) {
			stack.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, endpoint);
		}

		@Override
		public boolean acceptsHostFactory(Identifier hostFactoryId) {
			return factoryId().equals(hostFactoryId);
		}

		@Override
		public Compatibility getCompatibility(ServerLevel level, ItemStack stack, LinkedStorageHostDescriptor descriptor) {
			return compatibility;
		}
	};
	private MockedStatic<LinkedStorageGroupsSavedData> groups;
	private MockedStatic<LinkedStorageEndpointAdapters> adapters;

	@BeforeEach
	void setUp() {
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(player.getUUID()).thenReturn(UUID.randomUUID());
		Mockito.when(player.getInventory()).thenReturn(inventory);
		Mockito.when(inventory.add(Mockito.any(ItemStack.class))).thenReturn(true);
		endpoint.set(DataComponents.CUSTOM_NAME, Component.literal("Original storage"));
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(adapter.createHostDescriptor(level, endpoint)));
		ILinkedStorageVirtualHost host = Mockito.mock(ILinkedStorageVirtualHost.class);
		Mockito.when(host.getLinkedStorageDisplayName()).thenReturn(Optional.empty());
		Mockito.when(manager.resolveVirtualHost(groupId)).thenReturn(Optional.of(host));
		Mockito.when(manager.createGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(groupId);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		Mockito.when(savedData.manager()).thenReturn(manager);
		groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class);
		groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
		adapters = Mockito.mockStatic(LinkedStorageEndpointAdapters.class);
		adapters.when(() -> LinkedStorageEndpointAdapters.find(Mockito.any())).thenAnswer(invocation -> {
			ItemStack stack = invocation.getArgument(0);
			return adapter.supports(stack) ? Optional.of(adapter) : Optional.empty();
		});
	}

	@AfterEach
	void tearDown() {
		adapters.close();
		groups.close();
	}

	@ParameterizedTest
	@MethodSource("linkerStacks")
	void overrideStackedOnOtherLinksOnePrimaryWithBlankLinker(Params params) {
		singleItemOnly = params.singleItemOnly();
		linker.setCount(params.linkerCount());
		ItemStack originalEndpoint = endpoint.copy();
		Slot slot = new Slot(new SimpleContainer(endpoint), 0, 0, 0);

		boolean handled = linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player);

		assertTrue(handled);
		assertPrimaryLinked(originalEndpoint, params.linkerCount());
		assertStackEquals(endpoint, slot.getItem(), "Slot must contain the unlinked remainder");
	}

	@ParameterizedTest
	@MethodSource("linkerStacks")
	void overrideOtherStackedOnMeLinksOnePrimaryWithBlankLinker(Params params) {
		singleItemOnly = params.singleItemOnly();
		linker.setCount(params.linkerCount());
		ItemStack originalEndpoint = endpoint.copy();
		Slot slot = new Slot(new SimpleContainer(linker), 0, 0, 0);

		boolean handled = linkerItem.overrideOtherStackedOnMe(linker, endpoint, slot, ClickAction.SECONDARY, player, carriedAccess);

		assertTrue(handled);
		assertPrimaryLinked(originalEndpoint, params.linkerCount());
		Mockito.verify(carriedAccess).set(endpoint);
	}

	@ParameterizedTest
	@MethodSource("linkerStacks")
	void overrideStackedOnOtherLinksOneSecondaryWithBoundLinker(Params params) {
		singleItemOnly = params.singleItemOnly();
		linker.setCount(params.linkerCount());
		bindLinker();
		ItemStack originalEndpoint = endpoint.copy();
		Slot slot = new Slot(new SimpleContainer(endpoint), 0, 0, 0);

		boolean handled = linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player);

		assertTrue(handled);
		assertSecondaryLinked(originalEndpoint, params.linkerCount());
		assertStackEquals(endpoint, slot.getItem(), "Slot must contain the unlinked remainder");
	}

	@ParameterizedTest
	@MethodSource("linkerStacks")
	void overrideOtherStackedOnMeLinksOneSecondaryWithBoundLinker(Params params) {
		singleItemOnly = params.singleItemOnly();
		linker.setCount(params.linkerCount());
		bindLinker();
		ItemStack originalEndpoint = endpoint.copy();
		Slot slot = new Slot(new SimpleContainer(linker), 0, 0, 0);

		boolean handled = linkerItem.overrideOtherStackedOnMe(linker, endpoint, slot, ClickAction.SECONDARY, player, carriedAccess);

		assertTrue(handled);
		assertSecondaryLinked(originalEndpoint, params.linkerCount());
		Mockito.verify(carriedAccess).set(endpoint);
	}

	@Test
	void overrideOtherStackedOnMePreservesLinkedEndpointInConsumedLinkerSlot() {
		bindLinker();
		ItemStack originalEndpoint = endpoint.copy();
		Slot slot = new Slot(new SimpleContainer(linker), 0, 0, 0);
		// Inventory insertion can reuse the empty linker slot.
		Mockito.when(inventory.add(Mockito.any(ItemStack.class))).thenAnswer(invocation -> {
			slot.set(invocation.getArgument(0, ItemStack.class).copy());
			return true;
		});

		boolean handled = linkerItem.overrideOtherStackedOnMe(linker, endpoint, slot, ClickAction.SECONDARY, player, carriedAccess);

		assertTrue(handled);
		assertSecondaryLinked(originalEndpoint, 1);
		assertStackEquals(deliveredStacks().getFirst(), slot.getItem(), "Linked endpoint must survive insertion into the consumed linker's slot");
		InOrder deliveryOrder = Mockito.inOrder(carriedAccess, inventory);
		deliveryOrder.verify(carriedAccess).set(endpoint);
		deliveryOrder.verify(inventory).add(Mockito.any(ItemStack.class));
	}

	@ParameterizedTest
	@EnumSource(value = ILinkedStorageEndpointAdapter.Compatibility.class, names = {"INCOMPATIBLE", "HAS_CONTENTS"})
	void overrideStackedOnOtherPreservesStacksWhenLinkingIsRejected(ILinkedStorageEndpointAdapter.Compatibility rejection) {
		bindLinker();
		linker.setCount(3);
		compatibility = rejection;
		ItemStack originalEndpoint = endpoint.copy();
		ItemStack originalLinker = linker.copy();
		Slot slot = new Slot(new SimpleContainer(endpoint), 0, 0, 0);

		boolean handled = linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player);

		assertTrue(handled);
		assertRejectedLink(originalEndpoint, originalLinker);
	}

	@ParameterizedTest
	@EnumSource(value = ILinkedStorageEndpointAdapter.Compatibility.class, names = {"INCOMPATIBLE", "HAS_CONTENTS"})
	void overrideOtherStackedOnMePreservesStacksWhenLinkingIsRejected(ILinkedStorageEndpointAdapter.Compatibility rejection) {
		bindLinker();
		linker.setCount(3);
		compatibility = rejection;
		ItemStack originalEndpoint = endpoint.copy();
		ItemStack originalLinker = linker.copy();
		Slot slot = new Slot(new SimpleContainer(linker), 0, 0, 0);

		boolean handled = linkerItem.overrideOtherStackedOnMe(linker, endpoint, slot, ClickAction.SECONDARY, player, carriedAccess);

		assertTrue(handled);
		assertRejectedLink(originalEndpoint, originalLinker);
	}

	@Test
	void overrideStackedOnOtherDropsOneLinkedEndpointWhenInventoryIsFull() {
		bindLinker();
		Mockito.when(inventory.add(Mockito.any(ItemStack.class))).thenReturn(false);
		ItemStack originalEndpoint = endpoint.copy();
		Slot slot = new Slot(new SimpleContainer(endpoint), 0, 0, 0);

		boolean handled = linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player);

		assertTrue(handled);
		assertSecondaryLinked(originalEndpoint, 1);
		ItemStack droppedEndpoint = deliveredStacks().getFirst();
		Mockito.verify(player).drop(droppedEndpoint, false);
	}

	@Test
	void overrideOtherStackedOnMeDropsOneLinkedEndpointWhenInventoryIsFull() {
		bindLinker();
		Mockito.when(inventory.add(Mockito.any(ItemStack.class))).thenReturn(false);
		ItemStack originalEndpoint = endpoint.copy();
		Slot slot = new Slot(new SimpleContainer(linker), 0, 0, 0);

		boolean handled = linkerItem.overrideOtherStackedOnMe(linker, endpoint, slot, ClickAction.SECONDARY, player, carriedAccess);

		assertTrue(handled);
		assertSecondaryLinked(originalEndpoint, 1);
		ItemStack droppedEndpoint = deliveredStacks().getFirst();
		Mockito.verify(player).drop(droppedEndpoint, false);
	}

	@Test
	void overrideStackedOnOtherRecognizesSingleItemAdapterOnClientWithoutMutatingStacks() {
		singleItemOnly = true;
		Mockito.when(player.level()).thenReturn(Mockito.mock(Level.class));
		ItemStack originalEndpoint = endpoint.copy();
		ItemStack originalLinker = linker.copy();
		Slot slot = new Slot(new SimpleContainer(endpoint), 0, 0, 0);

		boolean handled = linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player);

		assertTrue(handled);
		assertStackEquals(originalEndpoint, endpoint, "Client-side linking must preserve the endpoint stack");
		assertStackEquals(originalLinker, linker, "Client-side linking must preserve the linker stack");
		Mockito.verifyNoInteractions(manager, inventory, carriedAccess);
	}

	@Test
	void overrideOtherStackedOnMeRecognizesSingleItemAdapterOnClientWithoutMutatingStacks() {
		singleItemOnly = true;
		Mockito.when(player.level()).thenReturn(Mockito.mock(Level.class));
		ItemStack originalEndpoint = endpoint.copy();
		ItemStack originalLinker = linker.copy();
		Slot slot = new Slot(new SimpleContainer(linker), 0, 0, 0);

		boolean handled = linkerItem.overrideOtherStackedOnMe(linker, endpoint, slot, ClickAction.SECONDARY, player, carriedAccess);

		assertTrue(handled);
		assertStackEquals(originalEndpoint, endpoint, "Client-side linking must preserve the endpoint stack");
		assertStackEquals(originalLinker, linker, "Client-side linking must preserve the linker stack");
		Mockito.verifyNoInteractions(manager, inventory, carriedAccess);
	}

	@Test
	void overrideStackedOnOtherRespectsSlotPickupRestrictions() {
		Slot slot = Mockito.mock(Slot.class);
		Mockito.when(slot.getItem()).thenReturn(endpoint);
		Mockito.when(slot.mayPickup(player)).thenReturn(false);
		ItemStack originalEndpoint = endpoint.copy();
		ItemStack originalLinker = linker.copy();

		boolean handled = linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player);

		assertFalse(handled);
		assertStackEquals(originalEndpoint, endpoint, "Pickup restrictions must preserve the endpoint stack");
		assertStackEquals(originalLinker, linker, "Pickup restrictions must preserve the linker stack");
		Mockito.verifyNoInteractions(manager, inventory, carriedAccess);
	}

	@Test
	void overrideOtherStackedOnMeRespectsSlotPickupRestrictions() {
		Slot slot = Mockito.mock(Slot.class);
		Mockito.when(slot.getItem()).thenReturn(linker);
		Mockito.when(slot.mayPickup(player)).thenReturn(false);
		ItemStack originalEndpoint = endpoint.copy();
		ItemStack originalLinker = linker.copy();

		boolean handled = linkerItem.overrideOtherStackedOnMe(linker, endpoint, slot, ClickAction.SECONDARY, player, carriedAccess);

		assertFalse(handled);
		assertStackEquals(originalEndpoint, endpoint, "Pickup restrictions must preserve the endpoint stack");
		assertStackEquals(originalLinker, linker, "Pickup restrictions must preserve the linker stack");
		Mockito.verifyNoInteractions(manager, inventory, carriedAccess);
	}

	private void assertPrimaryLinked(ItemStack originalEndpoint, int linkerCount) {
		ItemStack linked = assertSingleEndpointLinked(originalEndpoint);
		assertEquals(linkerCount == 1 ? 1 : linkerCount - 1, linker.getCount());
		ItemStack boundLinker = linkerCount == 1 ? linker : deliveredStacks().stream().filter(stack -> stack.is(Items.BLAZE_ROD)).findFirst().orElseThrow();
		assertEquals(1, boundLinker.getCount());
		assertEquals(groupId, boundLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
		assertEquals(linkerCount == 1, linker.has(ModCoreDataComponents.ENDER_LINKER_TARGET));
		Mockito.verify(manager).createGroup(Mockito.eq(player.getUUID()), Mockito.eq(linked.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).endpointId()),
				Mockito.any(), Mockito.any());
		Mockito.verify(manager, Mockito.never()).registerEndpoint(Mockito.any(), Mockito.any());
	}

	private void assertSecondaryLinked(ItemStack originalEndpoint, int linkerCount) {
		ItemStack linked = assertSingleEndpointLinked(originalEndpoint);
		assertEquals(linkerCount - 1, linker.getCount());
		Mockito.verify(manager).registerEndpoint(groupId, linked.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).endpointId());
		Mockito.verify(manager, Mockito.never()).createGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
	}

	private ItemStack assertSingleEndpointLinked(ItemStack originalEndpoint) {
		assertStackEquals(originalEndpoint.copyWithCount(originalEndpoint.getCount() - 1), endpoint, "Unlinked remainder must retain its components");
		List<ItemStack> linkedEndpoints = deliveredStacks().stream().filter(stack -> stack.is(Items.BONE)).toList();
		assertEquals(1, linkedEndpoints.size());
		ItemStack linked = linkedEndpoints.getFirst();
		assertEquals(1, linked.getCount());
		assertEquals(originalEndpoint.getHoverName(), linked.getHoverName());
		LinkedStorageEndpointData endpointData = linked.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		assertNotNull(endpointData);
		assertEquals(groupId, endpointData.groupId());
		return linked;
	}

	private void assertRejectedLink(ItemStack originalEndpoint, ItemStack originalLinker) {
		assertStackEquals(originalEndpoint, endpoint, "Rejected linking must preserve the endpoint stack");
		assertStackEquals(originalLinker, linker, "Rejected linking must preserve the linker stack");
		Mockito.verify(manager, Mockito.never()).registerEndpoint(Mockito.any(), Mockito.any());
		Mockito.verify(manager, Mockito.never()).createGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verifyNoInteractions(inventory, carriedAccess);
		Mockito.verify(player, Mockito.never()).drop(Mockito.any(ItemStack.class), Mockito.anyBoolean());
	}

	private List<ItemStack> deliveredStacks() {
		ArgumentCaptor<ItemStack> stacks = ArgumentCaptor.forClass(ItemStack.class);
		Mockito.verify(inventory, Mockito.atLeastOnce()).add(stacks.capture());
		return stacks.getAllValues();
	}

	private void bindLinker() {
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(groupId, Component.empty()));
	}

	private static Stream<Params> linkerStacks() {
		return Stream.of(new Params(false, 1), new Params(false, 3), new Params(true, 1), new Params(true, 3));
	}

	private record Params(boolean singleItemOnly, int linkerCount) {
	}
}
