package app;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main implements Bootable {
	public static void main(String[] args) {
		akka.kernel.Main.main(new String[] { Main.class.getName() });
	}

	ActorSystem actorSystem;

	@Override
	public void startup() {
		System.setProperty("java.net.preferIPv4Stack", "true");
		Config config = ConfigFactory.load();
		actorSystem = ActorSystem.create("default", config);
		ActorRef listener = actorSystem.actorOf(Props.create(LineLengthListener.class, config));
		listener.tell("start", null);
	}

	@Override
	public void shutdown() {
		actorSystem.shutdown();
	}
}
