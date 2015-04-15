package test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Inet.SocketOption;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;

public class LoadTest extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	ActorRef tcp;

	int count;

	Cancellable timer;

	Random random = new Random();

	@Override
	public void onReceive(Object message) throws Exception {
		if (message == "start") {
			start();
		} else if (message instanceof Tcp.Connected) {
			tcp = getSender();
			tcp.tell(TcpMessage.register(getSelf()), getSelf());
			getContext().watch(tcp);
			scheduleSend();
		} else if (message == "send") {
			send();
		} else if (message instanceof Tcp.Received) {
			count++;
		} else if (message == "log") {
			log.debug("count: {}", count);
		} else {
			unhandled(message);
		}
	}

	void scheduleSend() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		FiniteDuration interval = Duration.create(1000, MILLISECONDS);
		timer = getContext().system().scheduler()
				.schedule(interval, interval, getSelf(), "send", getContext().dispatcher(), getSelf());
	}

	void start() {
		ActorRef tcpManager = Tcp.get(getContext().system()).getManager();
		InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2000);
		InetSocketAddress localAddress = null;
		List<SocketOption> options = Collections.emptyList();
		FiniteDuration timeout = Duration.create(30, SECONDS);
		boolean pullMode = false;
		Tcp.Command cmd = TcpMessage.connect(remoteAddress, localAddress, options, timeout, pullMode);
		tcpManager.tell(cmd, getSelf());
	}

	void send() {
		byte[] bytes = new byte[200];
		for (int i = 0; i < bytes.length - 1; i++) {
			bytes[i] = (byte) random.nextInt('z' - '0');
		}
		bytes[bytes.length - 1] = '\n';
		ByteString data = ByteString.fromArray(bytes);
		tcp.tell(TcpMessage.write(data), getSelf());
	}
}
