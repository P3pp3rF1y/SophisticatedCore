package net.p3pp3rf1y.sophisticatedcore.settings;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class SettingsHandlerCanonicalRootTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void categoryWritesAfterReloadTargetTheReplacementRootRatherThanTheOldSnapshot() {
		CompoundTag oldRoot = new CompoundTag();
		CompoundTag newRoot = new CompoundTag();
		AtomicInteger dirty = new AtomicInteger();
		TestSettingsHandler handler = new TestSettingsHandler(oldRoot, dirty::incrementAndGet);

		handler.reloadFromStorageContents(newRoot);
		CompoundTag category = new CompoundTag();
		category.putInt("selection", 7);
		handler.saveGlobal(category);

		assertFalse(oldRoot.contains("settings"));
		assertSame(newRoot.getCompound("settings"), handler.getNbt());
		assertEquals(7, newRoot.getCompound("settings").getCompound("global").getInt("selection"));
		assertEquals(1, dirty.get());
	}

	private static class TestSettingsHandler extends SettingsHandler {
		private Consumer<CompoundTag> globalSave;

		private TestSettingsHandler(CompoundTag contents, Runnable dirty) {
			super(contents, dirty, () -> null, () -> null);
		}

		@Override
		protected CompoundTag getSettingsNbtFromContentsNbt(CompoundTag contents) {
			return contents.getCompound("settings");
		}

		@Override
		protected void addItemDisplayCategory(Supplier<InventoryHandler> inventory, Supplier<RenderInfo> render, CompoundTag settings) {
		}

		@Override
		public String getGlobalSettingsCategoryName() {
			return "global";
		}

		@Override
		public ISettingsCategory<?> instantiateGlobalSettingsCategory(CompoundTag category, Consumer<CompoundTag> save) {
			globalSave = save;
			return new MainSettingsCategory<>(category, save, "test");
		}

		@Override
		protected void saveCategoryNbt(CompoundTag settings, String name, CompoundTag category) {
			settings.put(name, category);
			contentsNbt.put("settings", settings);
		}

		private void saveGlobal(CompoundTag category) {
			globalSave.accept(category);
		}
	}
}
