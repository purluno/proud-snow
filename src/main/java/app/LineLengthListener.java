package app;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Inet.SocketOption;
import akka.io.Tcp;
import akka.io.TcpMessage;

import com.typesafe.config.Config;

public class LineLengthListener extends UntypedActor {
	ActorRef tcpListener;

	Config config;

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public LineLengthListener(Config config) {
		this.config = config;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message == "start") {
			onStart();
		} else if (message instanceof Tcp.Bound) {
			tcpListener = getSender();
			tcpListener.tell(TcpMessage.resumeAccepting(1), getSelf());
		} else if (message instanceof Tcp.Connected) {
			onConnected((Tcp.Connected) message);
		} else {
			unhandled(message);
		}
	}

	void onStart() {
		String host = config.getString("app.host");
		int port = config.getInt("app.port");
		InetSocketAddress endpoint = new InetSocketAddress(host, port);
		int backlog = 10;
		List<SocketOption> options = Collections.emptyList();
		boolean pullMode = true;
		ActorRef tcpManager = Tcp.get(getContext().system()).getManager();
		Tcp.Command cmd = TcpMessage.bind(getSelf(), endpoint, backlog, options, pullMode);
		tcpManager.tell(cmd, getSelf());
	}

	void onConnected(Tcp.Connected message) {
		ActorRef tcpConnection = getSender();
		InetSocketAddress remote = message.remoteAddress();
		getContext().actorOf(Props.create(LineLengthConnection.class, remote, tcpConnection));
		tcpListener.tell(TcpMessage.resumeAccepting(1), getSelf());
		log.info("A new connection from {}:{}", remote.getHostString(), remote.getPort());
	}

}
