package net.p3pp3rf1y.sophisticatedcore.settings.main;

import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.MainSetting;

import java.util.Optional;
import java.util.function.Consumer;

public class MainSettingsCategory<T extends MainSettingsCategory<?>> implements ISettingsCategory<T> {
	public static final String NAME = "global";
	private CompoundTag categoryNbt;
	private final Consumer<CompoundTag> saveNbt;

	private final String playerSettingsTagName;

	public MainSettingsCategory(CompoundTag categoryNbt, Consumer<CompoundTag> saveNbt, String playerSettingsTagName) {
		this.categoryNbt = categoryNbt;
		this.saveNbt = saveNbt;
		this.playerSettingsTagName = playerSettingsTagName;
	}

	public String getPlayerSettingsTagName() {
		return playerSettingsTagName;
	}

	public <T> Optional<T> getSettingValue(MainSetting<T> setting) {
		return setting.getValue(categoryNbt);
	}

	public <T> void setSettingValue(MainSetting<T> setting, T value) {
		setting.setValue(categoryNbt, value);
		saveNbt.accept(categoryNbt);
	}

	public <T> void removeSetting(MainSetting<T> setting) {
		setting.removeFrom(categoryNbt);
		saveNbt.accept(categoryNbt);
	}

	@Override
	public void reloadFrom(CompoundTag categoryNbt) {
		this.categoryNbt = categoryNbt;
	}

	@Override
	public void overwriteWith(T otherCategory) {
		//noop for now
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return false; //no slots in this category so it can't be too large
	}
}
