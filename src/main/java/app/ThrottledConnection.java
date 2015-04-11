package app;

import static java.util.concurrent.TimeUnit.SECONDS;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Scheduler;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;

public class ThrottledConnection extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	ActorRef tcp;

	ActorRef handler;

	int maxBytesPerSeconds = 1_000_000;

	int currentBytes = 0;

	Scheduler scheduler = getContext().system().scheduler();

	Cancellable tickTimer;

	public ThrottledConnection(ActorRef tcp) {
		this.tcp = tcp;
		this.handler = getContext().parent();
		tcp.tell(TcpMessage.register(getSelf()), getSelf());
		tcp.tell(TcpMessage.resumeReading(), getSelf());
		getContext().watch(tcp);
		scheduleTick();
	}

	@Override
	public void postStop() throws Exception {
		if (tickTimer != null && !tickTimer.isCancelled()) {
			tickTimer.cancel();
			tickTimer = null;
		}
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Tcp.Received) {
			onTcpReceived((Tcp.Received) message);
		} else if (message instanceof Tcp.Register) {
			handler = ((Tcp.Register) message).handler();
			getContext().watch(handler);
		} else if (message instanceof Tcp.Command) {
			tcp.tell(message, getSelf());
		} else if (message instanceof Tcp.Event) {
			handler.tell(message, getSelf());
		} else if (message == "tick") {
			tick();
		} else if (message instanceof Terminated) {
			getContext().stop(getSelf());
		} else {
			unhandled(message);
		}
	}

	void onTcpReceived(Tcp.Received message) throws Exception {
		handler.tell(message, getSelf());
		currentBytes += message.data().size();
		if (currentBytes < maxBytesPerSeconds) {
			tcp.tell(TcpMessage.resumeReading(), getSelf());
		}
	}

	void tick() {
		currentBytes = Math.max(0, currentBytes - maxBytesPerSeconds);
		if (currentBytes < maxBytesPerSeconds) {
			tcp.tell(TcpMessage.resumeReading(), getSelf());
		}
	}

	void scheduleTick() {
		if (tickTimer != null && !tickTimer.isCancelled()) {
			tickTimer.cancel();
			tickTimer = null;
		}
		FiniteDuration interval = Duration.create(1, SECONDS);
		tickTimer = scheduler.schedule(interval, interval, getSelf(), "tick", getContext().dispatcher(), getSelf());
	}
}
