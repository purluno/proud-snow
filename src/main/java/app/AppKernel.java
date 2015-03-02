package app;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;

public class AppKernel implements Bootable {
	ActorSystem actorSystem;

	@Override
	public void startup() {
		System.setProperty("java.net.preferIPv4Stack", "true");
		Config config = ConfigFactory.load();
		actorSystem = ActorSystem.create("default", config);
		ActorRef listener = actorSystem.actorOf(Props.create(Listener.class,
				config));
		listener.tell("start", null);
	}

	@Override
	public void shutdown() {
	}
}
