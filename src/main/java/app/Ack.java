package app;

import akka.io.Tcp;

public class Ack implements Tcp.Event {
	public static final Ack ACK = new Ack();

	private Ack() {
	}
}
