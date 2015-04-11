package app;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;
import app.LineReader.Result;

public class LineLengthService extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof LineReader.Result) {
			onLine((LineReader.Result) message);
		} else {
			unhandled(message);
		}
	}

	void onLine(Result result) {
		String line = result.get();
		String reply = String.format("%d\n", line.length());
		Tcp.Command cmd = TcpMessage.write(ByteString.fromString(reply, "UTF-8"));
		getSender().tell(cmd, getSelf());
	}

}
