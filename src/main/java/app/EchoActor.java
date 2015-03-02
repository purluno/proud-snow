package app;

import java.net.InetSocketAddress;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;

public class EchoActor extends UntypedActor {
	InetSocketAddress remote;

	ActorRef connection;

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public EchoActor(InetSocketAddress remote, ActorRef connection) {
		this.remote = remote;
		this.connection = connection;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Tcp.Received) {
			ByteString data = ((Tcp.Received) message).data();
			Tcp.Command cmd = TcpMessage.write(data);
			connection.tell(cmd, getSelf());
		} else if (message instanceof Tcp.ConnectionClosed) {
			log.info("The connection {}:{} is closed.", remote.getHostString(),
					remote.getPort());
			getContext().stop(getSelf());
		} else {
			unhandled(message);
		}
	}
}
