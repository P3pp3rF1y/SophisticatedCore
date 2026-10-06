package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.main.Context;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategoryData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class LinkedSettingsParityTest {
	@AfterEach
	void clearSnapshots() {
		ClientLinkedStorageContents.clear();
	}

	@Test
	void settingsComparisonIncludesContextAndSearchPhrase() {
		ContainerContents.SettingsData settings = new ContainerContents.SettingsData();
		ContainerContents.SettingsData original = settings.copy();

		settings.setMainSettingsContext(Context.CONTAINER);
		assertNotEquals(original, settings);
		settings.setMainSettingsContext(Context.PLAYER);
		settings.setSearchPhrase("linked");
		assertNotEquals(original, settings);
	}

	@Test
	void reloadReplacesCategoriesAndCopiesAllSettings() {
		ContainerContents.SettingsData settings = new ContainerContents.SettingsData();
		MainSettingsCategoryData existing = new MainSettingsCategoryData();
		settings.categories().put(MainSettingsCategory.NAME, existing);
		settings.categories().put(NoSortSettingsCategory.NAME, new NoSortSettingsCategoryData());
		ContainerContents.SettingsData replacement = new ContainerContents.SettingsData();
		MainSettingsCategoryData updated = new MainSettingsCategoryData();
		updated.setKeepTabOpen(false);
		replacement.categories().put(MainSettingsCategory.NAME, updated);
		MemorySettingsCategoryData added = new MemorySettingsCategoryData();
		replacement.categories().put(MemorySettingsCategory.NAME, added);
		replacement.setMainSettingsContext(Context.CONTAINER);
		replacement.setSearchPhrase("linked");

		settings.reloadFrom(replacement);

		assertSame(existing, settings.categories().get(MainSettingsCategory.NAME));
		assertFalse(existing.keepTabOpen());
		assertFalse(settings.categories().containsKey(NoSortSettingsCategory.NAME));
		assertNotSame(added, settings.categories().get(MemorySettingsCategory.NAME));
		assertEquals(replacement, settings);
	}

	@Test
	void mainSettingsCategoryUsesReloadedSettingsContext() {
		ContainerContents.SettingsData initial = new ContainerContents.SettingsData();
		SettingsHandler handler = new SettingsHandler(initial, () -> {
		}, () -> null, () -> null, "test") {
			@Override
			protected void addItemDisplayCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderDataHandler> renderDataHandlerSupplier,
					ContainerContents.SettingsData settingsData) {
			}
		};
		MainSettingsCategory category = handler.getTypeCategory(MainSettingsCategory.class);
		ContainerContents.SettingsData replacement = initial.copy();
		replacement.setMainSettingsContext(Context.CONTAINER);

		handler.reloadFrom(replacement);

		assertEquals(Context.CONTAINER, category.getContext());
		category.setContext(Context.PLAYER);
		assertEquals(Context.PLAYER, handler.getSettingsData().mainSettingsContext());
	}

	@Test
	void missingCategoryReloadsItsDefaultInsteadOfKeepingPreviousSettings() {
		SettingsHandler handler = new SettingsHandler(new ContainerContents.SettingsData(), () -> {
		}, () -> null, () -> null, "test") {
			@Override
			protected void addItemDisplayCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderDataHandler> renderDataHandlerSupplier,
					ContainerContents.SettingsData settingsData) {
			}
		};
		MainSettingsCategory category = handler.getTypeCategory(MainSettingsCategory.class);
		category.setValue(data -> data.setKeepTabOpen(false));

		handler.reloadFrom(new ContainerContents.SettingsData());

		assertTrue(category.getValue(MainSettingsCategoryData::keepTabOpen));
	}

	@Test
	void settingsUpdateReplacesClientSnapshotWithoutChangingRevision() {
		UUID groupId = UUID.randomUUID();
		ClientLinkedStorageContents.updateContents(groupId, 4, new ContainerContents(), Component.literal("Linked"), 27, 3, 2);
		ILinkedStorageContents contents = ClientLinkedStorageContents.getContents(groupId).orElseThrow();
		ContainerContents original = contents.contents();
		ContainerContents beforeUpdate = original.copy();
		ContainerContents.SettingsData settings = new ContainerContents.SettingsData();
		settings.categories().put(MainSettingsCategory.NAME, new MainSettingsCategoryData());
		settings.setMainSettingsContext(Context.CONTAINER);
		settings.setSearchPhrase("linked");

		ClientLinkedStorageContents.updateSettings(groupId, settings);
		settings.setSearchPhrase("changed after update");

		assertSame(contents, ClientLinkedStorageContents.getContents(groupId).orElseThrow());
		assertSame(original, contents.contents());
		assertTrue(beforeUpdate.settings().categories().isEmpty());
		assertEquals("linked", contents.contents().settings().searchPhrase());
		assertEquals(Context.CONTAINER, contents.contents().settings().mainSettingsContext());
		assertTrue(contents.contents().settings().categories().containsKey(MainSettingsCategory.NAME));
		assertEquals(4, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals(27, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());
	}
}
