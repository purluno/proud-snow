package test;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Counter extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	int count;

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Integer) {
			count += (int) message;
		} else if (message == "get") {
			getSender().tell(count, getSelf());
		} else if (message == "log") {
			log.info("{} = {}", getSelf().path().name(), count);
		} else if (message == "reset") {
			count = 0;
		} else {
			unhandled(message);
		}
	}
}
