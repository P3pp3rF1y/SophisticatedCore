package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InventoryHandlerTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void internalExtractionCanExceedStackSizeWithoutChangingCapabilityExtraction() {
		AtomicReference<ItemStack> contents = new AtomicReference<>(new ItemStack(Items.IRON_INGOT, 1000));
		InventoryHandler handler = mock(InventoryHandler.class, CALLS_REAL_METHODS);
		doNothing().when(handler).validateSlotIndex(0);
		doAnswer(i -> contents.get()).when(handler).getSlotStack(0);
		doAnswer(i -> {
			contents.set(i.getArgument(1));
			return null;
		}).when(handler).setSlotStack(eq(0), any(ItemStack.class));
		IInventoryPartHandler part = new IInventoryPartHandler.Default(handler, 1);

		assertEquals(64, part.extractItem(0, 900, true).getCount());
		assertEquals(900, part.extractItemIgnoringLimit(0, 900, true).getCount());
		assertEquals(1000, contents.get().getCount());

		assertEquals(900, part.extractItemIgnoringLimit(0, 900, false).getCount());
		assertEquals(100, contents.get().getCount());
		assertTrue(part.extractItemIgnoringLimit(0, -1, false).isEmpty());
		assertEquals(100, part.extractItemIgnoringLimit(0, Integer.MAX_VALUE, false).getCount());
		assertTrue(contents.get().isEmpty());
	}
}
