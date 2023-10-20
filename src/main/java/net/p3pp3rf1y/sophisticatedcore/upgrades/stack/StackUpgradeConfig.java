package net.p3pp3rf1y.sophisticatedcore.upgrades.stack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.Config;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StackUpgradeConfig {
	private static final String REGISTRY_NAME_MATCHER = "([a-z0-9_.-]+:[a-z0-9_/.-]+)";
	private final ForgeConfigSpec.ConfigValue<List<String>> nonStackableItemsList;
	@Nullable
	private Set<Item> nonStackableItems = null;

	public StackUpgradeConfig(ForgeConfigSpec.Builder builder) {
		builder.comment("Stack Upgrade Settings").push("stackUpgrade");
		nonStackableItemsList = builder.comment("List of items that are not supposed to stack in storage even when stack upgrade is inserted. Item registry names are expected here.").define("nonStackableItems", this::getDefaultNonStackableList, itemNames -> {
			List<String> registryNames = (List<String>) itemNames;
			return registryNames != null && registryNames.stream().allMatch(itemName -> itemName.matches(REGISTRY_NAME_MATCHER));
		});
		builder.pop();
	}

	private List<String> getDefaultNonStackableList() {
		List<String> ret = new ArrayList<>();

		ret.add(RegistryHelper.getItemKey(Items.BUNDLE).toString());
		ret.add(RegistryHelper.getItemKey(Items.SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.WHITE_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.ORANGE_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.MAGENTA_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.LIGHT_BLUE_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.YELLOW_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.LIME_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.PINK_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.GRAY_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.LIGHT_GRAY_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.CYAN_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.PURPLE_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.BLUE_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.BROWN_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.GREEN_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.RED_SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.BLACK_SHULKER_BOX).toString());

		return ret;
	}

	public boolean canStackItem(Item item) {
		if (!Config.COMMON_SPEC.isLoaded()) {
			return true;
		}
		if (nonStackableItems == null) {
			nonStackableItems = new HashSet<>();
			nonStackableItemsList.get().forEach(name -> {
				ResourceLocation registryName = new ResourceLocation(name);
				if (ForgeRegistries.ITEMS.containsKey(registryName)) {
					nonStackableItems.add(ForgeRegistries.ITEMS.getValue(registryName));
				} else {
					SophisticatedCore.LOGGER.error("Item {} is set to not be affected by stack upgrade in config, but it does not exist in item registry", name);
				}
			});
		}
		return !nonStackableItems.contains(item);
	}

	public void clearNonStackableItems() {
		nonStackableItems = null;
	}
}
