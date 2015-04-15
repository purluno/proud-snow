package test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;
import akka.routing.Broadcast;
import akka.routing.RoundRobinPool;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestMain implements Bootable {
	public static void main(String[] args) {
		akka.kernel.Main.main(new String[] { TestMain.class.getName() });
	}

	ActorSystem system;

	@Override
	public void startup() {
		System.setProperty("java.net.preferIPv4Stack", "true");
		Config config = ConfigFactory.load();
		system = ActorSystem.create("default", config);
		ActorRef testPool = system.actorOf(new RoundRobinPool(1000).props(Props.create(LoadTest.class)));
		testPool.tell(new Broadcast("start"), null);
	}

	@Override
	public void shutdown() {
		System.err.println("term: " + system.isTerminated());
		system.shutdown();
	}

}
