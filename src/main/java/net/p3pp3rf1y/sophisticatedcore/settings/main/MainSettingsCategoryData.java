package net.p3pp3rf1y.sophisticatedcore.settings.main;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.Objects;

public class MainSettingsCategoryData implements ContainerContents.ISettingsCategoryData<MainSettingsCategoryData> {
	public static final Codec<MainSettingsCategoryData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.BOOL.fieldOf("shiftClickIntoOpenTab").forGetter(MainSettingsCategoryData::shiftClickIntoOpenTab),
					Codec.BOOL.fieldOf("keepTabOpen").forGetter(MainSettingsCategoryData::keepTabOpen),
					Codec.BOOL.fieldOf("keepSearchPhrase").forGetter(MainSettingsCategoryData::keepSearchPhrase),
					Codec.BOOL.fieldOf("anotherPlayerCanOpen").forGetter(data -> data.anotherPlayerCanOpen)
			).apply(instance, MainSettingsCategoryData::new)
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, MainSettingsCategoryData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			MainSettingsCategoryData::shiftClickIntoOpenTab,
			ByteBufCodecs.BOOL,
			MainSettingsCategoryData::keepTabOpen,
			ByteBufCodecs.BOOL,
			MainSettingsCategoryData::keepSearchPhrase,
			ByteBufCodecs.BOOL,
			data -> data.anotherPlayerCanOpen,
			MainSettingsCategoryData::new);

	//TODO figure out if I need to serialize context here

	static {
		ContainerContents.SettingsCategoryDataRegistry.register(CODEC, STREAM_CODEC, MainSettingsCategory.NAME);
	}

	private boolean shiftClickIntoOpenTab = false;
	private boolean keepTabOpen = true;
	private boolean keepSearchPhrase = true;
	private boolean anotherPlayerCanOpen = true;

	public MainSettingsCategoryData() {
	}

	public MainSettingsCategoryData(boolean shiftClickIntoOpenTab, boolean keepTabOpen, boolean keepSearchPhrase, boolean anotherPlayerCanOpen) {
		this.shiftClickIntoOpenTab = shiftClickIntoOpenTab;
		this.keepTabOpen = keepTabOpen;
		this.keepSearchPhrase = keepSearchPhrase;
		this.anotherPlayerCanOpen = anotherPlayerCanOpen;
	}

	public boolean shiftClickIntoOpenTab() {
		return shiftClickIntoOpenTab;
	}

	public void setShiftClickIntoOpenTab(boolean shiftClickIntoOpenTab) {
		this.shiftClickIntoOpenTab = shiftClickIntoOpenTab;
	}

	public boolean keepTabOpen() {
		return keepTabOpen;
	}

	public void setKeepTabOpen(boolean keepTabOpen) {
		this.keepTabOpen = keepTabOpen;
	}

	public boolean keepSearchPhrase() {
		return keepSearchPhrase;
	}

	public void setKeepSearchPhrase(boolean keepSearchPhrase) {
		this.keepSearchPhrase = keepSearchPhrase;
	}

	@Override
	public String id() {
		return MainSettingsCategory.NAME;
	}

	public boolean anotherPlayerCanOpen() {
		return anotherPlayerCanOpen;
	}

	public void setAnotherPlayerCanOpen(boolean anotherPlayerCanOpen) {
		this.anotherPlayerCanOpen = anotherPlayerCanOpen;
	}

	@Override
	public MainSettingsCategoryData copy() {
		return new MainSettingsCategoryData(shiftClickIntoOpenTab, keepTabOpen, keepSearchPhrase, anotherPlayerCanOpen);
	}

	@Override
	public void reloadFrom(MainSettingsCategoryData other) {
		this.shiftClickIntoOpenTab = other.shiftClickIntoOpenTab;
		this.keepTabOpen = other.keepTabOpen;
		this.keepSearchPhrase = other.keepSearchPhrase;
		this.anotherPlayerCanOpen = other.anotherPlayerCanOpen;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MainSettingsCategoryData that)) return false;
		return shiftClickIntoOpenTab == that.shiftClickIntoOpenTab && keepTabOpen == that.keepTabOpen && keepSearchPhrase == that.keepSearchPhrase && anotherPlayerCanOpen == that.anotherPlayerCanOpen;
	}

	@Override
	public int hashCode() {
		return Objects.hash(shiftClickIntoOpenTab, keepTabOpen, keepSearchPhrase, anotherPlayerCanOpen);
	}
}
