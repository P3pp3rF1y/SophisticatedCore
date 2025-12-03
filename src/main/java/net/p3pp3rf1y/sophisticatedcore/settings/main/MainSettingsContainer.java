package net.p3pp3rf1y.sophisticatedcore.settings.main;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsContainerBase;

import java.util.function.Consumer;
import java.util.function.Function;

public class MainSettingsContainer extends SettingsContainerBase<MainSettingsCategory> {
	private static final String CONTEXT_TAG = "context";
	private static final String SHIFT_CLICK_INTO_OPEN_TAB_FIRST = "shiftClickIntoOpenTabFirst";
	private static final String KEEP_TAB_OPEN_TAG = "keepTabOpen";
	private static final String KEEP_SEARCH_PHRASE_TAG = "keepSearchPhrase";

	public MainSettingsContainer(SettingsContainerMenu<?> settingsContainer, String categoryName, MainSettingsCategory category) {
		super(settingsContainer, categoryName, category);
	}

	@Override
	public void handlePacket(CompoundTag data) {
		data.getString(CONTEXT_TAG).ifPresent(name -> getCategory().setContext(Context.fromName(name)));
		data.getBoolean(SHIFT_CLICK_INTO_OPEN_TAB_FIRST).ifPresent(this::setShiftClickIntoOpenTab);
		data.getBoolean(KEEP_TAB_OPEN_TAG).ifPresent(this::setKeepTabOpen);
		data.getBoolean(KEEP_SEARCH_PHRASE_TAG).ifPresent(this::setKeepSearchPhrase);
	}

	private void setShiftClickIntoOpenTab(boolean value) {
		setSettingValue(data -> data.setShiftClickIntoOpenTab(value), tag -> tag.putBoolean(SHIFT_CLICK_INTO_OPEN_TAB_FIRST, value));
	}

	public void toggleContext() {
		getCategory().toggleContext();
		sendStringToServer(CONTEXT_TAG, getContext().getSerializedName());
	}

	public Context getContext() {
		return getCategory().getContext();
	}

	protected Player getPlayer() {
		return getSettingsContainer().getPlayer();
	}

	public void toggleShiftClickIntoOpenTab() {
		setShiftClickIntoOpenTab(!shouldShiftClickIntoOpenTab());
	}

	public boolean shouldShiftClickIntoOpenTab() {
		return getSettingValue(MainSettingsCategoryData::shiftClickIntoOpenTab);
	}

	private void setKeepTabOpen(boolean value) {
		setSettingValue(data -> data.setKeepTabOpen(value), tag -> tag.putBoolean(KEEP_TAB_OPEN_TAG, value));
	}

	public void toggleKeepTabOpen() {
		setKeepTabOpen(!shouldKeepTabOpen());
	}

	private void setKeepSearchPhrase(boolean value) {
		setSettingValue(data -> data.setKeepSearchPhrase(value), tag -> tag.putBoolean(KEEP_SEARCH_PHRASE_TAG, value));
	}

	public void toggleKeepSearchPhrase() {
		setKeepSearchPhrase(!shouldKeepSearchPhrase());
	}

	public boolean shouldKeepTabOpen() {
		return getSettingValue(MainSettingsCategoryData::keepTabOpen);
	}

	public boolean shouldKeepSearchPhrase() {
		return getSettingValue(MainSettingsCategoryData::keepSearchPhrase);
	}

	protected <T> T getSettingValue(Function<MainSettingsCategoryData, T> getter) {
		return getSettingsContainer().getStorageWrapper().getSettingsHandler().getMainSettingValue(getPlayer(), getter);
	}

	public void setSettingValue(Consumer<MainSettingsCategoryData> setter, Consumer<CompoundTag> dataSetter) {
		if (getContext() == Context.PLAYER) {
			PlayerMainSettingsSavedData.get().setvalue(getPlayer().getUUID(), getCategory().getPlayerSettingsName(), setter);
		} else {
			getCategory().setValue(setter);
		}
		sendDataToServer(() -> {
			CompoundTag data = new CompoundTag();
			dataSetter.accept(data);
			return data;
		});
	}
}
