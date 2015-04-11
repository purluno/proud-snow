package app;

import java.net.InetSocketAddress;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;

import com.typesafe.config.Config;

public class Listener extends UntypedActor {
	final ActorRef tcpManager = Tcp.get(getContext().system()).getManager();

	Config config;

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public Listener(Config config) {
		this.config = config;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message == "start") {
			String host = config.getString("app.host");
			int port = config.getInt("app.port");
			InetSocketAddress endpoint = new InetSocketAddress(host, port);
			Tcp.Command cmd = TcpMessage.bind(getSelf(), endpoint, 10);
			tcpManager.tell(cmd, getSelf());
		} else if (message instanceof Tcp.Connected) {
			InetSocketAddress remote = ((Tcp.Connected) message).remoteAddress();
			log.info("A new connection from {}:{}", remote.getHostString(), remote.getPort());
			ActorRef handler = getContext().actorOf(Props.create(LineLengthReplier.class, remote, getSender()));
			Tcp.Command cmd = TcpMessage.register(handler);
			getSender().tell(cmd, getSelf());
		} else {
			unhandled(message);
		}
	}
}
