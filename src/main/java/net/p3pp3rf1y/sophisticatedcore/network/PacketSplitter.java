package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PacketSplitter {
	public static final byte STATE_FIRST = 1;
	public static final byte STATE_LAST = 2;

	private final int partSize;
	private final List<byte[]> receivedBuffers = new ArrayList<>();

	public PacketSplitter(int partSize) {
		if (partSize <= 0)
			throw new IllegalArgumentException("partSize must be > 0");
		this.partSize = partSize;
	}

	public void splitAndSend(byte[] fullStream, Consumer<byte[]> sendPayloadSlice) {
		if (fullStream.length <= partSize) {
			byte[] payloadSlice = new byte[fullStream.length + 1];
			payloadSlice[0] = STATE_LAST;
			System.arraycopy(fullStream, 0, payloadSlice, 1, fullStream.length);
			sendPayloadSlice.accept(payloadSlice);
			return;
		}

		int parts = (int) Math.ceil((double) fullStream.length / partSize);
		for (int part = 0; part < parts; part++) {
			int start = part * partSize;
			int size = Math.min(partSize, fullStream.length - start);

			byte state = (part == 0) ? STATE_FIRST : (part == parts - 1) ? STATE_LAST : 0;

			byte[] payloadSlice = new byte[size + 1];
			payloadSlice[0] = state;
			System.arraycopy(fullStream, start, payloadSlice, 1, size);

			sendPayloadSlice.accept(payloadSlice);
		}
	}

	@Nullable
	public FriendlyByteBuf acceptPart(@Nullable byte[] payloadSlice) {
		if (payloadSlice == null || payloadSlice.length == 0) {
			reset();
			return null;
		}

		byte state = payloadSlice[0];

		if (state == STATE_FIRST) {
			if (!receivedBuffers.isEmpty()) {
				receivedBuffers.clear();
			}
		}

		int contentSize = payloadSlice.length - 1;
		byte[] content = new byte[contentSize];
		if (contentSize > 0) {
			System.arraycopy(payloadSlice, 1, content, 0, contentSize);
		}
		receivedBuffers.add(content);

		if (state == STATE_LAST) {
			byte[][] buffers = receivedBuffers.toArray(new byte[0][]);
			receivedBuffers.clear();
			return new FriendlyByteBuf(Unpooled.wrappedBuffer(buffers));
		}

		return null;
	}

	public void reset() {
		receivedBuffers.clear();
	}
}
