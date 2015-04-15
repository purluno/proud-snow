package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akka.kernel.Bootable;
import akka.routing.RoundRobinPool;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestMain implements Bootable {
	Logger logger = LoggerFactory.getLogger(TestMain.class);

	public static void main(String[] args) {
		akka.kernel.Main.main(new String[] { TestMain.class.getName() });
	}

	ActorSystem system;

	List<Cancellable> timers = new ArrayList<>();

	@Override
	public void startup() {
		System.setProperty("java.net.preferIPv4Stack", "true");
		Config config = ConfigFactory.load();
		system = ActorSystem.create("default", config);
		ActorRef connectionCounter = system.actorOf(Props.create(Counter.class), "connection");
		ActorRef bytesCounter = system.actorOf(Props.create(Counter.class), "bytes");
		initTest(connectionCounter, bytesCounter);
		system.actorOf(Props.create(CounterProc.class, Arrays.asList(connectionCounter, bytesCounter)), "counter");
	}

	void initTest(ActorRef connectionCounter, ActorRef bytesCounter) {
		SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), new Function<Throwable, Directive>() {
			@Override
			public Directive apply(Throwable t) throws Exception {
				return SupervisorStrategy.restart();
			}
		});
		system.actorOf(new RoundRobinPool(2000).withSupervisorStrategy(strategy).props(
				Props.create(LoadTest.class, connectionCounter, bytesCounter)));
	}

	@Override
	public void shutdown() {
		for (Cancellable timer : timers) {
			timer.cancel();
		}
		System.err.println("term: " + system.isTerminated());
		system.shutdown();
	}

}
