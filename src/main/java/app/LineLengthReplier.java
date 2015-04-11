package app;

import java.net.InetSocketAddress;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;

public class LineLengthReplier extends UntypedActor {
	ActorRef lineReader;

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	InetSocketAddress remote;

	ActorRef connection;

	String charset;

	public LineLengthReplier(InetSocketAddress remote, ActorRef connection) {
		this(remote, connection, "UTF-8");
	}

	public LineLengthReplier(InetSocketAddress remote, ActorRef connection, String charset) {
		this.remote = remote;
		this.connection = connection;
		this.charset = charset;
		lineReader = getContext().actorOf(Props.create(LineReader.class, charset));
		getContext().watch(connection);
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Tcp.Received) {
			ByteString data = ((Tcp.Received) message).data();
			lineReader.tell(data, getSelf());
		} else if (message instanceof LineReader.Result) {
			String s = ((LineReader.Result) message).get();
			String reply = String.format("%d\n", s.length());
			Tcp.Command cmd = TcpMessage.write(ByteString.fromString(reply, charset));
			connection.tell(cmd, getSelf());
		} else if (message instanceof Tcp.ConnectionClosed) {
			log.info("The connection from {}:{} is closed.", remote.getHostString(), remote.getPort());
			getContext().stop(getSelf());
		} else {
			unhandled(message);
		}
	}
}
