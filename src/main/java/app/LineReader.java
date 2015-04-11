package app;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.ByteString;

public class LineReader extends UntypedActor {
	public static class Result {
		private String s;

		public Result(String s) {
			this.s = s;
		}

		public String get() {
			return s;
		}
	}

	private CharsetDecoder decoder;

	private CharBuffer cb = CharBuffer.allocate(1024);

	private StringBuilder sb = new StringBuilder();

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public LineReader() {
		this("UTF-8");
	}

	public LineReader(String charsetName) {
		decoder = Charset.forName(charsetName).newDecoder();
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof ByteString) {
			ByteBuffer bb = ((ByteString) message).asByteBuffer();
			while (bb.hasRemaining()) {
				CoderResult r = decoder.decode(bb, cb, false);
				if (r.isError()) {
					log.info("Decoding error: {}", r);
				}
				cb.flip();
				while (cb.hasRemaining()) {
					char c = cb.get();
					if (c == '\n') {
						String s = sb.toString();
						sb = new StringBuilder();
						getSender().tell(new Result(s), getContext().parent());
					} else if (c != '\r') {
						sb.append(c);
					}
				}
				cb.compact();
			}
		} else {
			unhandled(message);
		}
	}
}
