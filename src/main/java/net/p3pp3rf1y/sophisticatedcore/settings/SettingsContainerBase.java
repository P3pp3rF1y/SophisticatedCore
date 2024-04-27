package net.p3pp3rf1y.sophisticatedcore.settings;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.network.SyncContainerClientDataPacket;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.function.Supplier;

public abstract class SettingsContainerBase<C extends ISettingsCategory<?>> {
	private final SettingsContainerMenu<?> settingsContainer;
	private final String categoryName;
	private final C category;

	protected SettingsContainerBase(SettingsContainerMenu<?> settingsContainer, String categoryName, C category) {
		this.settingsContainer = settingsContainer;
		this.categoryName = categoryName;
		this.category = category;
	}

	protected C getCategory() {
		return category;
	}

	public SettingsContainerMenu<?> getSettingsContainer() {
		return settingsContainer;
	}

	public void sendIntToServer(String key, int value) {
		sendDataToServer(() -> {
			CompoundTag data = new CompoundTag();
			data.putInt(key, value);
			return data;
		});
	}

	public void sendStringToServer(String key, String value) {
		sendDataToServer(() -> {
			CompoundTag data = new CompoundTag();
			data.putString(key, value);
			return data;
		});
	}

	public void sendBooleanToServer(String key, boolean value) {
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), key, value));
	}

	public void sendDataToServer(Supplier<CompoundTag> supplyData) {
		if (isServer()) {
			return;
		}
		CompoundTag data = supplyData.get();
		data.putString("categoryName", categoryName);
		PacketDistributor.SERVER.noArg().send(new SyncContainerClientDataPacket(data));
	}

	protected boolean isServer() {
		return !settingsContainer.getPlayer().level().isClientSide;
	}

	public abstract void handlePacket(CompoundTag data);
}
