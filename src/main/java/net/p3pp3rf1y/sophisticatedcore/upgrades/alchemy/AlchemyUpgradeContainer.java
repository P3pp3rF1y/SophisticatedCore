package net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.FilterSlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.EntityMatch;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class AlchemyUpgradeContainer extends UpgradeContainerBase<AlchemyUpgradeWrapper, AlchemyUpgradeContainer> {
	public static final ResourceLocation EMPTY_POTION_SLOT_BACKGROUND = SophisticatedCore.getRL("item/empty_potion_slot");
	public static final String DATA_CONDITION = "condition";
	public static final String DATA_MATCH_ALL = "matchAll";
	private static final String DATA_MATCH_DURATION = "matchDuration";
	public static final String DATA_MATCH_AMPLIFIER = "matchAmplifier";
	private static final String DATA_ENTITY_MATCH = "entityMatch";

	public AlchemyUpgradeContainer(Player player, int upgradeContainerId, AlchemyUpgradeWrapper upgradeWrapper, UpgradeContainerType<AlchemyUpgradeWrapper, AlchemyUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		InventoryHelper.iterate(upgradeWrapper.getFilterHandler(), (slot, stack) -> {
			slots.add(new FilterSlotItemHandler(upgradeWrapper::getFilterHandler, slot, 0, 0) {
				@Override
				public boolean mayPickup(Player player) {
					return false;
				}

				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.isEmpty() || getItemHandler().isItemValid(slot, stack);
				}
			}.setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_POTION_SLOT_BACKGROUND));
		});
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains(DATA_CONDITION)) {
			NBTHelper.getEnumConstant(data, DATA_CONDITION, AlchemyCondition::fromName).ifPresent(
					condition -> setConditionValue(data.getInt("slot"), condition, data.getFloat("value")));
		} else if (data.contains(DATA_MATCH_ALL)) {
			setMatchAll(data.getBoolean(DATA_MATCH_ALL));
		} else if (data.contains(DATA_MATCH_DURATION)) {
			setMatchDuration(data.getBoolean(DATA_MATCH_DURATION));
		} else if (data.contains(DATA_MATCH_AMPLIFIER)) {
			setMatchAmplifier(data.getBoolean(DATA_MATCH_AMPLIFIER));
		} else if (data.contains(DATA_ENTITY_MATCH)) {
			NBTHelper.getEnumConstant(data, DATA_ENTITY_MATCH, EntityMatch::fromName).ifPresent(this::setEntityMatch);
		}
	}

	private void setConditionValue(int slot, AlchemyCondition enumConstant, float value) {
		upgradeWrapper.setConditionValue(slot, enumConstant, value);
	}

	public AlchemyCondition getCondition(int slot) {
		return getUpgradeWrapper().getCondition(slot);
	}

	public boolean hasNoFilter(int slot) {
		return slots.get(slot).getItem().isEmpty();
	}

	public float getValue(int slot) {
		return getUpgradeWrapper().getValue(slot);
	}

	public void toggleCondition(int slot) {
		if (upgradeWrapper.getFilterHandler().getStackInSlot(slot).isEmpty()) {
			return;
		}

		AlchemyCondition condition = upgradeWrapper.getCondition(slot).next();
		float value = condition.defaultValue();
		setConditionValue(slot, condition, value);
		sendConditionValue(slot, condition, value);
	}

	private void sendConditionValue(int slot, AlchemyCondition condition, float value) {
		sendDataToServer(() -> {
			CompoundTag tag = new CompoundTag();
			tag.putInt("slot", slot);
			NBTHelper.putEnumConstant(tag, DATA_CONDITION, condition);
			tag.putFloat("value", value);
			return tag;
		});
	}

	public void setValue(int slot, float value) {
		AlchemyCondition condition = upgradeWrapper.getCondition(slot);
		setConditionValue(slot, condition, value);
		sendConditionValue(slot, condition, value);
	}

	public void toggleMatchAll() {
		setMatchAll(!upgradeWrapper.shouldMatchAllEffects());
		sendBooleanToServer(DATA_MATCH_ALL, upgradeWrapper.shouldMatchAllEffects());
	}

	private void setMatchAll(boolean matchAllEffects) {
		upgradeWrapper.setMatchAllEffects(matchAllEffects);
	}

	public Boolean shouldMatchAll() {
		return upgradeWrapper.shouldMatchAllEffects();
	}

	public void toggleMatchDuration() {
		setMatchDuration(!upgradeWrapper.shouldMatchEffectDuration());
		sendBooleanToServer(DATA_MATCH_DURATION, upgradeWrapper.shouldMatchEffectDuration());
	}

	private void setMatchDuration(boolean matchDuration) {
		upgradeWrapper.setMatchEffectDuration(matchDuration);
	}

	public boolean shouldMatchDuration() {
		return upgradeWrapper.shouldMatchEffectDuration();
	}

	public void toggleMatchAmplifier() {
		setMatchAmplifier(!upgradeWrapper.shouldMatchEffectAmplifier());
		sendBooleanToServer(DATA_MATCH_AMPLIFIER, upgradeWrapper.shouldMatchEffectAmplifier());
	}

	private void setMatchAmplifier(boolean matchAmplifier) {
		upgradeWrapper.setMatchEffectAmplifier(matchAmplifier);
	}

	public boolean shouldMatchAmplifier() {
		return upgradeWrapper.shouldMatchEffectAmplifier();
	}

	public void toggleEntityMatch() {
		setEntityMatch(getEntityMatch().next());
		sendDataToServer(() -> {
			CompoundTag tag = new CompoundTag();
			NBTHelper.putEnumConstant(tag, DATA_ENTITY_MATCH, getEntityMatch());
			return tag;
		});
	}

	private void setEntityMatch(EntityMatch entityMatch) {
		upgradeWrapper.setEntityMatch(entityMatch);
	}

	public EntityMatch getEntityMatch() {
		return upgradeWrapper.getEntityMatch();
	}

	public boolean hasEntityMatchOption() {
		if (player.containerMenu instanceof StorageContainerMenuBase<?> storageMenu) {
			return storageMenu.getEntity().map(entity -> !(entity instanceof Player)).orElse(true);
		}

		return false;
	}
}
