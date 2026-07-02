package net.p3pp3rf1y.sophisticatedcore.upgrades.feeding;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class FeedingUpgradeContainer extends UpgradeContainerBase<FeedingUpgradeWrapper, FeedingUpgradeContainer> {
	private static final String DATA_HUNGER_LEVEL = "hungerLevel";
	private static final String DATA_FEED_IMMEDIATELY_WHEN_HURT = "feedImmediatelyWhenHurt";

	private final FilterLogicContainer<FilterLogic> filterLogicContainer;

	public FeedingUpgradeContainer(Player player, int containerId, FeedingUpgradeWrapper wrapper,
			UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		filterLogicContainer = new FilterLogicContainer<>(supplyFromWrapper(FeedingUpgradeWrapper::getFilterLogic), this, slots::add);
	}

	@Override
	public void handlePacket(CompoundTag data) {
		data.getString(DATA_HUNGER_LEVEL).ifPresent(hungerLevel -> setFeedAtHungerLevel(HungerLevel.fromName(hungerLevel)));
		data.getBoolean(DATA_FEED_IMMEDIATELY_WHEN_HURT).ifPresent(this::setFeedImmediatelyWhenHurt);
		filterLogicContainer.handlePacket(data);
	}

	public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public void setFeedAtHungerLevel(HungerLevel hungerLevel) {
		upgradeWrapper.setFeedAtHungerLevel(hungerLevel);
		sendDataToServer(() -> NBTHelper.putEnumConstant(new CompoundTag(), DATA_HUNGER_LEVEL, hungerLevel));
	}

	public HungerLevel getFeedAtHungerLevel() {
		return upgradeWrapper.getFeedAtHungerLevel();
	}

	public void setFeedImmediatelyWhenHurt(boolean feedImmediatelyWhenHurt) {
		upgradeWrapper.setFeedImmediatelyWhenHurt(feedImmediatelyWhenHurt);
		sendBooleanToServer(DATA_FEED_IMMEDIATELY_WHEN_HURT, feedImmediatelyWhenHurt);
	}

	public boolean shouldFeedImmediatelyWhenHurt() {
		return upgradeWrapper.shouldFeedImmediatelyWhenHurt();
	}
}
