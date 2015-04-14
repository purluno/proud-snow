package app.netty;

import java.nio.ByteBuffer;

public interface ChannelEvent extends ChannelMessage {
	public static final Connected CONNECTED = new Connected();

	public static final Disconnected DISCONNECTED = new Disconnected();

	public static Connected connected() {
		return CONNECTED;
	}

	public static Disconnected disconnected() {
		return DISCONNECTED;
	}

	public static Received received(ByteBuffer data) {
		return new Received(data);
	}

	public static ExceptionCaught exceptionCaught(Throwable cause) {
		return new ExceptionCaught(cause);
	}

	public static final class Connected implements ChannelEvent {
		private Connected() {
		}
	}

	public static final class Disconnected implements ChannelEvent {
		private Disconnected() {
		}
	}

	public static final class Received implements ChannelEvent {
		private ByteBuffer data;

		private Received(ByteBuffer data) {
			this.data = data;
		}

		public ByteBuffer getData() {
			return data;
		}
	}

	public static final class ExceptionCaught implements ChannelEvent {
		private Throwable cause;

		private ExceptionCaught(Throwable cause) {
			this.cause = cause;
		}

		public Throwable getCause() {
			return cause;
		}
	}
}
