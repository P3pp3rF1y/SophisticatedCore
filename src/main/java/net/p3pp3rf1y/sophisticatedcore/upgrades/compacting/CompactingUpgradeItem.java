package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;

public class CompactingUpgradeItem extends UpgradeItemBase<CompactingUpgradeWrapper> {
	private static final UpgradeType<CompactingUpgradeWrapper> TYPE = new UpgradeType<>(CompactingUpgradeWrapper::new);
	private final boolean shouldCompactThreeByThree;
	private final IntSupplier filterSlotCount;
	private final ConfiguredCompactingResultProvider configuredCompactingResultProvider;

	public CompactingUpgradeItem(boolean shouldCompactThreeByThree, IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig, Properties properties) {
		this(shouldCompactThreeByThree, filterSlotCount, upgradeTypeLimitConfig, properties, (stack, maxWidth, maxHeight) -> Optional.empty());
	}

	public CompactingUpgradeItem(boolean shouldCompactThreeByThree, IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig, Properties properties, ConfiguredCompactingResultProvider configuredCompactingResultProvider) {
		super(upgradeTypeLimitConfig, properties);
		this.shouldCompactThreeByThree = shouldCompactThreeByThree;
		this.filterSlotCount = filterSlotCount;
		this.configuredCompactingResultProvider = configuredCompactingResultProvider;
	}

	@Override
	public UpgradeType<CompactingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	public boolean shouldCompactThreeByThree() {
		return shouldCompactThreeByThree;
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}

	public Optional<CompactingUpgradeConfig.CompactingDefinition> getConfiguredCompactingResult(ItemStack stack, int maxWidth, int maxHeight) {
		return configuredCompactingResultProvider.getCompactingResult(stack, maxWidth, maxHeight);
	}

	@FunctionalInterface
	public interface ConfiguredCompactingResultProvider {
		Optional<CompactingUpgradeConfig.CompactingDefinition> getCompactingResult(ItemStack stack, int maxWidth, int maxHeight);
	}
}
