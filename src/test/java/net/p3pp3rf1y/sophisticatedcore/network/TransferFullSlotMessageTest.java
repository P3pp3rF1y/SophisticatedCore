package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class TransferFullSlotMessageTest {
	@ParameterizedTest
	@ValueSource(ints = {-1, Integer.MAX_VALUE, 100})
	void handleMessageRejectsNonStorageSlotBeforeLookingItUp(int slotId) throws ReflectiveOperationException {
		ServerPlayer player = mock(ServerPlayer.class);
		StorageContainerMenuBase<?> menu = mock(StorageContainerMenuBase.class);
		player.containerMenu = menu;
		Method handle = TransferFullSlotMessage.class.getDeclaredMethod("handleMessage", ServerPlayer.class, TransferFullSlotMessage.class);
		handle.setAccessible(true);

		handle.invoke(null, player, new TransferFullSlotMessage(slotId));

		verify(menu, never()).getSlot(anyInt());
		verify(menu, never()).quickMoveStack(player, slotId);
	}
}
