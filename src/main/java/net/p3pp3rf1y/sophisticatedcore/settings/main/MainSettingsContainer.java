package net.p3pp3rf1y.sophisticatedcore.settings.main;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.MainSetting;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsContainerBase;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;

public class MainSettingsContainer extends SettingsContainerBase<MainSettingsCategory> {
	private static final String CONTEXT_TAG = "context";
	private Context context = Context.PLAYER;

	public MainSettingsContainer(SettingsContainer<?> settingsContainer, String categoryName, MainSettingsCategory category) {
		super(settingsContainer, categoryName, category);
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains(CONTEXT_TAG)) {
			context = Context.fromId(data.getInt(CONTEXT_TAG));
		} else {
			for (String tagName : data.getAllKeys()) {
				SettingsManager.getSetting(tagName).ifPresent(setting -> setSettingValue(getPlayer(), setting, data));
			}
		}
	}

	public void toggleContext() {
		context = context == Context.PLAYER ? Context.STORAGE : Context.PLAYER;
		sendIntToServer(CONTEXT_TAG, context.getId());
	}

	public Context getContext() {
		return context;
	}

	protected Player getPlayer() {
		return getSettingsContainer().getPlayer();
	}

	public void toggleShiftClickIntoOpenTab() {
		toggleBooleanSetting(getPlayer(), SettingsManager.SHIFT_CLICK_INTO_OPEN_TAB_FIRST);
	}

	public boolean shouldShiftClickIntoOpenTab() {
		return getSettingValue(SettingsManager.SHIFT_CLICK_INTO_OPEN_TAB_FIRST);
	}

	public void toggleKeepTabOpen() {
		toggleBooleanSetting(getPlayer(), SettingsManager.KEEP_TAB_OPEN);
	}

	public boolean shouldKeepTabOpen() {
		return getSettingValue(SettingsManager.KEEP_TAB_OPEN);
	}

	protected  <T> T getSettingValue(MainSetting<T> setting) {
		if (context == Context.PLAYER) {
			return SettingsManager.getPlayerSettingOrDefault(getPlayer(), getCategory().getPlayerSettingsTagName(), setting);
		} else {
			return SettingsManager.getSettingValue(getPlayer(), getCategory().getPlayerSettingsTagName(), getCategory(), setting);
		}
	}

	private <T> void setSettingValue(Player player, MainSetting<T> setting, CompoundTag data) {
		setting.getValue(data).ifPresent(value -> {
			if (context == Context.PLAYER) {
				SettingsManager.setPlayerSetting(player, getCategory().getPlayerSettingsTagName(), setting, value);
			} else {
				SettingsManager.setSetting(player, getCategory().getPlayerSettingsTagName(), getCategory(), setting, value);
			}
		});
	}

	protected void toggleBooleanSetting(Player player, MainSetting<Boolean> setting) {
		if (context == Context.PLAYER) {
			boolean value = !SettingsManager.getPlayerSettingOrDefault(player, getCategory().getPlayerSettingsTagName(), setting);
			SettingsManager.setPlayerSetting(player, getCategory().getPlayerSettingsTagName(), setting, value);
			sendSettingValueToServer(setting, value);
		} else {
			boolean value = !SettingsManager.getSettingValue(player, getCategory().getPlayerSettingsTagName(), getCategory(), setting);
			sendSettingValueToServer(setting, value);
		}
	}

	private void sendSettingValueToServer(MainSetting<Boolean> setting, boolean value) {
		CompoundTag data = new CompoundTag();
		setting.setValue(data, value);
		sendDataToServer(() -> data);
	}
}
