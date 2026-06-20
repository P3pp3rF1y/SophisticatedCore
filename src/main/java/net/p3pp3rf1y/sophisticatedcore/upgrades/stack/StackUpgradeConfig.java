package net.p3pp3rf1y.sophisticatedcore.upgrades.stack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.Config;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StackUpgradeConfig {
	private static final String REGISTRY_NAME_MATCHER = "([a-z0-9_.-]+:[a-z0-9_/.-]+)";
	private final ModConfigSpec.ConfigValue<List<? extends String>> nonStackableItemsList;
	@Nullable
	private Set<Item> nonStackableItems = null;

	public StackUpgradeConfig(ModConfigSpec.Builder builder) {
		builder.comment("Stack Upgrade Settings").push("stackUpgrade");
		nonStackableItemsList = builder.comment("List of items that are not supposed to stack in storage even when stack upgrade is inserted. Item registry names are expected here.")
				.defineList("nonStackableItems", this::getDefaultNonStackableList, () -> "minecraft:bundle", itemName -> itemName instanceof String s && s.matches(REGISTRY_NAME_MATCHER));
		builder.pop();
	}

	private List<String> getDefaultNonStackableList() {
		List<String> ret = new ArrayList<>();

		ret.add(RegistryHelper.getItemKey(Items.BUNDLE).toString());
		ret.add(RegistryHelper.getItemKey(Items.SHULKER_BOX).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.white()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.orange()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.magenta()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.lightBlue()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.yellow()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.lime()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.pink()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.gray()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.lightGray()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.cyan()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.purple()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.blue()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.brown()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.green()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.red()).toString());
		ret.add(RegistryHelper.getItemKey(Items.DYED_SHULKER_BOX.black()).toString());

		return ret;
	}

	public boolean canStackItem(Item item) {
		if (!Config.COMMON_SPEC.isLoaded()) {
			return true;
		}
		if (nonStackableItems == null) {
			nonStackableItems = new HashSet<>();
			nonStackableItemsList.get().forEach(name -> {
				Identifier registryName = Identifier.parse(name);
				BuiltInRegistries.ITEM.get(registryName).ifPresentOrElse(
						e -> nonStackableItems.add(e.value()),
						() -> SophisticatedCore.LOGGER.error("Item {} is set to not be affected by stack upgrade in config, but it does not exist in item registry", name)
				);
			});
		}
		return !nonStackableItems.contains(item);
	}

	public void clearNonStackableItems() {
		nonStackableItems = null;
	}
}
