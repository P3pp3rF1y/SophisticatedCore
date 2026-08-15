package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PacketBufferHelperTest {
	@Test
	void readsCollectionsAtTheirLimit() {
		FriendlyByteBuf listBuffer = new FriendlyByteBuf(Unpooled.buffer());
		listBuffer.writeVarInt(2);
		listBuffer.writeInt(3);
		listBuffer.writeInt(7);

		FriendlyByteBuf mapBuffer = new FriendlyByteBuf(Unpooled.buffer());
		mapBuffer.writeVarInt(2);
		mapBuffer.writeInt(3);
		mapBuffer.writeInt(5);
		mapBuffer.writeInt(7);
		mapBuffer.writeInt(11);

		try {
			assertEquals(List.of(3, 7), PacketBufferHelper.readList(listBuffer, FriendlyByteBuf::readInt, 2));
			assertEquals(Map.of(3, 5, 7, 11), PacketBufferHelper.readMap(mapBuffer, FriendlyByteBuf::readInt, FriendlyByteBuf::readInt, 2));
		} finally {
			listBuffer.release();
			mapBuffer.release();
		}
	}

	@Test
	void rejectsInvalidCountsBeforeReadingElements() {
		FriendlyByteBuf tooLargeBuffer = new FriendlyByteBuf(Unpooled.buffer());
		tooLargeBuffer.writeVarInt(3);
		FriendlyByteBuf negativeBuffer = new FriendlyByteBuf(Unpooled.buffer());
		negativeBuffer.writeVarInt(-1);
		AtomicBoolean readElement = new AtomicBoolean();

		try {
			assertThrows(DecoderException.class, () -> PacketBufferHelper.readList(tooLargeBuffer, buffer -> {
				readElement.set(true);
				return buffer.readInt();
			}, 2));
			assertThrows(DecoderException.class, () -> PacketBufferHelper.readMap(negativeBuffer, FriendlyByteBuf::readInt, FriendlyByteBuf::readInt, 2));
			assertFalse(readElement.get());
		} finally {
			tooLargeBuffer.release();
			negativeBuffer.release();
		}
	}
}
