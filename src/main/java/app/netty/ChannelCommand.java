package app.netty;

import java.nio.ByteBuffer;

public interface ChannelCommand extends ChannelMessage {
	public static final Close CLOSE = new Close();

	public static Write write(ByteBuffer data) {
		return new Write(data);
	}

	public static Close close() {
		return CLOSE;
	}

	public static final class Write implements ChannelCommand {
		private ByteBuffer data;

		private Write(ByteBuffer data) {
			this.data = data;
		}

		public ByteBuffer getData() {
			return data;
		}
	}

	public static final class Close implements ChannelCommand {
		private Close() {
		}
	}
}
