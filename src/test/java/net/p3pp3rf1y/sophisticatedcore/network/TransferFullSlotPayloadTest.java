package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferFullSlotPayloadTest {
	@ParameterizedTest
	@ValueSource(ints = {-1, Integer.MAX_VALUE, 100})
	void handlePayloadRejectsNonStorageSlotBeforeLookingItUp(int slotId) {
		Player player = mock(Player.class);
		StorageContainerMenuBase<?> menu = mock(StorageContainerMenuBase.class);
		player.containerMenu = menu;
		IPayloadContext context = mock(IPayloadContext.class);
		when(context.player()).thenReturn(player);

		TransferFullSlotPayload.handlePayload(new TransferFullSlotPayload(slotId), context);

		verify(menu, never()).getSlot(anyInt());
		verify(menu, never()).quickMoveStack(player, slotId);
	}
}
