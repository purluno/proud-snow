package app;

import java.net.InetSocketAddress;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.util.ByteString;

public class LineLengthConnection extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	InetSocketAddress remote;

	ActorRef tcp;

	ActorRef lineReader;

	ActorRef lineLengthService;

	public LineLengthConnection(InetSocketAddress remote, ActorRef tcp) {
		this.remote = remote;
		this.tcp = tcp;
		lineReader = getContext().actorOf(Props.create(LineReader.class, "UTF-8"));
		lineLengthService = getContext().actorOf(Props.create(LineLengthService.class));
		getContext().watch(tcp);
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Tcp.Received) {
			ByteString data = ((Tcp.Received) message).data();
			lineReader.tell(data, lineLengthService);
		} else if (message instanceof Tcp.Write) {
			tcp.tell(message, getSelf());
		} else if (message instanceof Tcp.ConnectionClosed) {
			log.info("The connection from {}:{} is closed.", remote.getHostString(), remote.getPort());
			getContext().stop(getSelf());
		} else {
			unhandled(message);
		}
	}
}