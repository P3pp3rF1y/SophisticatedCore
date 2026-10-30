package net.p3pp3rf1y.sophisticatedcore.upgrades.magnet;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagnetUpgradeWrapperTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		Items.REDSTONE.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
	}

	@ParameterizedTest
	@CsvSource({"0, 6, 6, true", "60, 10, 4, false", "64, 6, 0, false"})
	void onlyDiscardsFullyConsumedEntities(int stored, int dropped, int accepted, boolean discarded) throws ReflectiveOperationException {
		ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(1);
		if (stored > 0) {
			try (Transaction tx = Transaction.openRoot()) {
				assertEquals(stored, inventory.insert(0, ItemResource.of(Items.REDSTONE), stored, tx));
				tx.commit();
			}
		}
		IStorageWrapper storage = mock(IStorageWrapper.class);
		when(storage.getInventoryForUpgradeProcessing()).thenReturn(mock(ITrackedContentsItemResourceHandler.class, delegatesTo(inventory)));
		when(storage.getSettingsHandler()).thenReturn(mock(SettingsHandler.class));
		ItemStack upgrade = mock(ItemStack.class);
		when(upgrade.getItem()).thenReturn(mock(MagnetUpgradeItem.class));
		MagnetUpgradeWrapper wrapper = new MagnetUpgradeWrapper(storage, upgrade, stack -> {
		});
		ItemStack original = new ItemStack(Items.REDSTONE, dropped);
		AtomicReference<ItemStack> remainder = new AtomicReference<>(original);
		ItemEntity entity = mock(ItemEntity.class);
		when(entity.getItem()).thenAnswer(invocation -> remainder.get());
		doAnswer(invocation -> {
			remainder.set(invocation.getArgument(0));
			return null;
		}).when(entity).setItem(any(ItemStack.class));
		Method insert = MagnetUpgradeWrapper.class.getDeclaredMethod("tryToInsertItem", Player.class, ItemEntity.class);
		insert.setAccessible(true);

		assertEquals(accepted > 0, insert.invoke(wrapper, null, entity));
		assertEquals(stored + accepted, inventory.getAmountAsInt(0));
		assertEquals(dropped - accepted, remainder.get().getCount());
		verify(entity, times(discarded ? 1 : 0)).discard();
		if (accepted == 0) {
			assertSame(original, remainder.get());
		}
	}
}
