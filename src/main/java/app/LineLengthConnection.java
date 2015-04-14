package app;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;

public class LineLengthConnection extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	InetSocketAddress remote;

	ActorRef tcp;

	ActorRef lineReader;

	ActorRef lineLengthService;

	public LineLengthConnection(InetSocketAddress remote, ActorRef tcpConnection) {
		this.remote = remote;
		this.tcp = tcpConnection;
		lineReader = getContext().actorOf(Props.create(LineReader.class, "UTF-8"));
		lineLengthService = getContext().actorOf(Props.create(LineLengthService.class));
		getContext().watch(tcp);
		tcp.tell(TcpMessage.register(getSelf()), getSelf());
		tcp.tell(TcpMessage.resumeReading(), getSelf());
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Tcp.Received) {
			ByteString data = ((Tcp.Received) message).data();
			for (ByteBuffer bb : data.getByteBuffers()) {
				if (bb.isDirect()) {
					log.debug("direct");
				}
			}
			lineReader.tell(data, lineLengthService);
			tcp.tell(TcpMessage.resumeReading(), getSelf());
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
