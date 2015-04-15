package test;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.builder.ToStringBuilder;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class CounterProc extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	List<ActorRef> counters;

	LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();

	Cancellable timer;

	public CounterProc(List<ActorRef> counters) {
		this.counters = counters;
		for (ActorRef counter : counters) {
			counts.put(counter.path().name(), 0);
		}
		FiniteDuration interval = Duration.create(1, SECONDS);
		timer = getContext().system().scheduler()
				.schedule(Duration.Zero(), interval, getSelf(), "tick", getContext().dispatcher(), getSelf());
	}

	@Override
	public void postStop() throws Exception {
		if (timer != null) {
			timer.cancel();
		}
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message == "tick") {
			ToStringBuilder tsb = new ToStringBuilder(null);
			for (Entry<String, Integer> e : counts.entrySet()) {
				tsb.append(e.getKey(), e.getValue());
			}
			log.info(tsb.toString());
			for (ActorRef counter : counters) {
				counter.tell("get", getSelf());
			}
		} else if (message instanceof Integer) {
			String name = getSender().path().name();
			counts.put(name, (int) message);
		} else {
			unhandled(message);
		}
	}
}
