package net.p3pp3rf1y.sophisticatedcore.settings.main;

import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;

import java.util.function.Consumer;
import java.util.function.Function;

public class MainSettingsCategory implements ISettingsCategory<MainSettingsCategory, MainSettingsCategoryData> {
	public static final String NAME = "global";
	private final Runnable save;

	private final String playerSettingsName;
	private final ContainerContents.SettingsData settingsData;
	private MainSettingsCategoryData data;

	public MainSettingsCategory(ContainerContents.SettingsData settingsData, MainSettingsCategoryData data, Runnable save, String playerSettingsName) {
		this.settingsData = settingsData;
		this.data = data;
		this.save = save;
		this.playerSettingsName = playerSettingsName;
	}

	public String getPlayerSettingsName() {
		return playerSettingsName;
	}

	@Override
	public void reloadFrom(MainSettingsCategoryData data) {
		this.data = data;
	}

	@Override
	public void overwriteWith(MainSettingsCategory otherCategory) {
		// noop for now
	}

	public void toggleContext() {
		setContext(getContext() == Context.PLAYER ? Context.CONTAINER : Context.PLAYER);
		save.run();
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return false; // no slots in this category so it can't be too large
	}

	@Override
	public void copyTo(MainSettingsCategory otherCategory, int startFromSlot, int slotOffset) {
		// noop just letting the other retain its state
	}

	@Override
	public void deleteSlotSettingsFrom(int slotIndex) {
		// noop no slots to delete
	}

	public Context getContext() {
		return settingsData.mainSettingsContext();
	}

	public void setContext(Context context) {
		settingsData.setMainSettingsContext(context);
		save.run();
	}

	public void setValue(Consumer<MainSettingsCategoryData> setter) {
		setter.accept(data);
		save.run();
	}

	public <T> T getValue(Function<MainSettingsCategoryData, T> getter) {
		return getter.apply(data);
	}
}
