package net.p3pp3rf1y.sophisticatedcore.settings;

import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.main.Context;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.main.PlayerMainSettingsSavedData;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategoryData;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class SettingsHandler {
	private final String playerSettingsName;

	protected ContainerContents.SettingsData settingsData;
	protected final Runnable markContentsDirty;
	protected final Map<String, ISettingsCategory<?, ?>> settingsCategories = new LinkedHashMap<>();
	private final Map<Class<?>, List<?>> interfaceCategories = new HashMap<>();
	private final Map<Class<? extends ISettingsCategory<?, ?>>, ISettingsCategory<?, ?>> typeCategories = new HashMap<>();

	protected SettingsHandler(ContainerContents.SettingsData settingsData, Runnable markContentsDirty, Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderDataHandler> renderDataHandlerSupplier, String playerSettingsName) {
		this.settingsData = settingsData;
		this.markContentsDirty = markContentsDirty;
		this.playerSettingsName = playerSettingsName;
		addSettingsCategories(inventoryHandlerSupplier, renderDataHandlerSupplier, settingsData, playerSettingsName);
	}

	public String getPlayerSettingsName() {
		return playerSettingsName;
	}

	private void addSettingsCategories(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderDataHandler> renderDataHandlerSupplier, ContainerContents.SettingsData settingsData, String playerSettingsName) {
		addSettingsCategory(settingsData, MainSettingsCategory.NAME, markContentsDirty, (MainSettingsCategoryData data, Runnable save) -> new MainSettingsCategory(settingsData, data, save, playerSettingsName), MainSettingsCategoryData::new);
		addSettingsCategory(settingsData, NoSortSettingsCategory.NAME, markContentsDirty, NoSortSettingsCategory::new, NoSortSettingsCategoryData::new);
		addSettingsCategory(settingsData, MemorySettingsCategory.NAME, markContentsDirty, (MemorySettingsCategoryData data, Runnable save) -> new MemorySettingsCategory(inventoryHandlerSupplier, data, save), MemorySettingsCategoryData::new);
		addItemDisplayCategory(inventoryHandlerSupplier, renderDataHandlerSupplier, settingsData);
	}

	protected abstract void addItemDisplayCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderDataHandler> renderDataHandlerSupplier, ContainerContents.SettingsData settingsData);

	protected <D extends ContainerContents.ISettingsCategoryData<D>, T extends ISettingsCategory<T, D>> void addSettingsCategory(ContainerContents.SettingsData settingsData, String categoryName, Runnable markContentsDirty, BiFunction<D, Runnable, T> instantiateCategory, Supplier<D> defaultDataSupplier) {
		T category = instantiateCategory.apply(getSettingsCategoryData(settingsData, categoryName, defaultDataSupplier), markContentsDirty);
		settingsCategories.put(categoryName, category);
		//noinspection unchecked
		typeCategories.put((Class<? extends ISettingsCategory<?, ?>>) category.getClass(), category);
	}

	private static <D extends ContainerContents.ISettingsCategoryData<D>> D getSettingsCategoryData(ContainerContents.SettingsData settingsData, String categoryName, Supplier<D> defaultDataSupplier) {
		//noinspection unchecked
		return (D) settingsData.categories().computeIfAbsent(categoryName, name -> defaultDataSupplier.get());
	}

	public Map<String, ISettingsCategory<?, ?>> getSettingsCategories() {
		return settingsCategories;
	}

	public <T> List<T> getCategoriesThatImplement(Class<T> categoryClass) {
		//noinspection unchecked
		return (List<T>) interfaceCategories.computeIfAbsent(categoryClass, this::getListOfWrappersThatImplement);
	}

	public <T extends ISettingsCategory<?, ?>> T getTypeCategory(Class<T> categoryClazz) {
		//noinspection unchecked - only inserted in one place where it's made sure that class is the same as the category instance
		return (T) typeCategories.get(categoryClazz);
	}

	private <T> List<T> getListOfWrappersThatImplement(Class<T> uc) {
		List<T> ret = new ArrayList<>();
		for (ISettingsCategory<?, ?> category : settingsCategories.values()) {
			if (uc.isInstance(category)) {
				//noinspection unchecked
				ret.add((T) category);
			}
		}
		return ret;
	}

	public ContainerContents.SettingsData getSettingsData() {
		return settingsData;
	}

	public void setSearchPhrase(String searchPhrase) {
		settingsData.setSearchPhrase(searchPhrase);
		markContentsDirty.run();
	}

	public void reloadFrom(ContainerContents.SettingsData settingsData) {
		this.settingsData = settingsData;
		getSettingsCategories().forEach((categoryName, category) -> {
			ContainerContents.ISettingsCategoryData<?> data = settingsData.categories().get(categoryName);
			if (data != null) {
				reloadFrom(category, data);
			}
		});
	}

	private static <D extends ContainerContents.ISettingsCategoryData<D>> void reloadFrom(ISettingsCategory<?, D> category, ContainerContents.ISettingsCategoryData<?> data) {
		//noinspection unchecked
		category.reloadFrom((D) data);
	}

	public <S> S getMainSettingValue(Player player, Function<MainSettingsCategoryData, S> getter) {
		MainSettingsCategory mainSettings = getTypeCategory(MainSettingsCategory.class);
		if (mainSettings.getContext() == Context.PLAYER) {
			return getter.apply(PlayerMainSettingsSavedData.get().get(player.getUUID(), mainSettings.getPlayerSettingsName()));
		} else {
			return mainSettings.getValue(getter);
		}
	}
}
