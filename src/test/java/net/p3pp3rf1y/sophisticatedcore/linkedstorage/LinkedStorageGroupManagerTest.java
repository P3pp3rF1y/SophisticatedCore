package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedStorageGroupManagerTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Items.STICK.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
	}

	@Test
	void getMigratesDoubledNamespacedSavedDataThroughSavedDataStorage(@TempDir Path dataPath) throws IOException {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "legacy_saved_data_test_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		UUID groupId = UUID.randomUUID();
		Path legacyDataFile = dataPath.resolve("sophisticatedcore/sophisticatedcore_linked_storage_groups.dat");
		Files.createDirectories(legacyDataFile.getParent());
		NbtIo.writeCompressed(legacySavedDataFile(groupId, factoryId), legacyDataFile);
		SavedDataStorage storage = new SavedDataStorage(null, dataPath, Mockito.mock(com.mojang.datafixers.DataFixer.class), RegistryAccess.EMPTY);

		LinkedStorageGroupsSavedData migrated = LinkedStorageGroupsSavedData.get(levelFor(storage, dataPath));

		assertTrue(migrated.manager().resolveContents(groupId).isPresent());
		assertTrue(migrated.isDirty());
		storage.saveAndJoin();
		assertFalse(migrated.isDirty());
		assertTrue(Files.exists(dataPath.resolve("sophisticatedcore/linked_storage_groups.dat")));
	}

	@Test
	void getMigratesLegacyRawSavedDataThroughSavedDataStorage(@TempDir Path dataPath) throws IOException {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "legacy_raw_saved_data_test_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		UUID groupId = UUID.randomUUID();
		NbtIo.writeCompressed(legacySavedDataFile(groupId, factoryId), dataPath.resolve("sophisticatedcore_linked_storage_groups.dat"));
		SavedDataStorage storage = new SavedDataStorage(null, dataPath, Mockito.mock(com.mojang.datafixers.DataFixer.class), RegistryAccess.EMPTY);

		LinkedStorageGroupsSavedData migrated = LinkedStorageGroupsSavedData.get(levelFor(storage, dataPath));

		assertTrue(migrated.manager().resolveContents(groupId).isPresent());
		assertTrue(migrated.isDirty());
		storage.saveAndJoin();
		assertFalse(migrated.isDirty());
		assertTrue(Files.exists(dataPath.resolve("sophisticatedcore/linked_storage_groups.dat")));
	}

	@Test
	void getPrefersCurrentSavedDataOverLegacyFiles(@TempDir Path dataPath) throws IOException {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "legacy_precedence_test_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		UUID currentGroupId = UUID.randomUUID();
		Path currentDataFile = dataPath.resolve("sophisticatedcore/linked_storage_groups.dat");
		Files.createDirectories(currentDataFile.getParent());
		NbtIo.writeCompressed(legacySavedDataFile(currentGroupId, factoryId), currentDataFile);
		UUID doubledNamespacedGroupId = UUID.randomUUID();
		NbtIo.writeCompressed(legacySavedDataFile(doubledNamespacedGroupId, factoryId),
				dataPath.resolve("sophisticatedcore/sophisticatedcore_linked_storage_groups.dat"));
		UUID rawGroupId = UUID.randomUUID();
		NbtIo.writeCompressed(legacySavedDataFile(rawGroupId, factoryId), dataPath.resolve("sophisticatedcore_linked_storage_groups.dat"));
		SavedDataStorage storage = new SavedDataStorage(null, dataPath, Mockito.mock(com.mojang.datafixers.DataFixer.class), RegistryAccess.EMPTY);
		LinkedStorageGroupsSavedData savedData = LinkedStorageGroupsSavedData.get(levelFor(storage, dataPath));

		assertTrue(savedData.manager().resolveContents(currentGroupId).isPresent());
		assertTrue(savedData.manager().resolveContents(doubledNamespacedGroupId).isEmpty());
		assertTrue(savedData.manager().resolveContents(rawGroupId).isEmpty());
	}

	private static ServerLevel levelFor(SavedDataStorage storage, Path dataPath) {
		MinecraftServer server = Mockito.mock(MinecraftServer.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Mockito.when(level.getServer()).thenReturn(server);
		Mockito.when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
		Mockito.when(level.getDataStorage()).thenReturn(storage);
		Mockito.when(server.getWorldPath(LevelResource.DATA)).thenReturn(dataPath);
		Mockito.when(server.registryAccess()).thenReturn(RegistryAccess.EMPTY);
		return level;
	}

	private static CompoundTag legacySavedDataFile(UUID groupId, Identifier factoryId) {
		UUID endpointId = UUID.randomUUID();
		CompoundTag endpoint = new CompoundTag();
		endpoint.store("id", UUIDUtil.CODEC, endpointId);
		endpoint.putLong("last_opened_at", 0);
		ListTag endpoints = new ListTag();
		endpoints.add(endpoint);
		CompoundTag group = new CompoundTag();
		group.store("id", UUIDUtil.CODEC, groupId);
		group.putLong("revision", 0);
		group.putLong("render_revision", 0);
		group.putInt("columns_taken", 0);
		group.store("owner_id", UUIDUtil.CODEC, UUID.randomUUID());
		group.store("primary_endpoint_id", UUIDUtil.CODEC, endpointId);
		group.put("endpoints", endpoints);
		group.putString("factory_id", factoryId.toString());
		group.put("virtual_carrier", new CompoundTag());
		group.put("contents", new CompoundTag());
		ListTag groups = new ListTag();
		groups.add(group);
		CompoundTag data = new CompoundTag();
		data.put("groups", groups);
		data.put("active_pending_claims", new ListTag());
		CompoundTag file = new CompoundTag();
		file.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
		file.put("data", data);
		return file;
	}

	@Test
	void resolveVirtualHostResolvesEndpointKeysToCachedVirtualHost() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "endpoint_key_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();
		UUID primaryEndpointId = UUID.randomUUID();
		UUID secondaryEndpointId = UUID.randomUUID();
		UUID groupId = manager.createGroup(UUID.randomUUID(), primaryEndpointId, new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new ContainerContents());
		manager.registerEndpoint(groupId, secondaryEndpointId);

		ILinkedStorageVirtualHost host = manager.resolveVirtualHost(groupId).orElseThrow();
		assertSame(host, manager.resolveVirtualHost(new LinkedStorageEndpointData(groupId, primaryEndpointId), true).orElseThrow());
		assertSame(host, manager.resolveVirtualHost(new LinkedStorageEndpointData(groupId, secondaryEndpointId), false).orElseThrow());
		assertTrue(manager.resolveVirtualHost(new LinkedStorageEndpointData(groupId, secondaryEndpointId), true).isEmpty());
	}

	@Test
	void setContentsRefreshesVirtualHostOnlyAfterFullRootReplacement() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "replacement_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new ContainerContents());
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		ILinkedStorageContents contents = manager.resolveContents(groupId).orElseThrow();

		contents.contents().inventory().stacks().add(new ItemStack(Items.STICK));
		contents.markDirty();
		contents.setContents(groupId, new ContainerContents());

		assertEquals(1, host.refreshes());
	}

	@Test
	void subscribeToRootContentsReplacementsIgnoresOrdinaryContentsMutationsAndSupportsUnsubscribe() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "root_listener_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new ContainerContents());
		AtomicInteger notifications = new AtomicInteger();
		Runnable unsubscribe = manager.subscribeToRootContentsReplacements(groupId, notifications::incrementAndGet);
		ILinkedStorageContents contents = manager.resolveContents(groupId).orElseThrow();

		contents.contents().inventory().stacks().add(new ItemStack(Items.STICK));
		contents.markDirty();
		assertEquals(0, notifications.get());

		contents.setContents(groupId, new ContainerContents());
		assertEquals(1, notifications.get());

		unsubscribe.run();
		contents.setContents(groupId, new ContainerContents());
		assertEquals(1, notifications.get());
	}

	@Test
	void markRenderDirtyUpdatesPersistedRenderRevision() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "render_revision_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new ContainerContents());
		ILinkedStorageContents contents = manager.resolveContents(groupId).orElseThrow();

		contents.markRenderDirty();

		assertEquals(1, manager.getRevision(groupId));
		assertEquals(1, manager.getRenderRevision(groupId));
		LinkedStorageGroupsSavedData loaded = LinkedStorageGroupsSavedData.load(savedData.save());
		assertEquals(1, loaded.manager().getRenderRevision(groupId));
	}

	@Test
	void setColumnsTakenPersistsCanonicalColumnsAndRefreshesVirtualHostBeforeFanOut() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "layout_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new ContainerContents());
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		AtomicInteger notifications = new AtomicInteger();

		manager.subscribeToGroupChanges(groupId, () -> {
			assertEquals(2, host.contents().getColumnsTaken());
			assertEquals(1, host.layoutRefreshes());
			notifications.incrementAndGet();
		});
		manager.resolveContents(groupId).orElseThrow().setColumnsTaken(2);

		assertEquals(2, manager.resolveContents(groupId).orElseThrow().getColumnsTaken());
		assertEquals(1, manager.getRevision(groupId));
		assertEquals(1, notifications.get());
		assertEquals(2, LinkedStorageGroupsSavedData.load(savedData.save()).manager().resolveContents(groupId).orElseThrow().getColumnsTaken());
	}

	@Test
	void updatePrimaryHostDescriptorUpdatesOnlyPrimaryVirtualCarrier() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "primary_carrier_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();
		UUID primaryEndpointId = UUID.randomUUID();
		UUID groupId = manager.createGroup(UUID.randomUUID(), primaryEndpointId, new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new ContainerContents());
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		CompoundTag updatedCarrier = new CompoundTag();
		updatedCarrier.putString("name", "Primary Backpack");

		assertFalse(manager.updatePrimaryHostDescriptor(groupId, UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, updatedCarrier)));
		assertTrue(manager.updatePrimaryHostDescriptor(groupId, primaryEndpointId, new LinkedStorageHostDescriptor(factoryId, updatedCarrier)));

		assertEquals("Primary Backpack", host.virtualCarrier.getStringOr("name", ""));
		assertEquals("Primary Backpack", manager.getHostDescriptor(groupId).orElseThrow().virtualCarrier().getStringOr("name", ""));
	}

	@Test
	void resolveContentsAndResolveVirtualHostReturnEmptyForUnknownGroups() {
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();

		assertTrue(manager.resolveContents(UUID.randomUUID()).isEmpty());
		assertTrue(manager.resolveVirtualHost(UUID.randomUUID()).isEmpty());
	}

	@Test
	void clearRetainsUnrelatedModelData() {
		ItemStack stack = new ItemStack(Items.STICK);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(42)));
		stack.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));

		LinkedStorageStackLifecycle.clear(stack);

		assertEquals(42, stack.get(DataComponents.CUSTOM_MODEL_DATA).colors().getFirst());
	}

	@Test
	void saveAndLoadPersistCanonicalRootAndRevision() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "persisted_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		CompoundTag virtualCarrier = new CompoundTag();
		virtualCarrier.putString("render", "current");
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, virtualCarrier),
				new ContainerContents());
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		host.virtualCarrier.putString("render", "refreshed");
		ILinkedStorageContents contents = manager.resolveContents(groupId).orElseThrow();
		contents.setContents(groupId, new ContainerContents());
		contents.contents().inventory().stacks().add(new ItemStack(Items.STICK));
		contents.markDirty();
		contents.markRenderDirty();

		LinkedStorageGroupsSavedData loaded = LinkedStorageGroupsSavedData.load(savedData.save());
		assertEquals(3, loaded.manager().getRevision(groupId));
		assertEquals(1, loaded.manager().resolveContents(groupId).orElseThrow().contents().inventory().stacks().size());
		assertEquals("refreshed", loaded.manager().getHostDescriptor(groupId).orElseThrow().virtualCarrier().getStringOr("render", ""));
	}

	@Test
	void codecPreservesLinkedStorageSavedDataFieldNames() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "codec_field_names_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		savedData.manager().createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new ContainerContents());

		CompoundTag tag = (CompoundTag) LinkedStorageGroupsSavedData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, savedData).getOrThrow();
		CompoundTag group = tag.getListOrEmpty("groups").getCompound(0).orElseThrow();

		assertTrue(group.contains("id"));
		assertTrue(group.contains("factory_id"));
		assertTrue(group.contains("virtual_carrier"));
		assertTrue(group.contains("contents"));
	}

	@Test
	void registerEndpointAndUnregisterEndpointPersistMembershipLedger() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "membership_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID primaryEndpointId = UUID.randomUUID();
		UUID secondaryEndpointId = UUID.randomUUID();
		UUID groupId = manager.createGroup(UUID.randomUUID(), primaryEndpointId, new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new ContainerContents());

		AtomicInteger notifications = new AtomicInteger();
		manager.subscribeToGroupChanges(groupId, notifications::incrementAndGet);
		manager.registerEndpoint(groupId, secondaryEndpointId);
		assertEquals(1, manager.getRevision(groupId));
		assertEquals(1, notifications.get());
		assertTrue(manager.unregisterEndpoint(groupId, secondaryEndpointId));
		assertEquals(2, manager.getRevision(groupId));
		assertEquals(2, notifications.get());
		manager.registerEndpoint(groupId, secondaryEndpointId);
		assertEquals(3, manager.getRevision(groupId));
		assertEquals(3, notifications.get());

		LinkedStorageGroupManager loaded = LinkedStorageGroupsSavedData.load(savedData.save()).manager();
		assertTrue(loaded.isEndpointMember(groupId, primaryEndpointId));
		assertTrue(loaded.isPrimaryEndpoint(groupId, primaryEndpointId));
		assertTrue(loaded.isEndpointMember(groupId, secondaryEndpointId));
		assertFalse(loaded.isPrimaryEndpoint(groupId, secondaryEndpointId));
	}

	@Test
	void recordEndpointOpenedPersistsOwnershipAndEndpointAccessWithoutChangingContentRevision() {
		Identifier factoryId = Identifier.fromNamespaceAndPath("sophisticatedcore", "ownership_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID ownerId = UUID.randomUUID();
		UUID otherPlayerId = UUID.randomUUID();
		UUID endpointId = UUID.randomUUID();
		UUID groupId = manager.createGroup(ownerId, endpointId, new LinkedStorageHostDescriptor(factoryId, new CompoundTag()), new ContainerContents());

		assertEquals(ownerId, savedData.findGroup(groupId).orElseThrow().ownerId());
		assertFalse(otherPlayerId.equals(savedData.findGroup(groupId).orElseThrow().ownerId()));
		manager.recordEndpointOpened(groupId, endpointId, otherPlayerId, 1200L);
		assertEquals(0L, manager.getRevision(groupId));
		assertEquals(otherPlayerId, getEndpoint(savedData, groupId, endpointId).lastOpenedBy());
		assertEquals(1200L, getEndpoint(savedData, groupId, endpointId).lastOpenedAt());

		LinkedStorageGroupsSavedData loaded = LinkedStorageGroupsSavedData.load(savedData.save());
		assertEquals(ownerId, loaded.findGroup(groupId).orElseThrow().ownerId());
		assertEquals(otherPlayerId, getEndpoint(loaded, groupId, endpointId).lastOpenedBy());
		assertEquals(1200L, getEndpoint(loaded, groupId, endpointId).lastOpenedAt());
	}

	private static LinkedStorageEndpointRecord getEndpoint(LinkedStorageGroupsSavedData savedData, UUID groupId, UUID endpointId) {
		return savedData.findGroup(groupId).orElseThrow().endpoints().stream().filter(endpoint -> endpoint.endpointId().equals(endpointId)).findFirst()
				.orElseThrow();
	}

	private static class TestHost implements ILinkedStorageVirtualHost {
		private final ILinkedStorageContents contents;
		private CompoundTag virtualCarrier;
		private int refreshes;
		private int layoutRefreshes;
		private int snapshots;

		private TestHost(ILinkedStorageContents contents, CompoundTag virtualCarrier) {
			this.contents = contents;
			this.virtualCarrier = virtualCarrier;
		}

		private ILinkedStorageContents contents() {
			return contents;
		}

		private int refreshes() {
			return refreshes;
		}

		private int layoutRefreshes() {
			return layoutRefreshes;
		}

		private int snapshots() {
			return snapshots;
		}

		@Override
		public void onLinkedStorageContentsChanged() {
			refreshes++;
		}

		@Override
		public void onLinkedStorageLayoutChanged() {
			layoutRefreshes++;
		}

		@Override
		public Optional<CompoundTag> getVirtualCarrierSnapshot() {
			snapshots++;
			return Optional.of(virtualCarrier.copy());
		}

		@Override
		public void onVirtualCarrierChanged(CompoundTag updatedVirtualCarrier) {
			virtualCarrier = updatedVirtualCarrier.copy();
		}
	}
}
